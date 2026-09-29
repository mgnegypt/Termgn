package studio.mgn.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import studio.mgn.data.save.BACKUP_SAVE_ID
import studio.mgn.data.save.CURRENT_SAVE_ID
import studio.mgn.data.save.MgnDatabase
import studio.mgn.data.save.SaveSlot
import studio.mgn.model.DiplomacyState
import studio.mgn.model.GameState
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

private fun world(): Map<String, DiplomacyState> = mapOf(
    "arzan" to DiplomacyState(
        countryId = "arzan",
        nameAr = "أرزان",
        behavior = studio.mgn.model.AiBehavior.TRADER,
        relation = 25.0,
        economicPower = 72.0,
        militaryPower = 48.0,
    ),
)

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GameRepositoryTest {

    private lateinit var db: MgnDatabase
    private lateinit var repo: GameRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, MgnDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = GameRepository(db, worldProvider = ::world)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `no save at first launch`() = runTest {
        assertFalse(repo.hasSave())
        assertEquals(null, repo.peekSummary())
        assertIs<LoadResult.NoSave>(repo.load())
    }

    @Test
    fun `newGame persists and loads back identically`() = runTest {
        val created = repo.newGame(
            GameSetup(countryName = "المجد", rulerTitle = "رئيس"),
        )
        assertTrue(repo.hasSave())

        val summary = repo.peekSummary()!!
        assertEquals("المجد", summary.countryName)
        assertEquals(1, summary.turnNumber)

        val loaded = repo.load()
        assertIs<LoadResult.Ok>(loaded)
        assertEquals(created, loaded.state)
    }

    @Test
    fun `save shifts the previous good row to backup`() = runTest {
        var state = repo.newGame(GameSetup("A", "رئيس"))
        state = state.copy(turnNumber = 5)
        repo.save(state)

        val rows = db.saveDao().getAll()
        assertEquals(2, rows.size)
        assertEquals(1, rows.first { it.id == BACKUP_SAVE_ID }.turnNumber)
        assertEquals(5, rows.first { it.id == CURRENT_SAVE_ID }.turnNumber)
    }

    @Test
    fun `corrupt current falls back to backup`() = runTest {
        var state = repo.newGame(GameSetup("المجد", "رئيس"))
        state = state.copy(turnNumber = 2)
        repo.save(state)
        // Corrupt only the current row; the shifted backup must rescue us.
        db.saveDao().upsert(
            db.saveDao().get(CURRENT_SAVE_ID)!!.copy(payloadJson = "{broken"),
        )

        val loaded = repo.load()
        assertIs<LoadResult.RecoveredFromBackup>(loaded)
        assertEquals("المجد", loaded.state.countryName)
        assertEquals(1, loaded.state.turnNumber)
    }

    @Test
    fun `both rows corrupt yields Corrupted never a crash`() = runTest {
        repo.newGame(GameSetup("المجد", "رئيس"))
        val bad = db.saveDao().get(CURRENT_SAVE_ID)!!.copy(payloadJson = "{broken")
        db.saveDao().upsert(bad)
        db.saveDao().upsert(bad.copy(id = BACKUP_SAVE_ID))

        assertIs<LoadResult.Corrupted>(repo.load())
    }

    @Test
    fun `deleteSave clears everything`() = runTest {        repo.newGame(GameSetup("المجد", "رئيس"))
        repo.deleteSave()
        assertFalse(repo.hasSave())
        assertIs<LoadResult.NoSave>(repo.load())
    }
}

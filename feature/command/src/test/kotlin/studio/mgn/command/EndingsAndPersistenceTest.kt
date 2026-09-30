package studio.mgn.command

import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import studio.mgn.content.ContentLoader
import studio.mgn.data.GameRepository
import studio.mgn.data.GameSetup
import studio.mgn.data.LoadResult
import studio.mgn.data.save.MgnDatabase
import studio.mgn.engine.ConstructionEngine
import studio.mgn.engine.DiplomacyEngine
import studio.mgn.model.AiBehavior
import studio.mgn.model.DiplomacyState
import studio.mgn.model.GameEnding
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun idleMain() {
    Shadows.shadowOf(Looper.getMainLooper()).idle()
}

private fun <T> awaitValue(
    flow: StateFlow<T>,
    timeoutMs: Long = 15000,
    predicate: (T) -> Boolean,
): T {
    val deadline = System.currentTimeMillis() + timeoutMs
    var value = flow.value
    while (!predicate(value)) {
        idleMain()
        Thread.sleep(15)
        check(System.currentTimeMillis() < deadline) { "timed out waiting for state" }
        value = flow.value
    }
    return value
}

private fun world() = mapOf(
    "arzan" to DiplomacyState(
        countryId = "arzan",
        nameAr = "أرزان",
        behavior = AiBehavior.TRADER,
        relation = 60.0,
    ),
    "sahran" to DiplomacyState(
        countryId = "sahran",
        nameAr = "سهران",
        behavior = AiBehavior.HOSTILE,
        relation = -10.0,
    ),
)

private class TestRegistrar : CommandViewModel.SessionRegistrar {
    var latest: studio.mgn.model.GameState? = null
    override fun registerSession(initial: studio.mgn.model.GameState) {
        latest = initial
    }

    override fun session(): CommandViewModel.SessionHandle? = null
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EndingsAndPersistenceTest {

    private lateinit var repo: GameRepository
    private val pack = ContentLoader.load()

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.inMemoryDatabaseBuilder(context, MgnDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = GameRepository(db, worldProvider = ::world)
    }

    @Test
    fun `save and load mid construction`() = runTest {
        var state = repo.newGame(GameSetup("المجد", "رئيس"))
        state = state.copy(treasuryCash = 2000000.0)
        val museum = pack.landmarkById("national_museum")!!
        state = ConstructionEngine.start(state, museum)!!
        // Two turns into the build, then persist mid-way.
        repeat(2) {
            state = ConstructionEngine.advance(state, pack.landmarkCatalog).state
        }
        repo.save(state)

        val loaded = repo.load()
        assertTrue(loaded is LoadResult.Ok)
        val queue = (loaded as LoadResult.Ok).state.underConstruction
        assertEquals(1, queue.size)
        assertEquals("national_museum", queue.single().id)
        assertEquals(museum.buildTurns - 2, queue.single().turnsRemaining)
    }

    @Test
    fun `save and load mid war`() = runTest {
        var state = repo.newGame(GameSetup("المجد", "رئيس"))
        state = DiplomacyEngine(Random(1)).declareWar(state, "sahran")
        // Fight two turns (below the minimum settlement length), then persist.
        val diplo = DiplomacyEngine(Random(1))
        repeat(2) {
            state = diplo.advanceTurn(state).state
        }
        val enemyBefore = state.countries.getValue("sahran")
        assertTrue(enemyBefore.atWar)
        assertEquals(2, enemyBefore.warTurns)
        repo.save(state)

        val loaded = repo.load() as LoadResult.Ok
        val enemyAfter = loaded.state.countries.getValue("sahran")
        assertTrue(enemyAfter.atWar)
        assertEquals(2, enemyAfter.warTurns)
        assertTrue(
            loaded.state.history.any { it.title.startsWith("إعلان حرب") },
        )
    }

    @Test
    fun `collapse ending blocks play and persists`() = runTest {
        var state = repo.newGame(GameSetup("المجد", "رئيس"))
        state = state.copy(legitimacy = 5.0, publicSatisfaction = 10.0, treasuryCash = 0.0)
        assertTrue(state.isCollapsed)
        repo.save(state)

        val vm = CommandViewModel(repo, pack, TestRegistrar(), rng = Random(1))
        val ui = awaitValue(vm.state) { !it.isLoading && !it.missing }
        assertEquals(GameEndingScreen.COLLAPSE, ui.ending)
        assertEquals(GameEnding.COLLAPSE, ui.state!!.ended)

        // Reload: collapse still blocks.
        val vm2 = CommandViewModel(repo, pack, TestRegistrar(), rng = Random(1))
        val ui2 = awaitValue(vm2.state) { !it.isLoading && !it.missing }
        assertEquals(GameEndingScreen.COLLAPSE, ui2.ending)
        vm2.onEvent(CommandEvent.EndTurn)
        idleMain()
        Thread.sleep(200)
        idleMain()
        assertEquals(1, vm2.state.value.state!!.turnNumber)
    }

    @Test
    fun `victory ending shows once and allows continue`() = runTest {
        var state = repo.newGame(GameSetup("المجد", "رئيس"))
        state = state.copy(turnNumber = 30, legitimacy = 80.0)
        for (key in studio.mgn.model.StateKeys.INDICATORS +
            studio.mgn.model.StateKeys.SECTORS
        ) {
            state = state.withKey(key, 95.0)
        }
        assertTrue(state.hasWon)
        repo.save(state)

        val vm = CommandViewModel(repo, pack, TestRegistrar(), rng = Random(1))
        val ui = awaitValue(vm.state) { !it.isLoading && !it.missing }
        assertEquals(GameEndingScreen.VICTORY, ui.ending)
        assertEquals(GameEnding.VICTORY, ui.state!!.ended)

        vm.onEvent(CommandEvent.DismissEnding)
        awaitValue(vm.state) { it.ending == null }
        // Continued play is allowed after victory.
        vm.onEvent(CommandEvent.EndTurn)
        val advanced = awaitValue(vm.state) { it.showReport || it.showDecisions }
        assertTrue(advanced.state!!.turnNumber > 30 || advanced.showDecisions)
    }

    @Test
    fun `continuation ending appears after a decade and allows play`() = runTest {
        var state = repo.newGame(GameSetup("المجد", "رئيس"))
        state = state.copy(
            turnNumber = 121,
            inGameDate = java.time.LocalDate.of(2035, 2, 1),
        )
        assertFalse(state.isCollapsed)
        repo.save(state)

        val vm = CommandViewModel(repo, pack, TestRegistrar(), rng = Random(1))
        val ui = awaitValue(vm.state) { !it.isLoading && !it.missing }
        // Either continuation (decade reached) or victory (great play) is shown.
        assertTrue(
            ui.ending == GameEndingScreen.CONTINUATION ||
                ui.ending == GameEndingScreen.VICTORY,
        )

        // Continuation never blocks play.
        if (ui.ending == GameEndingScreen.CONTINUATION) {
            vm.onEvent(CommandEvent.DismissEnding)
            awaitValue(vm.state) { it.ending == null }
            vm.onEvent(CommandEvent.EndTurn)
            val advanced = awaitValue(vm.state) { it.showReport || it.showDecisions }
            assertTrue(advanced.state!!.turnNumber >= 121)
        }
    }

    @Test
    fun `missions advance on real actions`() = runTest {
        var state = repo.newGame(GameSetup("المجد", "رئيس"))
        // A trade treaty must move the trade-deal mission.
        val diplo = DiplomacyEngine(Random(3))
        val signed = diplo.signTreaty(
            state, "arzan", studio.mgn.model.TreatyType.TRADE,
        )
        assertTrue(signed != null)
        state = signed
        val progress = studio.mgn.engine.MissionEngine.progress(
            state,
            pack.missions.first { it.id == "m_open_doors" },
        )
        assertTrue(progress.current >= 1.0)

        // A finished landmark must complete its mission.
        state = state.copy(treasuryCash = 2000000.0)
        val museum = pack.landmarkById("national_museum")!!
        state = ConstructionEngine.start(state, museum)!!
        repeat(museum.buildTurns) {
            state = ConstructionEngine.advance(state, pack.landmarkCatalog).state
        }
        val (_, unlocked) = studio.mgn.engine.MissionEngine.checkCompletions(
            state, pack.missions,
        )
        assertTrue(unlocked.map { it.id }.contains("m_first_landmark"))
        assertFalse(unlocked.map { it.id }.contains("m_open_doors"))
    }
}

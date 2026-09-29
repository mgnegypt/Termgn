package studio.mgn.diplomacy

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
import studio.mgn.data.save.MgnDatabase
import studio.mgn.model.AiBehavior
import studio.mgn.model.DiplomacyState
import studio.mgn.model.TreatyType
import studio.mgn.model.WarStance
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun idleMain() {
    Shadows.shadowOf(Looper.getMainLooper()).idle()
}

private fun <T> awaitValue(
    flow: StateFlow<T>,
    timeoutMs: Long = 8000,
    predicate: (T) -> Boolean,
): T {
    val deadline = System.currentTimeMillis() + timeoutMs
    var value = flow.value
    while (!predicate(value)) {
        idleMain()
        Thread.sleep(15)
        check(System.currentTimeMillis() < deadline) { "timed out" }
        value = flow.value
    }
    return value
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DiplomacyViewModelTest {

    private lateinit var repo: GameRepository
    private val pack = ContentLoader.load()

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.inMemoryDatabaseBuilder(context, MgnDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = GameRepository(
            db,
            worldProvider = {
                mapOf(
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
            },
        )
    }

    @Test
    fun `signing and breaking treaties round trip`() = runTest {
        repo.newGame(GameSetup("المجد", "رئيس"))
        val vm = DiplomacyViewModel(repo, pack)
        awaitValue(vm.state) { !it.isLoading && it.rows.isNotEmpty() }

        // Trade deal signable at relation 60.
        assertTrue(vm.state.value.rows.first { it.id == "arzan" }
            .signable[TreatyType.TRADE] == true)
        vm.onEvent(DiplomacyEvent.SignTreaty("arzan", TreatyType.TRADE))
        val signed = awaitValue(vm.state) {
            it.rows.first { r -> r.id == "arzan" }.treaties.contains(TreatyType.TRADE)
        }
        assertTrue(signed.rows.first { it.id == "arzan" }.treaties.contains(TreatyType.TRADE))

        vm.onEvent(DiplomacyEvent.BreakTreaty("arzan", TreatyType.TRADE))
        awaitValue(vm.state) {
            !it.rows.first { r -> r.id == "arzan" }.treaties.contains(TreatyType.TRADE)
        }
    }

    @Test
    fun `war declaration and stance persist`() = runTest {
        repo.newGame(GameSetup("المجد", "رئيس"))
        val vm = DiplomacyViewModel(repo, pack)
        awaitValue(vm.state) { !it.isLoading && it.rows.isNotEmpty() }

        vm.onEvent(DiplomacyEvent.DeclareWar("sahran"))
        val war = awaitValue(vm.state) {
            it.rows.first { r -> r.id == "sahran" }.atWar
        }
        assertTrue(war.rows.first { it.id == "sahran" }.atWar)

        vm.onEvent(DiplomacyEvent.SetStance("sahran", WarStance.OFFENSIVE))
        val stance = awaitValue(vm.state) {
            it.rows.first { r -> r.id == "sahran" }.stance == WarStance.OFFENSIVE
        }
        assertEquals(WarStance.OFFENSIVE, stance.rows.first { it.id == "sahran" }.stance)

        // Stances survive a reload (consumed by the next turn).
        val vm2 = DiplomacyViewModel(repo, pack)
        val reloaded = awaitValue(vm2.state) { !it.isLoading && it.rows.isNotEmpty() }
        assertEquals(
            WarStance.OFFENSIVE,
            reloaded.rows.first { it.id == "sahran" }.stance,
        )
        assertTrue(reloaded.rows.first { it.id == "sahran" }.atWar)
    }

    @Test
    fun `embassy without funds is not signable`() = runTest {
        var state = repo.newGame(GameSetup("المجد", "رئيس"))
        state = state.copy(treasuryCash = 10.0)
        repo.save(state)
        val vm = DiplomacyViewModel(repo, pack)
        val ui = awaitValue(vm.state) { !it.isLoading && it.rows.isNotEmpty() }
        assertFalse(ui.rows.first { it.id == "arzan" }.signable[TreatyType.EMBASSY] == true)
    }
}

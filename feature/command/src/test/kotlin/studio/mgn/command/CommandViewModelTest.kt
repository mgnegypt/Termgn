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
import studio.mgn.engine.BalanceConfig
import studio.mgn.engine.ConstructionEngine
import studio.mgn.engine.DiplomacyEngine
import studio.mgn.engine.EconomyEngine
import studio.mgn.engine.EventEngine
import studio.mgn.engine.TurnEngine
import studio.mgn.model.AiBehavior
import studio.mgn.model.DiplomacyState
import studio.mgn.model.GameState
import studio.mgn.model.TreatyType
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
    var initial: GameState? = null
    var latest: GameState? = null

    override fun registerSession(initial: GameState) {
        this.initial = initial
    }

    override fun session(): CommandViewModel.SessionHandle? {
        val current = latest ?: initial ?: return null
        return object : CommandViewModel.SessionHandle {
            override fun update(state: GameState) {
                latest = state
            }
        }.also { latest = current }
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CommandViewModelTest {

    private lateinit var db: MgnDatabase
    private lateinit var repo: GameRepository
    private val pack = ContentLoader.load()

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, MgnDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = GameRepository(db, worldProvider = ::world)
    }

    private fun viewModel(seed: Int = 42): Pair<CommandViewModel, TestRegistrar> {
        val registrar = TestRegistrar()
        val vm = CommandViewModel(repo, pack, registrar, rng = Random(seed))
        return vm to registrar
    }

    @Test
    fun `missing save surfaces immediately`() {
        val (vm, _) = viewModel()
        val ui = awaitValue(vm.state) { !it.isLoading }
        assertTrue(ui.missing)
    }

    @Test
    fun `end turn advances the game and autosaves`() = runTest {
        repo.newGame(GameSetup("المجد", "رئيس"))
        val (vm, _) = viewModel()
        awaitValue(vm.state) { !it.isLoading && !it.missing }

        vm.onEvent(CommandEvent.EndTurn)
        val reported = awaitValue(vm.state) { it.showReport }
        assertEquals(2, reported.state!!.turnNumber)
        assertFalse(reported.processing)

        // Auto-save persisted turn 2 (plus any resolved decisions first).
        val loaded = repo.load()
        assertTrue(loaded is LoadResult.Ok)

        vm.onEvent(CommandEvent.DismissReport)
        awaitValue(vm.state) { !it.showReport }
    }

    @Test
    fun `double end turn is ignored while processing`() = runTest {
        repo.newGame(GameSetup("المجد", "رئيس"))
        val (vm, _) = viewModel()
        awaitValue(vm.state) { !it.isLoading && !it.missing }

        vm.onEvent(CommandEvent.EndTurn)
        vm.onEvent(CommandEvent.EndTurn)
        val reported = awaitValue(vm.state) { it.showReport }
        assertEquals(2, reported.state!!.turnNumber)
    }

    /** Advances turns (dismissing reports, never resolving) until decisions pend. */
    private suspend fun accumulatePending(vm: CommandViewModel) {
        var guard = 0
        while (vm.state.value.pending.isEmpty() && guard < 40) {
            vm.onEvent(CommandEvent.EndTurn)
            awaitValue(vm.state) { !it.processing }
            if (vm.state.value.showReport) {
                vm.onEvent(CommandEvent.DismissReport)
                awaitValue(vm.state) { !it.showReport }
            }
            guard++
        }
        assertTrue(vm.state.value.pending.isNotEmpty(), "no decisions surfaced")
    }

    @Test
    fun `blocked end turn routes to decisions`() = runTest {
        repo.newGame(GameSetup("المجد", "رئيس"))
        val (vm, _) = viewModel()
        awaitValue(vm.state) { !it.isLoading && !it.missing }

        accumulatePending(vm)
        vm.onEvent(CommandEvent.EndTurn)
        assertTrue(vm.state.value.showDecisions)
    }

    @Test
    fun `choosing a decision applies effects and reroll works`() = runTest {
        repo.newGame(GameSetup("المجد", "رئيس"))
        val (vm, _) = viewModel(seed = 7)
        awaitValue(vm.state) { !it.isLoading && !it.missing }

        accumulatePending(vm)
        val event = vm.state.value.pending.first()
        val choiceIndex = vm.state.value.choiceStates.indexOfFirst { it.selectable }
        assertTrue(choiceIndex >= 0, "no selectable choice")
        vm.onEvent(CommandEvent.Choose(event.id, choiceIndex))
        awaitValue(vm.state) {
            it.pending.none { e -> e.id == event.id }
        }
    }

    @Test
    fun `construction starts and rushes through the viewmodel`() = runTest {
        var state = repo.newGame(GameSetup("المجد", "رئيس"))
        state = state.copy(treasuryCash = 2000000.0, gems = 100)
        repo.save(state)
        val (vm, _) = viewModel()
        awaitValue(vm.state) { !it.isLoading && !it.missing }

        assertTrue(vm.state.value.buildable.contains("national_museum"))
        vm.onEvent(CommandEvent.StartConstruction("national_museum"))
        awaitValue(vm.state) {
            it.state!!.underConstruction.any { b -> b.id == "national_museum" }
        }
        vm.onEvent(CommandEvent.RushConstruction("national_museum"))
        awaitValue(vm.state) {
            val q = it.state!!.underConstruction.firstOrNull { b ->
                b.id == "national_museum"
            }
            q == null || q.turnsRemaining < 6
        }
    }

    @Test
    fun `twenty consecutive turns play without crashing`() = runTest {
        val rng = Random(42)
        val eventEngine = EventEngine(pool = pack.events, rng = rng)
        val diplomacy = DiplomacyEngine(rng)
        val turnEngine = TurnEngine(
            eventEngine = eventEngine,
            achievements = pack.achievements,
            diplomacyEngine = diplomacy,
            landmarkCatalog = pack.landmarkCatalog,
            missions = pack.missions,
            rng = rng,
        )
        var state = GameState.newGame(
            countryName = "المجد",
            rulerTitle = "رئيس",
            countries = pack.initialWorld(),
        )
        var decisionsResolved = 0
        var missionsSeen = 0

        repeat(20) {
            val report = turnEngine.advance(state)
            state = report.state
            missionsSeen += report.newMissions.size

            for (event in report.pendingEvents) {
                val options = event.choices.filter {
                    eventEngine.isSelectable(it, state)
                }
                if (options.isEmpty()) continue
                state = eventEngine.applyChoice(state, event, options.first()).state
                decisionsResolved++
            }

            // Competent stewardship: repay, invest, befriend, build.
            if (state.debt > 0 && state.treasuryCash > 220000) {
                state = EconomyEngine.repayDebt(state, state.treasuryCash - 220000)
            }
            val weakest = studio.mgn.model.StateKeys.INVESTABLE_SECTORS.minByOrNull {
                state.readKey(it)
            }!!
            EconomyEngine.investInSector(state, weakest, 3.0)?.let { state = it }
            val friend = state.countries.values.firstOrNull {
                diplomacy.canSign(state, it.countryId, TreatyType.TRADE)
            }
            if (friend != null) {
                diplomacy.signTreaty(state, friend.countryId, TreatyType.TRADE)
                    ?.let { state = it }
            }
            val cheapest = pack.landmarks
                .filter { ConstructionEngine.canStart(state, it) }
                .minByOrNull { it.costCash }
            if (cheapest != null && state.underConstruction.isEmpty() && state.debt <= 0) {
                ConstructionEngine.start(state, cheapest)?.let { state = it }
            }

            for (key in studio.mgn.model.StateKeys.INDICATORS +
                studio.mgn.model.StateKeys.SECTORS
            ) {
                assertTrue(state.readKey(key) in 0.0..100.0, "$key out of range")
            }
            assertTrue(state.treasuryCash >= 0)
            repo.save(state)
        }

        assertEquals(21, state.turnNumber)
        assertTrue(decisionsResolved > 0, "expected some decisions in 20 turns")
        assertTrue(state.history.isNotEmpty())
        val loaded = repo.load()
        assertTrue(loaded is LoadResult.Ok)
        assertEquals(21, (loaded as LoadResult.Ok).state.turnNumber)
    }
}

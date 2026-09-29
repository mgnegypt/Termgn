package studio.mgn.engine

import studio.mgn.content.ContentLoader
import studio.mgn.model.GameState
import studio.mgn.model.StateKeys
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

private fun newGame() = GameState.newGame(
    countryName = "دولة الاختبار",
    rulerTitle = "رئيس",
    countries = ContentLoader.load().initialWorld(),
)

private fun engineFor(rng: Random): TurnEngine {
    val pack = ContentLoader.load()
    return TurnEngine(
        eventEngine = EventEngine(pool = pack.events, rng = rng),
        achievements = pack.achievements,
        landmarkCatalog = pack.landmarkCatalog,
        rng = rng,
    )
}

/**
 * Balance invariants. Deliberately loose: they guard the shape of the curve
 * (solvency, recoverability, progression, no snowball), not exact numbers.
 */
class BalanceInvariantsTest {

    @Test
    fun `the default policy runs a surplus at game start`() {
        val pack = ContentLoader.load()
        val finance = EconomyEngine.computeFinance(newGame(), pack.landmarkCatalog)
        assertTrue(finance.net > 0, "a new nation must not start insolvent")
        assertTrue(
            finance.totalExpenses / finance.totalIncome < 0.85,
            "expenses must leave room for decisions and investment",
        )
    }

    @Test
    fun `a passive nation stays solvent and stable for 60 turns`() {
        val rng = Random(21)
        var state = newGame()
        val engine = engineFor(rng)

        repeat(60) { state = engine.advance(state).state }

        assertTrue(state.debt == 0.0, "doing nothing must not create debt")
        assertTrue(state.economy > 35, "the economy must not collapse on its own")
        assertTrue(
            state.publicSatisfaction > 25,
            "satisfaction must not collapse on its own",
        )
        assertTrue(
            state.foodSecurity > 25,
            "food security must not collapse on its own",
        )
    }

    @Test
    fun `a collapsed nation can recover sectors have a subsistence floor`() {
        val rng = Random(33)
        var state = newGame().copy(
            agriculture = 0.0,
            industry = 0.0,
            health = 0.0,
            education = 0.0,
            energy = 0.0,
            technology = 0.0,
        )
        val engine = engineFor(rng)

        repeat(40) { state = engine.advance(state).state }

        assertTrue(
            state.agriculture > 10,
            "a zeroed sector must recover toward the subsistence floor",
        )
        assertTrue(state.health > 10)
    }

    @Test
    fun `a competent policy improves the nation over 120 turns`() {
        val rng = Random(42)
        var state = newGame()
        val engine = engineFor(rng)
        val startScore = state.nationScore
        val reserve = 220000.0
        val pack = ContentLoader.load()

        repeat(120) {
            val report = engine.advance(state)
            state = report.state
            var budget = maxOf(0.0, state.treasuryCash - reserve)

            for (event in report.pendingEvents) {
                val options = event.choices.filter { engine.events.isSelectable(it, state) }
                if (options.isEmpty()) continue
                val affordable = options.filter {
                    engine.events.effectiveCostCash(it) <= budget
                }
                val picked = if (affordable.isNotEmpty()) affordable.first() else options.first()
                budget = maxOf(0.0, budget - engine.events.effectiveCostCash(picked))
                state = engine.events.applyChoice(state, event, picked).state
            }

            if (state.debt > 0 && state.treasuryCash > reserve) {
                state = EconomyEngine.repayDebt(state, state.treasuryCash - reserve)
            }
            if (budget > 60000) {
                val weakest = StateKeys.INVESTABLE_SECTORS.reduce { a, b ->
                    if (state.readKey(a) <= state.readKey(b)) a else b
                }
                state = EconomyEngine.investInSector(state, weakest, 3.0) ?: state
            }
        }

        assertTrue(
            state.nationScore > startScore + 15,
            "good play must visibly improve the nation",
        )
        assertTrue(state.nationScore < 99, "the game must not be trivially maxed out")
        assertTrue(state.debt < 1000000, "competent play must not end in a debt spiral")
    }

    @Test
    fun `landmarks remain affordable to a saving player`() {
        val rng = Random(9)
        var state = newGame()
        val engine = engineFor(rng)
        val pack = ContentLoader.load()

        repeat(12) { state = engine.advance(state).state }
        val cheapest = pack.landmarks.minBy { it.costCash }
        assertTrue(
            ConstructionEngine.canStart(state, cheapest),
            "a year of saving should fund the cheapest landmark",
        )
    }

    @Test
    fun `gems stay scarce no economic source pays them out`() {
        val rng = Random(4)
        var state = newGame()
        val engine = engineFor(rng)
        repeat(36) { state = engine.advance(state).state }
        assertTrue(state.gems < 120, "hard currency must not be farmable by idling")
    }
}

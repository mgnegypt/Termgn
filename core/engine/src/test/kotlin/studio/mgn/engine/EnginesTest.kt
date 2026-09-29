package studio.mgn.engine

import studio.mgn.content.ContentLoader
import studio.mgn.model.GameState
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun newGame(): GameState {
    val pack = ContentLoader.load()
    return GameState.newGame(
        countryName = "دولة الاختبار",
        rulerTitle = "رئيس",
        countries = pack.initialWorld(),
    )
}

class EconomyEngineTest {

    @Test
    fun `income and expenses are both non-trivial at game start`() {
        val pack = ContentLoader.load()
        val finance = EconomyEngine.computeFinance(newGame(), pack.landmarkCatalog)
        assertTrue(finance.totalIncome > 0)
        assertTrue(finance.totalExpenses > 0)
        assertTrue(finance.incomeBreakdown.values.all { it >= 0 })
    }

    @Test
    fun `raising taxes raises income but costs satisfaction`() {
        val pack = ContentLoader.load()
        val low = newGame().copy(taxRate = 0.15)
        val high = newGame().copy(taxRate = 0.45)
        assertTrue(
            EconomyEngine.computeFinance(high, pack.landmarkCatalog).taxIncome >
                EconomyEngine.computeFinance(low, pack.landmarkCatalog).taxIncome,
        )

        val rng = Random(5)
        val lowAfter = EconomyEngine.applySociety(
            low,
            EconomyEngine.computeFinance(low, pack.landmarkCatalog),
            rng,
        )
        val highAfter = EconomyEngine.applySociety(
            high,
            EconomyEngine.computeFinance(high, pack.landmarkCatalog),
            rng,
        )
        assertTrue(highAfter.publicSatisfaction < lowAfter.publicSatisfaction)
    }

    @Test
    fun `a shortfall becomes debt instead of a negative treasury`() {
        val pack = ContentLoader.load()
        val state = newGame().copy(
            treasuryCash = 0.0,
            taxRate = 0.0,
            militarySpendingLevel = 1.0,
            subsidyLevel = 1.0,
        )
        val finance = EconomyEngine.computeFinance(state, pack.landmarkCatalog)
        val after = EconomyEngine.applyFinance(state, finance)
        assertEquals(0.0, after.treasuryCash)
        assertTrue(after.debt > 0)
    }

    @Test
    fun `gems are never granted by economic activity`() {
        val pack = ContentLoader.load()
        var state = newGame()
        val before = state.gems
        state = EconomyEngine.applyFinance(
            state,
            EconomyEngine.computeFinance(state, pack.landmarkCatalog),
        )
        state = EconomyEngine.applySectorDrift(state, pack.landmarkCatalog)
        state = EconomyEngine.applyPopulation(state)
        assertEquals(before, state.gems)
    }

    @Test
    fun `loans are capped and repayable`() {
        var state = newGame()
        state = EconomyEngine.takeLoan(state, 9999999.0)
        assertTrue(state.debt <= 400000.0 + 1e-6)
        val debtBefore = state.debt
        state = EconomyEngine.repayDebt(state, debtBefore + 1000.0)
        assertEquals(0.0, state.debt)
    }
}

class ConstructionEngineTest {

    @Test
    fun `a landmark completes after its build time and applies its effects`() {
        val pack = ContentLoader.load()
        var state = newGame().copy(treasuryCash = 2000000.0)
        val museum = pack.landmarkById("national_museum")!!
        val cultureBefore = state.culture

        val started = ConstructionEngine.start(state, museum)
        assertNotNull(started)
        state = started
        assertEquals(1, state.underConstruction.size)

        repeat(museum.buildTurns) {
            state = ConstructionEngine.advance(state, pack.landmarkCatalog).state
        }
        assertTrue(state.underConstruction.isEmpty())
        assertTrue(state.builtLandmarks.map { it.id }.contains(museum.id))
        assertTrue(state.culture > cultureBefore)
    }

    @Test
    fun `requirements and funds are enforced`() {
        val pack = ContentLoader.load()
        val poor = newGame().copy(treasuryCash = 1000.0)
        assertNull(
            ConstructionEngine.start(poor, pack.landmarkById("national_museum")!!),
        )

        val rich = newGame().copy(treasuryCash = 5000000.0, technology = 10.0)
        assertNull(
            ConstructionEngine.start(rich, pack.landmarkById("data_center")!!),
            "technology requirement should block this build",
        )
    }

    @Test
    fun `gems can rush a build but never below zero turns`() {
        val pack = ContentLoader.load()
        var state = newGame().copy(treasuryCash = 2000000.0, gems = 100)
        val museum = pack.landmarkById("national_museum")!!
        state = ConstructionEngine.start(state, museum)!!
        state = ConstructionEngine.rushWithGems(state, museum.id, 100)!!
        assertEquals(0, state.underConstruction.single().turnsRemaining)
        assertTrue(state.gems < 100)
    }

    @Test
    fun `unknown landmark ids are skipped gracefully`() {
        val pack = ContentLoader.load()
        val state = newGame()
        val outcome = ConstructionEngine.advance(state, pack.landmarkCatalog)
        assertTrue(outcome.completed.isEmpty())
    }
}

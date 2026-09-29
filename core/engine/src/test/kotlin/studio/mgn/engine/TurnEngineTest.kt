package studio.mgn.engine

import studio.mgn.content.ContentLoader
import studio.mgn.model.GameState
import studio.mgn.model.Season
import studio.mgn.model.StateKeys
import studio.mgn.model.TurnLength
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private fun newGame() = GameState.newGame(
    countryName = "دولة الاختبار",
    rulerTitle = "رئيس",
    countries = ContentLoader.load().initialWorld(),
)

private fun engine(rng: Random): TurnEngine {
    val pack = ContentLoader.load()
    return TurnEngine(
        eventEngine = EventEngine(pool = pack.events, rng = rng),
        achievements = pack.achievements,
        landmarkCatalog = pack.landmarkCatalog,
        rng = rng,
    )
}

class TurnEngineTest {

    @Test
    fun `200 turns stay inside every bound`() {
        val rng = Random(7)
        var state = newGame()
        val eng = engine(rng)

        repeat(200) {
            val report = eng.advance(state)
            state = report.state

            for (event in report.pendingEvents) {
                val options = event.choices.filter { eng.events.isSelectable(it, state) }
                if (options.isEmpty()) continue
                state = eng.events.applyChoice(state, event, options[rng.nextInt(options.size)]).state
            }

            for (key in StateKeys.INDICATORS + StateKeys.SECTORS) {
                val value = state.readKey(key)
                assertTrue(value.isFinite(), "$key is not finite")
                assertTrue(value in 0.0..100.0, "$key out of range: $value")
            }
            assertTrue(state.cleanEnergyRatio in 0.0..1.0)
            assertTrue(state.taxRate in 0.0..1.0)
            assertTrue(state.treasuryCash.isFinite())
            assertTrue(state.treasuryCash >= 0)
            assertTrue(state.debt >= 0)
            assertTrue(state.gems >= 0)
            assertTrue(state.population > 0)
            assertTrue(state.axisEconomic in -100.0..100.0)
        }

        assertEquals(201, state.turnNumber)
        assertTrue(state.inGameDate.year > 2025)
        assertTrue(state.history.isNotEmpty())
    }

    @Test
    fun `the calendar advances by turn length and tracks seasons`() {
        var monthly = newGame()
        val eng = engine(Random(1))
        repeat(12) { monthly = eng.advance(monthly).state }
        assertEquals(2026, monthly.inGameDate.year)
        assertEquals(Season.WINTER, monthly.currentSeason)
        assertEquals(13, monthly.turnNumber)
    }

    @Test
    fun `a yearly report fires once per in-game year and pays gems`() {
        var state = newGame()
        val eng = engine(Random(3))
        val gemsBefore = state.gems
        var reports = 0
        repeat(24) {
            val report = eng.advance(state)
            state = report.state
            if (report.yearlyReport != null) reports++
        }
        assertEquals(2, reports)
        assertTrue(state.gems > gemsBefore)
    }

    @Test
    fun `events respect their cooldown`() {
        val rng = Random(11)
        var state = newGame()
        val eng = engine(rng)
        val firings = mutableMapOf<String, MutableList<Int>>()

        repeat(120) {
            val report = eng.advance(state)
            state = report.state
            for (event in report.pendingEvents) {
                val options = event.choices.filter { eng.events.isSelectable(it, state) }
                if (options.isEmpty()) continue
                state = eng.events.applyChoice(state, event, options.first()).state
                firings.getOrPut(event.id) { mutableListOf() }.add(state.turnNumber)
            }
        }

        firings.forEach { (id, turns) ->
            for (i in 1 until turns.size) {
                assertTrue(turns[i] - turns[i - 1] >= 6, "$id repeated too soon")
            }
        }
        assertTrue(firings.keys.size > 10, "event variety is too low")
    }

    @Test
    fun `fresh game is ongoing no victory no collapse`() {
        val s = newGame()
        assertFalse(s.isCollapsed)
        assertFalse(s.hasWon)
        assertEquals("مستمرة", s.statusLabel)
    }

    @Test
    fun `collapse requires legitimacy satisfaction and treasury failure`() {
        val s = newGame().copy(
            legitimacy = 5.0,
            publicSatisfaction = 10.0,
            treasuryCash = 0.0,
        )
        assertTrue(s.isCollapsed)
        assertEquals("انهيار الدولة", s.statusLabel)
    }

    @Test
    fun `victory needs score legitimacy and time`() {
        var s = newGame().copy(turnNumber = 30, legitimacy = 80.0)
        for (key in StateKeys.INDICATORS + StateKeys.SECTORS) {
            s = s.withKey(key, 95.0)
        }
        assertTrue(s.nationScore > 85)
        assertTrue(s.hasWon)
    }

    @Test
    fun `state serializes and restores`() {
        val rng = Random(13)
        var state = newGame().copy(treasuryCash = 2000000.0)
        val eng = engine(rng)
        val pack = studio.mgn.content.ContentLoader.load()
        val museum = pack.landmarkById("national_museum")!!
        state = ConstructionEngine.start(state, museum)!!
        repeat(30) { state = eng.advance(state).state }

        val restored = Json.decodeFromString(
            GameState.serializer(),
            Json.encodeToString(GameState.serializer(), state),
        )

        assertEquals(state.turnNumber, restored.turnNumber)
        assertEquals(state.inGameDate, restored.inGameDate)
        assertTrue(abs(restored.treasuryCash - state.treasuryCash) < 0.001)
        assertEquals(state.population, restored.population)
        assertEquals(state.countries.size, restored.countries.size)
        assertEquals(state.builtLandmarks.size, restored.builtLandmarks.size)
        assertEquals(state.unlockedAchievements, restored.unlockedAchievements)
        assertEquals(state.lastEventTurns, restored.lastEventTurns)
        assertTrue(abs(restored.economy - state.economy) < 0.001)
    }

    @Test
    fun `day and week turn lengths advance the calendar`() {
        val eng = engine(Random(2))
        var daily = newGame().copy(turnLength = TurnLength.DAY)
        daily = eng.advance(daily).state
        assertEquals(2, daily.inGameDate.dayOfMonth)
        var weekly = newGame().copy(turnLength = TurnLength.WEEK)
        weekly = eng.advance(weekly).state
        assertEquals(8, weekly.inGameDate.dayOfMonth)
    }
}

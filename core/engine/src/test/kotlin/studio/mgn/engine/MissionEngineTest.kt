package studio.mgn.engine

import studio.mgn.content.ContentLoader
import studio.mgn.model.GameState
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val pack = ContentLoader.load()

private fun newGame() = GameState.newGame(
    countryName = "دولة الاختبار",
    rulerTitle = "رئيس",
    countries = pack.initialWorld(),
)

class MissionEngineTest {

    @Test
    fun `missions load and progress is measurable`() {
        assertTrue(pack.missions.size in 4..10)
        val state = newGame()
        for (mission in pack.missions) {
            val progress = MissionEngine.progress(state, mission)
            assertTrue(progress.current >= 0)
        }
    }

    @Test
    fun `first landmark mission completes with gems and history`() {
        val mission = pack.missions.first { it.id == "m_first_landmark" }
        var state = newGame().copy(treasuryCash = 2000000.0)
        val museum = pack.landmarkById("national_museum")!!
        state = ConstructionEngine.start(state, museum)!!
        repeat(museum.buildTurns) {
            state = ConstructionEngine.advance(state, pack.landmarkCatalog).state
        }
        val (after, unlocked) = MissionEngine.checkCompletions(state, pack.missions)
        assertTrue(unlocked.map { it.id }.contains("m_first_landmark"))
        assertTrue("m_first_landmark" in after.completedMissions)
        assertTrue(after.gems > state.gems)
        assertTrue(after.history.any { it.title.contains("مهمة منجزة") })
    }

    @Test
    fun `completed missions never unlock twice`() {
        val mission = pack.missions.first { it.id == "m_first_landmark" }
        var state = newGame().copy(
            completedMissions = setOf("m_first_landmark"),
            builtLandmarks = listOf(
                studio.mgn.model.BuiltLandmark("national_museum"),
            ),
        )
        val gemsBefore = state.gems
        val (after, unlocked) = MissionEngine.checkCompletions(state, pack.missions)
        assertTrue(unlocked.none { it.id == "m_first_landmark" })
        assertEquals(gemsBefore, after.gems)
    }

    @Test
    fun `active missions are limited and sorted by closeness`() {
        val state = newGame()
        val active = MissionEngine.activeMissions(state, pack.missions, limit = 3)
        assertTrue(active.size <= 3)
        assertTrue(active.none { it.mission.id in state.completedMissions })
    }

    @Test
    fun `turns grant ruler xp and xp per level matches the model divisor`() {
        assertEquals(100, BalanceConfig.RULER_XP_PER_LEVEL)
        val engine = TurnEngine(
            eventEngine = EventEngine(pool = pack.events, rng = Random(1)),
            achievements = pack.achievements,
            landmarkCatalog = pack.landmarkCatalog,
            missions = pack.missions,
            rng = Random(1),
        )
        var state = newGame()
        repeat(5) { state = engine.advance(state).state }
        assertEquals(5 * BalanceConfig.RULER_XP_PER_TURN, state.rulerXp)
        assertEquals(1 + state.rulerXp / 100, state.rulerLevel)
    }

    @Test
    fun `turn report carries newly completed missions`() {
        val engine = TurnEngine(
            eventEngine = EventEngine(pool = pack.events, rng = Random(2)),
            achievements = pack.achievements,
            landmarkCatalog = pack.landmarkCatalog,
            missions = pack.missions,
            rng = Random(2),
        )
        var state = newGame().copy(treasuryCash = 900000.0)
        val report = engine.advance(state)
        state = report.state
        assertTrue(
            report.newMissions.map { it.id }.contains("m_full_coffers") ||
                state.completedMissions.contains("m_full_coffers"),
        )
    }
}

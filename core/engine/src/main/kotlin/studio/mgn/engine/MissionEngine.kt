package studio.mgn.engine

import studio.mgn.model.GameState
import studio.mgn.model.HistoryEntry
import studio.mgn.model.HistoryTag
import studio.mgn.model.MissionDef

/** Progress snapshot for one mission: current value toward [MissionDef.goalValue]. */
data class MissionProgress(
    val mission: MissionDef,
    val current: Double,
    val completed: Boolean,
)

/**
 * Player-facing missions: measurable goals with gem rewards.
 * Pure; completion is checked once per turn by [TurnEngine].
 */
object MissionEngine {
    fun progress(state: GameState, mission: MissionDef): MissionProgress {
        val current = ConditionEvaluator.resolveKey(mission.goalKey, state)
        return MissionProgress(
            mission = mission,
            current = current,
            completed = mission.id in state.completedMissions ||
                current >= mission.goalValue,
        )
    }

    fun activeMissions(
        state: GameState,
        missions: List<MissionDef>,
        limit: Int = 3,
    ): List<MissionProgress> = missions
        .filter { it.id !in state.completedMissions }
        .map { progress(state, it) }
        .sortedByDescending { it.current / it.mission.goalValue.coerceAtLeast(1e-9) }
        .take(limit)

    internal fun checkCompletions(
        state: GameState,
        missions: List<MissionDef>,
    ): Pair<GameState, List<MissionDef>> {
        var next = state
        val unlocked = mutableListOf<MissionDef>()
        for (mission in missions) {
            if (mission.id in next.completedMissions) continue
            if (ConditionEvaluator.resolveKey(mission.goalKey, next) < mission.goalValue) {
                continue
            }
            next = next.copy(
                completedMissions = next.completedMissions + mission.id,
                gems = next.gems + mission.gemReward,
            )
            unlocked.add(mission)
            next = next.plusHistory(
                HistoryEntry(
                    turnNumber = next.turnNumber,
                    date = next.inGameDate,
                    title = "مهمة منجزة: ${mission.titleAr}",
                    detail = mission.descriptionAr,
                    tag = HistoryTag.ACHIEVEMENT,
                ),
            )
        }
        return next to unlocked
    }
}

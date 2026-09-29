package studio.mgn.engine

import kotlin.math.roundToLong
import studio.mgn.model.BuiltLandmark
import studio.mgn.model.GameState
import studio.mgn.model.HistoryEntry
import studio.mgn.model.HistoryTag
import studio.mgn.model.LandmarkDef
import studio.mgn.model.OngoingBuild

data class ConstructionOutcome(
    val state: GameState,
    val completed: List<LandmarkDef>,
)

/**
 * Landmark construction: eligibility, start, per-turn progress, completion.
 * Pure functions; catalogue definitions are resolved via [catalog].
 */
object ConstructionEngine {
    fun meetsRequirements(
        state: GameState,
        landmark: LandmarkDef,
    ): Boolean {
        if (state.builtLandmarks.any { it.id == landmark.id }) return false
        if (state.underConstruction.any { it.id == landmark.id }) return false
        if (landmark.era.ordinal > state.era.ordinal) return false
        for ((key, required) in landmark.requirements) {
            if (state.readKey(key) < required) return false
        }
        return true
    }

    fun canStart(state: GameState, landmark: LandmarkDef): Boolean =
        meetsRequirements(state, landmark) &&
            state.treasuryCash >= landmark.costCash

    fun start(state: GameState, landmark: LandmarkDef): GameState? {
        if (!canStart(state, landmark)) return null
        var next = state.copy(
            treasuryCash = state.treasuryCash - landmark.costCash,
            underConstruction = state.underConstruction +
                OngoingBuild(id = landmark.id, turnsRemaining = landmark.buildTurns),
        )
        next = next.plusHistory(
            HistoryEntry(
                turnNumber = next.turnNumber,
                date = next.inGameDate,
                title = "بدء بناء: ${landmark.nameAr}",
                detail = "التكلفة: ${landmark.costCash.roundToLong()} — المدة: " +
                    "${landmark.buildTurns} دور",
                tag = HistoryTag.CONSTRUCTION,
            ),
        )
        return next.clamped()
    }

    fun advance(
        state: GameState,
        catalog: Map<String, LandmarkDef>,
    ): ConstructionOutcome {
        var next = state
        val completed = mutableListOf<LandmarkDef>()
        val queue = next.underConstruction.toMutableList()

        for (i in queue.indices.reversed()) {
            val build = queue[i]
            val remaining = build.turnsRemaining - 1
            if (remaining > 0) {
                queue[i] = build.copy(turnsRemaining = remaining)
                continue
            }
            queue.removeAt(i)
            val def = catalog[build.id]
            next = next.copy(underConstruction = queue.toList())
            if (def != null) {
                next = next.copy(
                    builtLandmarks = next.builtLandmarks + BuiltLandmark(id = def.id),
                )
                var acc = next
                def.onCompleteEffects.forEach { (key, delta) ->
                    acc = acc.plusDelta(key, delta)
                }
                next = acc
                completed.add(def)
                next = next.plusHistory(
                    HistoryEntry(
                        turnNumber = next.turnNumber,
                        date = next.inGameDate,
                        title = "اكتمل بناء: ${def.nameAr}",
                        detail = def.descriptionAr,
                        tag = HistoryTag.CONSTRUCTION,
                        deltas = def.onCompleteEffects,
                    ),
                )
            }
        }

        return ConstructionOutcome(
            state = next.copy(underConstruction = queue.toList()).clamped(),
            completed = completed,
        )
    }

    fun rushWithGems(
        state: GameState,
        landmarkId: String,
        turns: Int,
    ): GameState? {
        val index = state.underConstruction.indexOfFirst { it.id == landmarkId }
        if (index < 0 || turns <= 0) return null

        val build = state.underConstruction[index]
        val skipped = minOf(turns, build.turnsRemaining)
        val cost = skipped * BalanceConfig.GEMS_PER_CONSTRUCTION_TURN_SKIP
        if (state.gems < cost) return null

        val queue = state.underConstruction.toMutableList()
        queue[index] = build.copy(turnsRemaining = build.turnsRemaining - skipped)
        return state.copy(
            gems = state.gems - cost,
            underConstruction = queue.toList(),
        )
    }
}

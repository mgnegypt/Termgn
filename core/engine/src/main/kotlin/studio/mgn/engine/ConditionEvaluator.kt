package studio.mgn.engine

import studio.mgn.model.Condition
import studio.mgn.model.CmpOp
import studio.mgn.model.GameState
import studio.mgn.model.StateKeys

/**
 * Evaluates structured [Condition]s against a [GameState].
 * Pure: no I/O, no randomness.
 */
object ConditionEvaluator {
    fun evaluate(condition: Condition?, state: GameState): Boolean {
        if (condition == null) return true
        return when (condition) {
            is Condition.All -> condition.conditions.all { evaluate(it, state) }
            is Condition.Any -> condition.conditions.any { evaluate(it, state) }
            is Condition.Not -> !evaluate(condition.condition, state)
            is Condition.HistoryContains ->
                state.history.any { it.title.contains(condition.text) }
            is Condition.Cmp ->
                condition.op.test(resolveKey(condition.key, state), condition.value)
        }
    }

    fun resolveKey(key: String, state: GameState): Double = when (key) {
        "workersSatisfaction" -> state.workersSatisfaction
        "middleClassSatisfaction" -> state.middleClassSatisfaction
        "eliteSatisfaction" -> state.eliteSatisfaction
        "legitimacy" -> state.legitimacy
        "builtLandmarksCount" -> state.builtLandmarks.size.toDouble()
        "turnNumber" -> state.turnNumber.toDouble()
        "inGameYear" -> state.inGameDate.year.toDouble()
        "nationScore" -> state.nationScore
        "tradeDealCount" ->
            state.countries.values.count { it.hasTradeDeal }.toDouble()
        "allyCount" ->
            state.countries.values.count { it.isAlly }.toDouble()
        "minRelation" ->
            state.countries.values.minOfOrNull { it.relation } ?: 0.0
        "isAtWar" -> if (state.isAtWar) 1.0 else 0.0
        else -> state.readKey(key)
    }

    /** All condition keys every content file may reference. */
    fun isKnownKey(key: String): Boolean = key in StateKeys.KNOWN_CONDITION_KEYS

    fun isKnownOp(op: String): Boolean = try {
        CmpOp.fromSymbol(op)
        true
    } catch (_: IllegalArgumentException) {
        false
    }
}

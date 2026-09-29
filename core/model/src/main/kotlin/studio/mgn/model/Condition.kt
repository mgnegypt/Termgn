package studio.mgn.model

/**
 * Structured appearance condition, replacing the legacy Dart closures so
 * content can live in JSON. Evaluated by ConditionEvaluator in :core:engine.
 */
sealed interface Condition {
    data class Cmp(val key: String, val op: CmpOp, val value: Double) : Condition
    data class All(val conditions: List<Condition>) : Condition
    data class Any(val conditions: List<Condition>) : Condition
    data class Not(val condition: Condition) : Condition
    data class HistoryContains(val text: String) : Condition
}

enum class CmpOp {
    LT, LTE, GT, GTE, EQ, NEQ;

    fun test(actual: Double, expected: Double): Boolean = when (this) {
        LT -> actual < expected
        LTE -> actual <= expected
        GT -> actual > expected
        GTE -> actual >= expected
        EQ -> actual == expected
        NEQ -> actual != expected
    }

    companion object {
        fun fromSymbol(symbol: String): CmpOp = when (symbol) {
            "<" -> LT
            "<=" -> LTE
            ">" -> GT
            ">=" -> GTE
            "==" -> EQ
            "!=" -> NEQ
            else -> throw IllegalArgumentException("Unknown comparison op: $symbol")
        }
    }
}

package studio.mgn.model

/** One option the player may pick when an event fires. Immutable. */
data class EventChoice(
    val label: String,
    val resultText: String,
    val stateEffects: Map<String, Double> = emptyMap(),
    val costCash: Double = 0.0,
    val costGems: Int = 0,
    val relationEffects: Map<String, Double> = emptyMap(),
    val allRelationsDelta: Double = 0.0,
)

/** An authored event definition. Only firing history is persisted. */
data class GameEvent(
    val id: String,
    val title: String,
    val description: String,
    val category: EventCategory,
    val choices: List<EventChoice>,
    val condition: Condition? = null,
    val weight: Int = 10,
    val cooldownTurns: Int? = null,
    val oncePerGame: Boolean = false,
    val minTurn: Int = 0,
    /** Reserved for future thumbnail art; empty in PLAN 1. */
    val imageKey: String = "",
) {
    val isCrisis: Boolean get() = category == EventCategory.CRISIS
}

/** An unlockable achievement; only unlocked ids are persisted. */
data class Achievement(
    val id: String,
    val titleAr: String,
    val descriptionAr: String,
    val condition: Condition,
    val gemReward: Int = 0,
    val hidden: Boolean = false,
)

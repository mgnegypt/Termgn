package studio.mgn.engine

import studio.mgn.model.GameEvent
import studio.mgn.model.EventChoice
import studio.mgn.model.GameState
import studio.mgn.model.HistoryEntry
import studio.mgn.model.HistoryTag
import kotlin.math.max
import kotlin.random.Random

/** Result of resolving one decision. */
data class EventResolution(
    val event: GameEvent,
    val choice: EventChoice,
    /** The effects actually applied, after clamping. */
    val appliedEffects: Map<String, Double>,
)

data class ChoiceResult(
    val state: GameState,
    val resolution: EventResolution,
)

/**
 * Selects which events fire each turn and applies the player's choices.
 * The engine owns no content: the pool is injected. Pure apart from [rng].
 */
class EventEngine(
    private val pool: List<GameEvent>,
    private val rng: Random = Random.Default,
) {
    fun drawForTurn(state: GameState): List<GameEvent> {
        if (state.turnNumber < BalanceConfig.FIRST_EVENT_TURN) {
            return emptyList()
        }

        val eligible = pool.filter { isEligible(it, state) }
        if (eligible.isEmpty()) return emptyList()

        val drawn = mutableListOf<GameEvent>()

        val crises = eligible.filter { it.isCrisis }
        if (crises.isNotEmpty() && rng.nextDouble() < BalanceConfig.CRISIS_PRIORITY_CHANCE) {
            weightedPick(crises)?.let { drawn.add(it) }
        }

        if (drawn.isEmpty()) {
            weightedPick(eligible)?.let { drawn.add(it) }
        }

        val wantsSecond = drawn.size < BalanceConfig.MAX_EVENTS_PER_TURN &&
            rng.nextDouble() < BalanceConfig.SECOND_EVENT_CHANCE
        if (wantsSecond) {
            val rest = eligible.filter { it !in drawn && !it.isCrisis }
            weightedPick(rest)?.let { drawn.add(it) }
        }

        return drawn
    }

    fun isEligible(event: GameEvent, state: GameState): Boolean {
        if (event.oncePerGame && event.id in state.firedOnceEvents) {
            return false
        }
        val lastTurn = state.lastEventTurns[event.id]
        if (lastTurn != null) {
            val cooldown = event.cooldownTurns
                ?: if (event.isCrisis) {
                    BalanceConfig.CRISIS_COOLDOWN
                } else {
                    BalanceConfig.DEFAULT_EVENT_COOLDOWN
                }
            if (state.turnNumber - lastTurn < cooldown) return false
        }
        if (!ConditionEvaluator.evaluate(event.condition, state)) return false
        if (state.turnNumber < event.minTurn) return false
        return event.choices.any { isSelectable(it, state) }
    }

    private fun weightedPick(candidates: List<GameEvent>): GameEvent? {
        if (candidates.isEmpty()) return null
        val total = candidates.sumOf { max(1, it.weight) }
        var roll = rng.nextInt(total)
        for (candidate in candidates) {
            roll -= max(1, candidate.weight)
            if (roll < 0) return candidate
        }
        return candidates.last()
    }

    fun effectiveCostCash(choice: EventChoice): Double =
        choice.costCash * BalanceConfig.DECISION_COST_MULTIPLIER

    fun canAfford(choice: EventChoice, state: GameState): Boolean =
        state.treasuryCash >= effectiveCostCash(choice) && state.gems >= choice.costGems

    fun isSelectable(choice: EventChoice, state: GameState): Boolean =
        canAfford(choice, state)

    fun applyChoice(
        state: GameState,
        event: GameEvent,
        choice: EventChoice,
    ): ChoiceResult {
        var next = state.copy(
            treasuryCash = state.treasuryCash - effectiveCostCash(choice),
            gems = state.gems - choice.costGems,
        )

        val before = choice.stateEffects.keys.associateWith { next.readKey(it) }
        next = next.applyEffects(choice.stateEffects)

        val applied = before.entries
            .mapNotNull { (key, old) ->
                val new = next.readKey(key)
                if (new != old) key to (new - old) else null
            }
            .toMap()

        var countries = next.countries
        choice.relationEffects.forEach { (countryId, delta) ->
            countries[countryId]?.let {
                countries = countries + (countryId to it.copy(relation = it.relation + delta))
            }
        }
        if (choice.allRelationsDelta != 0.0) {
            countries = countries.mapValues { (_, d) ->
                d.copy(relation = d.relation + choice.allRelationsDelta)
            }
        }
        next = next.copy(countries = countries)

        next = next.copy(
            lastEventTurns = next.lastEventTurns + (event.id to next.turnNumber),
            firedOnceEvents = if (event.oncePerGame) {
                next.firedOnceEvents + event.id
            } else {
                next.firedOnceEvents
            },
        )

        next = next.plusHistory(
            HistoryEntry(
                turnNumber = next.turnNumber,
                date = next.inGameDate,
                title = event.title,
                detail = "${choice.label} — ${choice.resultText}",
                tag = if (event.isCrisis) HistoryTag.CRISIS else HistoryTag.DECISION,
                deltas = applied,
            ),
        )

        next = next.clamped()
        next = EconomyEngine.settleNegativeTreasury(next)
        return ChoiceResult(
            state = next,
            resolution = EventResolution(event, choice, applied),
        )
    }

    fun reroll(state: GameState, current: GameEvent): GameEvent? {
        val candidates = pool.filter { it.id != current.id && isEligible(it, state) }
        return weightedPick(candidates)
    }
}

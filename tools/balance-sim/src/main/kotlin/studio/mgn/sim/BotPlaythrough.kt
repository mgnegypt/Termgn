package studio.mgn.sim

import studio.mgn.content.ContentLoader
import studio.mgn.engine.ConstructionEngine
import studio.mgn.engine.DiplomacyEngine
import studio.mgn.engine.EconomyEngine
import studio.mgn.engine.EventEngine
import studio.mgn.engine.TurnEngine
import studio.mgn.model.GameState
import studio.mgn.model.StateKeys
import studio.mgn.model.TreatyType
import studio.mgn.model.WarStance
import kotlin.math.max
import kotlin.random.Random

/** Bot personalities for the automated playthrough. */
enum class BotStyle { CAUTIOUS, BALANCED, AGGRESSIVE }

data class BotResult(
    val style: BotStyle,
    val seed: Int,
    val turns: Int,
    val finalScore: Double,
    val maxDebt: Double,
    val finalDebt: Double,
    val warsFought: Int,
    val ended: String?,
    val collapsed: Boolean,
    val won: Boolean,
    val decisions: Int,
    val legitimacy: Double = 0.0,
    val satisfaction: Double = 0.0,
    val treasury: Double = 0.0,
)

/**
 * Automated playthrough: 5 seeds x 120 turns x 3 behaviors, plus a sabotage
 * scenario that must reach collapse and a victory hunt. Fails loudly on any
 * violation so CI stays honest. Run with:
 *   ./gradlew :tools:balance-sim:balanceBot
 */
fun main() {
    val pack = ContentLoader.load()
    val seeds = listOf(7, 21, 42, 99, 1234)
    val results = mutableListOf<BotResult>()

    for (seed in seeds) {
        for (style in BotStyle.entries) {
            results.add(play(seed, style, pack))
        }
    }

    println("style      | seed | score | maxDebt  | debt   | wars | ended      | decisions")
    for (r in results) {
        println(
            "${r.style.name.padEnd(10)} | ${r.seed.toString().padStart(4)} | " +
                "${"%.1f".format(r.finalScore).padStart(5)} | " +
                "${r.maxDebt.toLong().toString().padStart(8)} | " +
                "${r.finalDebt.toLong().toString().padStart(6)} | " +
                "${r.warsFought.toString().padStart(4)} | " +
                "${(r.ended ?: "-").padEnd(10)} | ${r.decisions}",
        )
    }

    val failures = mutableListOf<String>()
    for (r in results) {
        if (r.turns != 120) failures.add("$r did not finish 120 turns")
        if (r.style != BotStyle.AGGRESSIVE && r.maxDebt > 1000000) {
            failures.add("$r debt out of control")
        }
    }

    // Sabotage must collapse: total war, max taxes, worst choices.
    val sabotage = playSabotage(9, pack)
    println(
        "SABOTAGE   |    9 | ${"%.1f".format(sabotage.finalScore).padStart(5)} | " +
            "collapsed=${sabotage.collapsed} ended=${sabotage.ended} " +
            "leg=${"%.1f".format(sabotage.legitimacy)} " +
            "sat=${"%.1f".format(sabotage.satisfaction)} " +
            "cash=${sabotage.treasury.toLong()}",
    )
    if (!sabotage.collapsed) failures.add("sabotage scenario never collapsed")

    // Victory must be reachable by competent play on at least one seed.
    val victories = results.count { it.won }
    println("victories across bots: $victories/${results.size}")
    if (victories == 0) failures.add("no victory reached by any bot")

    if (failures.isNotEmpty()) {
        println("BOT FAILURES:")
        failures.forEach { println(" - $it") }
        throw IllegalStateException("bot playthrough failed: ${failures.size} violations")
    }
    println("BOT PLAYTHROUGH OK: ${results.size} runs + sabotage")
}

private typealias Pack = studio.mgn.content.ContentPack

private fun play(seed: Int, style: BotStyle, pack: Pack): BotResult {
    val rng = Random(seed * 31 + style.ordinal)
    var state = GameState.newGame(
        countryName = "MGN",
        rulerTitle = "رئيس",
        countries = pack.initialWorld(),
    )
    val events = EventEngine(pool = pack.events, rng = rng)
    val engine = TurnEngine(
        eventEngine = events,
        achievements = pack.achievements,
        diplomacyEngine = DiplomacyEngine(rng),
        landmarkCatalog = pack.landmarkCatalog,
        missions = pack.missions,
        rng = rng,
    )
    val reserve = when (style) {
        BotStyle.CAUTIOUS -> 350000.0
        BotStyle.BALANCED -> 220000.0
        BotStyle.AGGRESSIVE -> 100000.0
    }
    var maxDebt = 0.0
    var warsFought = 0
    var decisions = 0

    repeat(120) {
        // Aggressive bot picks fights once it feels strong.
        if (style == BotStyle.AGGRESSIVE && !state.isAtWar && state.militarySecurity > 55) {
            val foe = state.countries.values.firstOrNull { d ->
                !d.atWar && d.behavior == studio.mgn.model.AiBehavior.HOSTILE
            }
            if (foe != null) {
                state = DiplomacyEngine(rng).declareWar(state, foe.countryId)
                warsFought++
            }
        }
        val stances = state.warEnemies.associate { enemy ->
            enemy.countryId to when (style) {
                BotStyle.CAUTIOUS -> WarStance.NEGOTIATE
                BotStyle.AGGRESSIVE -> WarStance.OFFENSIVE
                BotStyle.BALANCED -> WarStance.DEFENSIVE
            }
        }

        val report = engine.advance(state, stances)
        state = report.state
        maxDebt = max(maxDebt, state.debt)

        var budget = max(0.0, state.treasuryCash - reserve)
        for (event in report.pendingEvents) {
            val options = event.choices.filter { engine.events.isSelectable(it, state) }
            if (options.isEmpty()) continue
            val affordable = options.filter {
                engine.events.effectiveCostCash(it) <= budget
            }
            val picked = when (style) {
                BotStyle.CAUTIOUS -> (affordable.ifEmpty { options })
                    .minBy { engine.events.effectiveCostCash(it) }
                else -> (affordable.ifEmpty { options })
                    .maxBy { score(it) }
            }
            budget = max(0.0, budget - engine.events.effectiveCostCash(picked))
            state = events.applyChoice(state, event, picked).state
            decisions++
        }

        if (state.debt > 0 && state.treasuryCash > reserve) {
            state = EconomyEngine.repayDebt(state, state.treasuryCash - reserve)
            budget = max(0.0, state.treasuryCash - reserve)
        }

        if (style != BotStyle.AGGRESSIVE) {
            for (country in state.countries.values) {
                if (country.hasTradeDeal || country.atWar) continue
                DiplomacyEngine(rng).signTreaty(state, country.countryId, TreatyType.TRADE)
                    ?.let { state = it }
            }
        }

        val buildable = pack.landmarks
            .filter { ConstructionEngine.canStart(state, it) }
            .sortedBy { it.costCash }
        if (buildable.isNotEmpty() && state.underConstruction.isEmpty() &&
            state.debt <= 0 && budget > buildable.first().costCash
        ) {
            state = ConstructionEngine.start(state, buildable.first()) ?: state
            budget = max(0.0, state.treasuryCash - reserve)
        }

        val investThreshold = if (style == BotStyle.CAUTIOUS) 100000.0 else 60000.0
        if (budget > investThreshold) {
            val pool = if (style == BotStyle.AGGRESSIVE) {
                listOf(StateKeys.INDUSTRY, StateKeys.TECHNOLOGY, StateKeys.ENERGY)
            } else {
                StateKeys.INVESTABLE_SECTORS
            }
            val weakest = pool.minBy { state.readKey(it) }
            EconomyEngine.investInSector(state, weakest, 3.0)?.let { state = it }
        }
    }

    return BotResult(
        style = style,
        seed = seed,
        turns = 120,
        finalScore = state.nationScore,
        maxDebt = maxDebt,
        finalDebt = state.debt,
        warsFought = warsFought + state.warEnemies.size,
        ended = state.ended,
        collapsed = state.isCollapsed,
        won = state.hasWon,
        decisions = decisions,
    )
}

private fun playSabotage(seed: Int, pack: Pack): BotResult {
    val rng = Random(seed)
    var state = GameState.newGame(
        countryName = "MGN",
        rulerTitle = "رئيس",
        countries = pack.initialWorld(),
    ).copy(taxRate = 1.0, militarySpendingLevel = 1.0, subsidyLevel = 0.0)
    // Total war against everyone, from turn one.
    for (country in state.countries.values) {
        state = DiplomacyEngine(rng).declareWar(state, country.countryId)
    }
    val events = EventEngine(pool = pack.events, rng = rng)
    val engine = TurnEngine(
        eventEngine = events,
        achievements = pack.achievements,
        diplomacyEngine = DiplomacyEngine(rng),
        landmarkCatalog = pack.landmarkCatalog,
        missions = pack.missions,
        rng = rng,
    )
    var stances = state.warEnemies.associate {
        it.countryId to WarStance.OFFENSIVE
    }
    var collapsedAt: Int? = null
    var snapshot = state
    repeat(120) { turn ->
        // Perpetual war: re-declare on anyone at peace, every turn.
        for (country in state.countries.values) {
            if (!country.atWar) {
                state = DiplomacyEngine(rng).declareWar(state, country.countryId)
            }
        }
        val report = engine.advance(state, stances)
        state = report.state
        // Always pick the worst available choice.
        for (event in report.pendingEvents) {
            val options = event.choices.filter {
                engine.events.isSelectable(it, state)
            }
            if (options.isEmpty()) continue
            val worst = options.minBy { score(it) }
            state = events.applyChoice(state, event, worst).state
        }
        stances = state.warEnemies.associate {
            it.countryId to WarStance.OFFENSIVE
        }
        if (state.isCollapsed && collapsedAt == null) {
            collapsedAt = turn + 1
            snapshot = state
        }
        if (state.isCollapsed) return@repeat
    }
    val end = if (collapsedAt != null) snapshot else state
    return BotResult(
        style = BotStyle.CAUTIOUS,
        seed = seed,
        turns = 120,
        finalScore = end.nationScore,
        maxDebt = end.debt,
        finalDebt = end.debt,
        warsFought = 0,
        ended = end.ended,
        collapsed = collapsedAt != null,
        won = end.hasWon,
        decisions = 0,
        legitimacy = end.legitimacy,
        satisfaction = end.publicSatisfaction,
        treasury = end.treasuryCash,
    )
}

// score() is shared from BalanceSim.kt in the same package.

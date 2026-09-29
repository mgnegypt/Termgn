package studio.mgn.sim

import studio.mgn.content.ContentLoader
import studio.mgn.engine.ConstructionEngine
import studio.mgn.engine.DiplomacyEngine
import studio.mgn.engine.EconomyEngine
import studio.mgn.engine.EventEngine
import studio.mgn.engine.TurnEngine
import studio.mgn.model.EventChoice
import studio.mgn.model.GameState
import studio.mgn.model.StateKeys
import kotlin.math.max
import kotlin.math.roundToLong
import kotlin.random.Random

/**
 * Balance harness. Run with:
 *   ./gradlew :tools:balance-sim:run --args="120 42"
 *
 * Simulates a "reasonable player": always picks the highest-value affordable
 * choice, invests spare cash in the weakest sector, and builds landmarks when
 * it can afford them. Prints a table used for the balancing pass.
 */
fun main(args: Array<String>) {
    val turns = args.getOrNull(0)?.toInt() ?: 120
    val seed = args.getOrNull(1)?.toInt() ?: 42
    val rng = Random(seed)

    val pack = ContentLoader.load()
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
        rng = rng,
    )

    var spentDecisions = 0.0
    var spentInvestment = 0.0
    var spentLandmarks = 0.0
    var earnedIncome = 0.0
    var paidExpenses = 0.0
    var decisionCount = 0

    println(
        "turn | year | cash      | debt     | econ | satis | agri | food | tech " +
            "| env | culture | score | landmarks",
    )

    for (i in 1..turns) {
        val report = engine.advance(state)
        state = report.state
        earnedIncome += report.finance.totalIncome
        paidExpenses += report.finance.totalExpenses

        val reserve = 220000.0
        var budget = max(0.0, state.treasuryCash - reserve)

        for (event in report.pendingEvents) {
            val options = event.choices.filter { engine.events.isSelectable(it, state) }
            if (options.isEmpty()) continue

            val affordable = options
                .filter { engine.events.effectiveCostCash(it) <= budget }
                .sortedByDescending { score(it) }
            val picked = if (affordable.isNotEmpty()) {
                affordable.first()
            } else {
                options.minBy { engine.events.effectiveCostCash(it) }
            }

            spentDecisions += engine.events.effectiveCostCash(picked)
            decisionCount++
            budget = max(0.0, budget - engine.events.effectiveCostCash(picked))
            state = events.applyChoice(state, event, picked).state
        }

        if (state.debt > 0 && state.treasuryCash > reserve) {
            state = EconomyEngine.repayDebt(state, state.treasuryCash - reserve)
            budget = max(0.0, state.treasuryCash - reserve)
        }

        val buildable = pack.landmarks
            .filter { ConstructionEngine.canStart(state, it) }
            .sortedBy { it.costCash }
        if (buildable.isNotEmpty() &&
            state.underConstruction.isEmpty() &&
            state.debt <= 0 &&
            budget > buildable.first().costCash
        ) {
            spentLandmarks += buildable.first().costCash
            state = ConstructionEngine.start(state, buildable.first())!!
            budget = max(0.0, state.treasuryCash - reserve)
        }

        if (budget > 60000) {
            val weakest = StateKeys.INVESTABLE_SECTORS.reduce { a, b ->
                if (state.readKey(a) <= state.readKey(b)) a else b
            }
            val cost = EconomyEngine.sectorInvestmentCost(state, weakest, 3.0)
            val invested = EconomyEngine.investInSector(state, weakest, 3.0)
            if (invested != null) {
                state = invested
                spentInvestment += cost
            }
        }

        if (i % 6 == 0 || i == 1) {
            println(
                "${i.toString().padStart(4)} | " +
                    "${state.inGameDate.year} | " +
                    "${state.treasuryCash.roundToLong().toString().padStart(9)} | " +
                    "${state.debt.roundToLong().toString().padStart(8)} | " +
                    "${state.economy.roundToLong().toString().padStart(4)} | " +
                    "${state.publicSatisfaction.roundToLong().toString().padStart(5)} | " +
                    "${state.agriculture.roundToLong().toString().padStart(4)} | " +
                    "${state.foodSecurity.roundToLong().toString().padStart(4)} | " +
                    "${state.technology.roundToLong().toString().padStart(4)} | " +
                    "${state.environment.roundToLong().toString().padStart(3)} | " +
                    "${state.culture.roundToLong().toString().padStart(7)} | " +
                    "${"%.1f".format(state.nationScore).padStart(5)} | " +
                    "${state.builtLandmarks.size}",
            )
        }
    }

    println()
    println("── cash flow over $turns turns ──")
    println(
        "income total   : ${earnedIncome.roundToLong()} " +
            "(avg ${(earnedIncome / turns).roundToLong()}/turn)",
    )
    println(
        "expenses total : ${paidExpenses.roundToLong()} " +
            "(avg ${(paidExpenses / turns).roundToLong()}/turn)",
    )
    println(
        "surplus        : ${(earnedIncome - paidExpenses).roundToLong()} " +
            "(avg ${((earnedIncome - paidExpenses) / turns).roundToLong()}/turn)",
    )
    println(
        "decisions      : ${spentDecisions.roundToLong()} over $decisionCount " +
            "choices (avg ${if (decisionCount == 0) 0 else (spentDecisions / decisionCount).roundToLong()}/choice, " +
            "${(spentDecisions / turns).roundToLong()}/turn)",
    )
    println(
        "investment     : ${spentInvestment.roundToLong()} " +
            "(avg ${(spentInvestment / turns).roundToLong()}/turn)",
    )
    println("landmarks      : ${spentLandmarks.roundToLong()}")

    println()
    println("── summary after $turns turns ──")
    println("date          : ${state.inGameDate}")
    println("gems          : ${state.gems}")
    println("population    : ${state.population}")
    println("nation score  : ${"%.1f".format(state.nationScore)}")
    println("landmarks     : ${state.builtLandmarks.size}/${pack.landmarks.size}")
    println("achievements  : ${state.unlockedAchievements.size}/${pack.achievements.size}")
    println("unlocked      : ${state.unlockedAchievements.joinToString(", ")}")
    println("wars active   : ${state.warEnemies.size}")
    println("sanctions     : ${state.sanctionCount}")
    println("history size  : ${state.history.size}")
    println("events fired  : ${state.lastEventTurns.size}/${pack.events.size}")
    val locked = pack.achievements
        .filter { it.id !in state.unlockedAchievements }
        .map { it.id }
    println("still locked  : ${locked.joinToString(", ")}")
}

/** Crude utility score so the harness plays "reasonably" rather than randomly. */
fun score(choice: EventChoice): Double {
    var value = 0.0
    choice.stateEffects.forEach { (key, delta) ->
        value += when (key) {
            StateKeys.TREASURY_CASH -> delta / 30000
            StateKeys.DEBT -> -delta / 8000
            StateKeys.POPULATION -> delta / 30000
            StateKeys.CLEAN_ENERGY_RATIO -> delta * 40
            StateKeys.PUBLIC_SATISFACTION -> delta * 1.6
            StateKeys.ECONOMY -> delta * 1.6
            StateKeys.FOOD_SECURITY -> delta * 1.3
            else -> delta
        }
    }
    value -= choice.costCash / 30000
    value -= choice.costGems * 2
    value += choice.allRelationsDelta
    return value
}

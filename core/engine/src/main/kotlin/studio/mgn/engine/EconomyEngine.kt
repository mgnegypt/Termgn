package studio.mgn.engine

import studio.mgn.model.DiplomacyState
import studio.mgn.model.GameState
import studio.mgn.model.LandmarkDef
import studio.mgn.model.Season
import studio.mgn.model.StateKeys
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

/** A full per-turn financial breakdown. */
data class TurnFinance(
    val taxIncome: Double,
    val exportIncome: Double,
    val tourismIncome: Double,
    val customsIncome: Double,
    val digitalIncome: Double,
    val payroll: Double,
    val maintenance: Double,
    val militarySpending: Double,
    val subsidies: Double,
    val debtInterest: Double,
    val warCost: Double,
    /** Cash auto-borrowed because the treasury would have gone negative. */
    val emergencyBorrowed: Double,
) {
    val totalIncome: Double
        get() = taxIncome + exportIncome + tourismIncome + customsIncome + digitalIncome

    val totalExpenses: Double
        get() = payroll + maintenance + militarySpending + subsidies +
            debtInterest + warCost

    val net: Double get() = totalIncome - totalExpenses

    val incomeBreakdown: Map<String, Double> get() = mapOf(
        "ضرائب" to taxIncome,
        "تصدير" to exportIncome,
        "سياحة" to tourismIncome,
        "جمارك" to customsIncome,
        "اقتصاد رقمي" to digitalIncome,
    )

    val expenseBreakdown: Map<String, Double> get() = mapOf(
        "رواتب" to payroll,
        "صيانة" to maintenance,
        "إنفاق عسكري" to militarySpending,
        "دعم السلع" to subsidies,
        "فوائد الدين" to debtInterest,
        "تكاليف الحرب" to warCost,
    )
}

/**
 * Pure economic simulation: money, sectors, society and population.
 * Every function takes a state and returns a new state; nothing is mutated.
 */
object EconomyEngine {
    // ── Money ──

    fun computeFinance(
        state: GameState,
        landmarks: Map<String, LandmarkDef>,
    ): TurnFinance {
        val tradeDeals = state.countries.values.count { it.hasTradeDeal && !it.atWar }
        val sanctions = state.sanctionCount
        val wars = state.warEnemies.size

        val economyMultiplier = BalanceConfig.TAX_ECONOMY_FLOOR +
            (state.economy / 100) * BalanceConfig.TAX_ECONOMY_SLOPE

        val taxIncome = state.population *
            BalanceConfig.TAX_PER_CAPITA *
            state.taxRate *
            economyMultiplier

        val rawExports =
            (state.agriculture * BalanceConfig.AGRICULTURE_EXPORT_PER_POINT) +
                (state.industry * BalanceConfig.INDUSTRY_EXPORT_PER_POINT) +
                (state.energy * BalanceConfig.ENERGY_EXPORT_PER_POINT)
        val exportModifier = max(
            0.0,
            1 + (tradeDeals * BalanceConfig.EXPORT_BONUS_PER_TRADE_DEAL) -
                (sanctions * BalanceConfig.EXPORT_PENALTY_PER_SANCTION),
        )
        val exportIncome = rawExports * exportModifier

        val landmarkTourism = state.builtLandmarks.sumOf {
            landmarks[it.id]?.tourismIncomePerTurn ?: 0.0
        }
        val securityFactor = BalanceConfig.TOURISM_SECURITY_FLOOR +
            (state.militarySecurity / 100) * BalanceConfig.TOURISM_SECURITY_SLOPE
        val tourismIncome =
            ((state.tourism * BalanceConfig.TOURISM_INCOME_PER_POINT) +
                landmarkTourism) *
                securityFactor *
                (if (wars > 0) BalanceConfig.TOURISM_WAR_MULTIPLIER else 1.0)

        val customsIncome =
            tradeDeals * BalanceConfig.CUSTOMS_PER_TRADE_DEAL * economyMultiplier

        val digitalIncome =
            state.technology * BalanceConfig.DIGITAL_ECONOMY_PER_TECH_POINT

        val payroll = BalanceConfig.BASE_GOVERNMENT_PAYROLL +
            (state.population * BalanceConfig.PAYROLL_PER_CAPITA)

        val landmarkUpkeep = state.builtLandmarks.sumOf {
            landmarks[it.id]?.maintenancePerTurn ?: 0.0
        }
        val maintenance = landmarkUpkeep +
            (state.economy * BalanceConfig.INSTITUTIONAL_COST_PER_ECONOMY_POINT) +
            (state.industry * BalanceConfig.INDUSTRY_UPKEEP_PER_POINT) +
            (state.health * BalanceConfig.HEALTH_UPKEEP_PER_POINT) +
            (state.education * BalanceConfig.EDUCATION_UPKEEP_PER_POINT)

        val militarySpending =
            state.militarySpendingLevel * BalanceConfig.MILITARY_BUDGET_MAX
        val subsidies = state.subsidyLevel * BalanceConfig.SUBSIDY_BUDGET_MAX
        val debtInterest = state.debt * BalanceConfig.DEBT_INTEREST_RATE
        val warCost = wars * BalanceConfig.WAR_COST_PER_TURN

        val income = taxIncome + exportIncome + tourismIncome + customsIncome + digitalIncome
        val expenses = payroll + maintenance + militarySpending + subsidies +
            debtInterest + warCost

        val projected = state.treasuryCash + income - expenses
        val emergencyBorrowed = if (projected < 0) -projected else 0.0

        return TurnFinance(
            taxIncome = taxIncome,
            exportIncome = exportIncome,
            tourismIncome = tourismIncome,
            customsIncome = customsIncome,
            digitalIncome = digitalIncome,
            payroll = payroll,
            maintenance = maintenance,
            militarySpending = militarySpending,
            subsidies = subsidies,
            debtInterest = debtInterest,
            warCost = warCost,
            emergencyBorrowed = emergencyBorrowed,
        )
    }

    fun applyFinance(state: GameState, finance: TurnFinance): GameState {
        var next = state.copy(treasuryCash = state.treasuryCash + finance.net)
        if (finance.emergencyBorrowed > 0) {
            next = next.copy(
                debt = next.debt +
                    finance.emergencyBorrowed * BalanceConfig.EMERGENCY_LOAN_MARKUP,
                treasuryCash = 0.0,
            )
        }
        return next.clamped()
    }

    fun settleNegativeTreasury(state: GameState): GameState {
        if (state.treasuryCash >= 0) return state
        return state.copy(
            debt = state.debt + -state.treasuryCash * BalanceConfig.EMERGENCY_LOAN_MARKUP,
            treasuryCash = 0.0,
        ).clamped()
    }

    fun isDebtStressed(state: GameState, finance: TurnFinance): Boolean {
        if (finance.totalIncome <= 0) return state.debt > 0
        return state.debt / finance.totalIncome > BalanceConfig.DEBT_STRESS_THRESHOLD
    }

    fun takeLoan(state: GameState, amount: Double): GameState {
        val granted = amount.coerceIn(0.0, BalanceConfig.MAX_LOAN_PER_REQUEST)
        return state.copy(
            treasuryCash = state.treasuryCash + granted,
            debt = state.debt + granted,
            countries = state.countries.mapValues { (_, d) ->
                d.copy(relation = d.relation - BalanceConfig.LOAN_RELATION_PENALTY)
            },
        ).clamped()
    }

    fun repayDebt(state: GameState, amount: Double): GameState {
        val paid = amount.coerceIn(0.0, min(state.treasuryCash, state.debt))
        return state.copy(
            treasuryCash = state.treasuryCash - paid,
            debt = state.debt - paid,
        ).clamped()
    }

    // ── Energy ──

    fun energyDemand(state: GameState): Double {
        val base =
            (state.industry * BalanceConfig.ENERGY_DEMAND_PER_INDUSTRY_POINT) +
                (state.technology * BalanceConfig.ENERGY_DEMAND_PER_TECH_POINT)
        val seasonal = when (state.currentSeason) {
            Season.WINTER -> BalanceConfig.WINTER_ENERGY_DEMAND_BONUS
            Season.SUMMER -> BalanceConfig.SUMMER_ENERGY_DEMAND_BONUS
            Season.SPRING, Season.AUTUMN -> 1.0
        }
        return base * seasonal
    }

    /** Positive = surplus, negative = shortfall. */
    fun energyBalance(state: GameState): Double =
        state.energy - energyDemand(state)

    // ── Sectors ──

    fun applySectorDrift(
        state: GameState,
        landmarks: Map<String, LandmarkDef>,
    ): GameState {
        var next = state
        for (key in StateKeys.INVESTABLE_SECTORS) {
            next = next.plusDelta(key, -BalanceConfig.SECTOR_NATURAL_DECAY)
            val level = next.readKey(key)
            if (level < BalanceConfig.SECTOR_FLOOR) {
                next = next.plusDelta(
                    key,
                    (BalanceConfig.SECTOR_FLOOR - level) *
                        BalanceConfig.SECTOR_FLOOR_RECOVERY_RATE,
                )
            }
        }

        next = next.plusDelta(
            StateKeys.AGRICULTURE,
            when (next.currentSeason) {
                Season.SPRING -> BalanceConfig.SPRING_AGRICULTURE_BONUS
                Season.SUMMER -> BalanceConfig.SUMMER_AGRICULTURE_PENALTY
                Season.AUTUMN -> BalanceConfig.AUTUMN_AGRICULTURE_BONUS
                Season.WINTER -> BalanceConfig.WINTER_AGRICULTURE_PENALTY
            },
        )

        val populationPressure =
            (next.population / 1000000.0) * BalanceConfig.FOOD_SECURITY_POPULATION_PRESSURE
        next = next.plusDelta(
            StateKeys.FOOD_SECURITY,
            (next.agriculture - next.foodSecurity) *
                BalanceConfig.FOOD_SECURITY_AGRICULTURE_WEIGHT -
                populationPressure +
                ((next.subsidyLevel - BalanceConfig.NEUTRAL_SUBSIDY_LEVEL) *
                    BalanceConfig.SUBSIDY_FOOD_SUPPORT),
        )

        next = next.plusDelta(
            StateKeys.TECHNOLOGY,
            next.education * BalanceConfig.EDUCATION_TO_TECHNOLOGY_RATE,
        )
        next = next.plusDelta(
            StateKeys.CULTURE,
            next.education * BalanceConfig.EDUCATION_TO_CULTURE_RATE,
        )
        next = next.plusDelta(
            StateKeys.CYBER_SECURITY,
            next.technology * BalanceConfig.TECHNOLOGY_TO_CYBER_RATE,
        )

        val landmarkCount = next.builtLandmarks.size
        next = next.plusDelta(
            StateKeys.CULTURE,
            landmarkCount * BalanceConfig.CULTURE_PER_LANDMARK_PER_TURN,
        )
        next = next.plusDelta(
            StateKeys.TOURISM,
            (next.culture * BalanceConfig.TOURISM_CULTURE_WEIGHT * 0.1) +
                (landmarkCount * BalanceConfig.TOURISM_PER_LANDMARK),
        )

        for (built in next.builtLandmarks) {
            val def = landmarks[built.id] ?: continue
            var acc = next
            def.perTurnEffects.forEach { (key, delta) -> acc = acc.plusDelta(key, delta) }
            next = acc
        }

        return next.clamped()
    }

    fun sectorIndex(state: GameState): Double =
        (state.industry * BalanceConfig.ECONOMY_INDUSTRY_WEIGHT) +
            (state.technology * BalanceConfig.ECONOMY_TECHNOLOGY_WEIGHT) +
            (state.agriculture * BalanceConfig.ECONOMY_AGRICULTURE_WEIGHT) +
            (state.energy * BalanceConfig.ECONOMY_ENERGY_WEIGHT) +
            (state.education * BalanceConfig.ECONOMY_EDUCATION_WEIGHT) +
            (state.tourism * BalanceConfig.ECONOMY_TOURISM_WEIGHT)

    fun applyEconomicGrowth(state: GameState, debtStressed: Boolean): GameState {
        val target = sectorIndex(state) + BalanceConfig.ECONOMY_BASELINE_OFFSET
        val gap = target - state.economy
        var delta = gap * BalanceConfig.ECONOMY_REVERSION_RATE

        delta -= state.sanctionCount * BalanceConfig.ECONOMY_SANCTION_PENALTY
        delta -= state.warEnemies.size * BalanceConfig.ECONOMY_WAR_PENALTY
        if (debtStressed) delta -= BalanceConfig.ECONOMY_DEBT_STRESS_PENALTY

        return state.plusDelta(StateKeys.ECONOMY, delta).clamped()
    }

    fun applyEnvironment(state: GameState): GameState {
        var next = state
        val pollution = next.industry *
            BalanceConfig.INDUSTRY_POLLUTION_PER_POINT *
            (1 - (next.cleanEnergyRatio * BalanceConfig.CLEAN_ENERGY_OFFSET))
        next = next.plusDelta(
            StateKeys.ENVIRONMENT,
            BalanceConfig.ENVIRONMENT_RECOVERY_RATE - pollution,
        )

        val shortfall = -energyBalance(next)
        if (shortfall > 0) {
            next = next.plusDelta(
                StateKeys.ECONOMY,
                -shortfall * BalanceConfig.ENERGY_SHORTFALL_ECONOMY_PENALTY,
            )
        }
        return next.clamped()
    }

    // ── Society ──

    fun applySociety(
        state: GameState,
        finance: TurnFinance,
        rng: Random,
    ): GameState {
        var next = state
        val taxPressure = next.taxRate - BalanceConfig.COMFORTABLE_TAX_RATE
        val subsidyComfort = next.subsidyLevel - BalanceConfig.NEUTRAL_SUBSIDY_LEVEL

        val workersTarget = 50 +
            (subsidyComfort * BalanceConfig.WORKERS_SUBSIDY_WEIGHT * 10) +
            ((next.foodSecurity - 50) * BalanceConfig.WORKERS_FOOD_WEIGHT * 10) -
            (taxPressure * BalanceConfig.TAX_SATISFACTION_SENSITIVITY * 0.5)
        val middleTarget = 50 +
            ((next.economy - 50) * BalanceConfig.MIDDLE_CLASS_ECONOMY_WEIGHT * 10) -
            (taxPressure * BalanceConfig.MIDDLE_CLASS_TAX_WEIGHT)
        val eliteTarget = 50 +
            ((next.economy - 50) * BalanceConfig.ELITE_ECONOMY_WEIGHT * 10) -
            (taxPressure * BalanceConfig.ELITE_TAX_WEIGHT)

        val inertia = BalanceConfig.CLASS_SATISFACTION_INERTIA
        next = next.copy(
            workersSatisfaction =
                (next.workersSatisfaction * inertia) + (workersTarget * (1 - inertia)),
            middleClassSatisfaction =
                (next.middleClassSatisfaction * inertia) + (middleTarget * (1 - inertia)),
            eliteSatisfaction =
                (next.eliteSatisfaction * inertia) + (eliteTarget * (1 - inertia)),
        )

        var delta = 0.0
        delta -= taxPressure * BalanceConfig.TAX_SATISFACTION_SENSITIVITY * 0.1
        delta += subsidyComfort * BalanceConfig.SUBSIDY_SATISFACTION_SENSITIVITY * 0.1
        delta += (next.economy - 50) * BalanceConfig.ECONOMY_SATISFACTION_WEIGHT
        delta += (next.health - 50) * BalanceConfig.HEALTH_SATISFACTION_WEIGHT
        delta += (next.education - 50) * BalanceConfig.EDUCATION_SATISFACTION_WEIGHT

        if (next.foodSecurity < BalanceConfig.FOOD_CRISIS_THRESHOLD) {
            delta -= (BalanceConfig.FOOD_CRISIS_THRESHOLD - next.foodSecurity) *
                BalanceConfig.FOOD_CRISIS_PENALTY_PER_POINT
        }
        delta -= next.warEnemies.size * BalanceConfig.WAR_SATISFACTION_PENALTY
        delta -= next.sanctionCount * BalanceConfig.SANCTION_SATISFACTION_PENALTY
        if (isDebtStressed(next, finance)) {
            delta -= BalanceConfig.DEBT_STRESS_SATISFACTION_PENALTY
        }
        delta += (next.classSatisfactionAverage - next.publicSatisfaction) *
            BalanceConfig.SATISFACTION_REVERSION_RATE

        next = next.plusDelta(StateKeys.PUBLIC_SATISFACTION, delta)

        val noise =
            (rng.nextDouble() * 2 - 1) * BalanceConfig.DIGITAL_OPINION_VOLATILITY
        val digitalDelta =
            (next.publicSatisfaction - next.digitalOpinion) *
                BalanceConfig.DIGITAL_OPINION_REVERSION_RATE +
                (next.technology * BalanceConfig.DIGITAL_OPINION_TECH_WEIGHT) +
                noise
        next = next.plusDelta(StateKeys.DIGITAL_OPINION, digitalDelta)

        return next.clamped()
    }

    // ── Population ──

    fun applyPopulation(state: GameState): GameState {
        var rate = BalanceConfig.BASE_GROWTH_RATE +
            ((state.health - 50) / 100) * BalanceConfig.HEALTH_GROWTH_SENSITIVITY
        if (state.foodSecurity < BalanceConfig.FOOD_CRISIS_THRESHOLD) {
            rate -= BalanceConfig.FOOD_CRISIS_GROWTH_PENALTY
        }

        val attractiveness = (state.economy *
            BalanceConfig.MIGRATION_ECONOMY_WEIGHT) +
            (state.publicSatisfaction * BalanceConfig.MIGRATION_SATISFACTION_WEIGHT) +
            (state.health * BalanceConfig.MIGRATION_HEALTH_WEIGHT) +
            (state.militarySecurity * BalanceConfig.MIGRATION_SECURITY_WEIGHT) -
            50 -
            (if (state.isAtWar) BalanceConfig.MIGRATION_WAR_PENALTY else 0.0)

        val migrationBalance =
            state.population * attractiveness * BalanceConfig.MIGRATION_SCALE / 100

        val population =
            (state.population * (1 + rate) + migrationBalance).roundToInt()

        return state.copy(
            migrationBalance = migrationBalance,
            population = population,
        ).clamped()
    }

    // ── Investment ──

    fun sectorInvestmentCost(
        state: GameState,
        sectorKey: String,
        points: Double,
    ): Double {
        val level = state.readKey(sectorKey)
        val multiplier =
            1 + (level / 100) * BalanceConfig.SECTOR_INVESTMENT_COST_GROWTH
        return points * BalanceConfig.SECTOR_INVESTMENT_COST_PER_POINT * multiplier
    }

    fun investInSector(
        state: GameState,
        sectorKey: String,
        points: Double,
    ): GameState? {
        if (sectorKey !in StateKeys.INVESTABLE_SECTORS) return null
        val capped =
            points.coerceIn(0.0, BalanceConfig.MAX_SECTOR_INVESTMENT_PER_TURN)
        val cost = sectorInvestmentCost(state, sectorKey, capped)
        if (capped <= 0 || state.treasuryCash < cost) return null
        return state.copy(treasuryCash = state.treasuryCash - cost)
            .plusDelta(sectorKey, capped)
            .clamped()
    }

    /** Countries map helper: which states hold a trade deal and are at peace. */
    fun tradeDealCount(countries: Map<String, DiplomacyState>): Int =
        countries.values.count { it.hasTradeDeal && !it.atWar }
}

package studio.mgn.model

import kotlinx.serialization.Serializable
import studio.mgn.model.serializers.LocalDateSerializer
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.roundToInt

/** Maximum chronicle entries kept in memory and in saves. */
const val HISTORY_LIMIT = 400

/** Maximum points kept in the treasury/score chart series. */
const val HISTORY_CHART_LIMIT = 30

/** Ending markers persisted on [GameState.ended]. */
object GameEnding {
    const val VICTORY = "victory"
    const val COLLAPSE = "collapse"
}

/**
 * The complete state of one save file. Fully immutable: every engine
 * operation returns a new instance via [copy].
 */
@Serializable
data class GameState(
    // ── Identity ──
    val countryName: String,
    val rulerTitle: String,
    @Serializable(with = LocalDateSerializer::class)
    val inGameDate: LocalDate,
    val turnNumber: Int,
    val currentSeason: Season,
    val turnLength: TurnLength,
    val era: Era,
    // ── Economy ──
    val treasuryCash: Double,
    val gems: Int,
    /** 0.0 - 1.0 */
    val taxRate: Double,
    val debt: Double,
    /** Player-tuned spending dials, 0.0 - 1.0. */
    val militarySpendingLevel: Double,
    val subsidyLevel: Double,
    // ── Core indicators, 0-100 ──
    val economy: Double,
    val publicSatisfaction: Double,
    val digitalOpinion: Double,
    val militarySecurity: Double,
    val cyberSecurity: Double,
    val environment: Double,
    val culture: Double,
    /** Mandate strength, evaluated every referendum cycle. */
    val legitimacy: Double,
    // ── Sectors, 0-100 (cleanEnergyRatio is 0-1) ──
    val agriculture: Double,
    val industry: Double,
    val foodSecurity: Double,
    val energy: Double,
    val cleanEnergyRatio: Double,
    val technology: Double,
    val tourism: Double,
    val health: Double,
    val education: Double,
    // ── Population ──
    val population: Int,
    /** Positive = net inbound migration. */
    val migrationBalance: Double,
    val workersSatisfaction: Double,
    val middleClassSatisfaction: Double,
    val eliteSatisfaction: Double,
    // ── Ideology axes, -100 .. 100 ──
    val axisEconomic: Double,
    val axisSocial: Double,
    val axisForeign: Double,
    // ── World & build ──
    val countries: Map<String, DiplomacyState>,
    val builtLandmarks: List<BuiltLandmark>,
    val underConstruction: List<OngoingBuild>,
    // ── Log ──
    val history: List<HistoryEntry>,
    val unlockedAchievements: Set<String>,
    /** Completed mission ids (rewards granted once). */
    val completedMissions: Set<String> = emptySet(),
    /** Ruler experience: +RULER_XP_PER_TURN each turn, level every RULER_XP_PER_LEVEL. */
    val rulerXp: Int = 0,
    /**
     * Ending once reached: "victory" or "collapse", null while ongoing.
     * A collapsed save cannot be played without a restart.
     */
    val ended: String? = null,
    /** Last ≤30 end-of-turn treasury values, for the economy chart. */
    val treasuryHistory: List<Double> = emptyList(),
    /** Last ≤30 end-of-turn nation scores, for the economy chart. */
    val scoreHistory: List<Double> = emptyList(),
    /**
     * War stances chosen by the player, consumed by the next turn.
     * Cleared after every turn (same as the legacy controller behavior).
     */
    val warStances: Map<String, WarStance> = emptyMap(),
    /** Event id -> turn it last fired, for cooldown checks. */
    val lastEventTurns: Map<String, Int>,
    val firedOnceEvents: Set<String>,
    // ── Flag (rendered as generated art, never an uploaded image) ──
    val flagPrimaryColor: Int,
    val flagSecondaryColor: Int,
    val flagSymbolId: String,
) {
    // ── Derived helpers ──

    val isAtWar: Boolean get() = countries.values.any { it.atWar }

    val warEnemies: List<DiplomacyState>
        get() = countries.values.filter { it.atWar }

    val sanctionCount: Int get() = countries.values.count { it.sanctioned }

    val classSatisfactionAverage: Double
        get() = (workersSatisfaction + middleClassSatisfaction + eliteSatisfaction) / 3.0

    val nationScore: Double
        get() {
            val indicatorAvg = (
                economy + publicSatisfaction + digitalOpinion +
                    militarySecurity + cyberSecurity + environment + culture
                ) / 7.0
            val sectorAvg = (
                agriculture + industry + foodSecurity + energy +
                    technology + tourism + health + education
                ) / 8.0
            return indicatorAvg * 0.6 + sectorAvg * 0.4
        }

    /** Total collapse: no legitimacy, no public support, and broke. */
    val isCollapsed: Boolean
        get() = legitimacy < 10 && publicSatisfaction < 15 && treasuryCash <= 0

    /** Grand victory: excellent score with a solid mandate, after the opening. */
    val hasWon: Boolean
        get() = turnNumber > 24 && nationScore >= 85 && legitimacy >= 60

    val statusLabel: String
        get() = when {
            isCollapsed -> "انهيار الدولة"
            hasWon -> "نصر عظيم"
            else -> "مستمرة"
        }

    /**
     * Ruler level shown in the top bar (cosmetic progression).
     * The divisor must stay equal to BalanceConfig.RULER_XP_PER_LEVEL
     * (model cannot depend on engine; locked by an engine test).
     */
    val rulerLevel: Int get() = 1 + rulerXp / 100

    /** Progress toward the next ruler level, 0..1. */
    val rulerLevelProgress: Float
        get() = (rulerXp % 100) / 100f

    // ── Keyed access (used by event effects and conditions) ──

    fun readKey(key: String): Double = when (key) {
        StateKeys.TREASURY_CASH -> treasuryCash
        StateKeys.GEMS -> gems.toDouble()
        StateKeys.DEBT -> debt
        StateKeys.TAX_RATE -> taxRate
        StateKeys.ECONOMY -> economy
        StateKeys.PUBLIC_SATISFACTION -> publicSatisfaction
        StateKeys.DIGITAL_OPINION -> digitalOpinion
        StateKeys.MILITARY_SECURITY -> militarySecurity
        StateKeys.CYBER_SECURITY -> cyberSecurity
        StateKeys.ENVIRONMENT -> environment
        StateKeys.CULTURE -> culture
        StateKeys.AGRICULTURE -> agriculture
        StateKeys.INDUSTRY -> industry
        StateKeys.FOOD_SECURITY -> foodSecurity
        StateKeys.ENERGY -> energy
        StateKeys.CLEAN_ENERGY_RATIO -> cleanEnergyRatio
        StateKeys.TECHNOLOGY -> technology
        StateKeys.TOURISM -> tourism
        StateKeys.HEALTH -> health
        StateKeys.EDUCATION -> education
        StateKeys.POPULATION -> population.toDouble()
        StateKeys.MIGRATION_BALANCE -> migrationBalance
        StateKeys.AXIS_ECONOMIC -> axisEconomic
        StateKeys.AXIS_SOCIAL -> axisSocial
        StateKeys.AXIS_FOREIGN -> axisForeign
        else -> throw IllegalArgumentException("Unknown state key: $key")
    }

    fun withKey(key: String, value: Double): GameState = when (key) {
        StateKeys.TREASURY_CASH -> copy(treasuryCash = value)
        StateKeys.GEMS -> copy(gems = value.roundToInt())
        StateKeys.DEBT -> copy(debt = value)
        StateKeys.TAX_RATE -> copy(taxRate = value)
        StateKeys.ECONOMY -> copy(economy = value)
        StateKeys.PUBLIC_SATISFACTION -> copy(publicSatisfaction = value)
        StateKeys.DIGITAL_OPINION -> copy(digitalOpinion = value)
        StateKeys.MILITARY_SECURITY -> copy(militarySecurity = value)
        StateKeys.CYBER_SECURITY -> copy(cyberSecurity = value)
        StateKeys.ENVIRONMENT -> copy(environment = value)
        StateKeys.CULTURE -> copy(culture = value)
        StateKeys.AGRICULTURE -> copy(agriculture = value)
        StateKeys.INDUSTRY -> copy(industry = value)
        StateKeys.FOOD_SECURITY -> copy(foodSecurity = value)
        StateKeys.ENERGY -> copy(energy = value)
        StateKeys.CLEAN_ENERGY_RATIO -> copy(cleanEnergyRatio = value)
        StateKeys.TECHNOLOGY -> copy(technology = value)
        StateKeys.TOURISM -> copy(tourism = value)
        StateKeys.HEALTH -> copy(health = value)
        StateKeys.EDUCATION -> copy(education = value)
        StateKeys.POPULATION -> copy(population = value.roundToInt())
        StateKeys.MIGRATION_BALANCE -> copy(migrationBalance = value)
        StateKeys.AXIS_ECONOMIC -> copy(axisEconomic = value)
        StateKeys.AXIS_SOCIAL -> copy(axisSocial = value)
        StateKeys.AXIS_FOREIGN -> copy(axisForeign = value)
        else -> throw IllegalArgumentException("Unknown state key: $key")
    }

    fun plusDelta(key: String, delta: Double): GameState =
        withKey(key, readKey(key) + delta)

    fun applyEffects(effects: Map<String, Double>): GameState {
        var next = this
        effects.forEach { (key, delta) -> next = next.plusDelta(key, delta) }
        return next.clamped()
    }

    /** Appends a chronicle entry, keeping at most [HISTORY_LIMIT]. */
    fun plusHistory(entry: HistoryEntry): GameState {
        val grown = history + entry
        val trimmed = if (grown.size > HISTORY_LIMIT) {
            grown.drop(grown.size - HISTORY_LIMIT)
        } else {
            grown
        }
        return copy(history = trimmed)
    }

    /** Enforces every bound in one place. */
    fun clamped(): GameState = copy(
        economy = clamp100(economy),
        publicSatisfaction = clamp100(publicSatisfaction),
        digitalOpinion = clamp100(digitalOpinion),
        militarySecurity = clamp100(militarySecurity),
        cyberSecurity = clamp100(cyberSecurity),
        environment = clamp100(environment),
        culture = clamp100(culture),
        legitimacy = clamp100(legitimacy),
        agriculture = clamp100(agriculture),
        industry = clamp100(industry),
        foodSecurity = clamp100(foodSecurity),
        energy = clamp100(energy),
        technology = clamp100(technology),
        tourism = clamp100(tourism),
        health = clamp100(health),
        education = clamp100(education),
        workersSatisfaction = clamp100(workersSatisfaction),
        middleClassSatisfaction = clamp100(middleClassSatisfaction),
        eliteSatisfaction = clamp100(eliteSatisfaction),
        cleanEnergyRatio = cleanEnergyRatio.coerceIn(0.0, 1.0),
        taxRate = taxRate.coerceIn(0.0, 1.0),
        militarySpendingLevel = militarySpendingLevel.coerceIn(0.0, 1.0),
        subsidyLevel = subsidyLevel.coerceIn(0.0, 1.0),
        axisEconomic = axisEconomic.coerceIn(-100.0, 100.0),
        axisSocial = axisSocial.coerceIn(-100.0, 100.0),
        axisForeign = axisForeign.coerceIn(-100.0, 100.0),
        debt = max(0.0, debt),
        gems = max(0, gems),
        population = max(0, population),
        countries = countries.mapValues { (_, d) ->
            d.copy(
                relation = d.relation.coerceIn(-100.0, 100.0),
                economicPower = clamp100(d.economicPower),
                militaryPower = clamp100(d.militaryPower),
            )
        },
    )

    companion object {
        fun seasonForMonth(month: Int): Season = when (month) {
            in 3..5 -> Season.SPRING
            in 6..8 -> Season.SUMMER
            in 9..11 -> Season.AUTUMN
            else -> Season.WINTER
        }

        private fun clamp100(v: Double): Double = v.coerceIn(0.0, 100.0)

        /** A balanced starting state for a modern mid-sized nation. */
        fun newGame(
            countryName: String,
            rulerTitle: String,
            turnLength: TurnLength = TurnLength.MONTH,
            startDate: LocalDate = LocalDate.of(2025, 1, 1),
            flagPrimaryColor: Int = 0xFF0B0B0DL.toInt(),
            flagSecondaryColor: Int = 0xFFD4AF37L.toInt(),
            flagSymbolId: String = "crescent_star",
            countries: Map<String, DiplomacyState> = emptyMap(),
            axisEconomic: Double = 0.0,
            axisSocial: Double = 0.0,
            axisForeign: Double = 0.0,
        ): GameState = GameState(
            countryName = countryName,
            rulerTitle = rulerTitle,
            inGameDate = startDate,
            turnNumber = 1,
            currentSeason = seasonForMonth(startDate.monthValue),
            turnLength = turnLength,
            era = Era.MODERN,
            treasuryCash = 250000.0,
            gems = 25,
            taxRate = 0.22,
            debt = 0.0,
            militarySpendingLevel = 0.35,
            subsidyLevel = 0.40,
            economy = 50.0,
            publicSatisfaction = 55.0,
            digitalOpinion = 50.0,
            militarySecurity = 45.0,
            cyberSecurity = 35.0,
            environment = 55.0,
            culture = 30.0,
            legitimacy = 60.0,
            agriculture = 45.0,
            industry = 40.0,
            foodSecurity = 50.0,
            energy = 45.0,
            cleanEnergyRatio = 0.15,
            technology = 30.0,
            tourism = 25.0,
            health = 45.0,
            education = 40.0,
            population = 12000000,
            migrationBalance = 0.0,
            workersSatisfaction = 52.0,
            middleClassSatisfaction = 55.0,
            eliteSatisfaction = 58.0,
            axisEconomic = axisEconomic,
            axisSocial = axisSocial,
            axisForeign = axisForeign,
            countries = countries,
            builtLandmarks = emptyList(),
            underConstruction = emptyList(),
            history = emptyList(),
            unlockedAchievements = emptySet(),
            lastEventTurns = emptyMap(),
            firedOnceEvents = emptySet(),
            flagPrimaryColor = flagPrimaryColor,
            flagSecondaryColor = flagSecondaryColor,
            flagSymbolId = flagSymbolId,
        )
    }
}

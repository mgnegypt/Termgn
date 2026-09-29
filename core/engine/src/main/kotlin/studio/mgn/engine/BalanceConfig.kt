package studio.mgn.engine

/**
 * Every balance number in MGN lives here.
 *
 * Rule: no magic numbers anywhere else in the code base. A balance request
 * must be a one-number change in this file. Ported 1:1 from the legacy
 * Flutter BalanceConfig.
 */
object BalanceConfig {
    // ── INCOME ──

    /** Soft currency collected per citizen per turn at 100% tax and 100% economy. */
    const val TAX_PER_CAPITA = 0.118

    /** Economy acts as a multiplier floor+slope on tax yield. */
    const val TAX_ECONOMY_FLOOR = 0.60
    const val TAX_ECONOMY_SLOPE = 0.80

    /** Export yield per sector point, per turn. */
    const val AGRICULTURE_EXPORT_PER_POINT = 340.0
    const val INDUSTRY_EXPORT_PER_POINT = 650.0
    const val ENERGY_EXPORT_PER_POINT = 450.0

    /** Each active trade treaty lifts export income by this fraction. */
    const val EXPORT_BONUS_PER_TRADE_DEAL = 0.09

    /** Each active sanction cuts export income by this fraction. */
    const val EXPORT_PENALTY_PER_SANCTION = 0.16

    /** Tourism revenue per tourism point, before security/war modifiers. */
    const val TOURISM_INCOME_PER_POINT = 180.0

    /** Tourism is scaled by `securityFloor + militarySecurity/100 * slope`. */
    const val TOURISM_SECURITY_FLOOR = 0.35
    const val TOURISM_SECURITY_SLOPE = 0.65

    /** Multiplier applied to tourism income while any war is active. */
    const val TOURISM_WAR_MULTIPLIER = 0.35

    /** Customs revenue per trade treaty, scaled by economy. */
    const val CUSTOMS_PER_TRADE_DEAL = 6500.0

    /** Digital economy revenue per technology point. */
    const val DIGITAL_ECONOMY_PER_TECH_POINT = 240.0

    // ── EXPENSES ──

    /** Fixed ministry payroll per turn. */
    const val BASE_GOVERNMENT_PAYROLL = 38000.0

    /** Payroll scaling with population. */
    const val PAYROLL_PER_CAPITA = 0.008

    /** The main brake on late-game snowballing. */
    const val INSTITUTIONAL_COST_PER_ECONOMY_POINT = 350.0

    /** Infrastructure upkeep per industry point. */
    const val INDUSTRY_UPKEEP_PER_POINT = 140.0

    /** Health and education services upkeep per point. */
    const val HEALTH_UPKEEP_PER_POINT = 120.0
    const val EDUCATION_UPKEEP_PER_POINT = 100.0

    /** Military budget at `militarySpendingLevel == 1.0`. */
    const val MILITARY_BUDGET_MAX = 160000.0

    /** Subsidy budget (food + energy) at `subsidyLevel == 1.0`. */
    const val SUBSIDY_BUDGET_MAX = 150000.0

    /** Interest charged on outstanding debt each turn. */
    const val DEBT_INTEREST_RATE = 0.010

    /** Extra war spending per turn, per active war. */
    const val WAR_COST_PER_TURN = 120000.0

    // ── DEBT ──

    /** Emergency borrowing penalty markup. */
    const val EMERGENCY_LOAN_MARKUP = 1.10

    /** Debt above this multiple of one turn's income hurts relations/satisfaction. */
    const val DEBT_STRESS_THRESHOLD = 6.0
    const val DEBT_STRESS_SATISFACTION_PENALTY = 1.6
    const val DEBT_STRESS_RELATION_PENALTY = 0.8

    /** Maximum voluntary loan per request. */
    const val MAX_LOAN_PER_REQUEST = 400000.0

    /** Relation hit applied to every country when taking a voluntary loan. */
    const val LOAN_RELATION_PENALTY = 1.5

    // ── SATISFACTION & SOCIETY ──

    /** Tax rate the public accepts without complaint. */
    const val COMFORTABLE_TAX_RATE = 0.24

    /** Satisfaction points lost per 1.0 of tax above the comfortable rate. */
    const val TAX_SATISFACTION_SENSITIVITY = 26.0

    /** Subsidy level the public treats as neutral. */
    const val NEUTRAL_SUBSIDY_LEVEL = 0.35
    const val SUBSIDY_SATISFACTION_SENSITIVITY = 9.0

    /** Food security below this triggers a hunger penalty. */
    const val FOOD_CRISIS_THRESHOLD = 40.0
    const val FOOD_CRISIS_PENALTY_PER_POINT = 0.15

    /** Services pull satisfaction toward their own level at this rate. */
    const val HEALTH_SATISFACTION_WEIGHT = 0.035
    const val EDUCATION_SATISFACTION_WEIGHT = 0.020
    const val ECONOMY_SATISFACTION_WEIGHT = 0.040

    /** Satisfaction lost per turn of active war, and per active sanction. */
    const val WAR_SATISFACTION_PENALTY = 1.8
    const val SANCTION_SATISFACTION_PENALTY = 0.9

    /** Mean reversion toward the class-satisfaction average. */
    const val SATISFACTION_REVERSION_RATE = 0.20

    /** Class-specific reactions. */
    const val WORKERS_SUBSIDY_WEIGHT = 12.0
    const val WORKERS_FOOD_WEIGHT = 0.06
    const val MIDDLE_CLASS_ECONOMY_WEIGHT = 0.04
    const val MIDDLE_CLASS_TAX_WEIGHT = 18.0
    const val ELITE_TAX_WEIGHT = 30.0
    const val ELITE_ECONOMY_WEIGHT = 0.05

    /** Per-turn smoothing applied to every class satisfaction value. */
    const val CLASS_SATISFACTION_INERTIA = 0.75

    // ── DIGITAL OPINION ──

    const val DIGITAL_OPINION_REVERSION_RATE = 0.18
    const val DIGITAL_OPINION_VOLATILITY = 2.5
    const val DIGITAL_OPINION_TECH_WEIGHT = 0.015

    // ── ENVIRONMENT & ENERGY ──

    /** Environment damage per industry point per turn at 0% clean energy. */
    const val INDUSTRY_POLLUTION_PER_POINT = 0.012

    /** Clean energy ratio offsets pollution up to this fraction. */
    const val CLEAN_ENERGY_OFFSET = 0.9

    /** Natural environmental recovery per turn. */
    const val ENVIRONMENT_RECOVERY_RATE = 0.45

    /** Seasonal energy demand multipliers. */
    const val WINTER_ENERGY_DEMAND_BONUS = 1.18
    const val SUMMER_ENERGY_DEMAND_BONUS = 1.10

    /** Energy shortfall penalty to economy per point. */
    const val ENERGY_SHORTFALL_ECONOMY_PENALTY = 0.05

    /** Energy demand per industry point and per technology point. */
    const val ENERGY_DEMAND_PER_INDUSTRY_POINT = 0.55
    const val ENERGY_DEMAND_PER_TECH_POINT = 0.25

    // ── ECONOMIC GROWTH ──

    const val ECONOMY_INDUSTRY_WEIGHT = 0.30
    const val ECONOMY_TECHNOLOGY_WEIGHT = 0.20
    const val ECONOMY_AGRICULTURE_WEIGHT = 0.15
    const val ECONOMY_ENERGY_WEIGHT = 0.15
    const val ECONOMY_EDUCATION_WEIGHT = 0.10
    const val ECONOMY_TOURISM_WEIGHT = 0.10

    const val ECONOMY_BASELINE_OFFSET = 12.0
    const val ECONOMY_REVERSION_RATE = 0.07

    const val ECONOMY_SANCTION_PENALTY = 0.25
    const val ECONOMY_WAR_PENALTY = 0.40
    const val ECONOMY_DEBT_STRESS_PENALTY = 0.30

    // ── SECTOR DRIFT ──

    /** Sectors decay slowly without investment. */
    const val SECTOR_NATURAL_DECAY = 0.06

    /** Subsistence floor keeps a collapsed state recoverable. */
    const val SECTOR_FLOOR = 20.0
    const val SECTOR_FLOOR_RECOVERY_RATE = 0.30

    /** Seasonal agriculture modifiers. */
    const val SPRING_AGRICULTURE_BONUS = 0.55
    const val SUMMER_AGRICULTURE_PENALTY = -0.30
    const val AUTUMN_AGRICULTURE_BONUS = 0.35
    const val WINTER_AGRICULTURE_PENALTY = -0.45

    /** Food security tracks agriculture, offset by population pressure. */
    const val FOOD_SECURITY_AGRICULTURE_WEIGHT = 0.12
    const val FOOD_SECURITY_POPULATION_PRESSURE = 0.015

    const val SUBSIDY_FOOD_SUPPORT = 5.0

    /** Education slowly lifts technology and culture. */
    const val EDUCATION_TO_TECHNOLOGY_RATE = 0.012
    const val EDUCATION_TO_CULTURE_RATE = 0.008

    /** Technology slowly lifts cyber security. */
    const val TECHNOLOGY_TO_CYBER_RATE = 0.030

    /** Culture generated per completed landmark per turn. */
    const val CULTURE_PER_LANDMARK_PER_TURN = 0.08

    /** Tourism tracks culture and landmark count. */
    const val TOURISM_CULTURE_WEIGHT = 0.05
    const val TOURISM_PER_LANDMARK = 0.10

    /** Cost in cash to raise one sector by one point. */
    const val SECTOR_INVESTMENT_COST_PER_POINT = 14000.0
    const val SECTOR_INVESTMENT_COST_GROWTH = 5.0
    const val MAX_SECTOR_INVESTMENT_PER_TURN = 4.0

    // ── POPULATION ──

    const val BASE_GROWTH_RATE = 0.0018
    const val HEALTH_GROWTH_SENSITIVITY = 0.0022
    const val FOOD_CRISIS_GROWTH_PENALTY = 0.0030

    const val MIGRATION_SCALE = 0.0010
    const val MIGRATION_ECONOMY_WEIGHT = 0.4
    const val MIGRATION_SATISFACTION_WEIGHT = 0.3
    const val MIGRATION_HEALTH_WEIGHT = 0.2
    const val MIGRATION_SECURITY_WEIGHT = 0.1
    const val MIGRATION_WAR_PENALTY = 25.0

    // ── EVENT ENGINE ──

    const val DEFAULT_EVENT_COOLDOWN = 18
    const val MAX_EVENTS_PER_TURN = 2
    const val SECOND_EVENT_CHANCE = 0.28
    const val CRISIS_COOLDOWN = 6
    const val CRISIS_PRIORITY_CHANCE = 0.85
    const val FIRST_EVENT_TURN = 3
    const val DECISION_COST_MULTIPLIER = 0.3

    // ── DIPLOMACY ──

    const val RELATION_DRIFT_HOSTILE = -0.45
    const val RELATION_DRIFT_TRADER = 0.25
    const val RELATION_DRIFT_NEUTRAL = 0.05
    const val RELATION_DRIFT_ISOLATIONIST = -0.05

    const val EMBASSY_RELATION_DRIFT = 0.55
    const val TRADE_DEAL_RELATION_DRIFT = 0.35

    const val AXIS_FOREIGN_RELATION_WEIGHT = 0.010
    const val AXIS_ECONOMIC_RELATION_WEIGHT = 0.006

    const val SANCTION_THRESHOLD = -55.0
    const val SANCTION_LIFT_THRESHOLD = -35.0

    const val WAR_DECLARATION_THRESHOLD = -75.0
    const val WAR_DECLARATION_CHANCE = 0.12

    const val AI_OFFER_COOLDOWN = 10
    const val EMBASSY_COST = 85000.0

    const val TRADE_SIGN_RELATION_BONUS = 8.0
    const val DEFENSIVE_SIGN_RELATION_BONUS = 12.0
    const val NON_AGGRESSION_SIGN_RELATION_BONUS = 5.0
    const val EMBASSY_SIGN_RELATION_BONUS = 6.0

    const val TRADE_RELATION_REQUIREMENT = 15.0
    const val DEFENSIVE_RELATION_REQUIREMENT = 45.0
    const val NON_AGGRESSION_RELATION_REQUIREMENT = -10.0

    const val AI_POWER_DRIFT_MAX = 0.35

    // ── WAR ──

    const val WAR_MILITARY_WEIGHT = 0.55
    const val WAR_ECONOMY_WEIGHT = 0.20
    const val WAR_TECHNOLOGY_WEIGHT = 0.10
    const val WAR_SATISFACTION_WEIGHT = 0.15

    const val WAR_RANDOM_SWING = 12.0

    const val WAR_STANCE_OFFENSIVE_BONUS = 8.0
    const val WAR_STANCE_DEFENSIVE_BONUS = 4.0

    const val WAR_STANCE_DEFENSIVE_ATTRITION_FACTOR = 0.6
    const val WAR_STANCE_OFFENSIVE_ATTRITION_FACTOR = 1.4

    const val WAR_ATTRITION_MILITARY = 2.2
    const val WAR_ATTRITION_ECONOMY = 1.4
    const val WAR_ATTRITION_POPULATION_RATE = 0.0012

    const val WAR_DECISIVE_GAP = 18.0
    const val WAR_MINIMUM_TURNS = 3
    const val WAR_MAXIMUM_TURNS = 14

    const val ALLY_SUPPORT_BONUS = 10.0

    const val WAR_VICTORY_ECONOMY_BONUS = 6.0
    const val WAR_VICTORY_SATISFACTION_BONUS = 12.0
    const val WAR_VICTORY_REPARATIONS = 250000.0
    const val WAR_DEFEAT_ECONOMY_PENALTY = 12.0
    const val WAR_DEFEAT_SATISFACTION_PENALTY = 18.0
    const val WAR_DEFEAT_REPARATIONS = 300000.0
    const val WAR_STALEMATE_SATISFACTION_PENALTY = 6.0

    // ── CALENDAR ──

    const val REFERENDUM_INTERVAL_YEARS = 4
    const val REFERENDUM_PASS_THRESHOLD = 45.0
    const val LEGITIMACY_SATISFACTION_WEIGHT = 0.06
    const val LEGITIMACY_SCORE_WEIGHT = 0.04
    const val REFERENDUM_FAILURE_PENALTY = 15.0

    // ── GEMS ──

    const val YEARLY_REPORT_GEMS_BASE = 5
    const val YEARLY_REPORT_GEMS_BONUS_PER_TEN_SCORE = 1
    const val GEMS_PER_CONSTRUCTION_TURN_SKIP = 3
    const val GEMS_PER_DECISION_REROLL = 5
    const val CASH_PER_GEM = 12000.0

    // ── RULER PROGRESSION (cosmetic; never affects simulation balance) ──

    /** XP granted per completed turn. */
    const val RULER_XP_PER_TURN = 2

    /** XP per ruler level. Must match the divisor in GameState.rulerLevel. */
    const val RULER_XP_PER_LEVEL = 100
}

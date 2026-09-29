package studio.mgn.model

/**
 * Canonical keys for every numeric field an event/decision may modify.
 * Effect maps in content must only use [EFFECT_KEYS]; conditions may
 * additionally use [CONDITION_ONLY_KEYS].
 */
object StateKeys {
    const val TREASURY_CASH = "treasuryCash"
    const val GEMS = "gems"
    const val DEBT = "debt"
    const val TAX_RATE = "taxRate"

    const val ECONOMY = "economy"
    const val PUBLIC_SATISFACTION = "publicSatisfaction"
    const val DIGITAL_OPINION = "digitalOpinion"
    const val MILITARY_SECURITY = "militarySecurity"
    const val CYBER_SECURITY = "cyberSecurity"
    const val ENVIRONMENT = "environment"
    const val CULTURE = "culture"

    const val AGRICULTURE = "agriculture"
    const val INDUSTRY = "industry"
    const val FOOD_SECURITY = "foodSecurity"
    const val ENERGY = "energy"
    const val CLEAN_ENERGY_RATIO = "cleanEnergyRatio"
    const val TECHNOLOGY = "technology"
    const val TOURISM = "tourism"
    const val HEALTH = "health"
    const val EDUCATION = "education"

    const val POPULATION = "population"
    const val MIGRATION_BALANCE = "migrationBalance"

    const val AXIS_ECONOMIC = "axisEconomic"
    const val AXIS_SOCIAL = "axisSocial"
    const val AXIS_FOREIGN = "axisForeign"

    val INDICATORS = listOf(
        ECONOMY, PUBLIC_SATISFACTION, DIGITAL_OPINION, MILITARY_SECURITY,
        CYBER_SECURITY, ENVIRONMENT, CULTURE,
    )

    val SECTORS = listOf(
        AGRICULTURE, INDUSTRY, FOOD_SECURITY, ENERGY,
        TECHNOLOGY, TOURISM, HEALTH, EDUCATION,
    )

    /** Sectors the player can invest in directly (food security is derived). */
    val INVESTABLE_SECTORS = listOf(
        AGRICULTURE, INDUSTRY, ENERGY, TECHNOLOGY, TOURISM, HEALTH, EDUCATION,
    )

    /** Every key a content effect map, requirement map or landmark map may use. */
    val EFFECT_KEYS: Set<String> = (
        INDICATORS + SECTORS + listOf(
            TREASURY_CASH, GEMS, DEBT, TAX_RATE, CLEAN_ENERGY_RATIO,
            POPULATION, MIGRATION_BALANCE,
            AXIS_ECONOMIC, AXIS_SOCIAL, AXIS_FOREIGN,
        )
        ).toSet()

    /**
     * Derived values usable in conditions only (never as effect targets).
     * Resolved by [studio.mgn.engine.ConditionEvaluator].
     */
    val CONDITION_ONLY_KEYS = setOf(
        "builtLandmarksCount",
        "turnNumber",
        "inGameYear",
        "nationScore",
        "tradeDealCount",
        "allyCount",
        "minRelation",
        "isAtWar",
    )

    /**
     * Fields readable in conditions but never targeted by effects
     * (class satisfaction and legitimacy move only through the simulation).
     */
    val readableOnlyKeys = setOf(
        "workersSatisfaction",
        "middleClassSatisfaction",
        "eliteSatisfaction",
        "legitimacy",
    )

    val KNOWN_CONDITION_KEYS: Set<String> = EFFECT_KEYS + CONDITION_ONLY_KEYS +
        readableOnlyKeys
}

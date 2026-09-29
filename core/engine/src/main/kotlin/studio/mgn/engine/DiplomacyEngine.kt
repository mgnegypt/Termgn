package studio.mgn.engine

import studio.mgn.model.AiBehavior
import studio.mgn.model.DiplomacyState
import studio.mgn.model.GameState
import studio.mgn.model.HistoryEntry
import studio.mgn.model.HistoryTag
import studio.mgn.model.StateKeys
import studio.mgn.model.TreatyType
import studio.mgn.model.WarOutcome
import studio.mgn.model.WarStance
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

/** Outcome of one country's war, after a turn of fighting. */
data class WarResult(
    val countryId: String,
    val countryName: String,
    val outcome: WarOutcome,
    val playerScore: Double,
    val enemyScore: Double,
)

/** Everything diplomacy changed this turn. */
data class DiplomacyTurnReport(
    val declaredWars: List<String> = emptyList(),
    val newSanctions: List<String> = emptyList(),
    val liftedSanctions: List<String> = emptyList(),
    val warResults: List<WarResult> = emptyList(),
    val aiOffers: List<DiplomaticOffer> = emptyList(),
) {
    val isEmpty: Boolean get() =
        declaredWars.isEmpty() && newSanctions.isEmpty() &&
            liftedSanctions.isEmpty() && warResults.isEmpty() && aiOffers.isEmpty()
}

/** A treaty an AI country proposes to the player. */
data class DiplomaticOffer(
    val countryId: String,
    val countryName: String,
    val type: TreatyType,
)

data class DiplomacyOutcome(
    val state: GameState,
    val report: DiplomacyTurnReport,
)

/**
 * Rule-based diplomacy: relation drift, treaties, sanctions and simplified
 * war resolution. Pure apart from [rng]; returns new states.
 */
class DiplomacyEngine(private val rng: Random = Random.Default) {
    // ── Per-turn tick ──

    fun advanceTurn(
        state: GameState,
        stances: Map<String, WarStance> = emptyMap(),
        debtStressed: Boolean = false,
    ): DiplomacyOutcome {
        var next = state
        val declaredWars = mutableListOf<String>()
        val newSanctions = mutableListOf<String>()
        val liftedSanctions = mutableListOf<String>()
        val warResults = mutableListOf<WarResult>()
        val offers = mutableListOf<DiplomaticOffer>()

        for (country in next.countries.values) {
            var c = driftPower(next, country)

            if (c.atWar) {
                val (fought, fc, warResult) = fightTurn(
                    next, c, stances[c.countryId] ?: WarStance.DEFENSIVE,
                )
                warResults.add(warResult)
                if (warResult.outcome != WarOutcome.ONGOING) {
                    val settled = settleWar(fought, fc, warResult)
                    next = settled.first
                    c = settled.second
                } else {
                    next = putCountry(fought, fc)
                    c = fc
                }
                continue
            }

            c = driftRelation(next, c, debtStressed)

            if (!c.sanctioned && c.relation <= BalanceConfig.SANCTION_THRESHOLD) {
                c = c.copy(sanctioned = true)
                newSanctions.add(c.nameAr)
            } else if (c.sanctioned && c.relation >= BalanceConfig.SANCTION_LIFT_THRESHOLD) {
                c = c.copy(sanctioned = false)
                liftedSanctions.add(c.nameAr)
            }

            if (c.behavior == AiBehavior.HOSTILE &&
                c.relation <= BalanceConfig.WAR_DECLARATION_THRESHOLD &&
                TreatyType.NON_AGGRESSION !in c.treaties &&
                rng.nextDouble() < BalanceConfig.WAR_DECLARATION_CHANCE
            ) {
                val started = startWar(next, c, declaredBy = c.nameAr)
                next = started.first
                c = started.second
                declaredWars.add(c.nameAr)
                continue
            }

            val offered = maybeOffer(next, c)
            if (offered != null) {
                offers.add(offered.first)
                c = offered.second
            }
            next = putCountry(next, c)
        }

        next = next.clamped()
        return DiplomacyOutcome(
            state = next,
            report = DiplomacyTurnReport(
                declaredWars = declaredWars,
                newSanctions = newSanctions,
                liftedSanctions = liftedSanctions,
                warResults = warResults,
                aiOffers = offers,
            ),
        )
    }

    private fun putCountry(state: GameState, country: DiplomacyState): GameState =
        state.copy(countries = state.countries + (country.countryId to country))

    private fun driftPower(state: GameState, country: DiplomacyState): DiplomacyState {
        val swing = (rng.nextDouble() * 2 - 1) * BalanceConfig.AI_POWER_DRIFT_MAX
        return country.copy(
            economicPower = country.economicPower + swing,
            militaryPower = country.militaryPower + swing * 0.6,
        )
    }

    private fun driftRelation(
        state: GameState,
        country: DiplomacyState,
        debtStressed: Boolean,
    ): DiplomacyState {
        var drift = when (country.behavior) {
            AiBehavior.HOSTILE -> BalanceConfig.RELATION_DRIFT_HOSTILE
            AiBehavior.TRADER -> BalanceConfig.RELATION_DRIFT_TRADER
            AiBehavior.NEUTRAL -> BalanceConfig.RELATION_DRIFT_NEUTRAL
            AiBehavior.ISOLATIONIST -> BalanceConfig.RELATION_DRIFT_ISOLATIONIST
        }

        if (country.hasEmbassy) drift += BalanceConfig.EMBASSY_RELATION_DRIFT
        if (country.hasTradeDeal) drift += BalanceConfig.TRADE_DEAL_RELATION_DRIFT

        drift -= state.axisForeign * BalanceConfig.AXIS_FOREIGN_RELATION_WEIGHT
        if (country.behavior == AiBehavior.TRADER) {
            drift -= state.axisEconomic * BalanceConfig.AXIS_ECONOMIC_RELATION_WEIGHT
        }

        if (debtStressed) drift -= BalanceConfig.DEBT_STRESS_RELATION_PENALTY

        return country.copy(relation = country.relation + drift)
    }

    private fun maybeOffer(
        state: GameState,
        country: DiplomacyState,
    ): Pair<DiplomaticOffer, DiplomacyState>? {
        if (state.turnNumber - country.lastOfferTurn < BalanceConfig.AI_OFFER_COOLDOWN) {
            return null
        }
        val type: TreatyType? = when {
            country.relation >= BalanceConfig.DEFENSIVE_RELATION_REQUIREMENT &&
                !country.isAlly -> TreatyType.DEFENSIVE
            country.relation >= BalanceConfig.TRADE_RELATION_REQUIREMENT &&
                !country.hasTradeDeal &&
                country.behavior == AiBehavior.TRADER -> TreatyType.TRADE
            country.relation >= BalanceConfig.NON_AGGRESSION_RELATION_REQUIREMENT &&
                TreatyType.NON_AGGRESSION !in country.treaties &&
                country.behavior == AiBehavior.HOSTILE -> TreatyType.NON_AGGRESSION
            else -> null
        }
        if (type == null) return null

        val updated = country.copy(lastOfferTurn = state.turnNumber)
        return DiplomaticOffer(
            countryId = country.countryId,
            countryName = country.nameAr,
            type = type,
        ) to updated
    }

    // ── Player actions ──

    fun relationRequirement(type: TreatyType): Double = when (type) {
        TreatyType.TRADE -> BalanceConfig.TRADE_RELATION_REQUIREMENT
        TreatyType.DEFENSIVE -> BalanceConfig.DEFENSIVE_RELATION_REQUIREMENT
        TreatyType.NON_AGGRESSION -> BalanceConfig.NON_AGGRESSION_RELATION_REQUIREMENT
        TreatyType.EMBASSY -> BalanceConfig.NON_AGGRESSION_RELATION_REQUIREMENT
    }

    fun signRelationBonus(type: TreatyType): Double = when (type) {
        TreatyType.TRADE -> BalanceConfig.TRADE_SIGN_RELATION_BONUS
        TreatyType.DEFENSIVE -> BalanceConfig.DEFENSIVE_SIGN_RELATION_BONUS
        TreatyType.NON_AGGRESSION -> BalanceConfig.NON_AGGRESSION_SIGN_RELATION_BONUS
        TreatyType.EMBASSY -> BalanceConfig.EMBASSY_SIGN_RELATION_BONUS
    }

    fun canSign(state: GameState, countryId: String, type: TreatyType): Boolean {
        val country = state.countries[countryId] ?: return false
        if (country.atWar) return false
        if (type in country.treaties) return false
        if (type == TreatyType.EMBASSY && state.treasuryCash < BalanceConfig.EMBASSY_COST) {
            return false
        }
        return country.relation >= relationRequirement(type)
    }

    fun signTreaty(
        state: GameState,
        countryId: String,
        type: TreatyType,
    ): GameState? {
        if (!canSign(state, countryId, type)) return null
        val country = state.countries[countryId]!!

        var next = state
        if (type == TreatyType.EMBASSY) {
            next = next.copy(treasuryCash = next.treasuryCash - BalanceConfig.EMBASSY_COST)
        }
        next = putCountry(
            next,
            country.copy(
                treaties = country.treaties + type,
                relation = country.relation + signRelationBonus(type),
            ),
        )
        next = next.plusHistory(
            HistoryEntry(
                turnNumber = next.turnNumber,
                date = next.inGameDate,
                title = "اتفاقية جديدة مع ${country.nameAr}",
                detail = treatyLabel(type),
                tag = HistoryTag.TREATY,
            ),
        )
        return next.clamped()
    }

    fun acceptOffer(state: GameState, offer: DiplomaticOffer): GameState? {
        val country = state.countries[offer.countryId] ?: return null
        if (country.atWar) return null
        var next = putCountry(
            state,
            country.copy(
                treaties = country.treaties + offer.type,
                relation = country.relation + signRelationBonus(offer.type),
            ),
        )
        next = next.plusHistory(
            HistoryEntry(
                turnNumber = next.turnNumber,
                date = next.inGameDate,
                title = "قبول عرض ${country.nameAr}",
                detail = treatyLabel(offer.type),
                tag = HistoryTag.TREATY,
            ),
        )
        return next.clamped()
    }

    fun breakTreaty(
        state: GameState,
        countryId: String,
        type: TreatyType,
    ): GameState {
        val country = state.countries[countryId] ?: return state
        if (type !in country.treaties) return state
        val next = putCountry(
            state,
            country.copy(
                treaties = country.treaties - type,
                relation = country.relation - signRelationBonus(type) * 2,
            ),
        )
        return next.clamped()
    }

    fun declareWar(state: GameState, countryId: String): GameState {
        val country = state.countries[countryId] ?: return state
        if (country.atWar) return state
        return startWar(state, country, declaredBy = state.countryName).first
    }

    fun treatyLabel(type: TreatyType): String = when (type) {
        TreatyType.TRADE -> "معاهدة تجارية"
        TreatyType.DEFENSIVE -> "تحالف دفاعي"
        TreatyType.NON_AGGRESSION -> "اتفاقية عدم اعتداء"
        TreatyType.EMBASSY -> "سفارة"
    }

    // ── War ──

    private fun startWar(
        state: GameState,
        country: DiplomacyState,
        declaredBy: String,
    ): Pair<GameState, DiplomacyState> {
        val updated = country.copy(
            atWar = true,
            warTurns = 0,
            relation = -100.0,
            treaties = country.treaties - TreatyType.TRADE -
                TreatyType.DEFENSIVE - TreatyType.NON_AGGRESSION,
        )
        var next = putCountry(state, updated)
        next = next.plusHistory(
            HistoryEntry(
                turnNumber = next.turnNumber,
                date = next.inGameDate,
                title = "إعلان حرب: ${country.nameAr}",
                detail = "أعلنها $declaredBy",
                tag = HistoryTag.WAR,
            ),
        )
        return next to updated
    }

    fun playerWarScore(state: GameState, stance: WarStance): Double {
        var score = (state.militarySecurity * BalanceConfig.WAR_MILITARY_WEIGHT) +
            (state.economy * BalanceConfig.WAR_ECONOMY_WEIGHT) +
            (state.technology * BalanceConfig.WAR_TECHNOLOGY_WEIGHT) +
            (state.publicSatisfaction * BalanceConfig.WAR_SATISFACTION_WEIGHT)

        score += when (stance) {
            WarStance.OFFENSIVE -> BalanceConfig.WAR_STANCE_OFFENSIVE_BONUS
            WarStance.DEFENSIVE -> BalanceConfig.WAR_STANCE_DEFENSIVE_BONUS
            WarStance.NEGOTIATE -> 0.0
        }

        val allies = state.countries.values.count { it.isAlly && !it.atWar }
        score += allies * BalanceConfig.ALLY_SUPPORT_BONUS
        return score
    }

    fun enemyWarScore(enemy: DiplomacyState): Double =
        (enemy.militaryPower * BalanceConfig.WAR_MILITARY_WEIGHT) +
            (enemy.economicPower *
                (BalanceConfig.WAR_ECONOMY_WEIGHT +
                    BalanceConfig.WAR_TECHNOLOGY_WEIGHT +
                    BalanceConfig.WAR_SATISFACTION_WEIGHT))

    private fun fightTurn(
        state: GameState,
        enemy: DiplomacyState,
        stance: WarStance,
    ): Triple<GameState, DiplomacyState, WarResult> {
        var c = enemy.copy(warTurns = enemy.warTurns + 1)
        var next = state

        val swing = BalanceConfig.WAR_RANDOM_SWING
        val playerRoll = (rng.nextDouble() * 2 - 1) * swing
        val enemyRoll = (rng.nextDouble() * 2 - 1) * swing

        val playerScore = playerWarScore(next, stance) + playerRoll
        val enemyScore = enemyWarScore(c) + enemyRoll

        val attrited = applyAttrition(next, c, stance)
        next = attrited.first
        c = attrited.second

        var outcome = WarOutcome.ONGOING
        val gap = playerScore - enemyScore

        if (stance == WarStance.NEGOTIATE &&
            c.warTurns >= BalanceConfig.WAR_MINIMUM_TURNS
        ) {
            outcome = WarOutcome.NEGOTIATED_PEACE
        } else if (c.warTurns >= BalanceConfig.WAR_MINIMUM_TURNS &&
            abs(gap) >= BalanceConfig.WAR_DECISIVE_GAP
        ) {
            outcome = if (gap > 0) WarOutcome.VICTORY else WarOutcome.DEFEAT
        } else if (c.warTurns >= BalanceConfig.WAR_MAXIMUM_TURNS) {
            outcome = WarOutcome.STALEMATE
        }

        next = putCountry(next, c)
        return Triple(
            next,
            c,
            WarResult(
                countryId = c.countryId,
                countryName = c.nameAr,
                outcome = outcome,
                playerScore = playerScore,
                enemyScore = enemyScore,
            ),
        )
    }

    private fun applyAttrition(
        state: GameState,
        enemy: DiplomacyState,
        stance: WarStance,
    ): Pair<GameState, DiplomacyState> {
        val factor = when (stance) {
            WarStance.DEFENSIVE -> BalanceConfig.WAR_STANCE_DEFENSIVE_ATTRITION_FACTOR
            WarStance.OFFENSIVE -> BalanceConfig.WAR_STANCE_OFFENSIVE_ATTRITION_FACTOR
            WarStance.NEGOTIATE -> 1.0
        }
        var next = state.plusDelta(
            StateKeys.MILITARY_SECURITY,
            -BalanceConfig.WAR_ATTRITION_MILITARY * factor,
        )
        next = next.plusDelta(
            StateKeys.ECONOMY,
            -BalanceConfig.WAR_ATTRITION_ECONOMY * factor,
        )
        next = next.copy(
            population = (
                next.population *
                    (1 - BalanceConfig.WAR_ATTRITION_POPULATION_RATE * factor)
                ).roundToInt(),
        )

        val c = enemy.copy(
            militaryPower = enemy.militaryPower - BalanceConfig.WAR_ATTRITION_MILITARY * 0.8,
            economicPower = enemy.economicPower - BalanceConfig.WAR_ATTRITION_ECONOMY * 0.8,
        )
        return next.clamped() to c
    }

    private fun settleWar(
        state: GameState,
        enemy: DiplomacyState,
        result: WarResult,
    ): Pair<GameState, DiplomacyState> {
        var c = enemy.copy(atWar = false, warTurns = 0)
        var next = state

        when (result.outcome) {
            WarOutcome.VICTORY -> {
                next = next.plusDelta(
                    StateKeys.ECONOMY,
                    BalanceConfig.WAR_VICTORY_ECONOMY_BONUS,
                )
                next = next.plusDelta(
                    StateKeys.PUBLIC_SATISFACTION,
                    BalanceConfig.WAR_VICTORY_SATISFACTION_BONUS,
                )
                next = next.copy(
                    treasuryCash = next.treasuryCash + BalanceConfig.WAR_VICTORY_REPARATIONS,
                )
                c = c.copy(relation = -60.0)
            }
            WarOutcome.DEFEAT -> {
                next = next.plusDelta(
                    StateKeys.ECONOMY,
                    -BalanceConfig.WAR_DEFEAT_ECONOMY_PENALTY,
                )
                next = next.plusDelta(
                    StateKeys.PUBLIC_SATISFACTION,
                    -BalanceConfig.WAR_DEFEAT_SATISFACTION_PENALTY,
                )
                next = next.copy(debt = next.debt + BalanceConfig.WAR_DEFEAT_REPARATIONS)
                c = c.copy(relation = -40.0)
            }
            WarOutcome.STALEMATE -> {
                next = next.plusDelta(
                    StateKeys.PUBLIC_SATISFACTION,
                    -BalanceConfig.WAR_STALEMATE_SATISFACTION_PENALTY,
                )
                c = c.copy(relation = -50.0)
            }
            WarOutcome.NEGOTIATED_PEACE -> {
                c = c.copy(
                    relation = -25.0,
                    treaties = c.treaties + TreatyType.NON_AGGRESSION,
                )
            }
            WarOutcome.ONGOING -> {
                next = putCountry(next, c)
                return next to c
            }
        }

        next = putCountry(next, c)
        next = next.plusHistory(
            HistoryEntry(
                turnNumber = next.turnNumber,
                date = next.inGameDate,
                title = "نهاية الحرب مع ${c.nameAr}",
                detail = outcomeLabel(result.outcome),
                tag = HistoryTag.WAR,
            ),
        )
        next = next.clamped()
        return next to c
    }

    fun outcomeLabel(outcome: WarOutcome): String = when (outcome) {
        WarOutcome.VICTORY -> "انتصار"
        WarOutcome.DEFEAT -> "هزيمة"
        WarOutcome.STALEMATE -> "استنزاف بلا حاسم"
        WarOutcome.NEGOTIATED_PEACE -> "سلام تفاوضي"
        WarOutcome.ONGOING -> "مستمرة"
    }
}

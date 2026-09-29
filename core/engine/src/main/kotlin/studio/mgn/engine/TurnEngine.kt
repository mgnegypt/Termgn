package studio.mgn.engine

import studio.mgn.model.Achievement
import studio.mgn.model.GameEvent
import studio.mgn.model.GameState
import studio.mgn.model.HistoryEntry
import studio.mgn.model.HistoryTag
import studio.mgn.model.LandmarkDef
import studio.mgn.model.MissionDef
import studio.mgn.model.Season
import studio.mgn.model.StateKeys
import studio.mgn.model.TurnLength
import studio.mgn.model.WarStance
import java.time.LocalDate
import kotlin.math.roundToLong
import java.util.Locale
import kotlin.random.Random

/** Summary of one resolved turn. */
data class TurnReport(
    val state: GameState,
    val turnNumber: Int,
    val date: LocalDate,
    val finance: TurnFinance,
    val diplomacy: DiplomacyTurnReport,
    val completedLandmarks: List<LandmarkDef>,
    val pendingEvents: List<GameEvent>,
    val newAchievements: List<Achievement>,
    val newMissions: List<MissionDef>,
    val yearlyReport: YearlyReport?,
    val referendum: ReferendumResult?,
)

/** End-of-year recap, also the main gem payout moment. */
data class YearlyReport(
    val year: Int,
    val nationScore: Double,
    val gemsAwarded: Int,
    val highlights: List<String>,
)

/** Outcome of the periodic legitimacy vote. */
data class ReferendumResult(
    val year: Int,
    val passed: Boolean,
    val legitimacy: Double,
)

/**
 * Orchestrates a single turn in a fixed, deterministic order.
 *
 * Order matters for balance: calendar → construction → finance → sectors →
 * growth → environment → society → population → diplomacy → events →
 * legitimacy → achievements.
 */
class TurnEngine(
    private val eventEngine: EventEngine,
    private val achievements: List<Achievement>,
    private val diplomacyEngine: DiplomacyEngine = DiplomacyEngine(),
    private val landmarkCatalog: Map<String, LandmarkDef> = emptyMap(),
    private val missions: List<MissionDef> = emptyList(),
    private val rng: Random = Random.Default,
) {
    val diplomacy: DiplomacyEngine get() = diplomacyEngine
    val events: EventEngine get() = eventEngine

    fun advance(
        state: GameState,
        stances: Map<String, WarStance> = emptyMap(),
    ): TurnReport {
        val previousYear = state.inGameDate.year

        var next = advanceCalendar(state)

        val construction = ConstructionEngine.advance(next, landmarkCatalog)
        next = construction.state
        val completed = construction.completed

        val finance = EconomyEngine.computeFinance(next, landmarkCatalog)
        next = EconomyEngine.applyFinance(next, finance)
        val debtStressed = EconomyEngine.isDebtStressed(next, finance)

        next = EconomyEngine.applySectorDrift(next, landmarkCatalog)
        next = EconomyEngine.applyEconomicGrowth(next, debtStressed = debtStressed)
        next = EconomyEngine.applyEnvironment(next)
        next = EconomyEngine.applySociety(next, finance, rng)
        next = EconomyEngine.applyPopulation(next)

        val diplomacyOutcome = diplomacyEngine.advanceTurn(
            next,
            stances = stances,
            debtStressed = debtStressed,
        )
        next = diplomacyOutcome.state

        val pending = eventEngine.drawForTurn(next)

        next = updateLegitimacy(next)

        var yearly: YearlyReport? = null
        var referendum: ReferendumResult? = null
        if (next.inGameDate.year != previousYear) {
            val yearlyOutcome = buildYearlyReport(next, previousYear)
            next = yearlyOutcome.state
            yearly = yearlyOutcome.report
            if (next.inGameDate.year % BalanceConfig.REFERENDUM_INTERVAL_YEARS == 0) {
                val ref = runReferendum(next)
                next = ref.state
                referendum = ref.result
            }
        }

        val achievementOutcome = checkAchievements(next)
        next = achievementOutcome.state

        val (afterMissions, newMissions) =
            MissionEngine.checkCompletions(next, missions)
        next = afterMissions.copy(
            rulerXp = afterMissions.rulerXp + BalanceConfig.RULER_XP_PER_TURN,
        )

        next = next.clamped()
        return TurnReport(
            state = next,
            turnNumber = next.turnNumber,
            date = next.inGameDate,
            finance = finance,
            diplomacy = diplomacyOutcome.report,
            completedLandmarks = completed,
            pendingEvents = pending,
            newAchievements = achievementOutcome.unlocked,
            newMissions = newMissions,
            yearlyReport = yearly,
            referendum = referendum,
        )
    }

    // ── Calendar ──

    private fun advanceCalendar(state: GameState): GameState {
        val date = when (state.turnLength) {
            TurnLength.DAY -> state.inGameDate.plusDays(1)
            TurnLength.WEEK -> state.inGameDate.plusDays(7)
            TurnLength.MONTH -> state.inGameDate.plusMonths(1)
        }
        return state.copy(
            turnNumber = state.turnNumber + 1,
            inGameDate = date,
            currentSeason = GameState.seasonForMonth(date.monthValue),
        )
    }

    // ── Legitimacy, reports, achievements ──

    private fun updateLegitimacy(state: GameState): GameState {
        // Exponential moving average toward (satisfaction*0.6 + score*0.4):
        // legitimacy += target - legitimacy*0.1 == (target*10 - legitimacy)*0.1
        val target =
            (state.publicSatisfaction * BalanceConfig.LEGITIMACY_SATISFACTION_WEIGHT) +
                (state.nationScore * BalanceConfig.LEGITIMACY_SCORE_WEIGHT)
        return state.copy(
            legitimacy = state.legitimacy + target - (state.legitimacy * 0.10),
        ).clamped()
    }

    private data class YearlyOutcome(val state: GameState, val report: YearlyReport)

    private fun buildYearlyReport(state: GameState, year: Int): YearlyOutcome {
        val score = state.nationScore
        val gems = BalanceConfig.YEARLY_REPORT_GEMS_BASE +
            ((score / 10).toInt() * BalanceConfig.YEARLY_REPORT_GEMS_BONUS_PER_TEN_SCORE)
        var next = state.copy(gems = state.gems + gems)

        val highlights = mutableListOf(
            "تقييم الدولة: ${fixed1(score)}/100",
            "الخزينة: ${next.treasuryCash.roundToLong()}",
            "السكان: ${next.population}",
            "المعالم المكتملة: ${next.builtLandmarks.size}",
        )
        if (next.isAtWar) highlights.add("الدولة في حالة حرب")
        if (next.sanctionCount > 0) highlights.add("عقوبات دولية: ${next.sanctionCount}")

        next = next.plusHistory(
            HistoryEntry(
                turnNumber = next.turnNumber,
                date = next.inGameDate,
                title = "التقرير السنوي — $year",
                detail = highlights.joinToString(" • "),
                tag = HistoryTag.YEARLY_REPORT,
            ),
        )
        return YearlyOutcome(
            state = next,
            report = YearlyReport(
                year = year,
                nationScore = score,
                gemsAwarded = gems,
                highlights = highlights,
            ),
        )
    }

    private data class ReferendumOutcome(val state: GameState, val result: ReferendumResult)

    private fun runReferendum(state: GameState): ReferendumOutcome {
        var next = state
        val passed = next.legitimacy >= BalanceConfig.REFERENDUM_PASS_THRESHOLD
        if (!passed) {
            next = next.plusDelta(
                StateKeys.PUBLIC_SATISFACTION,
                -BalanceConfig.REFERENDUM_FAILURE_PENALTY * 0.5,
            )
            next = next.copy(
                legitimacy = next.legitimacy - BalanceConfig.REFERENDUM_FAILURE_PENALTY,
            )
        }
        next = next.plusHistory(
            HistoryEntry(
                turnNumber = next.turnNumber,
                date = next.inGameDate,
                title = "تقييم شرعية الحكم",
                detail = if (passed) "تم تجديد الثقة" else "فقدان جزء من الشرعية",
                tag = HistoryTag.INFO,
            ),
        )
        next = next.clamped()
        return ReferendumOutcome(
            state = next,
            result = ReferendumResult(
                year = next.inGameDate.year,
                passed = passed,
                legitimacy = next.legitimacy,
            ),
        )
    }

    private data class AchievementOutcome(
        val state: GameState,
        val unlocked: List<Achievement>,
    )

    private fun checkAchievements(state: GameState): AchievementOutcome {
        var next = state
        val unlocked = mutableListOf<Achievement>()
        for (achievement in achievements) {
            if (achievement.id in next.unlockedAchievements) continue
            if (!ConditionEvaluator.evaluate(achievement.condition, next)) continue

            next = next.copy(
                unlockedAchievements = next.unlockedAchievements + achievement.id,
                gems = next.gems + achievement.gemReward,
            )
            unlocked.add(achievement)

            next = next.plusHistory(
                HistoryEntry(
                    turnNumber = next.turnNumber,
                    date = next.inGameDate,
                    title = "إنجاز: ${achievement.titleAr}",
                    detail = achievement.descriptionAr,
                    tag = HistoryTag.ACHIEVEMENT,
                ),
            )
        }
        return AchievementOutcome(state = next, unlocked = unlocked)
    }

    private fun fixed1(value: Double): String =
        String.format(Locale.ROOT, "%.1f", value)
}

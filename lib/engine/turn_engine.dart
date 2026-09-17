import 'dart:math' as math;

import '../models/achievement.dart';
import '../models/enums.dart';
import '../models/event.dart';
import '../models/game_state.dart';
import '../models/history_entry.dart';
import '../models/landmark.dart';
import 'balance_config.dart';
import 'construction_engine.dart';
import 'diplomacy_engine.dart';
import 'economy_calculator.dart';
import 'event_engine.dart';

/// Summary of one resolved turn, consumed by the dashboard's turn-report sheet.
class TurnReport {
  const TurnReport({
    required this.turnNumber,
    required this.date,
    required this.finance,
    required this.diplomacy,
    required this.completedLandmarks,
    required this.pendingEvents,
    required this.newAchievements,
    this.yearlyReport,
    this.referendum,
  });

  final int turnNumber;
  final DateTime date;
  final TurnFinance finance;
  final DiplomacyTurnReport diplomacy;
  final List<Landmark> completedLandmarks;

  /// Events awaiting a player decision; the dashboard routes to the decision
  /// screen while this is non-empty.
  final List<GameEvent> pendingEvents;

  final List<Achievement> newAchievements;
  final YearlyReport? yearlyReport;
  final ReferendumResult? referendum;
}

/// End-of-year recap, also the main gem payout moment.
class YearlyReport {
  const YearlyReport({
    required this.year,
    required this.nationScore,
    required this.gemsAwarded,
    required this.highlights,
  });

  final int year;
  final double nationScore;
  final int gemsAwarded;
  final List<String> highlights;
}

/// Outcome of the periodic legitimacy vote.
class ReferendumResult {
  const ReferendumResult({
    required this.year,
    required this.passed,
    required this.legitimacy,
  });

  final int year;
  final bool passed;
  final double legitimacy;
}

/// Orchestrates a single turn in a fixed, deterministic order.
///
/// Order matters for balance: calendar → construction → finance → sectors →
/// growth → environment → society → population → diplomacy → events →
/// legitimacy → achievements.
class TurnEngine {
  TurnEngine({
    required EventEngine eventEngine,
    required List<Achievement> achievements,
    DiplomacyEngine? diplomacyEngine,
    math.Random? rng,
  })  : _events = eventEngine,
        _achievements = achievements,
        _diplomacy = diplomacyEngine ?? DiplomacyEngine(rng: rng),
        _rng = rng ?? math.Random();

  final EventEngine _events;
  final DiplomacyEngine _diplomacy;
  final List<Achievement> _achievements;
  final math.Random _rng;

  DiplomacyEngine get diplomacy => _diplomacy;
  EventEngine get events => _events;

  TurnReport advance(
    GameState state, {
    Map<String, WarStance> stances = const <String, WarStance>{},
  }) {
    final int previousYear = state.inGameDate.year;

    _advanceCalendar(state);

    final List<Landmark> completed = ConstructionEngine.advance(state);

    final TurnFinance finance = EconomyCalculator.computeFinance(state);
    EconomyCalculator.applyFinance(state, finance);
    final bool debtStressed = EconomyCalculator.isDebtStressed(state, finance);

    EconomyCalculator.applySectorDrift(state);
    EconomyCalculator.applyEconomicGrowth(state, debtStressed: debtStressed);
    EconomyCalculator.applyEnvironment(state);
    EconomyCalculator.applySociety(state, finance, _rng);
    EconomyCalculator.applyPopulation(state);

    final DiplomacyTurnReport diplomacyReport = _diplomacy.advanceTurn(
      state,
      stances: stances,
      debtStressed: debtStressed,
    );

    final List<GameEvent> pending = _events.drawForTurn(state);

    _updateLegitimacy(state);

    YearlyReport? yearly;
    ReferendumResult? referendum;
    if (state.inGameDate.year != previousYear) {
      yearly = _buildYearlyReport(state, previousYear);
      if (state.inGameDate.year % BalanceConfig.referendumIntervalYears == 0) {
        referendum = _runReferendum(state);
      }
    }

    final List<Achievement> unlocked = _checkAchievements(state);

    state.clampAll();
    return TurnReport(
      turnNumber: state.turnNumber,
      date: state.inGameDate,
      finance: finance,
      diplomacy: diplomacyReport,
      completedLandmarks: completed,
      pendingEvents: pending,
      newAchievements: unlocked,
      yearlyReport: yearly,
      referendum: referendum,
    );
  }

  // ── Calendar ──────────────────────────────────────────────────────────────

  void _advanceCalendar(GameState state) {
    state.turnNumber += 1;
    state.inGameDate = switch (state.turnLength) {
      TurnLength.day => state.inGameDate.add(const Duration(days: 1)),
      TurnLength.week => state.inGameDate.add(const Duration(days: 7)),
      TurnLength.month => _addMonth(state.inGameDate),
    };
    state.currentSeason = GameState.seasonForMonth(state.inGameDate.month);
  }

  static DateTime _addMonth(DateTime date) {
    final int year = date.month == 12 ? date.year + 1 : date.year;
    final int month = date.month == 12 ? 1 : date.month + 1;
    final int lastDay = DateTime(year, month + 1, 0).day;
    return DateTime(year, month, math.min(date.day, lastDay));
  }

  // ── Legitimacy, reports, achievements ─────────────────────────────────────

  void _updateLegitimacy(GameState state) {
    final double target =
        (state.publicSatisfaction * BalanceConfig.legitimacySatisfactionWeight) +
            (state.nationScore * BalanceConfig.legitimacyScoreWeight);
    state.legitimacy += target - (state.legitimacy * 0.10);
    state.clampAll();
  }

  YearlyReport _buildYearlyReport(GameState state, int year) {
    final double score = state.nationScore;
    final int gems = BalanceConfig.yearlyReportGemsBase +
        ((score / 10).floor() * BalanceConfig.yearlyReportGemsBonusPerTenScore);
    state.gems += gems;

    final List<String> highlights = <String>[
      'تقييم الدولة: ${score.toStringAsFixed(1)}/100',
      'الخزينة: ${state.treasuryCash.round()}',
      'السكان: ${state.population}',
      'المعالم المكتملة: ${state.builtLandmarks.length}',
      if (state.isAtWar) 'الدولة في حالة حرب',
      if (state.sanctionCount > 0) 'عقوبات دولية: ${state.sanctionCount}',
    ];

    state.history.add(HistoryEntry(
      turnNumber: state.turnNumber,
      date: state.inGameDate,
      title: 'التقرير السنوي — $year',
      detail: highlights.join(' • '),
      tag: HistoryTag.yearlyReport,
    ));

    return YearlyReport(
      year: year,
      nationScore: score,
      gemsAwarded: gems,
      highlights: highlights,
    );
  }

  ReferendumResult _runReferendum(GameState state) {
    final bool passed =
        state.legitimacy >= BalanceConfig.referendumPassThreshold;
    if (!passed) {
      state.applyDelta(
        StateKeys.publicSatisfaction,
        -BalanceConfig.referendumFailurePenalty * 0.5,
      );
      state.legitimacy -= BalanceConfig.referendumFailurePenalty;
    }
    state.history.add(HistoryEntry(
      turnNumber: state.turnNumber,
      date: state.inGameDate,
      title: 'تقييم شرعية الحكم',
      detail: passed ? 'تم تجديد الثقة' : 'فقدان جزء من الشرعية',
      tag: HistoryTag.info,
    ));
    state.clampAll();
    return ReferendumResult(
      year: state.inGameDate.year,
      passed: passed,
      legitimacy: state.legitimacy,
    );
  }

  List<Achievement> _checkAchievements(GameState state) {
    final List<Achievement> unlocked = <Achievement>[];
    for (final Achievement achievement in _achievements) {
      if (state.unlockedAchievements.contains(achievement.id)) continue;
      if (!achievement.condition(state)) continue;

      state.unlockedAchievements.add(achievement.id);
      state.gems += achievement.gemReward;
      unlocked.add(achievement);

      state.history.add(HistoryEntry(
        turnNumber: state.turnNumber,
        date: state.inGameDate,
        title: 'إنجاز: ${achievement.titleAr}',
        detail: achievement.descriptionAr,
        tag: HistoryTag.achievement,
      ));
    }
    return unlocked;
  }
}

import 'dart:math' as math;

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/achievements_data.dart';
import '../data/countries_data.dart';
import '../data/events_data.dart';
import '../engine/balance_config.dart';
import '../engine/construction_engine.dart';
import '../engine/diplomacy_engine.dart';
import '../engine/economy_calculator.dart';
import '../engine/event_engine.dart';
import '../engine/turn_engine.dart';
import '../models/enums.dart';
import '../models/event.dart';
import '../models/game_state.dart';
import '../models/landmark.dart';
import '../storage/hive_boxes.dart';

/// Everything the UI needs to render one moment of play.
class GameSession {
  const GameSession({
    required this.state,
    required this.lastReport,
    required this.pendingEvents,
    required this.lastResolution,
  });

  final GameState state;

  /// Report from the most recently resolved turn; null before the first turn.
  final TurnReport? lastReport;

  /// Decisions still awaiting the player. The dashboard blocks the
  /// "next turn" button while this is non-empty.
  final List<GameEvent> pendingEvents;

  /// Result of the last resolved decision, for the result panel.
  final EventResolution? lastResolution;

  GameSession copyWith({
    GameState? state,
    TurnReport? lastReport,
    List<GameEvent>? pendingEvents,
    EventResolution? lastResolution,
    bool clearResolution = false,
  }) {
    return GameSession(
      state: state ?? this.state,
      lastReport: lastReport ?? this.lastReport,
      pendingEvents: pendingEvents ?? this.pendingEvents,
      lastResolution:
          clearResolution ? null : (lastResolution ?? this.lastResolution),
    );
  }

  GameEvent? get currentEvent =>
      pendingEvents.isEmpty ? null : pendingEvents.first;

  bool get awaitingDecision => pendingEvents.isNotEmpty;
}

/// Owns the single active save and mediates every player action.
///
/// The UI never touches the engines directly: it reads [GameSession] and calls
/// the methods here, which mutate the state, persist it, and publish a new
/// immutable snapshot reference.
class GameController extends Notifier<GameSession?> {
  late final math.Random _rng = math.Random();
  late final EventEngine _events = EventEngine(pool: EventsData.all, rng: _rng);
  late final TurnEngine _turns = TurnEngine(
    eventEngine: _events,
    achievements: AchievementsData.all,
    rng: _rng,
  );

  /// War stance per enemy, chosen on the diplomacy screen, applied next turn.
  final Map<String, WarStance> _stances = <String, WarStance>{};

  DiplomacyEngine get diplomacy => _turns.diplomacy;

  @override
  GameSession? build() {
    final GameState? saved = GameStorage.hasSave ? GameStorage.loadGame() : null;
    if (saved == null) return null;
    return GameSession(
      state: saved,
      lastReport: null,
      pendingEvents: const <GameEvent>[],
      lastResolution: null,
    );
  }

  // ── Lifecycle ─────────────────────────────────────────────────────────────

  Future<void> startNewGame({
    required String countryName,
    required String rulerTitle,
    required TurnLength turnLength,
    required int flagPrimaryColor,
    required int flagSecondaryColor,
    required String flagSymbolId,
    double axisEconomic = 0,
    double axisSocial = 0,
    double axisForeign = 0,
  }) async {
    final GameState fresh = GameState.newGame(
      countryName: countryName,
      rulerTitle: rulerTitle,
      turnLength: turnLength,
      countries: CountriesData.initialWorld(),
      flagPrimaryColor: flagPrimaryColor,
      flagSecondaryColor: flagSecondaryColor,
      flagSymbolId: flagSymbolId,
      axisEconomic: axisEconomic,
      axisSocial: axisSocial,
      axisForeign: axisForeign,
    );
    state = GameSession(
      state: fresh,
      lastReport: null,
      pendingEvents: const <GameEvent>[],
      lastResolution: null,
    );
    await GameStorage.saveGame(fresh);
  }

  Future<void> deleteSave() async {
    await GameStorage.deleteSave();
    state = null;
  }

  Future<void> save() async {
    final GameSession? session = state;
    if (session != null) await GameStorage.saveGame(session.state);
  }

  // ── Turn loop ─────────────────────────────────────────────────────────────

  /// Advances one turn. Refuses while a decision is pending so the player
  /// cannot skip consequences.
  Future<TurnReport?> nextTurn() async {
    final GameSession? session = state;
    if (session == null || session.awaitingDecision) return null;

    final TurnReport report = _turns.advance(
      session.state,
      stances: Map<String, WarStance>.of(_stances),
    );
    _stances.clear();

    state = session.copyWith(
      lastReport: report,
      pendingEvents: report.pendingEvents,
      clearResolution: true,
    );
    await save();
    return report;
  }

  /// Applies the player's pick for the current decision.
  Future<EventResolution?> choose(GameEvent event, EventChoice choice) async {
    final GameSession? session = state;
    if (session == null || !choice.isSelectable(session.state)) return null;

    final EventResolution resolution =
        _events.applyChoice(session.state, event, choice);
    final List<GameEvent> remaining = session.pendingEvents
        .where((GameEvent e) => e.id != event.id)
        .toList(growable: false);

    state = session.copyWith(
      pendingEvents: remaining,
      lastResolution: resolution,
    );
    await save();
    return resolution;
  }

  /// Spends gems to swap the current decision for another one.
  Future<bool> rerollDecision() async {
    final GameSession? session = state;
    final GameEvent? current = session?.currentEvent;
    if (session == null || current == null) return false;
    if (session.state.gems < BalanceConfig.gemsPerDecisionReroll) return false;

    final GameEvent? replacement = _events.reroll(session.state, current);
    if (replacement == null) return false;

    session.state.gems -= BalanceConfig.gemsPerDecisionReroll;
    final List<GameEvent> updated = <GameEvent>[
      replacement,
      ...session.pendingEvents.where((GameEvent e) => e.id != current.id),
    ];
    state = session.copyWith(pendingEvents: updated);
    await save();
    return true;
  }

  // ── Player actions ────────────────────────────────────────────────────────

  Future<void> setTaxRate(double value) async {
    final GameSession? session = state;
    if (session == null) return;
    session.state.taxRate = value;
    session.state.clampAll();
    state = session.copyWith();
    await save();
  }

  Future<void> setMilitarySpending(double value) async {
    final GameSession? session = state;
    if (session == null) return;
    session.state.militarySpendingLevel = value;
    session.state.clampAll();
    state = session.copyWith();
    await save();
  }

  Future<void> setSubsidyLevel(double value) async {
    final GameSession? session = state;
    if (session == null) return;
    session.state.subsidyLevel = value;
    session.state.clampAll();
    state = session.copyWith();
    await save();
  }

  Future<bool> investInSector(String sectorKey, double points) async {
    final GameSession? session = state;
    if (session == null) return false;
    final bool ok =
        EconomyCalculator.investInSector(session.state, sectorKey, points);
    if (ok) {
      state = session.copyWith();
      await save();
    }
    return ok;
  }

  Future<bool> startConstruction(Landmark landmark) async {
    final GameSession? session = state;
    if (session == null) return false;
    final bool ok = ConstructionEngine.start(session.state, landmark);
    if (ok) {
      state = session.copyWith();
      await save();
    }
    return ok;
  }

  Future<bool> rushConstruction(String landmarkId, int turns) async {
    final GameSession? session = state;
    if (session == null) return false;
    final bool ok =
        ConstructionEngine.rushWithGems(session.state, landmarkId, turns);
    if (ok) {
      state = session.copyWith();
      await save();
    }
    return ok;
  }

  Future<bool> signTreaty(String countryId, TreatyType type) async {
    final GameSession? session = state;
    if (session == null) return false;
    final bool ok = diplomacy.signTreaty(session.state, countryId, type);
    if (ok) {
      state = session.copyWith();
      await save();
    }
    return ok;
  }

  Future<void> breakTreaty(String countryId, TreatyType type) async {
    final GameSession? session = state;
    if (session == null) return;
    diplomacy.breakTreaty(session.state, countryId, type);
    state = session.copyWith();
    await save();
  }

  Future<void> declareWar(String countryId) async {
    final GameSession? session = state;
    if (session == null) return;
    diplomacy.declareWar(session.state, countryId);
    state = session.copyWith();
    await save();
  }

  /// Sets the stance used for [countryId] when the next turn resolves.
  void setWarStance(String countryId, WarStance stance) {
    _stances[countryId] = stance;
    final GameSession? session = state;
    if (session != null) state = session.copyWith();
  }

  WarStance stanceFor(String countryId) =>
      _stances[countryId] ?? WarStance.defensive;

  Future<void> takeLoan(double amount) async {
    final GameSession? session = state;
    if (session == null) return;
    EconomyCalculator.takeLoan(session.state, amount);
    state = session.copyWith();
    await save();
  }

  Future<void> repayDebt(double amount) async {
    final GameSession? session = state;
    if (session == null) return;
    EconomyCalculator.repayDebt(session.state, amount);
    state = session.copyWith();
    await save();
  }
}

final NotifierProvider<GameController, GameSession?> gameProvider =
    NotifierProvider<GameController, GameSession?>(GameController.new);

/// Convenience: the current finance projection for the treasury panel.
final Provider<TurnFinance?> financeProvider = Provider<TurnFinance?>((ref) {
  final GameSession? session = ref.watch(gameProvider);
  if (session == null) return null;
  return EconomyCalculator.computeFinance(session.state);
});

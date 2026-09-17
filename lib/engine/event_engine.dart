import 'dart:math' as math;

import '../models/diplomacy_state.dart';
import '../models/event.dart';
import '../models/game_state.dart';
import '../models/history_entry.dart';
import 'balance_config.dart';
import 'economy_calculator.dart';

/// Result of resolving one decision, handed to the UI for its result panel.
class EventResolution {
  const EventResolution({
    required this.event,
    required this.choice,
    required this.appliedEffects,
  });

  final GameEvent event;
  final EventChoice choice;

  /// The effects actually applied, after clamping, for the "what changed"
  /// summary.
  final Map<String, double> appliedEffects;
}

/// Selects which events fire each turn and applies the player's choices.
///
/// The engine owns no content: the pool is injected, so content files stay
/// pure data.
class EventEngine {
  EventEngine({required List<GameEvent> pool, math.Random? rng})
      : _pool = pool,
        _rng = rng ?? math.Random();

  final List<GameEvent> _pool;
  final math.Random _rng;

  List<GameEvent> get pool => List<GameEvent>.unmodifiable(_pool);

  /// Draws the events for the current turn, respecting conditions, cooldowns
  /// and crisis priority.
  List<GameEvent> drawForTurn(GameState state) {
    if (state.turnNumber < BalanceConfig.firstEventTurn) {
      return const <GameEvent>[];
    }

    final List<GameEvent> eligible = _pool
        .where((GameEvent e) => _isEligible(e, state))
        .toList(growable: false);
    if (eligible.isEmpty) return const <GameEvent>[];

    final List<GameEvent> drawn = <GameEvent>[];

    // Crises get first pick when any of them qualifies.
    final List<GameEvent> crises =
        eligible.where((GameEvent e) => e.isCrisis).toList(growable: false);
    if (crises.isNotEmpty &&
        _rng.nextDouble() < BalanceConfig.crisisPriorityChance) {
      final GameEvent? crisis = _weightedPick(crises);
      if (crisis != null) drawn.add(crisis);
    }

    if (drawn.isEmpty) {
      final GameEvent? first = _weightedPick(eligible);
      if (first != null) drawn.add(first);
    }

    final bool wantsSecond = drawn.length < BalanceConfig.maxEventsPerTurn &&
        _rng.nextDouble() < BalanceConfig.secondEventChance;
    if (wantsSecond) {
      final List<GameEvent> rest = eligible
          .where((GameEvent e) => !drawn.contains(e) && !e.isCrisis)
          .toList(growable: false);
      final GameEvent? second = _weightedPick(rest);
      if (second != null) drawn.add(second);
    }

    return drawn;
  }

  bool _isEligible(GameEvent event, GameState state) {
    if (event.oncePerGame && state.firedOnceEvents.contains(event.id)) {
      return false;
    }
    final int? lastTurn = state.lastEventTurns[event.id];
    if (lastTurn != null) {
      final int cooldown = event.cooldownTurns ??
          (event.isCrisis
              ? BalanceConfig.crisisCooldown
              : BalanceConfig.defaultEventCooldown);
      if (state.turnNumber - lastTurn < cooldown) return false;
    }
    if (!event.matches(state)) return false;
    // An event whose every choice is unaffordable would be a dead end.
    return event.choices.any((EventChoice c) => c.isSelectable(state));
  }

  GameEvent? _weightedPick(List<GameEvent> candidates) {
    if (candidates.isEmpty) return null;
    final int total = candidates.fold<int>(
      0,
      (int sum, GameEvent e) => sum + math.max(1, e.weight),
    );
    int roll = _rng.nextInt(total);
    for (final GameEvent candidate in candidates) {
      roll -= math.max(1, candidate.weight);
      if (roll < 0) return candidate;
    }
    return candidates.last;
  }

  /// Applies a chosen option: costs, state effects, relation effects, and the
  /// bookkeeping that powers cooldowns.
  EventResolution applyChoice(
    GameState state,
    GameEvent event,
    EventChoice choice,
  ) {
    state.treasuryCash -= choice.effectiveCostCash;
    state.gems -= choice.costGems;

    final Map<String, double> before = <String, double>{
      for (final String key in choice.stateEffects.keys) key: state.readKey(key),
    };

    state.applyEffects(choice.stateEffects);

    final Map<String, double> applied = <String, double>{
      for (final MapEntry<String, double> e in before.entries)
        if (state.readKey(e.key) != e.value)
          e.key: state.readKey(e.key) - e.value,
    };

    choice.relationEffects.forEach((String countryId, double delta) {
      state.countries[countryId]?.relation += delta;
    });
    if (choice.allRelationsDelta != 0) {
      for (final DiplomacyState d in state.countries.values) {
        d.relation += choice.allRelationsDelta;
      }
    }

    state.lastEventTurns[event.id] = state.turnNumber;
    if (event.oncePerGame) state.firedOnceEvents.add(event.id);

    state.history.add(HistoryEntry(
      turnNumber: state.turnNumber,
      date: state.inGameDate,
      title: event.title,
      detail: '${choice.label} — ${choice.resultText}',
      tag: event.isCrisis ? HistoryTag.crisis : HistoryTag.decision,
      deltas: applied,
    ));

    state.clampAll();
    EconomyCalculator.settleNegativeTreasury(state);
    return EventResolution(
      event: event,
      choice: choice,
      appliedEffects: applied,
    );
  }

  /// Draws a replacement event, used by the gem-funded re-roll.
  GameEvent? reroll(GameState state, GameEvent current) {
    final List<GameEvent> candidates = _pool
        .where((GameEvent e) => e.id != current.id && _isEligible(e, state))
        .toList(growable: false);
    return _weightedPick(candidates);
  }
}

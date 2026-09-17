import '../engine/balance_config.dart';
import 'enums.dart';
import 'game_state.dart';

/// One option the player may pick when an event fires.
class EventChoice {
  const EventChoice({
    required this.label,
    required this.resultText,
    this.stateEffects = const <String, double>{},
    this.costCash = 0,
    this.costGems = 0,
    this.relationEffects = const <String, double>{},
    this.allRelationsDelta = 0,
    this.isAvailable,
  });

  final String label;

  /// Narrative text shown after the choice is applied.
  final String resultText;

  /// Deltas keyed by [StateKeys], e.g. `{'economy': -5,
  /// 'publicSatisfaction': 10}`.
  final Map<String, double> stateEffects;

  /// Soft/hard currency the choice costs up front.
  final double costCash;
  final int costGems;

  /// Relation deltas for specific countries, keyed by country id.
  final Map<String, double> relationEffects;

  /// Relation delta applied to every known country.
  final double allRelationsDelta;

  /// Extra gate beyond affordability (e.g. requires an existing alliance).
  final bool Function(GameState state)? isAvailable;

  /// The cash actually charged, after the global decision-cost multiplier.
  double get effectiveCostCash =>
      costCash * BalanceConfig.decisionCostMultiplier;

  bool canAfford(GameState state) =>
      state.treasuryCash >= effectiveCostCash && state.gems >= costGems;

  bool isSelectable(GameState state) =>
      canAfford(state) && (isAvailable?.call(state) ?? true);
}

/// An authored event definition. Definitions are static content; only the
/// firing history is persisted.
class GameEvent {
  const GameEvent({
    required this.id,
    required this.title,
    required this.description,
    required this.category,
    required this.choices,
    this.condition,
    this.weight = 10,
    this.cooldownTurns,
    this.oncePerGame = false,
    this.minTurn = 0,
  });

  final String id;
  final String title;
  final String description;
  final EventCategory category;

  /// Appearance condition. `null` means "always eligible".
  final bool Function(GameState state)? condition;

  /// Relative probability weight inside the eligible pool.
  final int weight;

  /// Per-event cooldown override; falls back to the global default.
  final int? cooldownTurns;

  /// Events that must never repeat within a single save.
  final bool oncePerGame;

  /// Earliest turn the event may fire, used to keep the opening turns calm.
  final int minTurn;

  final List<EventChoice> choices;

  bool get isCrisis => category == EventCategory.crisis;

  bool matches(GameState state) =>
      state.turnNumber >= minTurn && (condition?.call(state) ?? true);
}

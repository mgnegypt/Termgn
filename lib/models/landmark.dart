import 'enums.dart';

/// A buildable landmark (wonder).
///
/// The same class serves three roles:
///  * an immutable catalogue definition (in `data/landmarks_data.dart`),
///  * an in-progress build (`turnsRemaining != null`) inside
///    `GameState.underConstruction`,
///  * a completed build inside `GameState.builtLandmarks`.
class Landmark {
  Landmark({
    required this.id,
    required this.nameAr,
    required this.descriptionAr,
    required this.kind,
    required this.costCash,
    required this.buildTurns,
    this.onCompleteEffects = const <String, double>{},
    this.perTurnEffects = const <String, double>{},
    this.tourismIncomePerTurn = 0,
    this.maintenancePerTurn = 0,
    this.requirements = const <String, double>{},
    this.era = Era.modern,
    this.turnsRemaining,
  });

  final String id;
  final String nameAr;
  final String descriptionAr;
  final LandmarkKind kind;

  /// One-off construction cost in soft currency.
  final double costCash;

  /// Number of turns the construction takes.
  final int buildTurns;

  /// Applied once, permanently, when construction completes.
  final Map<String, double> onCompleteEffects;

  /// Applied every turn while the landmark stands (small drift values).
  final Map<String, double> perTurnEffects;

  /// Direct tourism revenue contribution per turn, in soft currency.
  final double tourismIncomePerTurn;

  /// Upkeep drawn from the treasury every turn.
  final double maintenancePerTurn;

  /// Minimum state values required before construction may start,
  /// e.g. `{'technology': 40}`.
  final Map<String, double> requirements;

  final Era era;

  /// Remaining build turns; `null` for catalogue definitions and completed
  /// landmarks.
  final int? turnsRemaining;

  bool get isComplete => turnsRemaining != null && turnsRemaining! <= 0;

  Landmark copyWith({int? turnsRemaining}) {
    return Landmark(
      id: id,
      nameAr: nameAr,
      descriptionAr: descriptionAr,
      kind: kind,
      costCash: costCash,
      buildTurns: buildTurns,
      onCompleteEffects: onCompleteEffects,
      perTurnEffects: perTurnEffects,
      tourismIncomePerTurn: tourismIncomePerTurn,
      maintenancePerTurn: maintenancePerTurn,
      requirements: requirements,
      era: era,
      turnsRemaining: turnsRemaining ?? this.turnsRemaining,
    );
  }

  /// Persisted form. Only identity and progress are stored; static design data
  /// is re-hydrated from the catalogue on load.
  Map<String, Object?> toMap() => <String, Object?>{
        'id': id,
        'turnsRemaining': turnsRemaining,
      };
}

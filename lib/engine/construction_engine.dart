import '../models/game_state.dart';
import '../models/history_entry.dart';
import '../models/landmark.dart';
import 'balance_config.dart';

/// Landmark construction: eligibility, start, per-turn progress, completion.
abstract final class ConstructionEngine {
  /// True when every requirement on the landmark is met and it is not already
  /// built or queued.
  static bool meetsRequirements(GameState state, Landmark landmark) {
    if (state.builtLandmarks.any((Landmark l) => l.id == landmark.id)) {
      return false;
    }
    if (state.underConstruction.any((Landmark l) => l.id == landmark.id)) {
      return false;
    }
    if (landmark.era.index > state.era.index) return false;
    for (final MapEntry<String, double> req in landmark.requirements.entries) {
      if (state.readKey(req.key) < req.value) return false;
    }
    return true;
  }

  static bool canStart(GameState state, Landmark landmark) =>
      meetsRequirements(state, landmark) &&
      state.treasuryCash >= landmark.costCash;

  /// Pays for a landmark and queues it. Returns false when not allowed.
  static bool start(GameState state, Landmark landmark) {
    if (!canStart(state, landmark)) return false;
    state.treasuryCash -= landmark.costCash;
    state.underConstruction
        .add(landmark.copyWith(turnsRemaining: landmark.buildTurns));
    state.history.add(HistoryEntry(
      turnNumber: state.turnNumber,
      date: state.inGameDate,
      title: 'بدء بناء: ${landmark.nameAr}',
      detail: 'التكلفة: ${landmark.costCash.round()} — المدة: '
          '${landmark.buildTurns} دور',
      tag: HistoryTag.construction,
    ));
    state.clampAll();
    return true;
  }

  /// Advances every queued build by one turn and returns those that finished.
  static List<Landmark> advance(GameState state) {
    final List<Landmark> completed = <Landmark>[];

    for (int i = state.underConstruction.length - 1; i >= 0; i--) {
      final Landmark build = state.underConstruction[i];
      final int remaining = (build.turnsRemaining ?? 0) - 1;

      if (remaining > 0) {
        state.underConstruction[i] = build.copyWith(turnsRemaining: remaining);
        continue;
      }

      state.underConstruction.removeAt(i);
      final Landmark finished = build.copyWith(turnsRemaining: 0);
      state.builtLandmarks.add(finished);
      finished.onCompleteEffects.forEach(state.applyDelta);
      completed.add(finished);

      state.history.add(HistoryEntry(
        turnNumber: state.turnNumber,
        date: state.inGameDate,
        title: 'اكتمل بناء: ${finished.nameAr}',
        detail: finished.descriptionAr,
        tag: HistoryTag.construction,
        deltas: finished.onCompleteEffects,
      ));
    }

    state.clampAll();
    return completed;
  }

  /// Spends gems to shave [turns] off a queued build.
  static bool rushWithGems(GameState state, String landmarkId, int turns) {
    final int index =
        state.underConstruction.indexWhere((Landmark l) => l.id == landmarkId);
    if (index < 0 || turns <= 0) return false;

    final Landmark build = state.underConstruction[index];
    final int remaining = build.turnsRemaining ?? 0;
    final int skipped = turns > remaining ? remaining : turns;
    final int cost = skipped * BalanceConfig.gemsPerConstructionTurnSkip;
    if (state.gems < cost) return false;

    state.gems -= cost;
    state.underConstruction[index] =
        build.copyWith(turnsRemaining: remaining - skipped);
    return true;
  }
}

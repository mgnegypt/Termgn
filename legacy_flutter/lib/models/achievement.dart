import 'game_state.dart';

/// An unlockable achievement. Definitions live in `data/achievements_data.dart`;
/// only unlocked ids are persisted.
class Achievement {
  const Achievement({
    required this.id,
    required this.titleAr,
    required this.descriptionAr,
    required this.condition,
    this.gemReward = 0,
    this.hidden = false,
  });

  final String id;
  final String titleAr;
  final String descriptionAr;

  /// Evaluated after every turn; true means "unlock now".
  final bool Function(GameState state) condition;

  /// Hard-currency reward granted once on unlock.
  final int gemReward;

  /// Hidden achievements stay masked in the list until unlocked.
  final bool hidden;
}

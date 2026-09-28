import 'package:hive_flutter/hive_flutter.dart';

import '../data/landmarks_data.dart';
import '../models/game_state.dart';
import '../models/history_entry.dart';
import '../models/landmark.dart';

/// Local-only persistence. No network, no cloud: Hive boxes on the device.
///
/// The save format is plain maps (no generated type adapters), so content
/// changes never invalidate an existing save.
abstract final class GameStorage {
  static const String gameStateBox = 'mgn_game_state';
  static const String historyBox = 'mgn_history';
  static const String settingsBox = 'mgn_settings';
  static const String achievementsBox = 'mgn_achievements';

  static const String _currentSaveKey = 'current';

  /// History is stored outside the state blob so a long chronicle does not
  /// have to be rewritten on every save.
  static const int historyLimit = 400;

  static Future<void> init() async {
    await Hive.initFlutter();
    await Future.wait(<Future<Box<Object?>>>[
      Hive.openBox<Object?>(gameStateBox),
      Hive.openBox<Object?>(historyBox),
      Hive.openBox<Object?>(settingsBox),
      Hive.openBox<Object?>(achievementsBox),
    ]);
  }

  static Box<Object?> get _game => Hive.box<Object?>(gameStateBox);
  static Box<Object?> get _history => Hive.box<Object?>(historyBox);
  static Box<Object?> get _settings => Hive.box<Object?>(settingsBox);
  static Box<Object?> get _achievements => Hive.box<Object?>(achievementsBox);

  static bool get hasSave => _game.containsKey(_currentSaveKey);

  // ── Game state ────────────────────────────────────────────────────────────

  static Future<void> saveGame(GameState state) async {
    final Map<String, Object?> map = state.toMap()..remove('history');
    await _game.put(_currentSaveKey, map);
    await _saveHistory(state.history);
    await _achievements.put('unlocked', state.unlockedAchievements.toList());
  }

  static GameState? loadGame() {
    try {
      final Object? raw = _game.get(_currentSaveKey);
      if (raw == null) return null;

      final Map<String, Object?> map =
          Map<String, Object?>.from(raw as Map<Object?, Object?>);
      final GameState state = GameState.fromMap(
        map,
        landmarkResolver: LandmarksData.byId,
      );
      state.history.addAll(loadHistory());
      // Keep in-memory history bounded like the box.
      if (state.history.length > historyLimit) {
        state.history.removeRange(0, state.history.length - historyLimit);
      }
      return state;
    } catch (_) {
      // Corrupt save must never crash the app: treat as no save.
      return null;
    }
  }

  static Future<void> deleteSave() async {
    await _game.delete(_currentSaveKey);
    await _history.clear();
  }

  // ── History ───────────────────────────────────────────────────────────────

  static Future<void> _saveHistory(List<HistoryEntry> entries) async {
    // Robust append: if the box is longer than memory (e.g. after a trim or
    // an older save), rewrite it instead of appending at a wrong offset.
    if (entries.length < _history.length) {
      await _history.clear();
      final Iterable<HistoryEntry> capped = entries.length > historyLimit
          ? entries.skip(entries.length - historyLimit)
          : entries;
      await _history.addAll(capped.map((HistoryEntry e) => e.toMap()));
      return;
    }
    if (entries.length > _history.length) {
      final Iterable<HistoryEntry> fresh = entries.skip(_history.length);
      await _history.addAll(
        fresh.map((HistoryEntry e) => e.toMap()),
      );
    }
    while (_history.length > historyLimit) {
      await _history.deleteAt(0);
    }
  }

  static List<HistoryEntry> loadHistory() {
    try {
      return <HistoryEntry>[
        for (final Object? raw in _history.values)
          HistoryEntry.fromMap(
            Map<String, Object?>.from(raw! as Map<Object?, Object?>),
          ),
      ];
    } catch (_) {
      return <HistoryEntry>[];
    }
  }

  // ── Achievements (persist across saves) ───────────────────────────────────

  static Set<String> loadLifetimeAchievements() {
    final Object? raw = _achievements.get('unlocked');
    if (raw is! List<Object?>) return <String>{};
    return raw.map((Object? e) => e.toString()).toSet();
  }

  // ── Settings ──────────────────────────────────────────────────────────────

  static T setting<T>(String key, T fallback) {
    final Object? value = _settings.get(key);
    return value is T ? value : fallback;
  }

  static Future<void> putSetting(String key, Object? value) =>
      _settings.put(key, value);

  /// Convenience accessors for the settings screen.
  static bool get soundEnabled => setting<bool>('soundEnabled', true);
  static Future<void> setSoundEnabled(bool value) =>
      putSetting('soundEnabled', value);

  static bool get animationsEnabled => setting<bool>('animationsEnabled', true);
  static Future<void> setAnimationsEnabled(bool value) =>
      putSetting('animationsEnabled', value);

  /// Resolver exposed for tests and tooling.
  static Landmark? resolveLandmark(String id) => LandmarksData.byId(id);
}

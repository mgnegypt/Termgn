import '../models/enums.dart';
import '../models/event.dart';
import 'events_classic.dart';
import 'events_crisis.dart';
import 'events_modern.dart';

/// The single event pool handed to the EventEngine.
abstract final class EventsData {
  static final List<GameEvent> all = <GameEvent>[
    ...EventsClassic.all,
    ...EventsModern.all,
    ...EventsCrisis.all,
  ];

  static int countOf(EventCategory category) =>
      all.where((GameEvent e) => e.category == category).length;

  static GameEvent? byId(String id) {
    for (final GameEvent event in all) {
      if (event.id == id) return event;
    }
    return null;
  }
}

/// Shared enumerations for the MGN game domain.
///
/// Every enum here is persisted by `name`, never by index, so reordering a
/// value can never corrupt an existing save file.
library;

enum Season { spring, summer, autumn, winter }

/// Length of a single turn in in-game time. Chosen once at game start.
enum TurnLength { day, week, month }

/// Era progression. Only [Era.modern] is active in the MVP, but the ladder is
/// modelled from the start so later eras are pure content additions.
enum Era { founding, industrial, digital, modern }

enum EventCategory { classic, modern, economic, diplomatic, crisis }

/// Behaviour profile of an AI-controlled country.
enum AiBehavior { hostile, trader, neutral, isolationist }

enum TreatyType { trade, defensive, nonAggression, embassy }

/// Player stance during an active war, chosen per turn.
enum WarStance { defensive, offensive, negotiate }

enum WarOutcome { ongoing, victory, defeat, stalemate, negotiatedPeace }

enum LandmarkKind { cultural, modern }

/// Canonical keys for every numeric field an event/decision may modify.
///
/// Event effects are expressed as `Map<String, double>` per the design spec;
/// these constants keep authored content and the engine in sync.
abstract final class StateKeys {
  static const String treasuryCash = 'treasuryCash';
  static const String gems = 'gems';
  static const String debt = 'debt';
  static const String taxRate = 'taxRate';

  static const String economy = 'economy';
  static const String publicSatisfaction = 'publicSatisfaction';
  static const String digitalOpinion = 'digitalOpinion';
  static const String militarySecurity = 'militarySecurity';
  static const String cyberSecurity = 'cyberSecurity';
  static const String environment = 'environment';
  static const String culture = 'culture';

  static const String agriculture = 'agriculture';
  static const String industry = 'industry';
  static const String foodSecurity = 'foodSecurity';
  static const String energy = 'energy';
  static const String cleanEnergyRatio = 'cleanEnergyRatio';
  static const String technology = 'technology';
  static const String tourism = 'tourism';
  static const String health = 'health';
  static const String education = 'education';

  static const String population = 'population';
  static const String migrationBalance = 'migrationBalance';

  static const String axisEconomic = 'axisEconomic';
  static const String axisSocial = 'axisSocial';
  static const String axisForeign = 'axisForeign';

  /// Indicators shown as 0-100 bars on the dashboard.
  static const List<String> indicators = <String>[
    economy,
    publicSatisfaction,
    digitalOpinion,
    militarySecurity,
    cyberSecurity,
    environment,
    culture,
  ];

  /// Economic sectors, each 0-100. Shown on the sectors screen.
  static const List<String> sectors = <String>[
    agriculture,
    industry,
    foodSecurity,
    energy,
    technology,
    tourism,
    health,
    education,
  ];

  /// Sectors the player can invest in directly.
  ///
  /// [foodSecurity] is excluded on purpose: it is derived from agriculture
  /// and population pressure, so it must be raised by farming, not by a
  /// direct cash injection.
  static const List<String> investableSectors = <String>[
    agriculture,
    industry,
    energy,
    technology,
    tourism,
    health,
    education,
  ];
}

/// Parses an enum value stored by `name`, falling back to [fallback].
T enumFromName<T extends Enum>(List<T> values, Object? name, T fallback) {
  for (final T value in values) {
    if (value.name == name) return value;
  }
  return fallback;
}

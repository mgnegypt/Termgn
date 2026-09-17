import 'dart:math' as math;

import 'diplomacy_state.dart';
import 'enums.dart';
import 'history_entry.dart';
import 'landmark.dart';

/// The complete mutable state of one save file.
///
/// Every 0-100 indicator is clamped by [clampAll], which the turn engine calls
/// after each mutation pass, so no caller needs to remember bounds.
class GameState {
  GameState({
    required this.countryName,
    required this.rulerTitle,
    required this.inGameDate,
    required this.turnNumber,
    required this.currentSeason,
    required this.turnLength,
    required this.era,
    required this.treasuryCash,
    required this.gems,
    required this.taxRate,
    required this.debt,
    required this.militarySpendingLevel,
    required this.subsidyLevel,
    required this.economy,
    required this.publicSatisfaction,
    required this.digitalOpinion,
    required this.militarySecurity,
    required this.cyberSecurity,
    required this.environment,
    required this.culture,
    required this.legitimacy,
    required this.agriculture,
    required this.industry,
    required this.foodSecurity,
    required this.energy,
    required this.cleanEnergyRatio,
    required this.technology,
    required this.tourism,
    required this.health,
    required this.education,
    required this.population,
    required this.migrationBalance,
    required this.workersSatisfaction,
    required this.middleClassSatisfaction,
    required this.eliteSatisfaction,
    required this.axisEconomic,
    required this.axisSocial,
    required this.axisForeign,
    required this.countries,
    required this.builtLandmarks,
    required this.underConstruction,
    required this.history,
    required this.unlockedAchievements,
    required this.lastEventTurns,
    required this.firedOnceEvents,
    required this.flagPrimaryColor,
    required this.flagSecondaryColor,
    required this.flagSymbolId,
  });

  /// A balanced starting state for a modern mid-sized nation.
  factory GameState.newGame({
    required String countryName,
    required String rulerTitle,
    TurnLength turnLength = TurnLength.month,
    DateTime? startDate,
    int flagPrimaryColor = 0xFF0B0B0D,
    int flagSecondaryColor = 0xFFD4AF37,
    String flagSymbolId = 'crescent_star',
    Map<String, DiplomacyState> countries = const <String, DiplomacyState>{},
    double axisEconomic = 0,
    double axisSocial = 0,
    double axisForeign = 0,
  }) {
    return GameState(
      countryName: countryName,
      rulerTitle: rulerTitle,
      inGameDate: startDate ?? DateTime(2025, 1, 1),
      turnNumber: 1,
      currentSeason: seasonForMonth((startDate ?? DateTime(2025, 1, 1)).month),
      turnLength: turnLength,
      era: Era.modern,
      treasuryCash: 250000,
      gems: 25,
      taxRate: 0.22,
      debt: 0,
      militarySpendingLevel: 0.35,
      subsidyLevel: 0.40,
      economy: 50,
      publicSatisfaction: 55,
      digitalOpinion: 50,
      militarySecurity: 45,
      cyberSecurity: 35,
      environment: 55,
      culture: 30,
      legitimacy: 60,
      agriculture: 45,
      industry: 40,
      foodSecurity: 50,
      energy: 45,
      cleanEnergyRatio: 0.15,
      technology: 30,
      tourism: 25,
      health: 45,
      education: 40,
      population: 12000000,
      migrationBalance: 0,
      workersSatisfaction: 52,
      middleClassSatisfaction: 55,
      eliteSatisfaction: 58,
      axisEconomic: axisEconomic,
      axisSocial: axisSocial,
      axisForeign: axisForeign,
      countries: Map<String, DiplomacyState>.of(countries),
      builtLandmarks: <Landmark>[],
      underConstruction: <Landmark>[],
      history: <HistoryEntry>[],
      unlockedAchievements: <String>{},
      lastEventTurns: <String, int>{},
      firedOnceEvents: <String>{},
      flagPrimaryColor: flagPrimaryColor,
      flagSecondaryColor: flagSecondaryColor,
      flagSymbolId: flagSymbolId,
    );
  }

  // ── Identity ──────────────────────────────────────────────────────────────
  String countryName;
  String rulerTitle;
  DateTime inGameDate;
  int turnNumber;
  Season currentSeason;
  TurnLength turnLength;
  Era era;

  // ── Economy ───────────────────────────────────────────────────────────────
  double treasuryCash;
  int gems;

  /// 0.0 - 1.0
  double taxRate;
  double debt;

  /// Player-tuned spending dials, 0.0 - 1.0.
  double militarySpendingLevel;
  double subsidyLevel;

  // ── Core indicators, 0-100 ────────────────────────────────────────────────
  double economy;
  double publicSatisfaction;
  double digitalOpinion;
  double militarySecurity;
  double cyberSecurity;
  double environment;
  double culture;

  /// Mandate strength, evaluated every referendum cycle.
  double legitimacy;

  // ── Sectors, 0-100 (cleanEnergyRatio is 0-1) ──────────────────────────────
  double agriculture;
  double industry;
  double foodSecurity;
  double energy;
  double cleanEnergyRatio;
  double technology;
  double tourism;
  double health;
  double education;

  // ── Population ────────────────────────────────────────────────────────────
  int population;

  /// Positive = net inbound migration.
  double migrationBalance;
  double workersSatisfaction;
  double middleClassSatisfaction;
  double eliteSatisfaction;

  // ── Ideology axes, -100 .. 100 ────────────────────────────────────────────
  double axisEconomic;
  double axisSocial;
  double axisForeign;

  // ── World & build ─────────────────────────────────────────────────────────
  final Map<String, DiplomacyState> countries;
  final List<Landmark> builtLandmarks;
  final List<Landmark> underConstruction;

  // ── Log ───────────────────────────────────────────────────────────────────
  final List<HistoryEntry> history;
  final Set<String> unlockedAchievements;

  /// Event id -> turn it last fired, for cooldown checks.
  final Map<String, int> lastEventTurns;
  final Set<String> firedOnceEvents;

  // ── Flag (rendered as generated SVG, never an uploaded image) ─────────────
  int flagPrimaryColor;
  int flagSecondaryColor;
  String flagSymbolId;

  // ── Derived helpers ───────────────────────────────────────────────────────

  bool get isAtWar =>
      countries.values.any((DiplomacyState d) => d.atWar);

  List<DiplomacyState> get warEnemies =>
      countries.values.where((DiplomacyState d) => d.atWar).toList();

  int get sanctionCount =>
      countries.values.where((DiplomacyState d) => d.sanctioned).length;

  /// Average of the class-level satisfaction values, used as the social
  /// pressure input for public satisfaction drift.
  double get classSatisfactionAverage =>
      (workersSatisfaction + middleClassSatisfaction + eliteSatisfaction) / 3;

  /// Headline score used for achievements and the yearly report.
  double get nationScore {
    final double indicatorAvg = (economy +
            publicSatisfaction +
            digitalOpinion +
            militarySecurity +
            cyberSecurity +
            environment +
            culture) /
        7;
    final double sectorAvg = (agriculture +
            industry +
            foodSecurity +
            energy +
            technology +
            tourism +
            health +
            education) /
        8;
    return (indicatorAvg * 0.6) + (sectorAvg * 0.4);
  }

  static Season seasonForMonth(int month) {
    if (month >= 3 && month <= 5) return Season.spring;
    if (month >= 6 && month <= 8) return Season.summer;
    if (month >= 9 && month <= 11) return Season.autumn;
    return Season.winter;
  }

  // ── Keyed access (used by event effects) ──────────────────────────────────

  /// Reads any numeric field by its [StateKeys] key.
  double readKey(String key) {
    switch (key) {
      case StateKeys.treasuryCash:
        return treasuryCash;
      case StateKeys.gems:
        return gems.toDouble();
      case StateKeys.debt:
        return debt;
      case StateKeys.taxRate:
        return taxRate;
      case StateKeys.economy:
        return economy;
      case StateKeys.publicSatisfaction:
        return publicSatisfaction;
      case StateKeys.digitalOpinion:
        return digitalOpinion;
      case StateKeys.militarySecurity:
        return militarySecurity;
      case StateKeys.cyberSecurity:
        return cyberSecurity;
      case StateKeys.environment:
        return environment;
      case StateKeys.culture:
        return culture;
      case StateKeys.agriculture:
        return agriculture;
      case StateKeys.industry:
        return industry;
      case StateKeys.foodSecurity:
        return foodSecurity;
      case StateKeys.energy:
        return energy;
      case StateKeys.cleanEnergyRatio:
        return cleanEnergyRatio;
      case StateKeys.technology:
        return technology;
      case StateKeys.tourism:
        return tourism;
      case StateKeys.health:
        return health;
      case StateKeys.education:
        return education;
      case StateKeys.population:
        return population.toDouble();
      case StateKeys.migrationBalance:
        return migrationBalance;
      case StateKeys.axisEconomic:
        return axisEconomic;
      case StateKeys.axisSocial:
        return axisSocial;
      case StateKeys.axisForeign:
        return axisForeign;
      default:
        assert(false, 'Unknown state key: $key');
        return 0;
    }
  }

  /// Writes any numeric field by its [StateKeys] key. Bounds are applied by
  /// [clampAll].
  void writeKey(String key, double value) {
    switch (key) {
      case StateKeys.treasuryCash:
        treasuryCash = value;
      case StateKeys.gems:
        gems = value.round();
      case StateKeys.debt:
        debt = value;
      case StateKeys.taxRate:
        taxRate = value;
      case StateKeys.economy:
        economy = value;
      case StateKeys.publicSatisfaction:
        publicSatisfaction = value;
      case StateKeys.digitalOpinion:
        digitalOpinion = value;
      case StateKeys.militarySecurity:
        militarySecurity = value;
      case StateKeys.cyberSecurity:
        cyberSecurity = value;
      case StateKeys.environment:
        environment = value;
      case StateKeys.culture:
        culture = value;
      case StateKeys.agriculture:
        agriculture = value;
      case StateKeys.industry:
        industry = value;
      case StateKeys.foodSecurity:
        foodSecurity = value;
      case StateKeys.energy:
        energy = value;
      case StateKeys.cleanEnergyRatio:
        cleanEnergyRatio = value;
      case StateKeys.technology:
        technology = value;
      case StateKeys.tourism:
        tourism = value;
      case StateKeys.health:
        health = value;
      case StateKeys.education:
        education = value;
      case StateKeys.population:
        population = value.round();
      case StateKeys.migrationBalance:
        migrationBalance = value;
      case StateKeys.axisEconomic:
        axisEconomic = value;
      case StateKeys.axisSocial:
        axisSocial = value;
      case StateKeys.axisForeign:
        axisForeign = value;
      default:
        assert(false, 'Unknown state key: $key');
    }
  }

  void applyDelta(String key, double delta) =>
      writeKey(key, readKey(key) + delta);

  /// Applies a whole effect map (the shape authored on [EventChoice]) and
  /// clamps afterwards.
  void applyEffects(Map<String, double> effects) {
    effects.forEach(applyDelta);
    clampAll();
  }

  /// Enforces every bound in one place.
  void clampAll() {
    economy = _c(economy);
    publicSatisfaction = _c(publicSatisfaction);
    digitalOpinion = _c(digitalOpinion);
    militarySecurity = _c(militarySecurity);
    cyberSecurity = _c(cyberSecurity);
    environment = _c(environment);
    culture = _c(culture);
    legitimacy = _c(legitimacy);

    agriculture = _c(agriculture);
    industry = _c(industry);
    foodSecurity = _c(foodSecurity);
    energy = _c(energy);
    technology = _c(technology);
    tourism = _c(tourism);
    health = _c(health);
    education = _c(education);

    workersSatisfaction = _c(workersSatisfaction);
    middleClassSatisfaction = _c(middleClassSatisfaction);
    eliteSatisfaction = _c(eliteSatisfaction);

    cleanEnergyRatio = cleanEnergyRatio.clamp(0.0, 1.0);
    taxRate = taxRate.clamp(0.0, 1.0);
    militarySpendingLevel = militarySpendingLevel.clamp(0.0, 1.0);
    subsidyLevel = subsidyLevel.clamp(0.0, 1.0);

    axisEconomic = axisEconomic.clamp(-100.0, 100.0);
    axisSocial = axisSocial.clamp(-100.0, 100.0);
    axisForeign = axisForeign.clamp(-100.0, 100.0);

    debt = math.max(0, debt);
    gems = math.max(0, gems);
    population = math.max(0, population);

    for (final DiplomacyState d in countries.values) {
      d.relation = d.relation.clamp(-100.0, 100.0);
      d.economicPower = _c(d.economicPower);
      d.militaryPower = _c(d.militaryPower);
    }
  }

  static double _c(double v) => v.clamp(0.0, 100.0);

  // ── Persistence ───────────────────────────────────────────────────────────

  Map<String, Object?> toMap() => <String, Object?>{
        'countryName': countryName,
        'rulerTitle': rulerTitle,
        'inGameDate': inGameDate.toIso8601String(),
        'turnNumber': turnNumber,
        'currentSeason': currentSeason.name,
        'turnLength': turnLength.name,
        'era': era.name,
        'treasuryCash': treasuryCash,
        'gems': gems,
        'taxRate': taxRate,
        'debt': debt,
        'militarySpendingLevel': militarySpendingLevel,
        'subsidyLevel': subsidyLevel,
        'economy': economy,
        'publicSatisfaction': publicSatisfaction,
        'digitalOpinion': digitalOpinion,
        'militarySecurity': militarySecurity,
        'cyberSecurity': cyberSecurity,
        'environment': environment,
        'culture': culture,
        'legitimacy': legitimacy,
        'agriculture': agriculture,
        'industry': industry,
        'foodSecurity': foodSecurity,
        'energy': energy,
        'cleanEnergyRatio': cleanEnergyRatio,
        'technology': technology,
        'tourism': tourism,
        'health': health,
        'education': education,
        'population': population,
        'migrationBalance': migrationBalance,
        'workersSatisfaction': workersSatisfaction,
        'middleClassSatisfaction': middleClassSatisfaction,
        'eliteSatisfaction': eliteSatisfaction,
        'axisEconomic': axisEconomic,
        'axisSocial': axisSocial,
        'axisForeign': axisForeign,
        'countries': <String, Object?>{
          for (final MapEntry<String, DiplomacyState> e in countries.entries)
            e.key: e.value.toMap(),
        },
        'builtLandmarks':
            builtLandmarks.map((Landmark l) => l.toMap()).toList(),
        'underConstruction':
            underConstruction.map((Landmark l) => l.toMap()).toList(),
        'history': history.map((HistoryEntry h) => h.toMap()).toList(),
        'unlockedAchievements': unlockedAchievements.toList(),
        'lastEventTurns': lastEventTurns,
        'firedOnceEvents': firedOnceEvents.toList(),
        'flagPrimaryColor': flagPrimaryColor,
        'flagSecondaryColor': flagSecondaryColor,
        'flagSymbolId': flagSymbolId,
      };

  /// Rebuilds a state from storage. [landmarkResolver] maps a stored landmark
  /// id back to its catalogue definition.
  static GameState fromMap(
    Map<String, Object?> map, {
    required Landmark? Function(String id) landmarkResolver,
  }) {
    double d(String key, double fallback) =>
        (map[key] as num?)?.toDouble() ?? fallback;

    List<Landmark> landmarks(String key) {
      final List<Object?> raw =
          (map[key] as List<Object?>?) ?? const <Object?>[];
      final List<Landmark> out = <Landmark>[];
      for (final Object? item in raw) {
        final Map<String, Object?> m = Map<String, Object?>.from(
          item! as Map<Object?, Object?>,
        );
        final Landmark? def = landmarkResolver(m['id']! as String);
        if (def == null) continue; // content removed since the save was made
        out.add(def.copyWith(
          turnsRemaining: (m['turnsRemaining'] as num?)?.toInt(),
        ));
      }
      return out;
    }

    final DateTime date =
        DateTime.tryParse((map['inGameDate'] as String?) ?? '') ??
            DateTime(2025);

    final GameState state = GameState(
      countryName: (map['countryName'] as String?) ?? 'MGN',
      rulerTitle: (map['rulerTitle'] as String?) ?? 'رئيس',
      inGameDate: date,
      turnNumber: (map['turnNumber'] as num?)?.toInt() ?? 1,
      currentSeason: enumFromName(
        Season.values,
        map['currentSeason'],
        seasonForMonth(date.month),
      ),
      turnLength:
          enumFromName(TurnLength.values, map['turnLength'], TurnLength.month),
      era: enumFromName(Era.values, map['era'], Era.modern),
      treasuryCash: d('treasuryCash', 0),
      gems: (map['gems'] as num?)?.toInt() ?? 0,
      taxRate: d('taxRate', 0.22),
      debt: d('debt', 0),
      militarySpendingLevel: d('militarySpendingLevel', 0.35),
      subsidyLevel: d('subsidyLevel', 0.40),
      economy: d('economy', 50),
      publicSatisfaction: d('publicSatisfaction', 50),
      digitalOpinion: d('digitalOpinion', 50),
      militarySecurity: d('militarySecurity', 45),
      cyberSecurity: d('cyberSecurity', 35),
      environment: d('environment', 55),
      culture: d('culture', 30),
      legitimacy: d('legitimacy', 60),
      agriculture: d('agriculture', 45),
      industry: d('industry', 40),
      foodSecurity: d('foodSecurity', 50),
      energy: d('energy', 45),
      cleanEnergyRatio: d('cleanEnergyRatio', 0.15),
      technology: d('technology', 30),
      tourism: d('tourism', 25),
      health: d('health', 45),
      education: d('education', 40),
      population: (map['population'] as num?)?.toInt() ?? 12000000,
      migrationBalance: d('migrationBalance', 0),
      workersSatisfaction: d('workersSatisfaction', 52),
      middleClassSatisfaction: d('middleClassSatisfaction', 55),
      eliteSatisfaction: d('eliteSatisfaction', 58),
      axisEconomic: d('axisEconomic', 0),
      axisSocial: d('axisSocial', 0),
      axisForeign: d('axisForeign', 0),
      countries: <String, DiplomacyState>{
        for (final MapEntry<Object?, Object?> e
            in ((map['countries'] as Map<Object?, Object?>?) ??
                    const <Object?, Object?>{})
                .entries)
          e.key.toString(): DiplomacyState.fromMap(
            Map<String, Object?>.from(e.value! as Map<Object?, Object?>),
          ),
      },
      builtLandmarks: landmarks('builtLandmarks'),
      underConstruction: landmarks('underConstruction'),
      history: <HistoryEntry>[
        for (final Object? item in (map['history'] as List<Object?>?) ??
            const <Object?>[])
          HistoryEntry.fromMap(
            Map<String, Object?>.from(item! as Map<Object?, Object?>),
          ),
      ],
      unlockedAchievements: <String>{
        for (final Object? id in (map['unlockedAchievements'] as List<Object?>?) ??
            const <Object?>[])
          id.toString(),
      },
      lastEventTurns: <String, int>{
        for (final MapEntry<Object?, Object?> e
            in ((map['lastEventTurns'] as Map<Object?, Object?>?) ??
                    const <Object?, Object?>{})
                .entries)
          e.key.toString(): (e.value as num).toInt(),
      },
      firedOnceEvents: <String>{
        for (final Object? id in (map['firedOnceEvents'] as List<Object?>?) ??
            const <Object?>[])
          id.toString(),
      },
      flagPrimaryColor: (map['flagPrimaryColor'] as num?)?.toInt() ?? 0xFF0B0B0D,
      flagSecondaryColor:
          (map['flagSecondaryColor'] as num?)?.toInt() ?? 0xFFD4AF37,
      flagSymbolId: (map['flagSymbolId'] as String?) ?? 'crescent_star',
    );
    state.clampAll();
    return state;
  }
}

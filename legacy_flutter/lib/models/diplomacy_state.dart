import 'enums.dart';

/// The player's live relationship with one AI country.
class DiplomacyState {
  DiplomacyState({
    required this.countryId,
    required this.nameAr,
    required this.behavior,
    this.relation = 0,
    this.economicPower = 50,
    this.militaryPower = 50,
    Set<TreatyType>? treaties,
    this.atWar = false,
    this.warTurns = 0,
    this.sanctioned = false,
    this.lastOfferTurn = -999,
  }) : treaties = treaties ?? <TreatyType>{};

  final String countryId;
  final String nameAr;
  AiBehavior behavior;

  /// -100 (hostile) .. 100 (allied).
  double relation;

  /// Rough power ratings, 0-100, drifting slowly over time.
  double economicPower;
  double militaryPower;

  final Set<TreatyType> treaties;

  bool atWar;

  /// Turns the current war has lasted; 0 when at peace.
  int warTurns;

  /// Economic sanctions currently imposed on the player.
  bool sanctioned;

  /// Turn of the last diplomatic proposal, used to rate-limit AI offers.
  int lastOfferTurn;

  bool get hasEmbassy => treaties.contains(TreatyType.embassy);
  bool get hasTradeDeal => treaties.contains(TreatyType.trade);
  bool get isAlly => treaties.contains(TreatyType.defensive);

  /// How much detail the player may see about this country. An embassy
  /// unlocks full numbers.
  bool get intelUnlocked => hasEmbassy;

  Map<String, Object?> toMap() => <String, Object?>{
        'countryId': countryId,
        'nameAr': nameAr,
        'behavior': behavior.name,
        'relation': relation,
        'economicPower': economicPower,
        'militaryPower': militaryPower,
        'treaties': treaties.map((TreatyType t) => t.name).toList(),
        'atWar': atWar,
        'warTurns': warTurns,
        'sanctioned': sanctioned,
        'lastOfferTurn': lastOfferTurn,
      };

  static DiplomacyState fromMap(Map<String, Object?> map) {
    return DiplomacyState(
      countryId: map['countryId']! as String,
      nameAr: (map['nameAr'] as String?) ?? '',
      behavior:
          enumFromName(AiBehavior.values, map['behavior'], AiBehavior.neutral),
      relation: (map['relation'] as num?)?.toDouble() ?? 0,
      economicPower: (map['economicPower'] as num?)?.toDouble() ?? 50,
      militaryPower: (map['militaryPower'] as num?)?.toDouble() ?? 50,
      treaties: <TreatyType>{
        for (final Object? name in (map['treaties'] as List<Object?>?) ??
            const <Object?>[])
          enumFromName(TreatyType.values, name, TreatyType.nonAggression),
      },
      atWar: (map['atWar'] as bool?) ?? false,
      warTurns: (map['warTurns'] as num?)?.toInt() ?? 0,
      sanctioned: (map['sanctioned'] as bool?) ?? false,
      lastOfferTurn: (map['lastOfferTurn'] as num?)?.toInt() ?? -999,
    );
  }
}

import '../models/diplomacy_state.dart';
import '../models/enums.dart';

/// The AI-controlled world: 10 fictional countries (MVP target 8-12).
///
/// Names are invented so no real state is portrayed; each has a behaviour
/// profile, starting relation and rough power ratings.
abstract final class CountriesData {
  static const List<CountrySeed> seeds = <CountrySeed>[
    CountrySeed(
      id: 'arzan',
      nameAr: 'أرزان',
      behavior: AiBehavior.trader,
      relation: 25,
      economicPower: 72,
      militaryPower: 48,
    ),
    CountrySeed(
      id: 'norvane',
      nameAr: 'نورفان',
      behavior: AiBehavior.trader,
      relation: 15,
      economicPower: 84,
      militaryPower: 55,
    ),
    CountrySeed(
      id: 'khalidia',
      nameAr: 'خالدية',
      behavior: AiBehavior.neutral,
      relation: 10,
      economicPower: 58,
      militaryPower: 52,
    ),
    CountrySeed(
      id: 'sahran',
      nameAr: 'سهران',
      behavior: AiBehavior.hostile,
      relation: -20,
      economicPower: 55,
      militaryPower: 78,
    ),
    CountrySeed(
      id: 'tavros',
      nameAr: 'تافروس',
      behavior: AiBehavior.hostile,
      relation: -10,
      economicPower: 66,
      militaryPower: 82,
    ),
    CountrySeed(
      id: 'meridan',
      nameAr: 'ميريدان',
      behavior: AiBehavior.neutral,
      relation: 5,
      economicPower: 62,
      militaryPower: 44,
    ),
    CountrySeed(
      id: 'qasira',
      nameAr: 'قصيرة',
      behavior: AiBehavior.isolationist,
      relation: 0,
      economicPower: 40,
      militaryPower: 38,
    ),
    CountrySeed(
      id: 'velmor',
      nameAr: 'فيلمور',
      behavior: AiBehavior.isolationist,
      relation: -5,
      economicPower: 49,
      militaryPower: 60,
    ),
    CountrySeed(
      id: 'azmara',
      nameAr: 'أزمارا',
      behavior: AiBehavior.trader,
      relation: 18,
      economicPower: 69,
      militaryPower: 41,
    ),
    CountrySeed(
      id: 'dranov',
      nameAr: 'درانوف',
      behavior: AiBehavior.neutral,
      relation: -2,
      economicPower: 75,
      militaryPower: 71,
    ),
  ];

  /// Builds the initial diplomacy map for a new game.
  static Map<String, DiplomacyState> initialWorld() => <String, DiplomacyState>{
        for (final CountrySeed seed in seeds)
          seed.id: DiplomacyState(
            countryId: seed.id,
            nameAr: seed.nameAr,
            behavior: seed.behavior,
            relation: seed.relation,
            economicPower: seed.economicPower,
            militaryPower: seed.militaryPower,
          ),
      };

  static String nameOf(String id) => seeds
      .firstWhere(
        (CountrySeed s) => s.id == id,
        orElse: () => const CountrySeed(
          id: 'unknown',
          nameAr: 'دولة مجهولة',
          behavior: AiBehavior.neutral,
          relation: 0,
          economicPower: 50,
          militaryPower: 50,
        ),
      )
      .nameAr;
}

class CountrySeed {
  const CountrySeed({
    required this.id,
    required this.nameAr,
    required this.behavior,
    required this.relation,
    required this.economicPower,
    required this.militaryPower,
  });

  final String id;
  final String nameAr;
  final AiBehavior behavior;
  final double relation;
  final double economicPower;
  final double militaryPower;
}

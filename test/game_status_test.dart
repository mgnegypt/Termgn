import 'package:flutter_test/flutter_test.dart';
import 'package:mgn/data/countries_data.dart';
import 'package:mgn/models/game_state.dart';

void main() {
  test('fresh game is ongoing: no victory, no collapse', () {
    final GameState s = GameState.newGame(
      countryName: 'T',
      rulerTitle: 'R',
      countries: CountriesData.initialWorld(),
    );
    expect(s.isCollapsed, isFalse);
    expect(s.hasWon, isFalse);
    expect(s.statusLabel, 'مستمرة');
  });

  test('collapse requires legitimacy, satisfaction and treasury failure', () {
    final GameState s = GameState.newGame(
      countryName: 'T',
      rulerTitle: 'R',
      countries: CountriesData.initialWorld(),
    );
    s.legitimacy = 5;
    s.publicSatisfaction = 10;
    s.treasuryCash = 0;
    expect(s.isCollapsed, isTrue);
    expect(s.statusLabel, 'انهيار الدولة');
  });

  test('victory needs score, legitimacy and time', () {
    final GameState s = GameState.newGame(
      countryName: 'T',
      rulerTitle: 'R',
      countries: CountriesData.initialWorld(),
    );
    s.turnNumber = 30;
    s.legitimacy = 80;
    for (final String k in <String>[
      'economy',
      'publicSatisfaction',
      'digitalOpinion',
      'militarySecurity',
      'cyberSecurity',
      'environment',
      'culture',
      'agriculture',
      'industry',
      'foodSecurity',
      'energy',
      'technology',
      'tourism',
      'health',
      'education',
    ]) {
      s.writeKey(k, 95);
    }
    expect(s.nationScore, greaterThan(85));
    expect(s.hasWon, isTrue);
  });
}

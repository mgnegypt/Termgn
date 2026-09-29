import 'dart:math' as math;

import 'package:flutter_test/flutter_test.dart';
import 'package:mgn/data/achievements_data.dart';
import 'package:mgn/data/countries_data.dart';
import 'package:mgn/data/events_data.dart';
import 'package:mgn/data/landmarks_data.dart';
import 'package:mgn/engine/construction_engine.dart';
import 'package:mgn/engine/economy_calculator.dart';
import 'package:mgn/engine/event_engine.dart';
import 'package:mgn/engine/turn_engine.dart';
import 'package:mgn/models/enums.dart';
import 'package:mgn/models/event.dart';
import 'package:mgn/models/game_state.dart';
import 'package:mgn/models/landmark.dart';

/// Balance invariants. These are deliberately loose: they guard the shape of
/// the curve (solvency, recoverability, progression, no snowball), not exact
/// numbers, so BalanceConfig stays tunable without breaking the suite.
void main() {
  GameState newGame() => GameState.newGame(
        countryName: 'دولة الاختبار',
        rulerTitle: 'رئيس',
        countries: CountriesData.initialWorld(),
      );

  TurnEngine engineFor(math.Random rng) => TurnEngine(
        eventEngine: EventEngine(pool: EventsData.all, rng: rng),
        achievements: AchievementsData.all,
        rng: rng,
      );

  test('the default policy runs a surplus at game start', () {
    final GameState state = newGame();
    final TurnFinance finance = EconomyCalculator.computeFinance(state);
    expect(finance.net, greaterThan(0),
        reason: 'a new nation must not start insolvent');
    expect(finance.totalExpenses / finance.totalIncome, lessThan(0.85),
        reason: 'expenses must leave room for decisions and investment');
  });

  test('a passive nation stays solvent and stable for 60 turns', () {
    final math.Random rng = math.Random(21);
    final GameState state = newGame();
    final TurnEngine engine = engineFor(rng);

    // No decisions, no investment: pure model drift.
    for (int i = 0; i < 60; i++) {
      engine.advance(state);
    }

    expect(state.debt, 0, reason: 'doing nothing must not create debt');
    expect(state.economy, greaterThan(35),
        reason: 'the economy must not collapse on its own');
    expect(state.publicSatisfaction, greaterThan(25),
        reason: 'satisfaction must not collapse on its own');
    expect(state.foodSecurity, greaterThan(25),
        reason: 'food security must not collapse on its own');
  });

  test('a collapsed nation can recover: sectors have a subsistence floor', () {
    final math.Random rng = math.Random(33);
    final GameState state = newGame()
      ..agriculture = 0
      ..industry = 0
      ..health = 0
      ..education = 0
      ..energy = 0
      ..technology = 0;
    final TurnEngine engine = engineFor(rng);

    for (int i = 0; i < 40; i++) {
      engine.advance(state);
    }

    expect(state.agriculture, greaterThan(10),
        reason: 'a zeroed sector must recover toward the subsistence floor');
    expect(state.health, greaterThan(10));
  });

  test('a competent policy improves the nation over 120 turns', () {
    final math.Random rng = math.Random(42);
    final GameState state = newGame();
    final TurnEngine engine = engineFor(rng);
    final double startScore = state.nationScore;
    const double reserve = 220000;

    for (int turn = 0; turn < 120; turn++) {
      final TurnReport report = engine.advance(state);
      double budget = math.max(0.0, state.treasuryCash - reserve);

      for (final GameEvent event in report.pendingEvents) {
        final List<EventChoice> options = event.choices
            .where((EventChoice c) => c.isSelectable(state))
            .toList();
        if (options.isEmpty) continue;
        final List<EventChoice> affordable = options
            .where((EventChoice c) => c.effectiveCostCash <= budget)
            .toList();
        final EventChoice picked =
            affordable.isNotEmpty ? affordable.first : options.first;
        budget = math.max(0.0, budget - picked.effectiveCostCash);
        engine.events.applyChoice(state, event, picked);
      }

      if (state.debt > 0 && state.treasuryCash > reserve) {
        EconomyCalculator.repayDebt(state, state.treasuryCash - reserve);
      }
      if (budget > 60000) {
        final String weakest = StateKeys.investableSectors.reduce(
          (String a, String b) => state.readKey(a) <= state.readKey(b) ? a : b,
        );
        EconomyCalculator.investInSector(state, weakest, 3);
      }
    }

    expect(state.nationScore, greaterThan(startScore + 15),
        reason: 'good play must visibly improve the nation');
    expect(state.nationScore, lessThan(99),
        reason: 'the game must not be trivially maxed out');
    expect(state.debt, lessThan(1000000),
        reason: 'competent play must not end in a debt spiral');
  });

  test('landmarks remain affordable to a saving player', () {
    final math.Random rng = math.Random(9);
    final GameState state = newGame();
    final TurnEngine engine = engineFor(rng);

    // Save without spending on decisions, then build.
    for (int i = 0; i < 12; i++) {
      engine.advance(state);
    }
    final Landmark cheapest = LandmarksData.all
        .reduce((Landmark a, Landmark b) => a.costCash <= b.costCash ? a : b);
    expect(ConstructionEngine.canStart(state, cheapest), isTrue,
        reason: 'a year of saving should fund the cheapest landmark');
  });

  test('gems stay scarce: no economic source pays them out', () {
    final math.Random rng = math.Random(4);
    final GameState state = newGame();
    final TurnEngine engine = engineFor(rng);
    for (int i = 0; i < 36; i++) {
      engine.advance(state);
    }
    // Three in-game years: yearly reports plus any achievements only.
    expect(state.gems, lessThan(120),
        reason: 'hard currency must not be farmable by idling');
  });
}

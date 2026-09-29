// ignore_for_file: avoid_print
// Balance harness. Run with:
//   dart run tool/balance_sim.dart [turns] [seed]
//
// Simulates a "reasonable player": always picks the highest-value affordable
// choice, invests spare cash in the weakest sector, and builds landmarks when
// it can afford them. Prints a table used for the balancing pass.
import 'dart:math' as math;

import 'package:mgn/data/achievements_data.dart';
import 'package:mgn/data/countries_data.dart';
import 'package:mgn/data/events_data.dart';
import 'package:mgn/data/landmarks_data.dart';
import 'package:mgn/engine/construction_engine.dart';
import 'package:mgn/engine/economy_calculator.dart';
import 'package:mgn/engine/event_engine.dart';
import 'package:mgn/engine/turn_engine.dart';
import 'package:mgn/models/achievement.dart';
import 'package:mgn/models/enums.dart';
import 'package:mgn/models/event.dart';
import 'package:mgn/models/game_state.dart';
import 'package:mgn/models/landmark.dart';

void main(List<String> args) {
  final int turns = args.isNotEmpty ? int.parse(args[0]) : 120;
  final int seed = args.length > 1 ? int.parse(args[1]) : 42;
  final math.Random rng = math.Random(seed);

  final GameState state = GameState.newGame(
    countryName: 'MGN',
    rulerTitle: 'رئيس',
    countries: CountriesData.initialWorld(),
  );
  final EventEngine events = EventEngine(pool: EventsData.all, rng: rng);
  final TurnEngine engine = TurnEngine(
    eventEngine: events,
    achievements: AchievementsData.all,
    rng: rng,
  );

  double spentDecisions = 0;
  double spentInvestment = 0;
  double spentLandmarks = 0;
  double earnedIncome = 0;
  double paidExpenses = 0;
  int decisionCount = 0;

  print('turn | year | cash      | debt     | econ | satis | agri | food | tech '
      '| env | culture | score | landmarks');

  for (int i = 1; i <= turns; i++) {
    final TurnReport report = engine.advance(state);
    earnedIncome += report.finance.totalIncome;
    paidExpenses += report.finance.totalExpenses;

    // Discretionary budget for this turn: whatever sits above the reserve.
    const double reserve = 220000;
    double budget = math.max(0.0, state.treasuryCash - reserve);

    for (final GameEvent event in report.pendingEvents) {
      final List<EventChoice> options = event.choices
          .where((EventChoice c) => c.isSelectable(state))
          .toList();
      if (options.isEmpty) continue;

      // Prefer the best option affordable within the budget; otherwise take
      // the cheapest available one.
      final List<EventChoice> affordable = options
          .where((EventChoice c) => c.effectiveCostCash <= budget)
          .toList()
        ..sort((EventChoice a, EventChoice b) => _score(b).compareTo(_score(a)));
      final EventChoice picked = affordable.isNotEmpty
          ? affordable.first
          : (options
              ..sort((EventChoice a, EventChoice b) =>
                  a.effectiveCostCash.compareTo(b.effectiveCostCash)))
              .first;

      spentDecisions += picked.effectiveCostCash;
      decisionCount++;
      budget = math.max(0.0, budget - picked.effectiveCostCash);
      events.applyChoice(state, event, picked);
    }

    // Service debt before anything discretionary.
    if (state.debt > 0 && state.treasuryCash > reserve) {
      EconomyCalculator.repayDebt(state, state.treasuryCash - reserve);
      budget = math.max(0.0, state.treasuryCash - reserve);
    }

    // Build the cheapest available landmark once there is real slack.
    final List<Landmark> buildable = LandmarksData.all
        .where((Landmark l) => ConstructionEngine.canStart(state, l))
        .toList()
      ..sort((Landmark a, Landmark b) => a.costCash.compareTo(b.costCash));
    if (buildable.isNotEmpty &&
        state.underConstruction.isEmpty &&
        state.debt <= 0 &&
        budget > buildable.first.costCash) {
      spentLandmarks += buildable.first.costCash;
      ConstructionEngine.start(state, buildable.first);
      budget = math.max(0.0, state.treasuryCash - reserve);
    }

    // Spend what is left maintaining the weakest sector.
    if (budget > 60000) {
      final String weakest = StateKeys.investableSectors.reduce(
        (String a, String b) => state.readKey(a) <= state.readKey(b) ? a : b,
      );
      final double cost =
          EconomyCalculator.sectorInvestmentCost(state, weakest, 3);
      if (EconomyCalculator.investInSector(state, weakest, 3)) {
        spentInvestment += cost;
      }
    }

    if (i % 6 == 0 || i == 1) {
      print('${i.toString().padLeft(4)} | '
          '${state.inGameDate.year} | '
          '${state.treasuryCash.round().toString().padLeft(9)} | '
          '${state.debt.round().toString().padLeft(8)} | '
          '${state.economy.toStringAsFixed(0).padLeft(4)} | '
          '${state.publicSatisfaction.toStringAsFixed(0).padLeft(5)} | '
          '${state.agriculture.toStringAsFixed(0).padLeft(4)} | '
          '${state.foodSecurity.toStringAsFixed(0).padLeft(4)} | '
          '${state.technology.toStringAsFixed(0).padLeft(4)} | '
          '${state.environment.toStringAsFixed(0).padLeft(3)} | '
          '${state.culture.toStringAsFixed(0).padLeft(7)} | '
          '${state.nationScore.toStringAsFixed(1).padLeft(5)} | '
          '${state.builtLandmarks.length}');
    }
  }

  print('\n── cash flow over $turns turns ──');
  print('income total   : ${earnedIncome.round()} '
      '(avg ${(earnedIncome / turns).round()}/turn)');
  print('expenses total : ${paidExpenses.round()} '
      '(avg ${(paidExpenses / turns).round()}/turn)');
  print('surplus        : ${(earnedIncome - paidExpenses).round()} '
      '(avg ${((earnedIncome - paidExpenses) / turns).round()}/turn)');
  print('decisions      : ${spentDecisions.round()} over $decisionCount '
      'choices (avg ${decisionCount == 0 ? 0 : (spentDecisions / decisionCount).round()}/choice, '
      '${(spentDecisions / turns).round()}/turn)');
  print('investment     : ${spentInvestment.round()} '
      '(avg ${(spentInvestment / turns).round()}/turn)');
  print('landmarks      : ${spentLandmarks.round()}');

  print('\n── summary after $turns turns ──');
  print('date          : ${state.inGameDate.toIso8601String().split('T').first}');
  print('gems          : ${state.gems}');
  print('population    : ${state.population}');
  print('nation score  : ${state.nationScore.toStringAsFixed(1)}');
  print('landmarks     : ${state.builtLandmarks.length}/'
      '${LandmarksData.all.length}');
  print('achievements  : ${state.unlockedAchievements.length}/'
      '${AchievementsData.all.length}');
  print('unlocked      : ${state.unlockedAchievements.join(', ')}');
  print('wars active   : ${state.warEnemies.length}');
  print('sanctions     : ${state.sanctionCount}');
  print('history size  : ${state.history.length}');
  print('events fired  : ${state.lastEventTurns.length}/${EventsData.all.length}');
  final List<Achievement> locked = AchievementsData.all
      .where((Achievement a) => !state.unlockedAchievements.contains(a.id))
      .toList();
  print('still locked  : ${locked.map((Achievement a) => a.id).join(', ')}');
}

/// Crude utility score so the harness plays "reasonably" rather than randomly.
double _score(EventChoice choice) {
  double value = 0;
  choice.stateEffects.forEach((String key, double delta) {
    value += switch (key) {
      // Cash is worth far less than a durable indicator point.
      StateKeys.treasuryCash => delta / 30000,
      StateKeys.debt => -delta / 8000,
      StateKeys.population => delta / 30000,
      StateKeys.cleanEnergyRatio => delta * 40,
      // Long-term win conditions get priority.
      StateKeys.publicSatisfaction => delta * 1.6,
      StateKeys.economy => delta * 1.6,
      StateKeys.foodSecurity => delta * 1.3,
      _ => delta,
    };
  });
  value -= choice.costCash / 30000;
  value -= choice.costGems * 2;
  value += choice.allRelationsDelta;
  return value;
}

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

GameState _newGame() => GameState.newGame(
      countryName: 'دولة الاختبار',
      rulerTitle: 'رئيس',
      countries: CountriesData.initialWorld(),
    );

TurnEngine _engine(math.Random rng) => TurnEngine(
      eventEngine: EventEngine(pool: EventsData.all, rng: rng),
      achievements: AchievementsData.all,
      rng: rng,
    );

void main() {
  group('content checklist', () {
    test('event counts match the MVP targets', () {
      expect(EventsData.countOf(EventCategory.classic), inInclusiveRange(25, 30));
      expect(EventsData.countOf(EventCategory.modern), inInclusiveRange(25, 30));
      expect(EventsData.countOf(EventCategory.crisis), inInclusiveRange(8, 10));
    });

    test('landmarks, countries and achievements match the MVP targets', () {
      expect(LandmarksData.all.length, inInclusiveRange(12, 15));
      expect(LandmarksData.culturalCount, greaterThanOrEqualTo(5));
      expect(LandmarksData.modernCount, greaterThanOrEqualTo(5));
      expect(CountriesData.seeds.length, inInclusiveRange(8, 12));
      expect(AchievementsData.all.length, inInclusiveRange(15, 20));
    });

    test('every event id is unique and every choice is authored', () {
      final Set<String> ids = <String>{};
      for (final GameEvent event in EventsData.all) {
        expect(ids.add(event.id), isTrue, reason: 'duplicate id ${event.id}');
        expect(event.choices, isNotEmpty, reason: '${event.id} has no choices');
        expect(event.title.trim(), isNotEmpty);
        expect(event.description.trim(), isNotEmpty);
        for (final EventChoice choice in event.choices) {
          expect(choice.label.trim(), isNotEmpty);
          expect(choice.resultText.trim(), isNotEmpty);
        }
      }
    });

    test('every effect key is a known state key', () {
      const Set<String> valid = <String>{
        ...StateKeys.indicators,
        ...StateKeys.sectors,
        StateKeys.treasuryCash,
        StateKeys.gems,
        StateKeys.debt,
        StateKeys.taxRate,
        StateKeys.cleanEnergyRatio,
        StateKeys.population,
        StateKeys.migrationBalance,
        StateKeys.axisEconomic,
        StateKeys.axisSocial,
        StateKeys.axisForeign,
      };
      for (final GameEvent event in EventsData.all) {
        for (final EventChoice choice in event.choices) {
          for (final String key in choice.stateEffects.keys) {
            expect(valid, contains(key), reason: '${event.id} -> $key');
          }
        }
      }
      for (final Landmark landmark in LandmarksData.all) {
        for (final String key in <String>[
          ...landmark.onCompleteEffects.keys,
          ...landmark.perTurnEffects.keys,
          ...landmark.requirements.keys,
        ]) {
          expect(valid, contains(key), reason: '${landmark.id} -> $key');
        }
      }
    });
  });

  group('turn engine simulation', () {
    test('200 turns stay inside every bound', () {
      final math.Random rng = math.Random(7);
      final GameState state = _newGame();
      final TurnEngine engine = _engine(rng);

      for (int turn = 0; turn < 200; turn++) {
        final TurnReport report = engine.advance(state);

        // Resolve every pending decision by picking a random valid choice.
        for (final GameEvent event in report.pendingEvents) {
          final List<EventChoice> options = event.choices
              .where((EventChoice c) => c.isSelectable(state))
              .toList();
          if (options.isEmpty) continue;
          engine.events.applyChoice(
            state,
            event,
            options[rng.nextInt(options.length)],
          );
        }

        for (final String key in <String>[
          ...StateKeys.indicators,
          ...StateKeys.sectors,
        ]) {
          final double value = state.readKey(key);
          expect(value.isFinite, isTrue, reason: '$key is not finite');
          expect(value, inInclusiveRange(0, 100), reason: '$key out of range');
        }
        expect(state.cleanEnergyRatio, inInclusiveRange(0, 1));
        expect(state.taxRate, inInclusiveRange(0, 1));
        expect(state.treasuryCash.isFinite, isTrue);
        expect(state.treasuryCash, greaterThanOrEqualTo(0));
        expect(state.debt, greaterThanOrEqualTo(0));
        expect(state.gems, greaterThanOrEqualTo(0));
        expect(state.population, greaterThan(0));
        expect(state.axisEconomic, inInclusiveRange(-100, 100));
      }

      expect(state.turnNumber, 201);
      expect(state.inGameDate.year, greaterThan(2025));
      expect(state.history, isNotEmpty);
    });

    test('the calendar advances by turn length and tracks seasons', () {
      final GameState monthly = _newGame();
      final TurnEngine engine = _engine(math.Random(1));
      for (int i = 0; i < 12; i++) {
        engine.advance(monthly);
      }
      expect(monthly.inGameDate.year, 2026);
      expect(monthly.currentSeason, Season.winter);
      expect(monthly.turnNumber, 13);
    });

    test('a yearly report fires once per in-game year and pays gems', () {
      final GameState state = _newGame();
      final TurnEngine engine = _engine(math.Random(3));
      final int gemsBefore = state.gems;
      int reports = 0;
      for (int i = 0; i < 24; i++) {
        if (engine.advance(state).yearlyReport != null) reports++;
      }
      expect(reports, 2);
      expect(state.gems, greaterThan(gemsBefore));
    });

    test('events respect their cooldown', () {
      final math.Random rng = math.Random(11);
      final GameState state = _newGame();
      final TurnEngine engine = _engine(rng);
      final Map<String, List<int>> firings = <String, List<int>>{};

      for (int i = 0; i < 120; i++) {
        final TurnReport report = engine.advance(state);
        for (final GameEvent event in report.pendingEvents) {
          final List<EventChoice> options = event.choices
              .where((EventChoice c) => c.isSelectable(state))
              .toList();
          if (options.isEmpty) continue;
          engine.events.applyChoice(state, event, options.first);
          firings.putIfAbsent(event.id, () => <int>[]).add(state.turnNumber);
        }
      }

      firings.forEach((String id, List<int> turns) {
        for (int i = 1; i < turns.length; i++) {
          expect(turns[i] - turns[i - 1], greaterThanOrEqualTo(6),
              reason: '$id repeated too soon');
        }
      });
      expect(firings.keys.length, greaterThan(10),
          reason: 'event variety is too low');
    });
  });

  group('economy', () {
    test('income and expenses are both non-trivial at game start', () {
      final GameState state = _newGame();
      final TurnFinance finance = EconomyCalculator.computeFinance(state);
      expect(finance.totalIncome, greaterThan(0));
      expect(finance.totalExpenses, greaterThan(0));
      expect(finance.incomeBreakdown.values.every((double v) => v >= 0), isTrue);
    });

    test('raising taxes raises income but costs satisfaction', () {
      final GameState low = _newGame()..taxRate = 0.15;
      final GameState high = _newGame()..taxRate = 0.45;
      expect(
        EconomyCalculator.computeFinance(high).taxIncome,
        greaterThan(EconomyCalculator.computeFinance(low).taxIncome),
      );

      final math.Random rng = math.Random(5);
      EconomyCalculator.applySociety(
        low,
        EconomyCalculator.computeFinance(low),
        rng,
      );
      EconomyCalculator.applySociety(
        high,
        EconomyCalculator.computeFinance(high),
        rng,
      );
      expect(high.publicSatisfaction, lessThan(low.publicSatisfaction));
    });

    test('a shortfall becomes debt instead of a negative treasury', () {
      final GameState state = _newGame()
        ..treasuryCash = 0
        ..taxRate = 0
        ..militarySpendingLevel = 1
        ..subsidyLevel = 1;
      final TurnFinance finance = EconomyCalculator.computeFinance(state);
      EconomyCalculator.applyFinance(state, finance);
      expect(state.treasuryCash, 0);
      expect(state.debt, greaterThan(0));
    });

    test('gems are never granted by economic activity', () {
      final GameState state = _newGame();
      final int before = state.gems;
      EconomyCalculator.applyFinance(
        state,
        EconomyCalculator.computeFinance(state),
      );
      EconomyCalculator.applySectorDrift(state);
      EconomyCalculator.applyPopulation(state);
      expect(state.gems, before);
    });
  });

  group('construction', () {
    test('a landmark completes after its build time and applies its effects',
        () {
      final GameState state = _newGame()..treasuryCash = 2000000;
      final Landmark museum = LandmarksData.byId('national_museum')!;
      final double cultureBefore = state.culture;

      expect(ConstructionEngine.start(state, museum), isTrue);
      expect(state.underConstruction, hasLength(1));

      for (int i = 0; i < museum.buildTurns; i++) {
        ConstructionEngine.advance(state);
      }
      expect(state.underConstruction, isEmpty);
      expect(state.builtLandmarks.map((Landmark l) => l.id), contains(museum.id));
      expect(state.culture, greaterThan(cultureBefore));
    });

    test('requirements and funds are enforced', () {
      final GameState poor = _newGame()..treasuryCash = 1000;
      expect(
        ConstructionEngine.start(poor, LandmarksData.byId('national_museum')!),
        isFalse,
      );

      final GameState rich = _newGame()
        ..treasuryCash = 5000000
        ..technology = 10;
      expect(
        ConstructionEngine.start(rich, LandmarksData.byId('data_center')!),
        isFalse,
        reason: 'technology requirement should block this build',
      );
    });

    test('gems can rush a build but never below zero turns', () {
      final GameState state = _newGame()
        ..treasuryCash = 2000000
        ..gems = 100;
      final Landmark museum = LandmarksData.byId('national_museum')!;
      ConstructionEngine.start(state, museum);
      expect(
        ConstructionEngine.rushWithGems(state, museum.id, 100),
        isTrue,
      );
      expect(state.underConstruction.single.turnsRemaining, 0);
      expect(state.gems, lessThan(100));
    });
  });

  group('persistence', () {
    test('a state survives a round trip through its map form', () {
      final math.Random rng = math.Random(13);
      final GameState state = _newGame()..treasuryCash = 2000000;
      final TurnEngine engine = _engine(rng);
      ConstructionEngine.start(state, LandmarksData.byId('national_museum')!);
      for (int i = 0; i < 30; i++) {
        engine.advance(state);
      }

      final GameState restored = GameState.fromMap(
        state.toMap(),
        landmarkResolver: LandmarksData.byId,
      );

      expect(restored.turnNumber, state.turnNumber);
      expect(restored.inGameDate, state.inGameDate);
      expect(restored.treasuryCash, closeTo(state.treasuryCash, 0.001));
      expect(restored.population, state.population);
      expect(restored.countries.length, state.countries.length);
      expect(restored.builtLandmarks.length, state.builtLandmarks.length);
      expect(restored.unlockedAchievements, state.unlockedAchievements);
      expect(restored.lastEventTurns, state.lastEventTurns);
      expect(restored.economy, closeTo(state.economy, 0.001));
    });
  });
}

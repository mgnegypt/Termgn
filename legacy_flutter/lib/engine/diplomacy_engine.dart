import 'dart:math' as math;

import '../models/diplomacy_state.dart';
import '../models/enums.dart';
import '../models/game_state.dart';
import '../models/history_entry.dart';
import 'balance_config.dart';

/// Outcome of one country's war, after a turn of fighting.
class WarResult {
  const WarResult({
    required this.countryId,
    required this.countryName,
    required this.outcome,
    required this.playerScore,
    required this.enemyScore,
  });

  final String countryId;
  final String countryName;
  final WarOutcome outcome;
  final double playerScore;
  final double enemyScore;
}

/// Everything diplomacy changed this turn, for the turn summary panel.
class DiplomacyTurnReport {
  DiplomacyTurnReport({
    this.declaredWars = const <String>[],
    this.newSanctions = const <String>[],
    this.liftedSanctions = const <String>[],
    this.warResults = const <WarResult>[],
    this.aiOffers = const <DiplomaticOffer>[],
  });

  final List<String> declaredWars;
  final List<String> newSanctions;
  final List<String> liftedSanctions;
  final List<WarResult> warResults;
  final List<DiplomaticOffer> aiOffers;

  bool get isEmpty =>
      declaredWars.isEmpty &&
      newSanctions.isEmpty &&
      liftedSanctions.isEmpty &&
      warResults.isEmpty &&
      aiOffers.isEmpty;
}

/// A treaty an AI country proposes to the player.
class DiplomaticOffer {
  const DiplomaticOffer({
    required this.countryId,
    required this.countryName,
    required this.type,
  });

  final String countryId;
  final String countryName;
  final TreatyType type;
}

/// Rule-based diplomacy: relation drift, treaties, sanctions and simplified
/// war resolution.
class DiplomacyEngine {
  DiplomacyEngine({math.Random? rng}) : _rng = rng ?? math.Random();

  final math.Random _rng;

  // ── Per-turn tick ─────────────────────────────────────────────────────────

  /// Advances every relationship by one turn. [stances] maps a country id to
  /// the player's chosen war stance; missing entries default to defensive.
  DiplomacyTurnReport advanceTurn(
    GameState state, {
    Map<String, WarStance> stances = const <String, WarStance>{},
    bool debtStressed = false,
  }) {
    final List<String> declaredWars = <String>[];
    final List<String> newSanctions = <String>[];
    final List<String> liftedSanctions = <String>[];
    final List<WarResult> warResults = <WarResult>[];
    final List<DiplomaticOffer> offers = <DiplomaticOffer>[];

    for (final DiplomacyState country in state.countries.values) {
      _driftPower(country);

      if (country.atWar) {
        final WarResult result = _fightTurn(
          state,
          country,
          stances[country.countryId] ?? WarStance.defensive,
        );
        warResults.add(result);
        if (result.outcome != WarOutcome.ongoing) {
          _settleWar(state, country, result);
        }
        continue; // no relation drift or offers while fighting
      }

      _driftRelation(state, country, debtStressed: debtStressed);

      // Sanctions follow the relation level automatically.
      if (!country.sanctioned &&
          country.relation <= BalanceConfig.sanctionThreshold) {
        country.sanctioned = true;
        newSanctions.add(country.nameAr);
      } else if (country.sanctioned &&
          country.relation >= BalanceConfig.sanctionLiftThreshold) {
        country.sanctioned = false;
        liftedSanctions.add(country.nameAr);
      }

      // Hostile neighbours may escalate to war.
      if (country.behavior == AiBehavior.hostile &&
          country.relation <= BalanceConfig.warDeclarationThreshold &&
          !country.treaties.contains(TreatyType.nonAggression) &&
          _rng.nextDouble() < BalanceConfig.warDeclarationChance) {
        _startWar(state, country, declaredBy: country.nameAr);
        declaredWars.add(country.nameAr);
        continue;
      }

      final DiplomaticOffer? offer = _maybeOffer(state, country);
      if (offer != null) offers.add(offer);
    }

    state.clampAll();
    return DiplomacyTurnReport(
      declaredWars: declaredWars,
      newSanctions: newSanctions,
      liftedSanctions: liftedSanctions,
      warResults: warResults,
      aiOffers: offers,
    );
  }

  void _driftPower(DiplomacyState country) {
    final double swing = (_rng.nextDouble() * 2 - 1) *
        BalanceConfig.aiPowerDriftMax;
    country.economicPower += swing;
    country.militaryPower += swing * 0.6;
  }

  void _driftRelation(
    GameState state,
    DiplomacyState country, {
    required bool debtStressed,
  }) {
    double drift = switch (country.behavior) {
      AiBehavior.hostile => BalanceConfig.relationDriftHostile,
      AiBehavior.trader => BalanceConfig.relationDriftTrader,
      AiBehavior.neutral => BalanceConfig.relationDriftNeutral,
      AiBehavior.isolationist => BalanceConfig.relationDriftIsolationist,
    };

    if (country.hasEmbassy) drift += BalanceConfig.embassyRelationDrift;
    if (country.hasTradeDeal) drift += BalanceConfig.tradeDealRelationDrift;

    // Expansionist players unsettle everyone; state-led economies unsettle
    // traders.
    drift -= state.axisForeign * BalanceConfig.axisForeignRelationWeight;
    if (country.behavior == AiBehavior.trader) {
      drift -= state.axisEconomic * BalanceConfig.axisEconomicRelationWeight;
    }

    if (debtStressed) drift -= BalanceConfig.debtStressRelationPenalty;

    country.relation += drift;
  }

  DiplomaticOffer? _maybeOffer(GameState state, DiplomacyState country) {
    if (state.turnNumber - country.lastOfferTurn <
        BalanceConfig.aiOfferCooldown) {
      return null;
    }
    TreatyType? type;
    if (country.relation >= BalanceConfig.defensiveRelationRequirement &&
        !country.isAlly) {
      type = TreatyType.defensive;
    } else if (country.relation >= BalanceConfig.tradeRelationRequirement &&
        !country.hasTradeDeal &&
        country.behavior == AiBehavior.trader) {
      type = TreatyType.trade;
    } else if (country.relation >=
            BalanceConfig.nonAggressionRelationRequirement &&
        !country.treaties.contains(TreatyType.nonAggression) &&
        country.behavior == AiBehavior.hostile) {
      type = TreatyType.nonAggression;
    }
    if (type == null) return null;

    country.lastOfferTurn = state.turnNumber;
    return DiplomaticOffer(
      countryId: country.countryId,
      countryName: country.nameAr,
      type: type,
    );
  }

  // ── Player actions ────────────────────────────────────────────────────────

  /// Relation required to sign [type].
  static double relationRequirement(TreatyType type) => switch (type) {
        TreatyType.trade => BalanceConfig.tradeRelationRequirement,
        TreatyType.defensive => BalanceConfig.defensiveRelationRequirement,
        TreatyType.nonAggression =>
          BalanceConfig.nonAggressionRelationRequirement,
        TreatyType.embassy => BalanceConfig.nonAggressionRelationRequirement,
      };

  static double signRelationBonus(TreatyType type) => switch (type) {
        TreatyType.trade => BalanceConfig.tradeSignRelationBonus,
        TreatyType.defensive => BalanceConfig.defensiveSignRelationBonus,
        TreatyType.nonAggression => BalanceConfig.nonAggressionSignRelationBonus,
        TreatyType.embassy => BalanceConfig.embassySignRelationBonus,
      };

  /// True when the player may currently sign [type] with [countryId].
  bool canSign(GameState state, String countryId, TreatyType type) {
    final DiplomacyState? country = state.countries[countryId];
    if (country == null || country.atWar) return false;
    if (country.treaties.contains(type)) return false;
    if (type == TreatyType.embassy &&
        state.treasuryCash < BalanceConfig.embassyCost) {
      return false;
    }
    return country.relation >= relationRequirement(type);
  }

  /// Signs a treaty. Returns false when the requirements are not met.
  bool signTreaty(GameState state, String countryId, TreatyType type) {
    if (!canSign(state, countryId, type)) return false;
    final DiplomacyState country = state.countries[countryId]!;

    if (type == TreatyType.embassy) {
      state.treasuryCash -= BalanceConfig.embassyCost;
    }
    country.treaties.add(type);
    country.relation += signRelationBonus(type);

    state.history.add(HistoryEntry(
      turnNumber: state.turnNumber,
      date: state.inGameDate,
      title: 'اتفاقية جديدة مع ${country.nameAr}',
      detail: _treatyLabel(type),
      tag: HistoryTag.treaty,
    ));
    state.clampAll();
    return true;
  }

  /// Accepts an AI offer; identical to signing but bypasses the cash cost of
  /// an embassy invitation.
  bool acceptOffer(GameState state, DiplomaticOffer offer) {
    final DiplomacyState? country = state.countries[offer.countryId];
    if (country == null || country.atWar) return false;
    country.treaties.add(offer.type);
    country.relation += signRelationBonus(offer.type);
    state.history.add(HistoryEntry(
      turnNumber: state.turnNumber,
      date: state.inGameDate,
      title: 'قبول عرض ${country.nameAr}',
      detail: _treatyLabel(offer.type),
      tag: HistoryTag.treaty,
    ));
    state.clampAll();
    return true;
  }

  /// Cancels a treaty, at a relation cost equal to twice its signing bonus.
  void breakTreaty(GameState state, String countryId, TreatyType type) {
    final DiplomacyState? country = state.countries[countryId];
    if (country == null || !country.treaties.remove(type)) return;
    country.relation -= signRelationBonus(type) * 2;
    state.clampAll();
  }

  /// The player declares war.
  void declareWar(GameState state, String countryId) {
    final DiplomacyState? country = state.countries[countryId];
    if (country == null || country.atWar) return;
    _startWar(state, country, declaredBy: state.countryName);
  }

  static String _treatyLabel(TreatyType type) => switch (type) {
        TreatyType.trade => 'معاهدة تجارية',
        TreatyType.defensive => 'تحالف دفاعي',
        TreatyType.nonAggression => 'اتفاقية عدم اعتداء',
        TreatyType.embassy => 'سفارة',
      };

  // ── War ───────────────────────────────────────────────────────────────────

  void _startWar(
    GameState state,
    DiplomacyState country, {
    required String declaredBy,
  }) {
    country.atWar = true;
    country.warTurns = 0;
    country.relation = -100;
    country.treaties
      ..remove(TreatyType.trade)
      ..remove(TreatyType.defensive)
      ..remove(TreatyType.nonAggression);

    state.history.add(HistoryEntry(
      turnNumber: state.turnNumber,
      date: state.inGameDate,
      title: 'إعلان حرب: ${country.nameAr}',
      detail: 'أعلنها $declaredBy',
      tag: HistoryTag.war,
    ));
  }

  /// Player war strength, including allies who honour their defensive pact.
  double playerWarScore(GameState state, WarStance stance) {
    double score = (state.militarySecurity * BalanceConfig.warMilitaryWeight) +
        (state.economy * BalanceConfig.warEconomyWeight) +
        (state.technology * BalanceConfig.warTechnologyWeight) +
        (state.publicSatisfaction * BalanceConfig.warSatisfactionWeight);

    score += switch (stance) {
      WarStance.offensive => BalanceConfig.warStanceOffensiveBonus,
      WarStance.defensive => BalanceConfig.warStanceDefensiveBonus,
      WarStance.negotiate => 0,
    };

    final int allies = state.countries.values
        .where((DiplomacyState d) => d.isAlly && !d.atWar)
        .length;
    score += allies * BalanceConfig.allySupportBonus;
    return score;
  }

  double enemyWarScore(DiplomacyState enemy) =>
      (enemy.militaryPower * BalanceConfig.warMilitaryWeight) +
      (enemy.economicPower *
          (BalanceConfig.warEconomyWeight +
              BalanceConfig.warTechnologyWeight +
              BalanceConfig.warSatisfactionWeight));

  WarResult _fightTurn(
    GameState state,
    DiplomacyState enemy,
    WarStance stance,
  ) {
    enemy.warTurns += 1;

    const double swing = BalanceConfig.warRandomSwing;
    final double playerRoll = (_rng.nextDouble() * 2 - 1) * swing;
    final double enemyRoll = (_rng.nextDouble() * 2 - 1) * swing;

    final double playerScore = playerWarScore(state, stance) + playerRoll;
    final double enemyScore = enemyWarScore(enemy) + enemyRoll;

    _applyAttrition(state, enemy, stance);

    WarOutcome outcome = WarOutcome.ongoing;
    final double gap = playerScore - enemyScore;

    if (stance == WarStance.negotiate &&
        enemy.warTurns >= BalanceConfig.warMinimumTurns) {
      outcome = WarOutcome.negotiatedPeace;
    } else if (enemy.warTurns >= BalanceConfig.warMinimumTurns &&
        gap.abs() >= BalanceConfig.warDecisiveGap) {
      outcome = gap > 0 ? WarOutcome.victory : WarOutcome.defeat;
    } else if (enemy.warTurns >= BalanceConfig.warMaximumTurns) {
      outcome = WarOutcome.stalemate;
    }

    return WarResult(
      countryId: enemy.countryId,
      countryName: enemy.nameAr,
      outcome: outcome,
      playerScore: playerScore,
      enemyScore: enemyScore,
    );
  }

  void _applyAttrition(GameState state, DiplomacyState enemy, WarStance stance) {
    final double factor = switch (stance) {
      WarStance.defensive => BalanceConfig.warStanceDefensiveAttritionFactor,
      WarStance.offensive => BalanceConfig.warStanceOffensiveAttritionFactor,
      WarStance.negotiate => 1.0,
    };
    state.applyDelta(
      StateKeys.militarySecurity,
      -BalanceConfig.warAttritionMilitary * factor,
    );
    state.applyDelta(
      StateKeys.economy,
      -BalanceConfig.warAttritionEconomy * factor,
    );
    state.population = (state.population *
            (1 - BalanceConfig.warAttritionPopulationRate * factor))
        .round();

    enemy.militaryPower -= BalanceConfig.warAttritionMilitary * 0.8;
    enemy.economicPower -= BalanceConfig.warAttritionEconomy * 0.8;
    state.clampAll();
  }

  void _settleWar(GameState state, DiplomacyState enemy, WarResult result) {
    enemy.atWar = false;
    enemy.warTurns = 0;

    switch (result.outcome) {
      case WarOutcome.victory:
        state.applyDelta(StateKeys.economy, BalanceConfig.warVictoryEconomyBonus);
        state.applyDelta(
          StateKeys.publicSatisfaction,
          BalanceConfig.warVictorySatisfactionBonus,
        );
        state.treasuryCash += BalanceConfig.warVictoryReparations;
        enemy.relation = -60;
      case WarOutcome.defeat:
        state.applyDelta(
          StateKeys.economy,
          -BalanceConfig.warDefeatEconomyPenalty,
        );
        state.applyDelta(
          StateKeys.publicSatisfaction,
          -BalanceConfig.warDefeatSatisfactionPenalty,
        );
        state.debt += BalanceConfig.warDefeatReparations;
        enemy.relation = -40;
      case WarOutcome.stalemate:
        state.applyDelta(
          StateKeys.publicSatisfaction,
          -BalanceConfig.warStalemateSatisfactionPenalty,
        );
        enemy.relation = -50;
      case WarOutcome.negotiatedPeace:
        enemy.relation = -25;
        enemy.treaties.add(TreatyType.nonAggression);
      case WarOutcome.ongoing:
        return;
    }

    state.history.add(HistoryEntry(
      turnNumber: state.turnNumber,
      date: state.inGameDate,
      title: 'نهاية الحرب مع ${enemy.nameAr}',
      detail: _outcomeLabel(result.outcome),
      tag: HistoryTag.war,
    ));
    state.clampAll();
  }

  static String _outcomeLabel(WarOutcome outcome) => switch (outcome) {
        WarOutcome.victory => 'انتصار',
        WarOutcome.defeat => 'هزيمة',
        WarOutcome.stalemate => 'استنزاف بلا حاسم',
        WarOutcome.negotiatedPeace => 'سلام تفاوضي',
        WarOutcome.ongoing => 'مستمرة',
      };
}

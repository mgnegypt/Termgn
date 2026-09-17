import 'dart:math' as math;

import '../models/diplomacy_state.dart';
import '../models/enums.dart';
import '../models/game_state.dart';
import '../models/landmark.dart';
import 'balance_config.dart';

/// A full per-turn financial breakdown, shown to the player in the treasury
/// panel and the yearly report.
class TurnFinance {
  const TurnFinance({
    required this.taxIncome,
    required this.exportIncome,
    required this.tourismIncome,
    required this.customsIncome,
    required this.digitalIncome,
    required this.payroll,
    required this.maintenance,
    required this.militarySpending,
    required this.subsidies,
    required this.debtInterest,
    required this.warCost,
    required this.emergencyBorrowed,
  });

  final double taxIncome;
  final double exportIncome;
  final double tourismIncome;
  final double customsIncome;
  final double digitalIncome;

  final double payroll;
  final double maintenance;
  final double militarySpending;
  final double subsidies;
  final double debtInterest;
  final double warCost;

  /// Cash auto-borrowed because the treasury would have gone negative.
  final double emergencyBorrowed;

  double get totalIncome =>
      taxIncome + exportIncome + tourismIncome + customsIncome + digitalIncome;

  double get totalExpenses =>
      payroll + maintenance + militarySpending + subsidies + debtInterest +
      warCost;

  double get net => totalIncome - totalExpenses;

  Map<String, double> get incomeBreakdown => <String, double>{
        'ضرائب': taxIncome,
        'تصدير': exportIncome,
        'سياحة': tourismIncome,
        'جمارك': customsIncome,
        'اقتصاد رقمي': digitalIncome,
      };

  Map<String, double> get expenseBreakdown => <String, double>{
        'رواتب': payroll,
        'صيانة': maintenance,
        'إنفاق عسكري': militarySpending,
        'دعم السلع': subsidies,
        'فوائد الدين': debtInterest,
        'تكاليف الحرب': warCost,
      };
}

/// Pure economic simulation: money, sectors, society and population.
///
/// Every coefficient comes from [BalanceConfig]; this class holds no numbers.
abstract final class EconomyCalculator {
  // ── Money ─────────────────────────────────────────────────────────────────

  /// Computes the turn's finances without mutating [state].
  static TurnFinance computeFinance(GameState state) {
    final int tradeDeals = state.countries.values
        .where((DiplomacyState d) => d.hasTradeDeal && !d.atWar)
        .length;
    final int sanctions = state.sanctionCount;
    final int wars = state.warEnemies.length;

    final double economyMultiplier = BalanceConfig.taxEconomyFloor +
        (state.economy / 100) * BalanceConfig.taxEconomySlope;

    final double taxIncome = state.population *
        BalanceConfig.taxPerCapita *
        state.taxRate *
        economyMultiplier;

    final double rawExports =
        (state.agriculture * BalanceConfig.agricultureExportPerPoint) +
            (state.industry * BalanceConfig.industryExportPerPoint) +
            (state.energy * BalanceConfig.energyExportPerPoint);
    final double exportModifier = math.max(
      0,
      1 +
          (tradeDeals * BalanceConfig.exportBonusPerTradeDeal) -
          (sanctions * BalanceConfig.exportPenaltyPerSanction),
    );
    final double exportIncome = rawExports * exportModifier;

    final double landmarkTourism = state.builtLandmarks.fold<double>(
      0,
      (double sum, Landmark l) => sum + l.tourismIncomePerTurn,
    );
    final double securityFactor = BalanceConfig.tourismSecurityFloor +
        (state.militarySecurity / 100) * BalanceConfig.tourismSecuritySlope;
    final double tourismIncome =
        ((state.tourism * BalanceConfig.tourismIncomePerPoint) +
                landmarkTourism) *
            securityFactor *
            (wars > 0 ? BalanceConfig.tourismWarMultiplier : 1.0);

    final double customsIncome =
        tradeDeals * BalanceConfig.customsPerTradeDeal * economyMultiplier;

    final double digitalIncome =
        state.technology * BalanceConfig.digitalEconomyPerTechPoint;

    final double payroll = BalanceConfig.baseGovernmentPayroll +
        (state.population * BalanceConfig.payrollPerCapita);

    final double landmarkUpkeep = state.builtLandmarks.fold<double>(
      0,
      (double sum, Landmark l) => sum + l.maintenancePerTurn,
    );
    final double maintenance = landmarkUpkeep +
        (state.economy * BalanceConfig.institutionalCostPerEconomyPoint) +
        (state.industry * BalanceConfig.industryUpkeepPerPoint) +
        (state.health * BalanceConfig.healthUpkeepPerPoint) +
        (state.education * BalanceConfig.educationUpkeepPerPoint);

    final double militarySpending =
        state.militarySpendingLevel * BalanceConfig.militaryBudgetMax;
    final double subsidies = state.subsidyLevel * BalanceConfig.subsidyBudgetMax;
    final double debtInterest = state.debt * BalanceConfig.debtInterestRate;
    final double warCost = wars * BalanceConfig.warCostPerTurn;

    final double income =
        taxIncome + exportIncome + tourismIncome + customsIncome + digitalIncome;
    final double expenses = payroll +
        maintenance +
        militarySpending +
        subsidies +
        debtInterest +
        warCost;

    final double projected = state.treasuryCash + income - expenses;
    final double emergencyBorrowed = projected < 0 ? -projected : 0;

    return TurnFinance(
      taxIncome: taxIncome,
      exportIncome: exportIncome,
      tourismIncome: tourismIncome,
      customsIncome: customsIncome,
      digitalIncome: digitalIncome,
      payroll: payroll,
      maintenance: maintenance,
      militarySpending: militarySpending,
      subsidies: subsidies,
      debtInterest: debtInterest,
      warCost: warCost,
      emergencyBorrowed: emergencyBorrowed,
    );
  }

  /// Settles the treasury, converting any shortfall into debt.
  static void applyFinance(GameState state, TurnFinance finance) {
    state.treasuryCash += finance.net;
    if (finance.emergencyBorrowed > 0) {
      state.debt +=
          finance.emergencyBorrowed * BalanceConfig.emergencyLoanMarkup;
      state.treasuryCash = 0;
    }
    state.clampAll();
  }

  /// Converts a negative treasury into debt. Called after any mutation that
  /// can spend money outside the turn settlement (decisions, purchases), so a
  /// negative balance can never be displayed.
  static void settleNegativeTreasury(GameState state) {
    if (state.treasuryCash >= 0) return;
    state.debt += -state.treasuryCash * BalanceConfig.emergencyLoanMarkup;
    state.treasuryCash = 0;
    state.clampAll();
  }

  /// True when debt has grown large relative to the turn's income.
  static bool isDebtStressed(GameState state, TurnFinance finance) {
    if (finance.totalIncome <= 0) return state.debt > 0;
    return state.debt / finance.totalIncome > BalanceConfig.debtStressThreshold;
  }

  /// Grants a voluntary loan from the international fund.
  static void takeLoan(GameState state, double amount) {
    final double granted = amount.clamp(0.0, BalanceConfig.maxLoanPerRequest);
    state.treasuryCash += granted;
    state.debt += granted;
    for (final DiplomacyState d in state.countries.values) {
      d.relation -= BalanceConfig.loanRelationPenalty;
    }
    state.clampAll();
  }

  /// Repays debt from the treasury, capped by both balances.
  static void repayDebt(GameState state, double amount) {
    final double paid = amount.clamp(
      0.0,
      math.min(state.treasuryCash, state.debt),
    );
    state.treasuryCash -= paid;
    state.debt -= paid;
    state.clampAll();
  }

  // ── Energy ────────────────────────────────────────────────────────────────

  /// Energy demand for the turn, including the seasonal modifier.
  static double energyDemand(GameState state) {
    final double base =
        (state.industry * BalanceConfig.energyDemandPerIndustryPoint) +
            (state.technology * BalanceConfig.energyDemandPerTechPoint);
    final double seasonal = switch (state.currentSeason) {
      Season.winter => BalanceConfig.winterEnergyDemandBonus,
      Season.summer => BalanceConfig.summerEnergyDemandBonus,
      Season.spring || Season.autumn => 1.0,
    };
    return base * seasonal;
  }

  /// Positive = surplus, negative = shortfall.
  static double energyBalance(GameState state) =>
      state.energy - energyDemand(state);

  // ── Sectors ───────────────────────────────────────────────────────────────

  /// Applies natural decay, seasonal effects and cross-sector spillover.
  static void applySectorDrift(GameState state) {
    const double decay = BalanceConfig.sectorNaturalDecay;
    for (final String key in StateKeys.investableSectors) {
      state.applyDelta(key, -decay);

      // Subsistence floor keeps a collapsed sector recoverable.
      final double level = state.readKey(key);
      if (level < BalanceConfig.sectorFloor) {
        state.applyDelta(
          key,
          (BalanceConfig.sectorFloor - level) *
              BalanceConfig.sectorFloorRecoveryRate,
        );
      }
    }

    state.applyDelta(
      StateKeys.agriculture,
      switch (state.currentSeason) {
        Season.spring => BalanceConfig.springAgricultureBonus,
        Season.summer => BalanceConfig.summerAgriculturePenalty,
        Season.autumn => BalanceConfig.autumnAgricultureBonus,
        Season.winter => BalanceConfig.winterAgriculturePenalty,
      },
    );

    // Food security follows agriculture, fighting population pressure.
    final double populationPressure =
        (state.population / 1000000) * BalanceConfig.foodSecurityPopulationPressure;
    state.applyDelta(
      StateKeys.foodSecurity,
      (state.agriculture - state.foodSecurity) *
              BalanceConfig.foodSecurityAgricultureWeight -
          populationPressure +
          ((state.subsidyLevel - BalanceConfig.neutralSubsidyLevel) *
              BalanceConfig.subsidyFoodSupport),
    );

    // Long-term education payoff.
    state.applyDelta(
      StateKeys.technology,
      state.education * BalanceConfig.educationToTechnologyRate,
    );
    state.applyDelta(
      StateKeys.culture,
      state.education * BalanceConfig.educationToCultureRate,
    );
    state.applyDelta(
      StateKeys.cyberSecurity,
      state.technology * BalanceConfig.technologyToCyberRate,
    );

    // Culture and tourism from standing landmarks.
    final int landmarkCount = state.builtLandmarks.length;
    state.applyDelta(
      StateKeys.culture,
      landmarkCount * BalanceConfig.culturePerLandmarkPerTurn,
    );
    state.applyDelta(
      StateKeys.tourism,
      (state.culture * BalanceConfig.tourismCultureWeight * 0.1) +
          (landmarkCount * BalanceConfig.tourismPerLandmark),
    );

    // Permanent per-turn effects authored on each landmark.
    for (final Landmark landmark in state.builtLandmarks) {
      landmark.perTurnEffects.forEach(state.applyDelta);
    }

    state.clampAll();
  }

  /// Weighted index of the productive sectors: the level the economy tends
  /// toward.
  static double sectorIndex(GameState state) =>
      (state.industry * BalanceConfig.economyIndustryWeight) +
      (state.technology * BalanceConfig.economyTechnologyWeight) +
      (state.agriculture * BalanceConfig.economyAgricultureWeight) +
      (state.energy * BalanceConfig.economyEnergyWeight) +
      (state.education * BalanceConfig.economyEducationWeight) +
      (state.tourism * BalanceConfig.economyTourismWeight);

  /// Moves the economy indicator toward its target (sector index plus the
  /// baseline offset), then applies the
  /// penalties that suppress growth (sanctions, war, debt stress).
  static void applyEconomicGrowth(GameState state, {required bool debtStressed}) {
    final double target =
        sectorIndex(state) + BalanceConfig.economyBaselineOffset;
    final double gap = target - state.economy;
    double delta = gap * BalanceConfig.economyReversionRate;

    delta -= state.sanctionCount * BalanceConfig.economySanctionPenalty;
    delta -= state.warEnemies.length * BalanceConfig.economyWarPenalty;
    if (debtStressed) delta -= BalanceConfig.economyDebtStressPenalty;

    state.applyDelta(StateKeys.economy, delta);
    state.clampAll();
  }

  /// Environment damage from industry, offset by clean energy, plus recovery.
  static void applyEnvironment(GameState state) {    final double pollution = state.industry *
        BalanceConfig.industryPollutionPerPoint *
        (1 - (state.cleanEnergyRatio * BalanceConfig.cleanEnergyOffset));
    state.applyDelta(
      StateKeys.environment,
      BalanceConfig.environmentRecoveryRate - pollution,
    );

    final double shortfall = -energyBalance(state);
    if (shortfall > 0) {
      state.applyDelta(
        StateKeys.economy,
        -shortfall * BalanceConfig.energyShortfallEconomyPenalty,
      );
    }
    state.clampAll();
  }

  // ── Society ───────────────────────────────────────────────────────────────

  /// Updates class satisfaction, public satisfaction and digital opinion.
  static void applySociety(GameState state, TurnFinance finance, math.Random rng) {
    final double taxPressure =
        (state.taxRate - BalanceConfig.comfortableTaxRate);
    final double subsidyComfort =
        (state.subsidyLevel - BalanceConfig.neutralSubsidyLevel);

    // Class-level targets, smoothed by inertia.
    final double workersTarget = 50 +
        (subsidyComfort * BalanceConfig.workersSubsidyWeight * 10) +
        ((state.foodSecurity - 50) * BalanceConfig.workersFoodWeight * 10) -
        (taxPressure * BalanceConfig.taxSatisfactionSensitivity * 0.5);
    final double middleTarget = 50 +
        ((state.economy - 50) * BalanceConfig.middleClassEconomyWeight * 10) -
        (taxPressure * BalanceConfig.middleClassTaxWeight);
    final double eliteTarget = 50 +
        ((state.economy - 50) * BalanceConfig.eliteEconomyWeight * 10) -
        (taxPressure * BalanceConfig.eliteTaxWeight);

    const double inertia = BalanceConfig.classSatisfactionInertia;
    state.workersSatisfaction =
        (state.workersSatisfaction * inertia) + (workersTarget * (1 - inertia));
    state.middleClassSatisfaction = (state.middleClassSatisfaction * inertia) +
        (middleTarget * (1 - inertia));
    state.eliteSatisfaction =
        (state.eliteSatisfaction * inertia) + (eliteTarget * (1 - inertia));

    // Public satisfaction.
    double delta = 0;
    delta -= taxPressure * BalanceConfig.taxSatisfactionSensitivity * 0.1;
    delta += subsidyComfort * BalanceConfig.subsidySatisfactionSensitivity * 0.1;
    delta += (state.economy - 50) * BalanceConfig.economySatisfactionWeight;
    delta += (state.health - 50) * BalanceConfig.healthSatisfactionWeight;
    delta += (state.education - 50) * BalanceConfig.educationSatisfactionWeight;

    if (state.foodSecurity < BalanceConfig.foodCrisisThreshold) {
      delta -= (BalanceConfig.foodCrisisThreshold - state.foodSecurity) *
          BalanceConfig.foodCrisisPenaltyPerPoint;
    }
    delta -= state.warEnemies.length * BalanceConfig.warSatisfactionPenalty;
    delta -= state.sanctionCount * BalanceConfig.sanctionSatisfactionPenalty;
    if (isDebtStressed(state, finance)) {
      delta -= BalanceConfig.debtStressSatisfactionPenalty;
    }
    delta += (state.classSatisfactionAverage - state.publicSatisfaction) *
        BalanceConfig.satisfactionReversionRate;

    state.applyDelta(StateKeys.publicSatisfaction, delta);

    // Digital opinion: noisier, tech-sensitive echo of public mood.
    final double noise =
        (rng.nextDouble() * 2 - 1) * BalanceConfig.digitalOpinionVolatility;
    final double digitalDelta =
        (state.publicSatisfaction - state.digitalOpinion) *
                BalanceConfig.digitalOpinionReversionRate +
            (state.technology * BalanceConfig.digitalOpinionTechWeight) +
            noise;
    state.applyDelta(StateKeys.digitalOpinion, digitalDelta);

    state.clampAll();
  }

  // ── Population ────────────────────────────────────────────────────────────

  static void applyPopulation(GameState state) {
    double rate = BalanceConfig.baseGrowthRate +
        ((state.health - 50) / 100) * BalanceConfig.healthGrowthSensitivity;
    if (state.foodSecurity < BalanceConfig.foodCrisisThreshold) {
      rate -= BalanceConfig.foodCrisisGrowthPenalty;
    }

    final double attractiveness = (state.economy *
                BalanceConfig.migrationEconomyWeight) +
            (state.publicSatisfaction * BalanceConfig.migrationSatisfactionWeight) +
            (state.health * BalanceConfig.migrationHealthWeight) +
            (state.militarySecurity * BalanceConfig.migrationSecurityWeight) -
            50 -
            (state.isAtWar ? BalanceConfig.migrationWarPenalty : 0);

    state.migrationBalance =
        state.population * attractiveness * BalanceConfig.migrationScale / 100;

    state.population =
        (state.population * (1 + rate) + state.migrationBalance).round();
    state.clampAll();
  }

  // ── Investment ────────────────────────────────────────────────────────────

  /// Cost of raising [sectorKey] by [points], including diminishing returns
  /// based on the sector's current level.
  static double sectorInvestmentCost(GameState state, String sectorKey,
      double points) {
    final double level = state.readKey(sectorKey);
    final double multiplier =
        1 + (level / 100) * BalanceConfig.sectorInvestmentCostGrowth;
    return points * BalanceConfig.sectorInvestmentCostPerPoint * multiplier;
  }

  /// Invests in a sector if the treasury allows it. Returns true on success.
  ///
  /// Only [StateKeys.investableSectors] may be targeted; food security is
  /// derived from agriculture.
  static bool investInSector(GameState state, String sectorKey, double points) {
    if (!StateKeys.investableSectors.contains(sectorKey)) return false;
    final double capped =
        points.clamp(0.0, BalanceConfig.maxSectorInvestmentPerTurn);
    final double cost = sectorInvestmentCost(state, sectorKey, capped);
    if (capped <= 0 || state.treasuryCash < cost) return false;
    state.treasuryCash -= cost;
    state.applyDelta(sectorKey, capped);
    state.clampAll();
    return true;
  }
}

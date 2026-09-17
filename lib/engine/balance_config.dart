/// Every balance number in MGN lives here.
///
/// Rule from the design document: no magic numbers anywhere else in the code
/// base. A balance request must be a one-number change in this file.
abstract final class BalanceConfig {
  // ══════════════════════════════════════════════════════════════════════════
  // INCOME
  // ══════════════════════════════════════════════════════════════════════════

  /// Soft currency collected per citizen per turn at 100% tax and 100% economy.
  static const double taxPerCapita = 0.118;

  /// Economy acts as a multiplier floor+slope on tax yield:
  /// `taxEconomyFloor + economy/100 * taxEconomySlope`.
  static const double taxEconomyFloor = 0.60;
  static const double taxEconomySlope = 0.80;

  /// Export yield per sector point, per turn.
  static const double agricultureExportPerPoint = 340;
  static const double industryExportPerPoint = 650;
  static const double energyExportPerPoint = 450;

  /// Each active trade treaty lifts export income by this fraction.
  static const double exportBonusPerTradeDeal = 0.09;

  /// Each active sanction cuts export income by this fraction.
  static const double exportPenaltyPerSanction = 0.16;

  /// Tourism revenue per tourism point, before security/war modifiers.
  static const double tourismIncomePerPoint = 180;

  /// Tourism is scaled by `securityFloor + militarySecurity/100 * slope`.
  static const double tourismSecurityFloor = 0.35;
  static const double tourismSecuritySlope = 0.65;

  /// Multiplier applied to tourism income while any war is active.
  static const double tourismWarMultiplier = 0.35;

  /// Customs revenue per trade treaty, scaled by economy.
  static const double customsPerTradeDeal = 6500;

  /// Digital economy revenue per technology point.
  static const double digitalEconomyPerTechPoint = 240;

  // ══════════════════════════════════════════════════════════════════════════
  // EXPENSES
  // ══════════════════════════════════════════════════════════════════════════

  /// Fixed ministry payroll per turn.
  static const double baseGovernmentPayroll = 38000;

  /// Payroll scaling with population.
  static const double payrollPerCapita = 0.008;

  /// A larger economy means a larger state to run: bureaucracy, regulation
  /// and services scale with the economy indicator. This is the main brake on
  /// late-game snowballing.
  static const double institutionalCostPerEconomyPoint = 350;

  /// Infrastructure upkeep per industry point.
  static const double industryUpkeepPerPoint = 140;

  /// Health and education services upkeep per point.
  static const double healthUpkeepPerPoint = 120;
  static const double educationUpkeepPerPoint = 100;

  /// Military budget at `militarySpendingLevel == 1.0`.
  static const double militaryBudgetMax = 160000;

  /// Subsidy budget (food + energy) at `subsidyLevel == 1.0`.
  static const double subsidyBudgetMax = 150000;

  /// Interest charged on outstanding debt each turn.
  static const double debtInterestRate = 0.010;

  /// Extra war spending per turn, per active war.
  static const double warCostPerTurn = 120000;

  // ══════════════════════════════════════════════════════════════════════════
  // DEBT
  // ══════════════════════════════════════════════════════════════════════════

  /// A negative treasury is converted into debt automatically (emergency
  /// borrowing) with this penalty markup.
  static const double emergencyLoanMarkup = 1.10;

  /// Debt above this multiple of one turn's income hurts relations and
  /// satisfaction.
  static const double debtStressThreshold = 6.0;
  static const double debtStressSatisfactionPenalty = 1.6;
  static const double debtStressRelationPenalty = 0.8;

  /// Maximum voluntary loan the international fund will grant per request.
  static const double maxLoanPerRequest = 400000;

  /// Relation hit applied to every country when taking a voluntary loan.
  static const double loanRelationPenalty = 1.5;

  // ══════════════════════════════════════════════════════════════════════════
  // SATISFACTION & SOCIETY
  // ══════════════════════════════════════════════════════════════════════════

  /// Tax rate the public accepts without complaint.
  static const double comfortableTaxRate = 0.24;

  /// Satisfaction points lost per 1.0 of tax above the comfortable rate.
  static const double taxSatisfactionSensitivity = 26.0;

  /// Subsidy level the public treats as neutral.
  static const double neutralSubsidyLevel = 0.35;
  static const double subsidySatisfactionSensitivity = 9.0;

  /// Food security below this triggers a hunger penalty.
  static const double foodCrisisThreshold = 40;
  static const double foodCrisisPenaltyPerPoint = 0.15;

  /// Services pull satisfaction toward their own level at this rate.
  static const double healthSatisfactionWeight = 0.035;
  static const double educationSatisfactionWeight = 0.020;
  static const double economySatisfactionWeight = 0.040;

  /// Satisfaction lost per turn of active war, and per active sanction.
  static const double warSatisfactionPenalty = 1.8;
  static const double sanctionSatisfactionPenalty = 0.9;

  /// Satisfaction regained per turn when nothing is wrong (mean reversion
  /// toward the class-satisfaction average).
  static const double satisfactionReversionRate = 0.20;

  /// Class-specific reactions. Workers feel subsidies and food; the elite
  /// feels tax and economy.
  static const double workersSubsidyWeight = 12.0;
  static const double workersFoodWeight = 0.06;
  static const double middleClassEconomyWeight = 0.04;
  static const double middleClassTaxWeight = 18.0;
  static const double eliteTaxWeight = 30.0;
  static const double eliteEconomyWeight = 0.05;

  /// Per-turn smoothing applied to every class satisfaction value.
  static const double classSatisfactionInertia = 0.75;

  // ══════════════════════════════════════════════════════════════════════════
  // DIGITAL OPINION (modern-era indicator)
  // ══════════════════════════════════════════════════════════════════════════

  /// Digital opinion drifts toward public satisfaction, but faster and noisier.
  static const double digitalOpinionReversionRate = 0.18;
  static const double digitalOpinionVolatility = 2.5;

  /// Technology and cyber security raise the ceiling of digital opinion.
  static const double digitalOpinionTechWeight = 0.015;

  // ══════════════════════════════════════════════════════════════════════════
  // ENVIRONMENT & ENERGY
  // ══════════════════════════════════════════════════════════════════════════

  /// Environment damage per industry point per turn at 0% clean energy.
  static const double industryPollutionPerPoint = 0.012;

  /// Clean energy ratio offsets pollution up to this fraction.
  static const double cleanEnergyOffset = 0.9;

  /// Natural environmental recovery per turn.
  static const double environmentRecoveryRate = 0.45;

  /// Seasonal energy demand multipliers.
  static const double winterEnergyDemandBonus = 1.18;
  static const double summerEnergyDemandBonus = 1.10;

  /// Energy shortfall (demand over supply) penalty to economy per point.
  static const double energyShortfallEconomyPenalty = 0.05;

  /// Energy demand per industry point and per technology point.
  static const double energyDemandPerIndustryPoint = 0.55;
  static const double energyDemandPerTechPoint = 0.25;

  // ══════════════════════════════════════════════════════════════════════════
  // ECONOMIC GROWTH
  // ══════════════════════════════════════════════════════════════════════════

  /// The economy indicator drifts toward a weighted index of the productive
  /// sectors. This is the core loop: invest in sectors → economy grows → tax
  /// income grows.
  static const double economyIndustryWeight = 0.30;
  static const double economyTechnologyWeight = 0.20;
  static const double economyAgricultureWeight = 0.15;
  static const double economyEnergyWeight = 0.15;
  static const double economyEducationWeight = 0.10;
  static const double economyTourismWeight = 0.10;

  /// A developed state sustains an economy somewhat above its raw sector
  /// average (services, trade, institutions). Added to the sector index to
  /// form the economy's target level.
  static const double economyBaselineOffset = 12.0;

  /// Share of the gap to the target closed each turn.
  static const double economyReversionRate = 0.07;

  /// Economy penalty per active sanction and per active war.
  static const double economySanctionPenalty = 0.25;
  static const double economyWarPenalty = 0.40;

  /// Debt stress drags growth as well as morale.
  static const double economyDebtStressPenalty = 0.30;

  // ══════════════════════════════════════════════════════════════════════════
  // SECTOR DRIFT
  // ══════════════════════════════════════════════════════════════════════════

  /// Sectors decay slowly without investment.
  static const double sectorNaturalDecay = 0.06;

  /// Subsistence floor: land, clinics and schools do not vanish entirely.
  /// A sector below this level recovers toward it, which keeps a collapsed
  /// state recoverable instead of locking the player into a death spiral.
  static const double sectorFloor = 20.0;
  static const double sectorFloorRecoveryRate = 0.30;

  /// Seasonal agriculture modifiers (added to agriculture each turn).
  static const double springAgricultureBonus = 0.55;
  static const double summerAgriculturePenalty = -0.30;
  static const double autumnAgricultureBonus = 0.35;
  static const double winterAgriculturePenalty = -0.45;

  /// Food security tracks agriculture, offset by population pressure.
  static const double foodSecurityAgricultureWeight = 0.12;
  static const double foodSecurityPopulationPressure = 0.015;

  /// Food and energy subsidies also prop up food security directly, giving
  /// the subsidy dial a real strategic use beyond morale.
  static const double subsidyFoodSupport = 5.0;

  /// Education slowly lifts technology and culture (long-term payoff).
  static const double educationToTechnologyRate = 0.012;
  static const double educationToCultureRate = 0.008;

  /// Technology slowly lifts cyber security.
  static const double technologyToCyberRate = 0.030;

  /// Culture generated per completed landmark per turn.
  static const double culturePerLandmarkPerTurn = 0.08;

  /// Tourism tracks culture and landmark count.
  static const double tourismCultureWeight = 0.05;
  static const double tourismPerLandmark = 0.10;

  /// Cost in cash to raise one sector by one point (investment screen).
  static const double sectorInvestmentCostPerPoint = 14000;

  /// Diminishing returns: investment cost is multiplied by
  /// `1 + level/100 * sectorInvestmentCostGrowth`, so pushing a sector from 80
  /// to 90 costs far more than from 20 to 30.
  static const double sectorInvestmentCostGrowth = 5.0;

  /// Maximum points a single sector may be raised per turn by investment.
  static const double maxSectorInvestmentPerTurn = 4;

  // ══════════════════════════════════════════════════════════════════════════
  // POPULATION
  // ══════════════════════════════════════════════════════════════════════════

  /// Base natural growth per turn (fraction of population).
  static const double baseGrowthRate = 0.0018;

  /// Health shifts growth: `(health - 50) / 100 * healthGrowthSensitivity`.
  static const double healthGrowthSensitivity = 0.0022;

  /// Food crisis suppresses growth by this fraction.
  static const double foodCrisisGrowthPenalty = 0.0030;

  /// Migration attractiveness is a weighted blend; the result is a fraction of
  /// population moving in/out per turn.
  static const double migrationScale = 0.0010;
  static const double migrationEconomyWeight = 0.4;
  static const double migrationSatisfactionWeight = 0.3;
  static const double migrationHealthWeight = 0.2;
  static const double migrationSecurityWeight = 0.1;

  /// Migration penalty while at war.
  static const double migrationWarPenalty = 25.0;

  // ══════════════════════════════════════════════════════════════════════════
  // EVENT ENGINE
  // ══════════════════════════════════════════════════════════════════════════

  /// Default cooldown, in turns, before an event may repeat.
  static const int defaultEventCooldown = 18;

  /// Events drawn per turn (a second one only fires on a dice roll).
  static const int maxEventsPerTurn = 2;
  static const double secondEventChance = 0.28;

  /// Crisis events ignore the standard cooldown and use this shorter one.
  static const int crisisCooldown = 6;

  /// Probability that the engine looks for a crisis event first, when crisis
  /// conditions are met.
  static const double crisisPriorityChance = 0.85;

  /// Turn number before which no event fires (tutorial breathing room).
  static const int firstEventTurn = 3;

  /// Global multiplier on the cash cost of every decision option. Authored
  /// content keeps readable round numbers; this scales them all at once
  /// against the income model.
  static const double decisionCostMultiplier = 0.3;

  // ══════════════════════════════════════════════════════════════════════════
  // DIPLOMACY
  // ══════════════════════════════════════════════════════════════════════════

  /// Passive relation drift per turn, by AI behaviour.
  static const double relationDriftHostile = -0.45;
  static const double relationDriftTrader = 0.25;
  static const double relationDriftNeutral = 0.05;
  static const double relationDriftIsolationist = -0.05;

  /// Relation drift bonus per turn from an embassy and a trade deal.
  static const double embassyRelationDrift = 0.55;
  static const double tradeDealRelationDrift = 0.35;

  /// Traders react to the player's economic axis, hawks to the foreign axis.
  static const double axisForeignRelationWeight = 0.010;
  static const double axisEconomicRelationWeight = 0.006;

  /// Relation below this triggers automatic sanctions; above this they lift.
  static const double sanctionThreshold = -55;
  static const double sanctionLiftThreshold = -35;

  /// Relation below this lets a hostile AI declare war.
  static const double warDeclarationThreshold = -75;

  /// Per-turn chance a qualifying hostile AI actually declares war.
  static const double warDeclarationChance = 0.12;

  /// Minimum turns between AI diplomatic proposals to the player.
  static const int aiOfferCooldown = 10;

  /// Cost of establishing an embassy.
  static const double embassyCost = 85000;

  /// Relation granted immediately by signing each treaty type.
  static const double tradeSignRelationBonus = 8;
  static const double defensiveSignRelationBonus = 12;
  static const double nonAggressionSignRelationBonus = 5;
  static const double embassySignRelationBonus = 6;

  /// Relation required before a treaty may be proposed.
  static const double tradeRelationRequirement = 15;
  static const double defensiveRelationRequirement = 45;
  static const double nonAggressionRelationRequirement = -10;

  /// Slow power drift per turn for AI countries.
  static const double aiPowerDriftMax = 0.35;

  // ══════════════════════════════════════════════════════════════════════════
  // WAR
  // ══════════════════════════════════════════════════════════════════════════

  /// Weight of each factor in the war strength score.
  static const double warMilitaryWeight = 0.55;
  static const double warEconomyWeight = 0.20;
  static const double warTechnologyWeight = 0.10;
  static const double warSatisfactionWeight = 0.15;

  /// Random swing applied to each side's score, in points.
  static const double warRandomSwing = 12.0;

  /// Stance modifiers applied to the player's score.
  static const double warStanceOffensiveBonus = 8.0;
  static const double warStanceDefensiveBonus = 4.0;

  /// Defensive stance reduces attrition; offensive increases it.
  static const double warStanceDefensiveAttritionFactor = 0.6;
  static const double warStanceOffensiveAttritionFactor = 1.4;

  /// Base attrition per war turn.
  static const double warAttritionMilitary = 2.2;
  static const double warAttritionEconomy = 1.4;
  static const double warAttritionPopulationRate = 0.0012;

  /// Score gap required to resolve a war, and minimum war duration in turns.
  static const double warDecisiveGap = 18.0;
  static const int warMinimumTurns = 3;

  /// Turns after which a war is forced into a stalemate.
  static const int warMaximumTurns = 14;

  /// Each ally joining shifts the player's score by this much.
  static const double allySupportBonus = 10.0;

  /// Outcome effects.
  static const double warVictoryEconomyBonus = 6;
  static const double warVictorySatisfactionBonus = 12;
  static const double warVictoryReparations = 250000;
  static const double warDefeatEconomyPenalty = 12;
  static const double warDefeatSatisfactionPenalty = 18;
  static const double warDefeatReparations = 300000;
  static const double warStalemateSatisfactionPenalty = 6;

  // ══════════════════════════════════════════════════════════════════════════
  // CALENDAR
  // ══════════════════════════════════════════════════════════════════════════

  /// Years between legitimacy referendums.
  static const int referendumIntervalYears = 4;

  /// Legitimacy required to pass a referendum.
  static const double referendumPassThreshold = 45;

  /// Legitimacy tracks satisfaction and the nation score.
  static const double legitimacySatisfactionWeight = 0.06;
  static const double legitimacyScoreWeight = 0.04;

  /// Legitimacy penalty for failing a referendum.
  static const double referendumFailurePenalty = 15;

  // ══════════════════════════════════════════════════════════════════════════
  // GEMS (hard currency)
  // ══════════════════════════════════════════════════════════════════════════

  /// Gems granted by the yearly report, scaled by performance.
  static const int yearlyReportGemsBase = 5;
  static const int yearlyReportGemsBonusPerTenScore = 1;

  /// Gem cost to instantly finish one construction turn.
  static const int gemsPerConstructionTurnSkip = 3;

  /// Gem cost to re-roll the current decision, and to cancel a crisis penalty.
  static const int gemsPerDecisionReroll = 5;

  /// Cash granted per gem when converting gems to cash (soft sink, never the
  /// reverse — gems must not be farmable).
  static const double cashPerGem = 12000;
}

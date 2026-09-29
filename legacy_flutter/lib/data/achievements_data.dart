import '../models/achievement.dart';
import '../models/diplomacy_state.dart';
import '../models/game_state.dart';
import '../models/history_entry.dart';
import '../models/landmark.dart';

/// 18 achievements (MVP target 15-20), each granting hard currency once.
abstract final class AchievementsData {
  static final List<Achievement> all = <Achievement>[
    Achievement(
      id: 'first_landmark',
      titleAr: 'أول معلم',
      descriptionAr: 'أكمل بناء أول معلم في الدولة.',
      gemReward: 5,
      condition: (GameState s) => s.builtLandmarks.isNotEmpty,
    ),
    Achievement(
      id: 'five_landmarks',
      titleAr: 'عصر البناء',
      descriptionAr: 'أكمل بناء 5 معالم.',
      gemReward: 12,
      condition: (GameState s) => s.builtLandmarks.length >= 5,
    ),
    Achievement(
      id: 'ten_landmarks',
      titleAr: 'حضارة راسخة',
      descriptionAr: 'أكمل بناء 10 معالم.',
      gemReward: 25,
      condition: (GameState s) => s.builtLandmarks.length >= 10,
    ),
    Achievement(
      id: 'strong_economy',
      titleAr: 'اقتصاد قوي',
      descriptionAr: 'ارفع مؤشر الاقتصاد إلى 80.',
      gemReward: 10,
      condition: (GameState s) => s.economy >= 80,
    ),
    Achievement(
      id: 'beloved_ruler',
      titleAr: 'حاكم محبوب',
      descriptionAr: 'ارفع رضا الشعب إلى 85.',
      gemReward: 12,
      condition: (GameState s) => s.publicSatisfaction >= 85,
    ),
    Achievement(
      id: 'digital_darling',
      titleAr: 'نجم الترند',
      descriptionAr: 'ارفع الرأي العام الرقمي إلى 85.',
      gemReward: 10,
      condition: (GameState s) => s.digitalOpinion >= 85,
    ),
    Achievement(
      id: 'cyber_fortress',
      titleAr: 'حصن سيبراني',
      descriptionAr: 'ارفع الأمن السيبراني إلى 80.',
      gemReward: 12,
      condition: (GameState s) => s.cyberSecurity >= 80,
    ),
    Achievement(
      id: 'green_transition',
      titleAr: 'التحول الأخضر',
      descriptionAr: 'اجعل 60% من الطاقة نظيفة.',
      gemReward: 15,
      condition: (GameState s) => s.cleanEnergyRatio >= 0.60,
    ),
    Achievement(
      id: 'food_independence',
      titleAr: 'اكتفاء غذائي',
      descriptionAr: 'ارفع الأمن الغذائي إلى 85.',
      gemReward: 12,
      condition: (GameState s) => s.foodSecurity >= 85,
    ),
    Achievement(
      id: 'knowledge_nation',
      titleAr: 'دولة المعرفة',
      descriptionAr: 'ارفع التعليم والتقنية إلى 75 معًا.',
      gemReward: 15,
      condition: (GameState s) => s.education >= 75 && s.technology >= 75,
    ),
    Achievement(
      id: 'cultural_capital',
      titleAr: 'عاصمة ثقافية',
      descriptionAr: 'ارفع مؤشر الثقافة إلى 80.',
      gemReward: 12,
      condition: (GameState s) => s.culture >= 80,
    ),
    Achievement(
      id: 'tourism_hub',
      titleAr: 'قِبلة السياح',
      descriptionAr: 'ارفع السياحة إلى 75.',
      gemReward: 10,
      condition: (GameState s) => s.tourism >= 75,
    ),
    Achievement(
      id: 'debt_free',
      titleAr: 'خزينة نظيفة',
      descriptionAr: 'اسدد كل الديون بعد أن تجاوزت 200 ألف.',
      gemReward: 12,
      condition: (GameState s) =>
          s.debt <= 0 && s.treasuryCash >= 500000 && s.turnNumber > 24,
    ),
    Achievement(
      id: 'treaty_network',
      titleAr: 'شبكة معاهدات',
      descriptionAr: 'وقّع معاهدات تجارية مع 4 دول.',
      gemReward: 15,
      condition: (GameState s) =>
          s.countries.values.where((DiplomacyState d) => d.hasTradeDeal).length >=
          4,
    ),
    Achievement(
      id: 'alliance_builder',
      titleAr: 'صانع التحالفات',
      descriptionAr: 'أسّس تحالفين دفاعيين.',
      gemReward: 15,
      condition: (GameState s) =>
          s.countries.values.where((DiplomacyState d) => d.isAlly).length >= 2,
    ),
    Achievement(
      id: 'war_survivor',
      titleAr: 'صمود',
      descriptionAr: 'انتهِ من حرب واحفظ رضا الشعب فوق 50.',
      gemReward: 18,
      condition: (GameState s) =>
          !s.isAtWar &&
          s.publicSatisfaction >= 50 &&
          s.history.any(
            (HistoryEntry h) => h.title.contains('نهاية الحرب'),
          ),
    ),
    Achievement(
      id: 'decade_of_rule',
      titleAr: 'عقد من الحكم',
      descriptionAr: 'احكم 10 سنوات داخل اللعبة.',
      gemReward: 20,
      condition: (GameState s) => s.inGameDate.year >= 2035,
    ),
    Achievement(
      id: 'golden_age',
      titleAr: 'العصر الذهبي',
      descriptionAr: 'ارفع تقييم الدولة العام إلى 85.',
      gemReward: 30,
      hidden: true,
      condition: (GameState s) => s.nationScore >= 85,
    ),
  ];

  static Achievement? byId(String id) {
    for (final Achievement a in all) {
      if (a.id == id) return a;
    }
    return null;
  }

  /// Guard used by tests: landmark-count achievements must stay reachable.
  static bool isReachable(List<Landmark> catalogue) =>
      catalogue.length >= 10;
}

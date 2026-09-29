import '../models/enums.dart';
import '../models/landmark.dart';

/// The buildable landmark catalogue (MVP target: 12-15 entries, split between
/// classic/cultural and modern).
///
/// Costs and build times are content, not balance coefficients: they are
/// tuned per landmark, while all systemic multipliers live in BalanceConfig.
abstract final class LandmarksData {
  static final List<Landmark> all = <Landmark>[
    // ── Cultural / classic ────────────────────────────────────────────────
    Landmark(
      id: 'national_museum',
      nameAr: 'المتحف الوطني',
      descriptionAr:
          'متحف يحفظ ذاكرة الدولة ويعرض تاريخها للأجيال والزوار.',
      kind: LandmarkKind.cultural,
      costCash: 320000,
      buildTurns: 6,
      onCompleteEffects: <String, double>{
        StateKeys.culture: 8,
        StateKeys.tourism: 5,
      },
      tourismIncomePerTurn: 14000,
      maintenancePerTurn: 4200,
    ),
    Landmark(
      id: 'grand_library',
      nameAr: 'المكتبة الكبرى',
      descriptionAr: 'مكتبة وطنية ضخمة ترفع مستوى التعليم والثقافة.',
      kind: LandmarkKind.cultural,
      costCash: 280000,
      buildTurns: 5,
      onCompleteEffects: <String, double>{
        StateKeys.culture: 6,
        StateKeys.education: 6,
      },
      tourismIncomePerTurn: 6000,
      maintenancePerTurn: 3400,
    ),
    Landmark(
      id: 'national_university',
      nameAr: 'الجامعة الوطنية الكبرى',
      descriptionAr:
          'جامعة بحثية تخرّج كوادر الدولة وترفع سقف التطور طويل المدى.',
      kind: LandmarkKind.cultural,
      costCash: 520000,
      buildTurns: 9,
      onCompleteEffects: <String, double>{
        StateKeys.education: 12,
        StateKeys.technology: 5,
        StateKeys.culture: 4,
      },
      maintenancePerTurn: 9800,
      requirements: <String, double>{StateKeys.education: 35},
    ),
    Landmark(
      id: 'heritage_tower',
      nameAr: 'برج التراث',
      descriptionAr: 'معلم معماري يرمز لهوية الدولة ويجذب السياح.',
      kind: LandmarkKind.cultural,
      costCash: 400000,
      buildTurns: 7,
      onCompleteEffects: <String, double>{
        StateKeys.culture: 9,
        StateKeys.tourism: 8,
      },
      tourismIncomePerTurn: 22000,
      maintenancePerTurn: 5600,
      requirements: <String, double>{StateKeys.culture: 30},
    ),
    Landmark(
      id: 'grand_opera',
      nameAr: 'دار الفنون الكبرى',
      descriptionAr: 'مسرح ودار فنون ترفع الرضا الشعبي والحياة الثقافية.',
      kind: LandmarkKind.cultural,
      costCash: 350000,
      buildTurns: 6,
      onCompleteEffects: <String, double>{
        StateKeys.culture: 7,
        StateKeys.publicSatisfaction: 5,
      },
      tourismIncomePerTurn: 11000,
      maintenancePerTurn: 5200,
    ),
    Landmark(
      id: 'central_hospital',
      nameAr: 'المدينة الطبية المركزية',
      descriptionAr: 'مجمع طبي وطني يرفع مستوى الصحة العامة بشكل دائم.',
      kind: LandmarkKind.cultural,
      costCash: 480000,
      buildTurns: 8,
      onCompleteEffects: <String, double>{
        StateKeys.health: 14,
        StateKeys.publicSatisfaction: 6,
      },
      maintenancePerTurn: 11000,
    ),
    Landmark(
      id: 'grand_mosque_plaza',
      nameAr: 'الساحة الكبرى',
      descriptionAr:
          'ساحة وطنية للمناسبات والتجمعات، رمز للتماسك الاجتماعي.',
      kind: LandmarkKind.cultural,
      costCash: 240000,
      buildTurns: 4,
      onCompleteEffects: <String, double>{
        StateKeys.culture: 5,
        StateKeys.publicSatisfaction: 7,
      },
      tourismIncomePerTurn: 5000,
      maintenancePerTurn: 2600,
    ),

    // ── Modern ────────────────────────────────────────────────────────────
    Landmark(
      id: 'smart_tower',
      nameAr: 'برج الاتصالات الذكي',
      descriptionAr:
          'بنية اتصالات متقدمة ترفع التقنية والأمن السيبراني معًا.',
      kind: LandmarkKind.modern,
      costCash: 450000,
      buildTurns: 7,
      onCompleteEffects: <String, double>{
        StateKeys.technology: 10,
        StateKeys.cyberSecurity: 8,
      },
      maintenancePerTurn: 8200,
      requirements: <String, double>{StateKeys.technology: 35},
    ),
    Landmark(
      id: 'data_center',
      nameAr: 'مركز البيانات الوطني',
      descriptionAr:
          'مركز سيادي لبيانات الدولة، يقوي الاقتصاد الرقمي والحماية.',
      kind: LandmarkKind.modern,
      costCash: 560000,
      buildTurns: 8,
      onCompleteEffects: <String, double>{
        StateKeys.technology: 12,
        StateKeys.cyberSecurity: 12,
        StateKeys.economy: 4,
      },
      maintenancePerTurn: 13500,
      requirements: <String, double>{
        StateKeys.technology: 45,
        StateKeys.energy: 40,
      },
    ),
    Landmark(
      id: 'solar_farm',
      nameAr: 'محطة الطاقة الشمسية الضخمة',
      descriptionAr:
          'مجمع شمسي يرفع الطاقة ونسبة الطاقة النظيفة ويحسن البيئة.',
      kind: LandmarkKind.modern,
      costCash: 520000,
      buildTurns: 8,
      onCompleteEffects: <String, double>{
        StateKeys.energy: 12,
        StateKeys.cleanEnergyRatio: 0.15,
        StateKeys.environment: 6,
      },
      maintenancePerTurn: 7400,
    ),
    Landmark(
      id: 'metro_network',
      nameAr: 'شبكة المترو الوطنية',
      descriptionAr:
          'نقل جماعي حديث يخفض التلوث ويرفع الرضا الشعبي والاقتصاد.',
      kind: LandmarkKind.modern,
      costCash: 700000,
      buildTurns: 11,
      onCompleteEffects: <String, double>{
        StateKeys.economy: 8,
        StateKeys.publicSatisfaction: 9,
        StateKeys.environment: 5,
      },
      maintenancePerTurn: 16000,
      requirements: <String, double>{StateKeys.industry: 45},
    ),
    Landmark(
      id: 'agri_research',
      nameAr: 'مركز أبحاث الزراعة الحديثة',
      descriptionAr:
          'مركز يطور المحاصيل ويرفع الأمن الغذائي على المدى الطويل.',
      kind: LandmarkKind.modern,
      costCash: 330000,
      buildTurns: 6,
      onCompleteEffects: <String, double>{
        StateKeys.agriculture: 10,
        StateKeys.foodSecurity: 8,
      },
      perTurnEffects: <String, double>{StateKeys.foodSecurity: 0.05},
      maintenancePerTurn: 5800,
    ),
    Landmark(
      id: 'industrial_park',
      nameAr: 'المدينة الصناعية الكبرى',
      descriptionAr:
          'مجمع صناعي يرفع الصناعة والتوظيف، لكنه يضغط على البيئة.',
      kind: LandmarkKind.modern,
      costCash: 600000,
      buildTurns: 9,
      onCompleteEffects: <String, double>{
        StateKeys.industry: 14,
        StateKeys.economy: 6,
        StateKeys.environment: -6,
      },
      maintenancePerTurn: 12500,
      requirements: <String, double>{StateKeys.energy: 45},
    ),
    Landmark(
      id: 'cyber_command',
      nameAr: 'قيادة الأمن السيبراني',
      descriptionAr:
          'جهاز وطني لحماية البنية الرقمية من الهجمات والتسريبات.',
      kind: LandmarkKind.modern,
      costCash: 420000,
      buildTurns: 6,
      onCompleteEffects: <String, double>{
        StateKeys.cyberSecurity: 16,
        StateKeys.digitalOpinion: 4,
      },
      maintenancePerTurn: 9200,
      requirements: <String, double>{StateKeys.technology: 40},
    ),
    Landmark(
      id: 'defense_academy',
      nameAr: 'الأكاديمية الدفاعية',
      descriptionAr: 'مؤسسة تدريب عسكري ترفع جاهزية الدولة الأمنية.',
      kind: LandmarkKind.modern,
      costCash: 390000,
      buildTurns: 7,
      onCompleteEffects: <String, double>{
        StateKeys.militarySecurity: 14,
      },
      maintenancePerTurn: 10500,
    ),
  ];

  static final Map<String, Landmark> _index = <String, Landmark>{
    for (final Landmark landmark in all) landmark.id: landmark,
  };

  static Landmark? byId(String id) => _index[id];

  static List<Landmark> ofKind(LandmarkKind kind) =>
      all.where((Landmark l) => l.kind == kind).toList(growable: false);

  static int get culturalCount => ofKind(LandmarkKind.cultural).length;
  static int get modernCount => ofKind(LandmarkKind.modern).length;
}

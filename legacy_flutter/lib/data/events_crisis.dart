import '../models/diplomacy_state.dart';
import '../models/enums.dart';
import '../models/event.dart';
import '../models/game_state.dart';

/// Major crisis events. These bypass the standard cooldown (see
/// BalanceConfig.crisisCooldown) and take priority in the draw.
/// MVP target: 8-10 events.
abstract final class EventsCrisis {
  static final List<GameEvent> all = <GameEvent>[
    const GameEvent(
      id: 'x_pandemic',
      title: 'تفشي وباء',
      description:
          'مرض معدٍ ينتشر بسرعة والمستشفيات تحذر من انهيار المنظومة الصحية.',
      category: EventCategory.crisis,
      weight: 12,
      minTurn: 8,
      choices: <EventChoice>[
        EventChoice(
          label: 'إغلاق شامل',
          resultText: 'الانتشار توقف والاقتصاد تلقى ضربة قوية.',
          costCash: 120000,
          stateEffects: <String, double>{
            StateKeys.health: 6,
            StateKeys.economy: -10,
            StateKeys.industry: -5,
            StateKeys.publicSatisfaction: -5,
          },
        ),
        EventChoice(
          label: 'إغلاق جزئي مع دعم طبي مكثف',
          resultText: 'توازن بين الصحة والاقتصاد.',
          costCash: 180000,
          stateEffects: <String, double>{
            StateKeys.health: 3,
            StateKeys.economy: -4,
            StateKeys.publicSatisfaction: -2,
          },
        ),
        EventChoice(
          label: 'إبقاء الحياة طبيعية',
          resultText: 'الاقتصاد نجا والخسائر البشرية ثقيلة.',
          stateEffects: <String, double>{
            StateKeys.health: -14,
            StateKeys.publicSatisfaction: -12,
            StateKeys.population: -60000,
          },
        ),
      ],
    ),
    const GameEvent(
      id: 'x_global_recession',
      title: 'كساد عالمي',
      description: 'انهيار في الأسواق العالمية يضرب التصدير والاستثمار.',
      category: EventCategory.crisis,
      weight: 11,
      minTurn: 10,
      choices: <EventChoice>[
        EventChoice(
          label: 'حزمة تحفيز كبيرة',
          resultText: 'الاقتصاد صمد جزئيًا بثمن مالي كبير.',
          costCash: 220000,
          stateEffects: <String, double>{
            StateKeys.economy: -4,
            StateKeys.publicSatisfaction: 2,
          },
        ),
        EventChoice(
          label: 'تقشف وحماية الاحتياطي',
          resultText: 'الخزينة محفوظة والشارع غاضب.',
          stateEffects: <String, double>{
            StateKeys.economy: -8,
            StateKeys.publicSatisfaction: -8,
            StateKeys.industry: -4,
          },
        ),
        EventChoice(
          label: 'الاقتراض من الصندوق الدولي',
          resultText: 'سيولة فورية ودين طويل الأجل.',
          stateEffects: <String, double>{
            StateKeys.treasuryCash: 300000,
            StateKeys.debt: 330000,
            StateKeys.economy: -3,
          },
          allRelationsDelta: -2,
        ),
      ],
    ),
    const GameEvent(
      id: 'x_earthquake',
      title: 'زلزال مدمر',
      description: 'زلزال قوي يضرب منطقة مأهولة ويخلف دمارًا واسعًا.',
      category: EventCategory.crisis,
      weight: 10,
      minTurn: 6,
      choices: <EventChoice>[
        EventChoice(
          label: 'استجابة طارئة واسعة',
          resultText: 'الإنقاذ كان سريعًا والشعب قدّر الموقف.',
          costCash: 200000,
          stateEffects: <String, double>{
            StateKeys.publicSatisfaction: 6,
            StateKeys.health: -3,
            StateKeys.population: -15000,
            StateKeys.industry: -3,
          },
        ),
        EventChoice(
          label: 'استجابة محدودة وطلب مساعدة دولية',
          resultText: 'المساعدات وصلت متأخرة.',
          costCash: 60000,
          stateEffects: <String, double>{
            StateKeys.publicSatisfaction: -6,
            StateKeys.health: -6,
            StateKeys.population: -35000,
          },
          allRelationsDelta: 3,
        ),
      ],
    ),
    const GameEvent(
      id: 'x_flood',
      title: 'فيضانات مناخية',
      description: 'أمطار غير مسبوقة تغرق مناطق زراعية وسكنية.',
      category: EventCategory.crisis,
      weight: 10,
      minTurn: 6,
      choices: <EventChoice>[
        EventChoice(
          label: 'تعويضات وبنية تصريف جديدة',
          resultText: 'استثمار وقائي طويل المدى.',
          costCash: 170000,
          stateEffects: <String, double>{
            StateKeys.agriculture: -4,
            StateKeys.environment: 3,
            StateKeys.publicSatisfaction: 3,
          },
        ),
        EventChoice(
          label: 'إغاثة عاجلة فقط',
          resultText: 'الأزمة احتُويت والمشكلة قابلة للتكرار.',
          costCash: 60000,
          stateEffects: <String, double>{
            StateKeys.agriculture: -8,
            StateKeys.foodSecurity: -6,
            StateKeys.publicSatisfaction: -4,
          },
        ),
      ],
    ),
    GameEvent(
      id: 'x_blackout',
      title: 'انقطاع كهرباء وطني',
      description: 'انهيار في الشبكة يوقف الحياة في معظم المدن.',
      category: EventCategory.crisis,
      weight: 10,
      minTurn: 6,
      condition: (GameState s) => s.energy < 65,
      choices: <EventChoice>[
        const EventChoice(
          label: 'إصلاح شامل وتحديث الشبكة',
          resultText: 'الشبكة أصبحت أكثر استقرارًا.',
          costCash: 190000,
          stateEffects: <String, double>{
            StateKeys.energy: 8,
            StateKeys.economy: -3,
            StateKeys.publicSatisfaction: -3,
          },
        ),
        const EventChoice(
          label: 'إعادة تشغيل سريعة بأقل تكلفة',
          resultText: 'الكهرباء عادت والهشاشة باقية.',
          costCash: 50000,
          stateEffects: <String, double>{
            StateKeys.economy: -6,
            StateKeys.industry: -4,
            StateKeys.publicSatisfaction: -8,
            StateKeys.digitalOpinion: -6,
          },
        ),
      ],
    ),
    GameEvent(
      id: 'x_food_crisis',
      title: 'أزمة غذاء حادة',
      description: 'نقص حاد في السلع الأساسية وطوابير في الأسواق.',
      category: EventCategory.crisis,
      weight: 11,
      minTurn: 5,
      condition: (GameState s) => s.foodSecurity < 45,
      choices: <EventChoice>[
        const EventChoice(
          label: 'استيراد طارئ واسع',
          resultText: 'الأسواق استقرت بتكلفة كبيرة.',
          costCash: 210000,
          stateEffects: <String, double>{
            StateKeys.foodSecurity: 10,
            StateKeys.publicSatisfaction: 4,
          },
        ),
        const EventChoice(
          label: 'بطاقات تقنين',
          resultText: 'التوزيع صار عادلًا والغضب مستمر.',
          costCash: 70000,
          stateEffects: <String, double>{
            StateKeys.foodSecurity: 4,
            StateKeys.publicSatisfaction: -5,
          },
        ),
        const EventChoice(
          label: 'الاعتماد على المساعدات الدولية',
          resultText: 'الأزمة خفّت والاعتماد على الخارج زاد.',
          stateEffects: <String, double>{
            StateKeys.foodSecurity: 6,
            StateKeys.axisForeign: -6,
            StateKeys.publicSatisfaction: -2,
          },
          allRelationsDelta: 4,
        ),
      ],
    ),
    GameEvent(
      id: 'x_cyber_attack',
      title: 'هجوم سيبراني على البنية التحتية',
      description:
          'هجوم منسق يستهدف شبكات الكهرباء والمصارف في وقت واحد.',
      category: EventCategory.crisis,
      weight: 11,
      minTurn: 8,
      condition: (GameState s) => s.cyberSecurity < 75,
      choices: <EventChoice>[
        const EventChoice(
          label: 'استجابة طارئة وبناء قدرات دفاعية',
          resultText: 'الهجوم صُد وارتفعت جاهزية الدولة.',
          costCash: 180000,
          stateEffects: <String, double>{
            StateKeys.cyberSecurity: 12,
            StateKeys.economy: -3,
          },
        ),
        const EventChoice(
          label: 'عزل الأنظمة مؤقتًا',
          resultText: 'الخسائر احتُويت بتعطل واسع للخدمات.',
          stateEffects: <String, double>{
            StateKeys.economy: -7,
            StateKeys.digitalOpinion: -8,
            StateKeys.cyberSecurity: 3,
          },
        ),
        const EventChoice(
          label: 'الاستعانة بشركة أمن أجنبية',
          resultText: 'حل سريع بثمن سيادي.',
          costCash: 120000,
          stateEffects: <String, double>{
            StateKeys.cyberSecurity: 8,
            StateKeys.axisEconomic: -5,
          },
        ),
      ],
    ),
    GameEvent(
      id: 'x_mass_protests',
      title: 'احتجاجات شعبية واسعة',
      description:
          'مظاهرات كبرى في العاصمة تطالب بإصلاح سياسي واقتصادي شامل.',
      category: EventCategory.crisis,
      weight: 12,
      minTurn: 6,
      condition: (GameState s) => s.publicSatisfaction < 35,
      choices: <EventChoice>[
        const EventChoice(
          label: 'حزمة إصلاح واستجابة للمطالب',
          resultText: 'الشارع هدأ وبدأت الثقة تعود.',
          costCash: 200000,
          stateEffects: <String, double>{
            StateKeys.publicSatisfaction: 14,
            StateKeys.digitalOpinion: 10,
            StateKeys.economy: -2,
          },
        ),
        const EventChoice(
          label: 'حوار وطني بلا تنازلات كبيرة',
          resultText: 'تهدئة مؤقتة.',
          costCash: 60000,
          stateEffects: <String, double>{
            StateKeys.publicSatisfaction: 5,
            StateKeys.digitalOpinion: 2,
          },
        ),
        const EventChoice(
          label: 'تفريق الاحتجاجات أمنيًا',
          resultText: 'الهدوء فُرض والشرعية اهتزت.',
          stateEffects: <String, double>{
            StateKeys.publicSatisfaction: -10,
            StateKeys.digitalOpinion: -15,
            StateKeys.militarySecurity: 2,
            StateKeys.axisSocial: 8,
          },
          allRelationsDelta: -5,
        ),
      ],
    ),
    GameEvent(
      id: 'x_war_ultimatum',
      title: 'إنذار حربي',
      description:
          'دولة معادية تقدم إنذارًا نهائيًا بمطالب حدودية واقتصادية.',
      category: EventCategory.crisis,
      weight: 10,
      minTurn: 12,
      condition: (GameState s) =>
          !s.isAtWar &&
          s.countries.values.any((DiplomacyState d) => d.relation < -50),
      choices: <EventChoice>[
        const EventChoice(
          label: 'الرفض والتعبئة العامة',
          resultText: 'الدولة في حالة استعداد قتالي.',
          costCash: 150000,
          stateEffects: <String, double>{
            StateKeys.militarySecurity: 8,
            StateKeys.publicSatisfaction: 3,
            StateKeys.economy: -3,
          },
          allRelationsDelta: -4,
        ),
        const EventChoice(
          label: 'تنازلات اقتصادية لتجنب الحرب',
          resultText: 'الحرب تأجلت بثمن سيادي.',
          costCash: 180000,
          stateEffects: <String, double>{
            StateKeys.publicSatisfaction: -8,
            StateKeys.axisForeign: -8,
          },
          allRelationsDelta: 5,
        ),
        const EventChoice(
          label: 'وساطة دولية',
          resultText: 'الوسطاء دخلوا والملف بات دوليًا.',
          costCash: 70000,
          stateEffects: <String, double>{StateKeys.militarySecurity: 2},
          allRelationsDelta: 6,
        ),
      ],
    ),
    GameEvent(
      id: 'x_currency_collapse',
      title: 'انهيار في قيمة العملة',
      description: 'هبوط حاد في سعر العملة يضرب القدرة الشرائية.',
      category: EventCategory.crisis,
      weight: 10,
      minTurn: 10,
      condition: (GameState s) => s.economy < 45 || s.debt > 400000,
      choices: <EventChoice>[
        const EventChoice(
          label: 'تدخل نقدي حاد',
          resultText: 'العملة استقرت باستهلاك الاحتياطي.',
          costCash: 250000,
          stateEffects: <String, double>{
            StateKeys.economy: 4,
            StateKeys.publicSatisfaction: 2,
          },
        ),
        const EventChoice(
          label: 'تعويم مُدار',
          resultText: 'ألم قصير المدى وتصدير أكثر تنافسية.',
          stateEffects: <String, double>{
            StateKeys.economy: -3,
            StateKeys.industry: 4,
            StateKeys.publicSatisfaction: -9,
            StateKeys.foodSecurity: -3,
          },
        ),
      ],
    ),
  ];
}

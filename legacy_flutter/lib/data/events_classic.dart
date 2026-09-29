import '../models/enums.dart';
import '../models/event.dart';
import '../models/game_state.dart';

/// Classic-era events: governance, agriculture, labour, security, society.
/// MVP target: 25-30 events.
abstract final class EventsClassic {
  static final List<GameEvent> all = <GameEvent>[
    GameEvent(
      id: 'c_drought',
      title: 'موجة جفاف',
      description:
          'تقرير وزارة الزراعة يحذر من انخفاض منسوب المياه وتأثيره على المحاصيل هذا الموسم.',
      category: EventCategory.classic,
      weight: 12,
      condition: (GameState s) => s.agriculture < 70,
      choices: <EventChoice>[
        const EventChoice(
          label: 'ضخ استثمار طارئ في الري',
          resultText: 'أنقذت جزءًا كبيرًا من الموسم، لكن الخزينة تحمّلت الفاتورة.',
          costCash: 90000,
          stateEffects: <String, double>{
            StateKeys.agriculture: 4,
            StateKeys.foodSecurity: 3,
          },
        ),
        const EventChoice(
          label: 'استيراد الغذاء مؤقتًا',
          resultText: 'الأسواق استقرت، لكن الاكتفاء الذاتي تراجع.',
          costCash: 55000,
          stateEffects: <String, double>{
            StateKeys.foodSecurity: -4,
            StateKeys.publicSatisfaction: 2,
          },
        ),
        const EventChoice(
          label: 'لا تدخّل — ترشيد الاستهلاك',
          resultText: 'وفّرت المال لكن الشعب شعر بالإهمال.',
          stateEffects: <String, double>{
            StateKeys.agriculture: -6,
            StateKeys.foodSecurity: -6,
            StateKeys.publicSatisfaction: -5,
          },
        ),
      ],
    ),
    GameEvent(
      id: 'c_bumper_harvest',
      title: 'موسم زراعي وفير',
      description: 'المحاصيل تجاوزت التوقعات وهناك فائض قابل للتصدير.',
      category: EventCategory.classic,
      weight: 10,
      condition: (GameState s) => s.agriculture >= 45,
      choices: <EventChoice>[
        const EventChoice(
          label: 'تصدير الفائض',
          resultText: 'دخل إضافي دخل الخزينة فورًا.',
          stateEffects: <String, double>{
            StateKeys.treasuryCash: 120000,
            StateKeys.economy: 2,
            StateKeys.foodSecurity: -2,
          },
        ),
        const EventChoice(
          label: 'تخزين احتياطي استراتيجي',
          resultText: 'مخازن الدولة امتلأت وارتفع الأمن الغذائي.',
          stateEffects: <String, double>{
            StateKeys.foodSecurity: 7,
            StateKeys.publicSatisfaction: 3,
          },
        ),
      ],
    ),
    GameEvent(
      id: 'c_labor_strike',
      title: 'إضراب عمالي',
      description:
          'عمال القطاع الصناعي يطالبون بزيادة الأجور ويهددون بتوقف الإنتاج.',
      category: EventCategory.classic,
      weight: 11,
      condition: (GameState s) => s.workersSatisfaction < 55,
      choices: <EventChoice>[
        const EventChoice(
          label: 'زيادة الأجور',
          resultText: 'الإضراب انتهى والعمال راضون، والميزانية تحمّلت العبء.',
          costCash: 110000,
          stateEffects: <String, double>{
            StateKeys.publicSatisfaction: 6,
            StateKeys.industry: 2,
          },
        ),
        const EventChoice(
          label: 'التفاوض مع تنازلات جزئية',
          resultText: 'حل وسط: لا أحد سعيد تمامًا، لكن العمل عاد.',
          costCash: 40000,
          stateEffects: <String, double>{
            StateKeys.publicSatisfaction: 1,
          },
        ),
        const EventChoice(
          label: 'رفض المطالب',
          resultText: 'الإنتاج تعطل والغضب اتسع.',
          stateEffects: <String, double>{
            StateKeys.industry: -5,
            StateKeys.publicSatisfaction: -8,
            StateKeys.digitalOpinion: -5,
          },
        ),
      ],
    ),
    const GameEvent(
      id: 'c_corruption_scandal',
      title: 'فضيحة فساد',
      description:
          'تحقيق صحفي يكشف تلاعبًا في عقود حكومية كبرى داخل إحدى الوزارات.',
      category: EventCategory.classic,
      weight: 10,
      choices: <EventChoice>[
        EventChoice(
          label: 'محاكمة علنية وإقالة المسؤولين',
          resultText: 'الشعب رأى جدية في المحاسبة.',
          stateEffects: <String, double>{
            StateKeys.publicSatisfaction: 7,
            StateKeys.digitalOpinion: 8,
            StateKeys.economy: -2,
          },
        ),
        EventChoice(
          label: 'تسوية هادئة بعيدًا عن الإعلام',
          resultText: 'الملف أُغلق، لكن الثقة تأثرت.',
          stateEffects: <String, double>{
            StateKeys.publicSatisfaction: -4,
            StateKeys.digitalOpinion: -7,
            StateKeys.treasuryCash: 60000,
          },
        ),
        EventChoice(
          label: 'إنكار التقرير',
          resultText: 'الاتهامات تضخمت على المنصات.',
          stateEffects: <String, double>{
            StateKeys.digitalOpinion: -12,
            StateKeys.publicSatisfaction: -6,
          },
        ),
      ],
    ),
    GameEvent(
      id: 'c_tax_protest',
      title: 'احتجاج على الضرائب',
      description: 'تجمعات في المدن الكبرى ضد مستوى الضرائب الحالي.',
      category: EventCategory.classic,
      weight: 12,
      condition: (GameState s) => s.taxRate > 0.30,
      choices: <EventChoice>[
        const EventChoice(
          label: 'تخفيض الضرائب فورًا',
          resultText: 'الغضب هدأ وتراجع الدخل.',
          stateEffects: <String, double>{
            StateKeys.taxRate: -0.05,
            StateKeys.publicSatisfaction: 8,
          },
        ),
        const EventChoice(
          label: 'إعفاءات للفئات الأكثر تأثرًا',
          resultText: 'حل مُوازن استقبله الشارع بهدوء.',
          costCash: 70000,
          stateEffects: <String, double>{
            StateKeys.publicSatisfaction: 4,
          },
        ),
        const EventChoice(
          label: 'التمسك بالسياسة الحالية',
          resultText: 'الاحتجاجات استمرت أسابيع.',
          stateEffects: <String, double>{
            StateKeys.publicSatisfaction: -7,
            StateKeys.digitalOpinion: -6,
          },
        ),
      ],
    ),
    const GameEvent(
      id: 'c_border_tension',
      title: 'توتر حدودي',
      description: 'حادث حدودي محدود مع دولة مجاورة يثير قلق الرأي العام.',
      category: EventCategory.classic,
      weight: 9,
      choices: <EventChoice>[
        EventChoice(
          label: 'تعزيز الحدود عسكريًا',
          resultText: 'الحدود أصبحت آمنة، والجوار غير مرتاح.',
          costCash: 95000,
          stateEffects: <String, double>{StateKeys.militarySecurity: 6},
          allRelationsDelta: -3,
        ),
        EventChoice(
          label: 'قناة تفاوض هادئة',
          resultText: 'الحادث احتُوى دبلوماسيًا.',
          stateEffects: <String, double>{StateKeys.militarySecurity: -1},
          allRelationsDelta: 4,
        ),
      ],
    ),
    const GameEvent(
      id: 'c_merchant_delegation',
      title: 'وفد تجاري',
      description: 'وفد من رجال أعمال أجانب يطلب تسهيلات للاستثمار.',
      category: EventCategory.classic,
      weight: 10,
      choices: <EventChoice>[
        EventChoice(
          label: 'منح تسهيلات ضريبية',
          resultText: 'استثمارات جديدة دخلت السوق.',
          stateEffects: <String, double>{
            StateKeys.economy: 5,
            StateKeys.industry: 3,
            StateKeys.axisEconomic: -6,
          },
          allRelationsDelta: 3,
        ),
        EventChoice(
          label: 'شروط صارمة لحماية السوق المحلي',
          resultText: 'الصناعة المحلية ارتاحت، والمستثمرون تحفّظوا.',
          stateEffects: <String, double>{
            StateKeys.industry: 2,
            StateKeys.axisEconomic: 6,
            StateKeys.economy: -1,
          },
        ),
      ],
    ),
    const GameEvent(
      id: 'c_university_demands',
      title: 'مطالب جامعية',
      description: 'اتحادات الطلاب تطالب بتوسيع المنح وتحديث المعامل.',
      category: EventCategory.classic,
      weight: 10,
      choices: <EventChoice>[
        EventChoice(
          label: 'تمويل التوسع',
          resultText: 'التعليم تقدم خطوة ملموسة.',
          costCash: 85000,
          stateEffects: <String, double>{
            StateKeys.education: 5,
            StateKeys.publicSatisfaction: 3,
          },
        ),
        EventChoice(
          label: 'تأجيل للعام القادم',
          resultText: 'الطلاب غاضبون على المنصات.',
          stateEffects: <String, double>{
            StateKeys.digitalOpinion: -5,
            StateKeys.education: -2,
          },
        ),
      ],
    ),
    GameEvent(
      id: 'c_infrastructure_collapse',
      title: 'انهيار جزئي في جسر رئيسي',
      description: 'إهمال الصيانة تسبب في حادث أوقف شريان مروري مهم.',
      category: EventCategory.classic,
      weight: 9,
      condition: (GameState s) => s.industry < 65,
      choices: <EventChoice>[
        const EventChoice(
          label: 'إصلاح عاجل وخطة صيانة شاملة',
          resultText: 'الثقة عادت وتحسّنت البنية التحتية.',
          costCash: 130000,
          stateEffects: <String, double>{
            StateKeys.industry: 4,
            StateKeys.publicSatisfaction: 4,
          },
        ),
        const EventChoice(
          label: 'ترميم سطحي سريع',
          resultText: 'الطريق فُتح، والمشكلة مؤجلة.',
          costCash: 45000,
          stateEffects: <String, double>{StateKeys.publicSatisfaction: -2},
        ),
      ],
    ),
    GameEvent(
      id: 'c_energy_shortage',
      title: 'نقص في الطاقة',
      description: 'الطلب تجاوز قدرة الشبكة وبدأت انقطاعات متفرقة.',
      category: EventCategory.classic,
      weight: 11,
      condition: (GameState s) => s.energy < 55,
      choices: <EventChoice>[
        const EventChoice(
          label: 'تشغيل محطات تقليدية إضافية',
          resultText: 'الكهرباء استقرت على حساب البيئة.',
          costCash: 75000,
          stateEffects: <String, double>{
            StateKeys.energy: 6,
            StateKeys.environment: -5,
            StateKeys.cleanEnergyRatio: -0.04,
          },
        ),
        const EventChoice(
          label: 'تسريع مشروع طاقة نظيفة',
          resultText: 'استثمار أغلى لكنه يبني المستقبل.',
          costCash: 140000,
          stateEffects: <String, double>{
            StateKeys.energy: 4,
            StateKeys.cleanEnergyRatio: 0.06,
            StateKeys.environment: 2,
          },
        ),
        const EventChoice(
          label: 'ترشيد إجباري للاستهلاك',
          resultText: 'الأزمة احتُويت بغضب شعبي.',
          stateEffects: <String, double>{
            StateKeys.publicSatisfaction: -6,
            StateKeys.industry: -3,
          },
        ),
      ],
    ),
    const GameEvent(
      id: 'c_national_festival',
      title: 'مهرجان وطني',
      description: 'اقتراح بتنظيم مهرجان ثقافي كبير يجمع الفنون والتراث.',
      category: EventCategory.classic,
      weight: 9,
      choices: <EventChoice>[
        EventChoice(
          label: 'تنظيم مهرجان ضخم',
          resultText: 'الثقافة والسياحة انتعشتا.',
          costCash: 80000,
          stateEffects: <String, double>{
            StateKeys.culture: 6,
            StateKeys.tourism: 4,
            StateKeys.publicSatisfaction: 4,
          },
        ),
        EventChoice(
          label: 'نسخة مصغرة بميزانية محدودة',
          resultText: 'حدث لطيف بأثر محدود.',
          costCash: 25000,
          stateEffects: <String, double>{StateKeys.culture: 2},
        ),
      ],
    ),
    const GameEvent(
      id: 'c_military_parade',
      title: 'طلب استعراض عسكري',
      description: 'القيادة العسكرية تقترح استعراضًا لرفع الروح الوطنية.',
      category: EventCategory.classic,
      weight: 8,
      choices: <EventChoice>[
        EventChoice(
          label: 'الموافقة',
          resultText: 'الروح الوطنية ارتفعت، والجوار راقب بحذر.',
          costCash: 60000,
          stateEffects: <String, double>{
            StateKeys.publicSatisfaction: 4,
            StateKeys.militarySecurity: 2,
          },
          allRelationsDelta: -2,
        ),
        EventChoice(
          label: 'الرفض وتوجيه المبلغ للتسليح',
          resultText: 'الجيش تعزز بهدوء.',
          costCash: 60000,
          stateEffects: <String, double>{StateKeys.militarySecurity: 5},
        ),
      ],
    ),
    const GameEvent(
      id: 'c_smuggling_network',
      title: 'شبكة تهريب',
      description: 'الأجهزة الأمنية تكشف شبكة تهريب كبيرة عبر الحدود.',
      category: EventCategory.classic,
      weight: 9,
      choices: <EventChoice>[
        EventChoice(
          label: 'حملة أمنية واسعة',
          resultText: 'الشبكة تفككت وزادت هيبة الدولة.',
          costCash: 70000,
          stateEffects: <String, double>{
            StateKeys.militarySecurity: 5,
            StateKeys.economy: 2,
          },
        ),
        EventChoice(
          label: 'اكتفاء بمراقبة استخباراتية',
          resultText: 'معلومات أكثر، لكن التهريب مستمر.',
          stateEffects: <String, double>{
            StateKeys.economy: -2,
            StateKeys.militarySecurity: 1,
          },
        ),
      ],
    ),
    const GameEvent(
      id: 'c_foreign_investment',
      title: 'عرض استثمار أجنبي كبير',
      description: 'صندوق أجنبي يعرض تمويل مشروع صناعي ضخم مقابل حصة أرباح.',
      category: EventCategory.classic,
      weight: 10,
      choices: <EventChoice>[
        EventChoice(
          label: 'القبول',
          resultText: 'ضخ مالي فوري ونمو صناعي.',
          stateEffects: <String, double>{
            StateKeys.treasuryCash: 220000,
            StateKeys.industry: 6,
            StateKeys.environment: -3,
            StateKeys.axisEconomic: -8,
          },
        ),
        EventChoice(
          label: 'الرفض والاعتماد على التمويل المحلي',
          resultText: 'سيادة اقتصادية أعلى ونمو أبطأ.',
          stateEffects: <String, double>{
            StateKeys.axisEconomic: 8,
            StateKeys.publicSatisfaction: 2,
          },
        ),
      ],
    ),
    GameEvent(
      id: 'c_brain_drain',
      title: 'هجرة الكفاءات',
      description: 'تقارير عن مغادرة أعداد من الأطباء والمهندسين للخارج.',
      category: EventCategory.classic,
      weight: 10,
      condition: (GameState s) => s.publicSatisfaction < 55 || s.economy < 45,
      choices: <EventChoice>[
        const EventChoice(
          label: 'حزمة تحفيز للكفاءات',
          resultText: 'جزء كبير قرر البقاء.',
          costCash: 120000,
          stateEffects: <String, double>{
            StateKeys.technology: 3,
            StateKeys.health: 3,
            StateKeys.migrationBalance: 2,
          },
        ),
        const EventChoice(
          label: 'تجاهل الظاهرة',
          resultText: 'الخسارة في رأس المال البشري تفاقمت.',
          stateEffects: <String, double>{
            StateKeys.technology: -4,
            StateKeys.health: -3,
            StateKeys.education: -2,
          },
        ),
      ],
    ),
    GameEvent(
      id: 'c_hospital_overload',
      title: 'ضغط على المستشفيات',
      description: 'أقسام الطوارئ تعمل فوق طاقتها في المدن الكبرى.',
      category: EventCategory.classic,
      weight: 10,
      condition: (GameState s) => s.health < 60,
      choices: <EventChoice>[
        const EventChoice(
          label: 'توسيع الطاقة الاستيعابية',
          resultText: 'الخدمة الصحية تحسنت.',
          costCash: 100000,
          stateEffects: <String, double>{
            StateKeys.health: 6,
            StateKeys.publicSatisfaction: 4,
          },
        ),
        const EventChoice(
          label: 'إعادة توزيع الكوادر فقط',
          resultText: 'حل إداري بأثر محدود.',
          stateEffects: <String, double>{
            StateKeys.health: 1,
            StateKeys.publicSatisfaction: -1,
          },
        ),
      ],
    ),
    const GameEvent(
      id: 'c_housing_shortage',
      title: 'أزمة إسكان',
      description: 'ارتفاع الإيجارات يثقل الطبقة الوسطى في المدن.',
      category: EventCategory.classic,
      weight: 10,
      choices: <EventChoice>[
        EventChoice(
          label: 'مشروع إسكان وطني',
          resultText: 'الأسعار بدأت تستقر تدريجيًا.',
          costCash: 160000,
          stateEffects: <String, double>{
            StateKeys.publicSatisfaction: 7,
            StateKeys.industry: 3,
          },
        ),
        EventChoice(
          label: 'تحفيز القطاع الخاص',
          resultText: 'حركة عمرانية بلا أثر فوري على الأسعار.',
          costCash: 50000,
          stateEffects: <String, double>{
            StateKeys.economy: 3,
            StateKeys.publicSatisfaction: 1,
          },
        ),
        EventChoice(
          label: 'تجميد الإيجارات بقرار إداري',
          resultText: 'ارتياح شعبي وتراجع في الاستثمار العقاري.',
          stateEffects: <String, double>{
            StateKeys.publicSatisfaction: 5,
            StateKeys.economy: -4,
            StateKeys.axisEconomic: 8,
          },
        ),
      ],
    ),
    const GameEvent(
      id: 'c_water_dispute',
      title: 'خلاف على المياه',
      description: 'دولة مجاورة تبني سدًا يؤثر على حصة الدولة من المياه.',
      category: EventCategory.classic,
      weight: 9,
      choices: <EventChoice>[
        EventChoice(
          label: 'مفاوضات دولية',
          resultText: 'اتفاق جزئي على الحصص.',
          stateEffects: <String, double>{StateKeys.agriculture: -1},
          allRelationsDelta: 3,
        ),
        EventChoice(
          label: 'تصعيد سياسي حاد',
          resultText: 'الموقف الوطني قوي والعلاقات متوترة.',
          stateEffects: <String, double>{
            StateKeys.publicSatisfaction: 4,
            StateKeys.axisForeign: 6,
          },
          allRelationsDelta: -8,
        ),
        EventChoice(
          label: 'استثمار في تحلية وترشيد',
          resultText: 'حل تقني طويل المدى.',
          costCash: 150000,
          stateEffects: <String, double>{
            StateKeys.agriculture: 4,
            StateKeys.technology: 2,
          },
        ),
      ],
    ),
    const GameEvent(
      id: 'c_archaeology',
      title: 'كشف أثري',
      description: 'بعثة محلية تكتشف موقعًا أثريًا نادرًا.',
      category: EventCategory.classic,
      weight: 8,
      choices: <EventChoice>[
        EventChoice(
          label: 'تحويله لموقع سياحي',
          resultText: 'الموقع أصبح وجهة زوار.',
          costCash: 65000,
          stateEffects: <String, double>{
            StateKeys.tourism: 6,
            StateKeys.culture: 5,
          },
        ),
        EventChoice(
          label: 'حماية علمية بلا استثمار سياحي',
          resultText: 'قيمة ثقافية عالية بلا عائد مباشر.',
          stateEffects: <String, double>{StateKeys.culture: 7},
        ),
      ],
    ),
    const GameEvent(
      id: 'c_inflation_pressure',
      title: 'ضغوط تضخمية',
      description: 'أسعار السلع الأساسية ترتفع بوتيرة تقلق الأسواق.',
      category: EventCategory.classic,
      weight: 11,
      choices: <EventChoice>[
        EventChoice(
          label: 'زيادة الدعم مؤقتًا',
          resultText: 'الأسعار هدأت والميزانية تحملت.',
          costCash: 130000,
          stateEffects: <String, double>{StateKeys.publicSatisfaction: 6},
        ),
        EventChoice(
          label: 'تشديد نقدي وتقليل الإنفاق',
          resultText: 'التضخم تراجع والنمو تباطأ.',
          stateEffects: <String, double>{
            StateKeys.economy: -3,
            StateKeys.publicSatisfaction: -3,
            StateKeys.treasuryCash: 60000,
          },
        ),
      ],
    ),
    const GameEvent(
      id: 'c_regional_unrest',
      title: 'توتر في إقليم طرفي',
      description: 'مطالب تنموية في إقليم يشعر بالإهمال.',
      category: EventCategory.classic,
      weight: 9,
      choices: <EventChoice>[
        EventChoice(
          label: 'خطة تنمية للإقليم',
          resultText: 'التوتر هدأ والثقة عادت.',
          costCash: 110000,
          stateEffects: <String, double>{
            StateKeys.publicSatisfaction: 6,
            StateKeys.economy: 2,
          },
        ),
        EventChoice(
          label: 'حل أمني',
          resultText: 'الهدوء فُرض والغضب كُتم.',
          costCash: 40000,
          stateEffects: <String, double>{
            StateKeys.militarySecurity: 2,
            StateKeys.publicSatisfaction: -6,
            StateKeys.digitalOpinion: -7,
          },
        ),
      ],
    ),
    const GameEvent(
      id: 'c_press_debate',
      title: 'جدل حول حرية الصحافة',
      description: 'قانون جديد للإعلام يثير نقاشًا واسعًا.',
      category: EventCategory.classic,
      weight: 9,
      choices: <EventChoice>[
        EventChoice(
          label: 'توسيع هامش الحرية',
          resultText: 'صورة الدولة تحسنت داخليًا وخارجيًا.',
          stateEffects: <String, double>{
            StateKeys.digitalOpinion: 8,
            StateKeys.publicSatisfaction: 3,
            StateKeys.axisSocial: -8,
          },
          allRelationsDelta: 2,
        ),
        EventChoice(
          label: 'تشديد الرقابة',
          resultText: 'سيطرة أكبر على الخطاب العام وثمن سُمعي.',
          stateEffects: <String, double>{
            StateKeys.digitalOpinion: -10,
            StateKeys.axisSocial: 8,
            StateKeys.militarySecurity: 2,
          },
        ),
      ],
    ),
    const GameEvent(
      id: 'c_transport_demands',
      title: 'مطالب بتحسين المواصلات',
      description: 'سكان المدن يطالبون بشبكة نقل عام أفضل.',
      category: EventCategory.classic,
      weight: 9,
      choices: <EventChoice>[
        EventChoice(
          label: 'توسيع النقل العام',
          resultText: 'الزحام تراجع وارتاح الناس.',
          costCash: 120000,
          stateEffects: <String, double>{
            StateKeys.publicSatisfaction: 6,
            StateKeys.environment: 3,
          },
        ),
        EventChoice(
          label: 'تأجيل المشروع',
          resultText: 'التذمر استمر.',
          stateEffects: <String, double>{StateKeys.publicSatisfaction: -4},
        ),
      ],
    ),
    const GameEvent(
      id: 'c_army_modernization',
      title: 'عرض تحديث الجيش',
      description: 'عرض لشراء منظومات دفاعية حديثة بشروط ميسّرة.',
      category: EventCategory.classic,
      weight: 9,
      choices: <EventChoice>[
        EventChoice(
          label: 'الشراء',
          resultText: 'القدرات الدفاعية تحسّنت بشكل ملموس.',
          costCash: 190000,
          stateEffects: <String, double>{StateKeys.militarySecurity: 10},
        ),
        EventChoice(
          label: 'الاعتماد على التصنيع المحلي',
          resultText: 'استثمار أبطأ لكنه يبني صناعة وطنية.',
          costCash: 100000,
          stateEffects: <String, double>{
            StateKeys.militarySecurity: 4,
            StateKeys.industry: 4,
            StateKeys.technology: 2,
          },
        ),
        EventChoice(
          label: 'الاعتذار عن العرض',
          resultText: 'وفّرت المال وتأجل التحديث.',
          stateEffects: <String, double>{StateKeys.militarySecurity: -2},
        ),
      ],
    ),
    const GameEvent(
      id: 'c_refugee_inflow',
      title: 'تدفق لاجئين',
      description: 'أزمة في دولة مجاورة تدفع آلافًا إلى الحدود.',
      category: EventCategory.classic,
      weight: 9,
      choices: <EventChoice>[
        EventChoice(
          label: 'استقبال منظم مع دعم دولي',
          resultText: 'موقف إنساني رفع سمعة الدولة.',
          costCash: 90000,
          stateEffects: <String, double>{
            StateKeys.migrationBalance: 4,
            StateKeys.publicSatisfaction: -2,
            StateKeys.culture: 2,
          },
          allRelationsDelta: 5,
        ),
        EventChoice(
          label: 'إغلاق الحدود',
          resultText: 'ضغط داخلي أقل وانتقادات خارجية أكثر.',
          stateEffects: <String, double>{
            StateKeys.publicSatisfaction: 2,
            StateKeys.axisSocial: 6,
          },
          allRelationsDelta: -6,
        ),
      ],
    ),
    GameEvent(
      id: 'c_irrigation_project',
      title: 'مشروع ري كبير',
      description: 'خطة لتحويل مساحات صحراوية إلى أراضٍ زراعية.',
      category: EventCategory.classic,
      weight: 9,
      condition: (GameState s) => s.treasuryCash > 200000,
      choices: <EventChoice>[
        const EventChoice(
          label: 'إطلاق المشروع',
          resultText: 'رقعة زراعية جديدة وأمن غذائي أعلى.',
          costCash: 180000,
          stateEffects: <String, double>{
            StateKeys.agriculture: 8,
            StateKeys.foodSecurity: 6,
          },
        ),
        const EventChoice(
          label: 'دراسة إضافية قبل التنفيذ',
          resultText: 'قرار حذر أجّل الفائدة.',
          costCash: 20000,
          stateEffects: <String, double>{},
        ),
      ],
    ),
    const GameEvent(
      id: 'c_mining_concession',
      title: 'امتياز تعديني',
      description: 'شركة دولية تطلب امتياز تعدين في منطقة غنية بالمعادن.',
      category: EventCategory.classic,
      weight: 9,
      choices: <EventChoice>[
        EventChoice(
          label: 'منح الامتياز',
          resultText: 'عائد مالي كبير وثمن بيئي.',
          stateEffects: <String, double>{
            StateKeys.treasuryCash: 200000,
            StateKeys.industry: 4,
            StateKeys.environment: -8,
          },
        ),
        EventChoice(
          label: 'تشغيل وطني بشراكة محدودة',
          resultText: 'عائد أقل وسيادة أعلى.',
          costCash: 60000,
          stateEffects: <String, double>{
            StateKeys.industry: 5,
            StateKeys.axisEconomic: 6,
            StateKeys.environment: -3,
          },
        ),
        EventChoice(
          label: 'الرفض لأسباب بيئية',
          resultText: 'البيئة محمية والفرصة ضاعت.',
          stateEffects: <String, double>{
            StateKeys.environment: 4,
            StateKeys.publicSatisfaction: 2,
          },
        ),
      ],
    ),
    const GameEvent(
      id: 'c_teacher_salaries',
      title: 'مطالب المعلمين',
      description: 'نقابة المعلمين تطالب بتحسين الأجور وظروف العمل.',
      category: EventCategory.classic,
      weight: 10,
      choices: <EventChoice>[
        EventChoice(
          label: 'زيادة الأجور',
          resultText: 'استقرار في قطاع التعليم.',
          costCash: 95000,
          stateEffects: <String, double>{
            StateKeys.education: 5,
            StateKeys.publicSatisfaction: 4,
          },
        ),
        EventChoice(
          label: 'حوافز أداء بدلًا من زيادة عامة',
          resultText: 'حل جزئي أثار جدلًا.',
          costCash: 45000,
          stateEffects: <String, double>{
            StateKeys.education: 2,
            StateKeys.publicSatisfaction: -1,
          },
        ),
      ],
    ),
    const GameEvent(
      id: 'c_census',
      title: 'تعداد سكاني شامل',
      description: 'اقتراح بإجراء تعداد وطني لتحديث بيانات التخطيط.',
      category: EventCategory.classic,
      weight: 7,
      choices: <EventChoice>[
        EventChoice(
          label: 'إجراء التعداد',
          resultText: 'بيانات أدق حسّنت كفاءة الخدمات.',
          costCash: 70000,
          stateEffects: <String, double>{
            StateKeys.economy: 2,
            StateKeys.health: 2,
            StateKeys.education: 2,
          },
        ),
        EventChoice(
          label: 'تأجيله',
          resultText: 'التخطيط استمر ببيانات قديمة.',
          stateEffects: <String, double>{StateKeys.economy: -1},
        ),
      ],
    ),
    const GameEvent(
      id: 'c_heritage_restoration',
      title: 'ترميم حي تاريخي',
      description: 'حي تاريخي مهدد بالتدهور يحتاج خطة ترميم.',
      category: EventCategory.classic,
      weight: 8,
      choices: <EventChoice>[
        EventChoice(
          label: 'ترميم شامل',
          resultText: 'الحي عاد للحياة وصار مقصدًا للزوار.',
          costCash: 100000,
          stateEffects: <String, double>{
            StateKeys.culture: 6,
            StateKeys.tourism: 5,
          },
        ),
        EventChoice(
          label: 'إزالته وبناء مشروع حديث',
          resultText: 'عائد اقتصادي مقابل خسارة ثقافية.',
          stateEffects: <String, double>{
            StateKeys.economy: 4,
            StateKeys.culture: -6,
            StateKeys.publicSatisfaction: -3,
          },
        ),
      ],
    ),
  ];
}

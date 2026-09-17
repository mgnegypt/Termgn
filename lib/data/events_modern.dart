import '../models/enums.dart';
import '../models/event.dart';
import '../models/game_state.dart';

/// Modern-era events: digital opinion, cyber security, AI policy, climate,
/// crypto and the remote economy. MVP target: 25-30 events.
abstract final class EventsModern {
  static final List<GameEvent> all = <GameEvent>[
    const GameEvent(
      id: 'm_hashtag_wave',
      title: 'هاشتاج ضد وزير',
      description:
          'حملة إلكترونية واسعة تطالب بإقالة أحد الوزراء بعد تصريح مثير للجدل.',
      category: EventCategory.modern,
      weight: 12,
      choices: <EventChoice>[
        EventChoice(
          label: 'إقالة الوزير',
          resultText: 'الشارع الرقمي احتفل بالقرار.',
          stateEffects: <String, double>{
            StateKeys.digitalOpinion: 10,
            StateKeys.publicSatisfaction: 4,
            StateKeys.economy: -1,
          },
        ),
        EventChoice(
          label: 'اعتذار رسمي بدون إقالة',
          resultText: 'الغضب خفّ جزئيًا.',
          stateEffects: <String, double>{StateKeys.digitalOpinion: 2},
        ),
        EventChoice(
          label: 'تجاهل الحملة',
          resultText: 'الحملة تضخمت لأسابيع.',
          stateEffects: <String, double>{
            StateKeys.digitalOpinion: -12,
            StateKeys.publicSatisfaction: -3,
          },
        ),
      ],
    ),
    GameEvent(
      id: 'm_data_leak',
      title: 'تسريب بيانات حكومية',
      description: 'تسريب قاعدة بيانات تحتوي معلومات مواطنين على الإنترنت.',
      category: EventCategory.modern,
      weight: 11,
      condition: (GameState s) => s.cyberSecurity < 70,
      choices: <EventChoice>[
        const EventChoice(
          label: 'تحقيق شفاف وتحديث أنظمة الحماية',
          resultText: 'الثقة تضررت لكن النظام أصبح أقوى.',
          costCash: 120000,
          stateEffects: <String, double>{
            StateKeys.cyberSecurity: 8,
            StateKeys.digitalOpinion: -3,
          },
        ),
        const EventChoice(
          label: 'التقليل من حجم الحادث',
          resultText: 'الغضب الرقمي تصاعد بسرعة.',
          stateEffects: <String, double>{
            StateKeys.digitalOpinion: -14,
            StateKeys.cyberSecurity: -3,
          },
        ),
      ],
    ),
    GameEvent(
      id: 'm_ransomware_hospital',
      title: 'هجوم فدية على مستشفى',
      description: 'برمجية فدية عطّلت أنظمة أحد أكبر المستشفيات.',
      category: EventCategory.modern,
      weight: 10,
      condition: (GameState s) => s.cyberSecurity < 65,
      choices: <EventChoice>[
        const EventChoice(
          label: 'دفع الفدية لاستعادة الخدمة فورًا',
          resultText: 'الخدمة عادت بسرعة وسابقة خطيرة تأسست.',
          costCash: 140000,
          stateEffects: <String, double>{
            StateKeys.health: 1,
            StateKeys.cyberSecurity: -4,
            StateKeys.digitalOpinion: -4,
          },
        ),
        const EventChoice(
          label: 'رفض الدفع واستعادة الأنظمة يدويًا',
          resultText: 'أسابيع من التعطل، لكن بلا تنازل.',
          costCash: 60000,
          stateEffects: <String, double>{
            StateKeys.health: -6,
            StateKeys.cyberSecurity: 6,
          },
        ),
      ],
    ),
    const GameEvent(
      id: 'm_ai_regulation',
      title: 'قانون تنظيم الذكاء الاصطناعي',
      description:
          'ضغوط متضاربة: الشركات تريد حرية أوسع، والنقابات تخشى فقدان الوظائف.',
      category: EventCategory.modern,
      weight: 12,
      choices: <EventChoice>[
        EventChoice(
          label: 'تحرير واسع للاستخدام',
          resultText: 'قفزة تقنية وقلق اجتماعي.',
          stateEffects: <String, double>{
            StateKeys.technology: 8,
            StateKeys.economy: 4,
            StateKeys.publicSatisfaction: -5,
            StateKeys.axisEconomic: -6,
          },
        ),
        EventChoice(
          label: 'تنظيم صارم يحمي الوظائف',
          resultText: 'استقرار اجتماعي ونمو تقني أبطأ.',
          stateEffects: <String, double>{
            StateKeys.technology: -3,
            StateKeys.publicSatisfaction: 6,
            StateKeys.axisEconomic: 6,
          },
        ),
        EventChoice(
          label: 'تنظيم متوازن مع برامج إعادة تأهيل',
          resultText: 'مسار وسط مكلف لكنه مستقر.',
          costCash: 110000,
          stateEffects: <String, double>{
            StateKeys.technology: 4,
            StateKeys.education: 4,
            StateKeys.publicSatisfaction: 2,
          },
        ),
      ],
    ),
    const GameEvent(
      id: 'm_crypto_decision',
      title: 'قرار بشأن العملات الرقمية',
      description: 'انتشار تداول الكريبتو يفرض قرارًا تنظيميًا واضحًا.',
      category: EventCategory.modern,
      weight: 11,
      choices: <EventChoice>[
        EventChoice(
          label: 'ترخيص منظّم وفرض ضرائب',
          resultText: 'إيراد جديد وتقلب مالي مقبول.',
          stateEffects: <String, double>{
            StateKeys.treasuryCash: 150000,
            StateKeys.technology: 4,
            StateKeys.economy: 2,
          },
          allRelationsDelta: -2,
        ),
        EventChoice(
          label: 'حظر كامل',
          resultText: 'استقرار مالي وغضب في مجتمع التقنية.',
          stateEffects: <String, double>{
            StateKeys.digitalOpinion: -7,
            StateKeys.technology: -3,
            StateKeys.economy: -1,
          },
        ),
        EventChoice(
          label: 'تأجيل القرار ومراقبة السوق',
          resultText: 'الغموض التنظيمي استمر.',
          stateEffects: <String, double>{StateKeys.economy: -1},
        ),
      ],
    ),
    const GameEvent(
      id: 'm_remote_work_tax',
      title: 'ضرائب العمل عن بعد',
      description:
          'آلاف يعملون عن بعد لشركات أجنبية بلا إطار ضريبي واضح.',
      category: EventCategory.modern,
      weight: 10,
      choices: <EventChoice>[
        EventChoice(
          label: 'إطار ضريبي مبسّط',
          resultText: 'إيراد منتظم وقبول واسع.',
          stateEffects: <String, double>{
            StateKeys.treasuryCash: 90000,
            StateKeys.technology: 2,
            StateKeys.digitalOpinion: -2,
          },
        ),
        EventChoice(
          label: 'إعفاء كامل لجذب الكفاءات',
          resultText: 'الكفاءات بقيت والخزينة خسرت فرصة.',
          stateEffects: <String, double>{
            StateKeys.migrationBalance: 3,
            StateKeys.technology: 4,
            StateKeys.digitalOpinion: 5,
          },
        ),
      ],
    ),
    const GameEvent(
      id: 'm_deepfake',
      title: 'مقطع مفبرك للحاكم',
      description: 'فيديو مزيف بالذكاء الاصطناعي ينتشر بسرعة على المنصات.',
      category: EventCategory.modern,
      weight: 11,
      choices: <EventChoice>[
        EventChoice(
          label: 'رد تقني موثّق وسريع',
          resultText: 'الفبركة انكشفت وزادت الثقة في الدولة.',
          costCash: 40000,
          stateEffects: <String, double>{
            StateKeys.digitalOpinion: 8,
            StateKeys.cyberSecurity: 3,
          },
        ),
        EventChoice(
          label: 'حجب المنصة مؤقتًا',
          resultText: 'انتشار أقل وانتقادات أكبر.',
          stateEffects: <String, double>{
            StateKeys.digitalOpinion: -10,
            StateKeys.axisSocial: 6,
          },
        ),
        EventChoice(
          label: 'الصمت',
          resultText: 'الرواية المزيفة سيطرت على النقاش.',
          stateEffects: <String, double>{StateKeys.digitalOpinion: -8},
        ),
      ],
    ),
    GameEvent(
      id: 'm_startup_unicorn',
      title: 'شركة ناشئة تحقق نجاحًا عالميًا',
      description: 'شركة محلية تصل لتقييم ضخم وتضع الدولة على الخريطة التقنية.',
      category: EventCategory.modern,
      weight: 9,
      condition: (GameState s) => s.technology >= 40,
      choices: <EventChoice>[
        const EventChoice(
          label: 'دعمها كواجهة وطنية',
          resultText: 'موجة استثمار واهتمام دولي.',
          costCash: 50000,
          stateEffects: <String, double>{
            StateKeys.technology: 6,
            StateKeys.economy: 5,
            StateKeys.digitalOpinion: 6,
          },
        ),
        const EventChoice(
          label: 'الحياد وترك السوق يعمل',
          resultText: 'نجاح خاص بأثر عام محدود.',
          stateEffects: <String, double>{StateKeys.economy: 2},
        ),
      ],
    ),
    const GameEvent(
      id: 'm_platform_ban_demand',
      title: 'مطالب بحجب منصة',
      description: 'تيار محافظ يطالب بحجب منصة اجتماعية واسعة الانتشار.',
      category: EventCategory.modern,
      weight: 10,
      choices: <EventChoice>[
        EventChoice(
          label: 'الحجب',
          resultText: 'رضا فئة ورفض فئة أكبر رقميًا.',
          stateEffects: <String, double>{
            StateKeys.digitalOpinion: -12,
            StateKeys.axisSocial: 10,
            StateKeys.publicSatisfaction: -2,
          },
        ),
        EventChoice(
          label: 'تنظيم المحتوى بدل الحجب',
          resultText: 'حل وسط هدّأ الجدل.',
          costCash: 30000,
          stateEffects: <String, double>{
            StateKeys.digitalOpinion: 2,
            StateKeys.cyberSecurity: 2,
          },
        ),
      ],
    ),
    const GameEvent(
      id: 'm_climate_summit',
      title: 'قمة مناخية دولية',
      description: 'ضغط دولي لتقديم التزامات بخفض الانبعاثات.',
      category: EventCategory.modern,
      weight: 11,
      choices: <EventChoice>[
        EventChoice(
          label: 'التزام طموح',
          resultText: 'إشادة دولية وتكلفة صناعية.',
          costCash: 120000,
          stateEffects: <String, double>{
            StateKeys.environment: 7,
            StateKeys.cleanEnergyRatio: 0.06,
            StateKeys.industry: -3,
          },
          allRelationsDelta: 6,
        ),
        EventChoice(
          label: 'التزام رمزي',
          resultText: 'موقف محسوب بلا مكاسب كبيرة.',
          stateEffects: <String, double>{StateKeys.environment: 2},
          allRelationsDelta: 1,
        ),
        EventChoice(
          label: 'رفض أي التزام',
          resultText: 'حرية صناعية وعزلة أكبر.',
          stateEffects: <String, double>{
            StateKeys.industry: 3,
            StateKeys.environment: -4,
            StateKeys.axisForeign: 5,
          },
          allRelationsDelta: -7,
        ),
      ],
    ),
    GameEvent(
      id: 'm_carbon_tax',
      title: 'ضريبة الكربون',
      description: 'مقترح بفرض ضريبة على الانبعاثات الصناعية.',
      category: EventCategory.modern,
      weight: 10,
      condition: (GameState s) => s.industry >= 40,
      choices: <EventChoice>[
        const EventChoice(
          label: 'إقرار الضريبة',
          resultText: 'إيراد جديد وضغط على الصناعة.',
          stateEffects: <String, double>{
            StateKeys.treasuryCash: 110000,
            StateKeys.environment: 5,
            StateKeys.industry: -4,
          },
        ),
        const EventChoice(
          label: 'تأجيلها',
          resultText: 'الصناعة ارتاحت والانبعاثات مستمرة.',
          stateEffects: <String, double>{StateKeys.environment: -2},
        ),
      ],
    ),
    const GameEvent(
      id: 'm_solar_offer',
      title: 'عرض مشروع شمسي',
      description: 'تحالف دولي يعرض تمويل مشروع طاقة شمسية كبير.',
      category: EventCategory.modern,
      weight: 10,
      choices: <EventChoice>[
        EventChoice(
          label: 'القبول بشراكة',
          resultText: 'طاقة نظيفة أسرع بتنازل عن حصة أرباح.',
          costCash: 80000,
          stateEffects: <String, double>{
            StateKeys.energy: 7,
            StateKeys.cleanEnergyRatio: 0.10,
            StateKeys.environment: 4,
          },
          allRelationsDelta: 3,
        ),
        EventChoice(
          label: 'تنفيذ وطني كامل',
          resultText: 'تكلفة أعلى وسيادة كاملة على المشروع.',
          costCash: 200000,
          stateEffects: <String, double>{
            StateKeys.energy: 8,
            StateKeys.cleanEnergyRatio: 0.12,
            StateKeys.axisEconomic: 5,
          },
        ),
      ],
    ),
    GameEvent(
      id: 'm_ev_mandate',
      title: 'إلزام السيارات الكهربائية',
      description: 'مقترح لإلزام تحويل أسطول النقل إلى كهربائي تدريجيًا.',
      category: EventCategory.modern,
      weight: 9,
      condition: (GameState s) => s.energy >= 45,
      choices: <EventChoice>[
        const EventChoice(
          label: 'إقرار خطة تحول',
          resultText: 'بيئة أفضل وتكلفة انتقالية.',
          costCash: 130000,
          stateEffects: <String, double>{
            StateKeys.environment: 6,
            StateKeys.technology: 3,
            StateKeys.publicSatisfaction: -2,
          },
        ),
        const EventChoice(
          label: 'تحفيز طوعي فقط',
          resultText: 'تحول أبطأ بلا صدام.',
          costCash: 40000,
          stateEffects: <String, double>{StateKeys.environment: 2},
        ),
      ],
    ),
    const GameEvent(
      id: 'm_gig_protest',
      title: 'احتجاج عمال التطبيقات',
      description: 'سائقو ومندوبو التطبيقات يطالبون بحقوق وتأمين صحي.',
      category: EventCategory.modern,
      weight: 10,
      choices: <EventChoice>[
        EventChoice(
          label: 'قانون يمنحهم حقوقًا كاملة',
          resultText: 'عدالة اجتماعية بتكلفة على الشركات.',
          stateEffects: <String, double>{
            StateKeys.publicSatisfaction: 7,
            StateKeys.digitalOpinion: 5,
            StateKeys.economy: -3,
            StateKeys.axisEconomic: 6,
          },
        ),
        EventChoice(
          label: 'تسوية مع الشركات',
          resultText: 'تحسن جزئي في الظروف.',
          costCash: 50000,
          stateEffects: <String, double>{StateKeys.publicSatisfaction: 3},
        ),
        EventChoice(
          label: 'ترك الأمر للسوق',
          resultText: 'الاحتجاجات استمرت رقميًا.',
          stateEffects: <String, double>{
            StateKeys.digitalOpinion: -7,
            StateKeys.publicSatisfaction: -4,
          },
        ),
      ],
    ),
    GameEvent(
      id: 'm_smart_city',
      title: 'مشروع مدينة ذكية',
      description: 'خطة لبناء حي ذكي بالكامل كنموذج تقني.',
      category: EventCategory.modern,
      weight: 9,
      condition: (GameState s) => s.technology >= 45 && s.treasuryCash > 250000,
      choices: <EventChoice>[
        const EventChoice(
          label: 'إطلاق المشروع',
          resultText: 'واجهة تقنية للدولة وعائد طويل المدى.',
          costCash: 230000,
          stateEffects: <String, double>{
            StateKeys.technology: 8,
            StateKeys.economy: 4,
            StateKeys.digitalOpinion: 5,
          },
        ),
        const EventChoice(
          label: 'الاكتفاء بمشروع تجريبي صغير',
          resultText: 'تجربة محدودة بمخاطر أقل.',
          costCash: 70000,
          stateEffects: <String, double>{StateKeys.technology: 3},
        ),
      ],
    ),
    const GameEvent(
      id: 'm_egov',
      title: 'تحول رقمي في الخدمات الحكومية',
      description: 'خطة لنقل كل الخدمات الحكومية إلى منصة واحدة.',
      category: EventCategory.modern,
      weight: 11,
      choices: <EventChoice>[
        EventChoice(
          label: 'تنفيذ شامل',
          resultText: 'كفاءة أعلى ورضا رقمي واضح.',
          costCash: 150000,
          stateEffects: <String, double>{
            StateKeys.technology: 6,
            StateKeys.digitalOpinion: 8,
            StateKeys.economy: 3,
          },
        ),
        EventChoice(
          label: 'تنفيذ تدريجي',
          resultText: 'تقدم بطيء لكنه آمن.',
          costCash: 60000,
          stateEffects: <String, double>{
            StateKeys.technology: 3,
            StateKeys.digitalOpinion: 3,
          },
        ),
      ],
    ),
    const GameEvent(
      id: 'm_cable_cut',
      title: 'انقطاع كابل بحري',
      description: 'تلف كابل إنترنت بحري يبطئ الاتصال في البلاد.',
      category: EventCategory.modern,
      weight: 9,
      choices: <EventChoice>[
        EventChoice(
          label: 'إصلاح عاجل وتنويع المسارات',
          resultText: 'الشبكة أصبحت أكثر مرونة.',
          costCash: 100000,
          stateEffects: <String, double>{
            StateKeys.technology: 4,
            StateKeys.cyberSecurity: 4,
          },
        ),
        EventChoice(
          label: 'إصلاح الحد الأدنى',
          resultText: 'الخدمة عادت بهشاشة كما كانت.',
          costCash: 35000,
          stateEffects: <String, double>{
            StateKeys.digitalOpinion: -4,
            StateKeys.economy: -2,
          },
        ),
      ],
    ),
    const GameEvent(
      id: 'm_chip_shortage',
      title: 'نقص عالمي في الرقائق',
      description: 'أزمة إمداد عالمية تعطل خطوط الإنتاج التقني.',
      category: EventCategory.modern,
      weight: 9,
      choices: <EventChoice>[
        EventChoice(
          label: 'شراء بأسعار مرتفعة',
          resultText: 'الإنتاج استمر بتكلفة عالية.',
          costCash: 140000,
          stateEffects: <String, double>{StateKeys.industry: 2},
        ),
        EventChoice(
          label: 'استثمار في تصنيع محلي',
          resultText: 'بداية سلسلة إمداد وطنية.',
          costCash: 200000,
          stateEffects: <String, double>{
            StateKeys.technology: 6,
            StateKeys.industry: 5,
          },
        ),
        EventChoice(
          label: 'تقليص الإنتاج مؤقتًا',
          resultText: 'توفير مالي بخسارة اقتصادية.',
          stateEffects: <String, double>{
            StateKeys.industry: -5,
            StateKeys.economy: -3,
          },
        ),
      ],
    ),
    const GameEvent(
      id: 'm_tech_giant_office',
      title: 'شركة تقنية عالمية تطلب مقرًا',
      description: 'عملاق تقني يريد مقرًا إقليميًا مقابل حوافز وبيانات.',
      category: EventCategory.modern,
      weight: 10,
      choices: <EventChoice>[
        EventChoice(
          label: 'الموافقة بشروط سيادة البيانات',
          resultText: 'وظائف واستثمار مع حفظ البيانات محليًا.',
          stateEffects: <String, double>{
            StateKeys.economy: 5,
            StateKeys.technology: 5,
            StateKeys.cyberSecurity: 2,
          },
          allRelationsDelta: 2,
        ),
        EventChoice(
          label: 'الموافقة بحوافز سخية',
          resultText: 'استثمار أكبر بتنازلات تنظيمية.',
          costCash: 90000,
          stateEffects: <String, double>{
            StateKeys.economy: 7,
            StateKeys.technology: 6,
            StateKeys.cyberSecurity: -4,
            StateKeys.axisEconomic: -6,
          },
        ),
        EventChoice(
          label: 'الرفض',
          resultText: 'سيادة أعلى وفرصة اقتصادية ضائعة.',
          stateEffects: <String, double>{
            StateKeys.axisEconomic: 5,
            StateKeys.technology: -2,
          },
        ),
      ],
    ),
    GameEvent(
      id: 'm_cbdc',
      title: 'عملة رقمية للبنك المركزي',
      description: 'مقترح بإصدار نسخة رقمية رسمية من عملة الدولة.',
      category: EventCategory.modern,
      weight: 9,
      condition: (GameState s) => s.technology >= 40,
      choices: <EventChoice>[
        const EventChoice(
          label: 'الإصدار',
          resultText: 'كفاءة مالية أعلى وجدل حول الخصوصية.',
          costCash: 120000,
          stateEffects: <String, double>{
            StateKeys.economy: 5,
            StateKeys.technology: 4,
            StateKeys.digitalOpinion: -3,
          },
        ),
        const EventChoice(
          label: 'دراسة وتأجيل',
          resultText: 'قرار محافظ بلا مخاطر.',
          stateEffects: <String, double>{},
        ),
      ],
    ),
    const GameEvent(
      id: 'm_misinformation',
      title: 'موجة تضليل إعلامي',
      description: 'حسابات منظمة تنشر أخبارًا مزيفة عن أزمة داخلية.',
      category: EventCategory.modern,
      weight: 11,
      choices: <EventChoice>[
        EventChoice(
          label: 'غرفة تحقق ووعي رقمي',
          resultText: 'الرواية الرسمية استعادت الثقة.',
          costCash: 60000,
          stateEffects: <String, double>{
            StateKeys.digitalOpinion: 7,
            StateKeys.cyberSecurity: 3,
          },
        ),
        EventChoice(
          label: 'ملاحقة قانونية للحسابات',
          resultText: 'ردع فعّال بثمن على صورة الحريات.',
          stateEffects: <String, double>{
            StateKeys.digitalOpinion: -4,
            StateKeys.cyberSecurity: 4,
            StateKeys.axisSocial: 5,
          },
        ),
      ],
    ),
    GameEvent(
      id: 'm_ai_job_loss',
      title: 'أتمتة تهدد وظائف',
      description: 'شركات كبرى تستبدل آلاف الوظائف بأنظمة ذكية.',
      category: EventCategory.modern,
      weight: 11,
      condition: (GameState s) => s.technology >= 50,
      choices: <EventChoice>[
        const EventChoice(
          label: 'برنامج وطني لإعادة التأهيل',
          resultText: 'انتقال أهدأ لسوق العمل.',
          costCash: 140000,
          stateEffects: <String, double>{
            StateKeys.education: 6,
            StateKeys.publicSatisfaction: 5,
          },
        ),
        const EventChoice(
          label: 'ضريبة على الأتمتة',
          resultText: 'إيراد وحماية جزئية للوظائف.',
          stateEffects: <String, double>{
            StateKeys.treasuryCash: 100000,
            StateKeys.technology: -3,
            StateKeys.publicSatisfaction: 3,
          },
        ),
        const EventChoice(
          label: 'ترك السوق يتكيّف',
          resultText: 'نمو تقني سريع وتوتر اجتماعي.',
          stateEffects: <String, double>{
            StateKeys.technology: 5,
            StateKeys.publicSatisfaction: -7,
            StateKeys.digitalOpinion: -5,
          },
        ),
      ],
    ),
    const GameEvent(
      id: 'm_green_bonds',
      title: 'سندات خضراء',
      description: 'فرصة لإصدار سندات مخصصة لمشاريع بيئية.',
      category: EventCategory.modern,
      weight: 9,
      choices: <EventChoice>[
        EventChoice(
          label: 'الإصدار',
          resultText: 'تمويل بيئي فوري مقابل دين إضافي.',
          stateEffects: <String, double>{
            StateKeys.treasuryCash: 200000,
            StateKeys.debt: 200000,
            StateKeys.environment: 4,
            StateKeys.cleanEnergyRatio: 0.05,
          },
          allRelationsDelta: 3,
        ),
        EventChoice(
          label: 'الاعتذار',
          resultText: 'لا دين جديد ولا تمويل بيئي.',
          stateEffects: <String, double>{},
        ),
      ],
    ),
    const GameEvent(
      id: 'm_data_sovereignty',
      title: 'قانون سيادة البيانات',
      description: 'مقترح بإلزام تخزين بيانات المواطنين داخل الدولة.',
      category: EventCategory.modern,
      weight: 10,
      choices: <EventChoice>[
        EventChoice(
          label: 'الإقرار',
          resultText: 'حماية أعلى وتكلفة على الشركات الأجنبية.',
          costCash: 80000,
          stateEffects: <String, double>{
            StateKeys.cyberSecurity: 8,
            StateKeys.technology: 2,
            StateKeys.economy: -2,
          },
          allRelationsDelta: -3,
        ),
        EventChoice(
          label: 'إطار مرن',
          resultText: 'توازن بين الحماية وجذب الاستثمار.',
          stateEffects: <String, double>{
            StateKeys.cyberSecurity: 3,
            StateKeys.economy: 1,
          },
        ),
      ],
    ),
    const GameEvent(
      id: 'm_drone_incursion',
      title: 'اختراق بطائرة مسيّرة',
      description: 'مسيّرة مجهولة تحلق فوق منشأة حساسة.',
      category: EventCategory.modern,
      weight: 9,
      choices: <EventChoice>[
        EventChoice(
          label: 'منظومة مضادة للمسيّرات',
          resultText: 'الأجواء الحساسة أصبحت محمية.',
          costCash: 130000,
          stateEffects: <String, double>{
            StateKeys.militarySecurity: 6,
            StateKeys.cyberSecurity: 3,
          },
        ),
        EventChoice(
          label: 'تحقيق أمني فقط',
          resultText: 'معلومات أكثر بلا حماية جديدة.',
          stateEffects: <String, double>{StateKeys.militarySecurity: 1},
        ),
      ],
    ),
    GameEvent(
      id: 'm_satellite_program',
      title: 'برنامج أقمار صناعية',
      description: 'مقترح لإطلاق أول قمر صناعي وطني للاتصالات والرصد.',
      category: EventCategory.modern,
      weight: 8,
      condition: (GameState s) => s.technology >= 55,
      choices: <EventChoice>[
        const EventChoice(
          label: 'إطلاق البرنامج',
          resultText: 'قفزة رمزية وتقنية للدولة.',
          costCash: 260000,
          stateEffects: <String, double>{
            StateKeys.technology: 9,
            StateKeys.militarySecurity: 4,
            StateKeys.digitalOpinion: 6,
            StateKeys.culture: 3,
          },
        ),
        const EventChoice(
          label: 'استئجار خدمات أقمار أجنبية',
          resultText: 'حل عملي أرخص بلا سيادة تقنية.',
          costCash: 70000,
          stateEffects: <String, double>{StateKeys.technology: 2},
        ),
      ],
    ),
    GameEvent(
      id: 'm_viral_tourism',
      title: 'لحظة سياحية فيروسية',
      description: 'مقطع لأحد معالم الدولة يحقق انتشارًا عالميًا هائلًا.',
      category: EventCategory.modern,
      weight: 9,
      condition: (GameState s) => s.builtLandmarks.isNotEmpty,
      choices: <EventChoice>[
        const EventChoice(
          label: 'حملة تسويق سياحي فورية',
          resultText: 'موجة زوار غير مسبوقة.',
          costCash: 70000,
          stateEffects: <String, double>{
            StateKeys.tourism: 8,
            StateKeys.digitalOpinion: 6,
            StateKeys.culture: 3,
          },
        ),
        const EventChoice(
          label: 'الاستفادة المجانية من الزخم',
          resultText: 'ارتفاع محدود لكنه بلا تكلفة.',
          stateEffects: <String, double>{StateKeys.tourism: 3},
        ),
      ],
    ),
    const GameEvent(
      id: 'm_influencer_scandal',
      title: 'جدل حول مؤثر مدعوم حكوميًا',
      description: 'حملة دعائية رسمية عبر مؤثرين تتحول إلى مادة للسخرية.',
      category: EventCategory.modern,
      weight: 9,
      choices: <EventChoice>[
        EventChoice(
          label: 'وقف الحملة والاعتراف بالخطأ',
          resultText: 'شفافية أعادت جزءًا من الثقة.',
          stateEffects: <String, double>{StateKeys.digitalOpinion: 4},
        ),
        EventChoice(
          label: 'مضاعفة الميزانية الدعائية',
          resultText: 'ضجيج أكثر ومصداقية أقل.',
          costCash: 60000,
          stateEffects: <String, double>{
            StateKeys.digitalOpinion: -8,
            StateKeys.publicSatisfaction: -2,
          },
        ),
      ],
    ),
    const GameEvent(
      id: 'm_open_data',
      title: 'مبادرة البيانات المفتوحة',
      description: 'مقترح بنشر بيانات الدولة غير الحساسة للعامة والباحثين.',
      category: EventCategory.modern,
      weight: 8,
      choices: <EventChoice>[
        EventChoice(
          label: 'الإطلاق',
          resultText: 'شفافية وبيئة بحثية أنشط.',
          costCash: 45000,
          stateEffects: <String, double>{
            StateKeys.digitalOpinion: 6,
            StateKeys.education: 3,
            StateKeys.technology: 2,
          },
        ),
        EventChoice(
          label: 'الاكتفاء بتقارير رسمية',
          resultText: 'سيطرة أكبر على المعلومة.',
          stateEffects: <String, double>{StateKeys.digitalOpinion: -3},
        ),
      ],
    ),
  ];
}

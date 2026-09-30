package studio.mgn.command

import studio.mgn.content.ContentPack
import studio.mgn.content.CountryDto
import studio.mgn.content.LandmarkPosition
import studio.mgn.model.Achievement
import studio.mgn.model.AiBehavior
import studio.mgn.model.DiplomacyState
import studio.mgn.model.Era
import studio.mgn.model.EventCategory
import studio.mgn.model.EventChoice
import studio.mgn.model.GameEvent
import studio.mgn.model.GameState
import studio.mgn.model.LandmarkDef
import studio.mgn.model.LandmarkKind
import studio.mgn.model.MissionDef
import java.time.LocalDate

/** Minimal fake content pack for @Previews (no resource loading). */
fun previewPack(): ContentPack = ContentPack(
    events = listOf(
        GameEvent(
            id = "preview_event",
            title = "حدث تجريبي",
            description = "وصف تجريبي للمعاينة فقط.",
            category = EventCategory.CLASSIC,
            choices = listOf(
                EventChoice(label = "خيار أول", resultText = "نتيجة أولى"),
                EventChoice(label = "خيار ثانٍ", resultText = "نتيجة ثانية"),
            ),
        ),
    ),
    landmarks = listOf(
        LandmarkDef(
            id = "national_museum",
            nameAr = "المتحف الوطني",
            descriptionAr = "وصف تجريبي.",
            kind = LandmarkKind.CULTURAL,
            costCash = 320000.0,
            buildTurns = 6,
            era = Era.MODERN,
        ),
    ),
    countries = listOf(
        CountryDto(
            id = "arzan",
            nameAr = "أرزان",
            behavior = "trader",
            relation = 40.0,
        ),
    ),
    achievements = listOf(
        Achievement(
            id = "preview_ach",
            titleAr = "إنجاز تجريبي",
            descriptionAr = "وصف.",
            condition = studio.mgn.model.Condition.Cmp(
                "economy",
                studio.mgn.model.CmpOp.GTE,
                80.0,
            ),
        ),
    ),
    missions = listOf(
        MissionDef(
            id = "preview_mission",
            titleAr = "مهمة تجريبية",
            descriptionAr = "وصف.",
            goalKey = "economy",
            goalValue = 60.0,
            gemReward = 5,
        ),
    ),
    landmarkPositions = listOf(
        LandmarkPosition("national_museum", 0.3, 0.7, false),
        LandmarkPosition("presidential_palace", 0.5, 0.3, true, "زخرفي"),
    ),
)
fun previewState(): GameState = GameState.newGame(
    countryName = "المجد",
    rulerTitle = "رئيس",
    countries = mapOf(
        "arzan" to DiplomacyState(
            countryId = "arzan",
            nameAr = "أرزان",
            behavior = AiBehavior.TRADER,
            relation = 40.0,
        ),
        "sahran" to DiplomacyState(
            countryId = "sahran",
            nameAr = "سهران",
            behavior = AiBehavior.HOSTILE,
            relation = -70.0,
        ),
    ),
).copy(
    turnNumber = 14,
    inGameDate = LocalDate.of(2026, 3, 1),
    treasuryCash = 312500.0,
    rulerXp = 26,
)

fun previewStrings(): CommandStrings {
    val keys = (
        TOP_CELLS + STATE_INDICATORS.map { it.first } + listOf(
            "publicSatisfaction",
            "digitalOpinion",
            "cyberSecurity",
            "environment",
            "foodSecurity",
            "energy",
            "tourism",
            "health",
            "education",
            "debt",
            "gems",
            "population",
        )
        ).associateWith { it }
    val sections = mapOf(
        "DASHBOARD" to "القيادة",
        "ECONOMY" to "الاقتصاد",
        "DIPLOMACY" to "الدبلوماسية",
        "DEVELOPMENT" to "التطوير",
        "RESEARCH" to "البحث",
        "INTEL" to "المخابرات",
        "HISTORY" to "السجل",
        "ACHIEVEMENTS" to "الإنجازات",
        "SETTINGS" to "الإعدادات",
    )
    return CommandStrings(
        rulerTitlePrefix = "رئيس",
        level = "مستوى",
        turn = "الدور",
        endTurn = "إنهاء الدور",
        processing = "جارٍ معالجة الدور…",
        decisionsPending = "قرارات معلقة",
        councilTitle = "اجتماع مجلس الأمن",
        councilOpen = "افتح القرارات",
        latestEvents = "آخر الأحداث",
        indicatorsTitle = "مؤشرات الدولة",
        missionsTitle = "المهام",
        showAll = "عرض الكل",
        reportTitle = "تقرير الدور",
        reportClose = "إغلاق",
        reportTreasury = "الخزينة",
        reportNewEvents = "أحداث جديدة",
        reportAchievements = "إنجازات",
        reportMissions = "مهام منجزة",
        unlocksLabel = "مكافآت جديدة",
        decisionsTitle = "القرارات المعلقة",
        decisionsEmpty = "لا قرارات معلقة",
        reroll = "استبدال",
        gemsSuffix = "جواهر",
        notEnoughGems = "جواهر غير كافية",
        chooseResult = "النتيجة",
        continueLabel = "متابعة",
        buildNow = "ابنِ الآن",
        rushLabel = "تسريع",
        builtLabel = "مكتمل",
        underConstructionLabel = "قيد البناء",
        lockedSoon = "قريبًا",
        placeholderSection = "تصل في الخطة 4",
        relationLabel = "العلاقة",
        treatiesLabel = "المعاهدات",
        noTreaties = "لا معاهدات",
        remainingTurns = "دور متبقٍ",
        costLabel = "التكلفة",
        effectsPreview = "الأثر المتوقع",
        turnBadge = { turn -> "د$turn" },
        keyLabels = keys,
        sectionLabels = sections,
        history = HistoryStrings(
            title = "السجل",
            empty = "لا سجل بعد",
            turnLabel = "الدور",
            back = "رجوع",
            filterAll = "الكل",
            filterEconomy = "اقتصاد",
            filterDiplomacy = "دبلوماسية",
            filterEvents = "أحداث",
            turnBadge = { turn -> "د$turn" },
        ),
        achievementsGrid = AchievementsGridStrings(
            title = "الإنجازات",
            empty = "لا إنجازات",
            close = "إغلاق",
            gemsSuffix = "جواهر",
            progress = { a, b -> "$a/$b" },
        ),
        yearly = YearlyStrings(
            title = "التقرير السنوي",
            score = "التقييم",
            gems = "الجواهر",
            bestDecision = "أفضل قرار",
            worstDecision = "أسوأ قرار",
            close = "إغلاق",
        ),
        referendum = ReferendumStrings(
            title = "الاستفتاء",
            passed = "تم تجديد الثقة",
            failed = "فشل الاستفتاء",
            close = "إغلاق",
        ),
        ending = EndingStrings(
            victoryTitle = "نصر عظيم",
            collapseTitle = "انهيار الدولة",
            continuationTitle = "عقد من الحكم",
            turnsLabel = "الأدوار",
            scoreLabel = "التقييم",
            achievementsLabel = "الإنجازات",
            bestDecision = "أفضل قرار",
            worstDecision = "أسوأ قرار",
            bestTurn = "أفضل دور",
            worstTurn = "أسوأ دور",
            menu = "القائمة",
            newGame = "لعبة جديدة",
            continuePlaying = "متابعة اللعب",
        ),
    )
}

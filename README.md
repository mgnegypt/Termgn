# MGN

محاكاة إدارة دولة وبناء حضارة — **إنتاج MGN STUDIO**.

لعبة أندرويد (Flutter) تعمل **بلا إنترنت بالكامل**: لا سيرفر، لا صلاحيات شبكة،
تخزين محلي فقط. الرسومات مكتوبة كـ كود بلا أي ملفات صور خارجية.

> الأجهزة المستهدفة: **Android 15+ فقط (minSdk 35)**. لا دعم للإصدارات الأقدم.

---

## Current Status (v0.2.0)

| المرحلة | المحتوى | الحالة |
|---|---|---|
| 1 | Models + Hive | ✅ مكتملة |
| 2 | Economy + Event + Diplomacy + Turn engines | ✅ مكتملة |
| 3 | Theme (ألوان/خطوط) | ✅ أساسي يعمل (SVG/Painters لاحقًا) |
| 4 | المحتوى (68 حدث، 15 معلم، 10 دول، 18 إنجاز) | ✅ مكتملة |
| 5 | شاشات اللعبة | ✅ مكتملة وظيفيًا: الرئيسية، القرارات، القطاعات، الدبلوماسية، المعالم |
| 6 | إعداد الدولة + Settings | ✅ مكتملة |
| 7 | حفظ/تحميل + توازن | ✅ حفظ متين + 25 اختبارًا + محاكاة 120 دورًا |
| 8 | تلميع وانيميشن | ⏳ أساسي فقط (الأولوية للوظيفة) |
| 9 | نشر Google Play | ⏳ لاحقًا (التوقيع الإنتاجي عند الحاجة) |

اللعبة **قابلة للعب فعليًا**: تأسيس دولة ← دور ← قرارات ← قطاعات ←
دبلوماسية ← معالم ← تقارير سنوية ← استفتاء ← نصر/انهيار/استمرار.

---

## البنية

```
lib/
  main.dart                    # Hive + Riverpod + RTL → AppShell
  models/                      # GameState (+isCollapsed/hasWon), GameEvent, Landmark, ...
  engine/                      # balance_config, economy, event, diplomacy, construction, turn
  data/                        # أحداث، معالم، دول، إنجازات
  storage/hive_boxes.dart      # تخزين محلي متين (معالجة save فاسد + history محدود 400)
  graphics/theme/              # palette + typography (هوية MGN)
  state/game_provider.dart     # الواجهة الوحيدة بين الشاشات والمحركات
  screens/
    app_shell.dart             # تبويب سفلي: رئيسية/قرارات/قطاعات/دبلوماسية/معالم/المزيد
    setup_screen.dart          # تأسيس الدولة (اسم، لقب، طول الدور، راية، توجهات)
    dashboard_screen.dart      # نظرة عامة + الدور القادم + التقارير + المؤشرات
    decision_screen.dart       # القرارات المعلقة + استبدال بالجواهر
    sectors_screen.dart        # ضرائب/عسكر/دعم + استثمار + قروض
    diplomacy_screen.dart      # علاقات + معاهدات + حرب + مواقف
    landmarks_screen.dart      # كتالوج + قيد البناء + تسريع بالجواهر
    settings_screen.dart       # إعدادات + إنجازات + سجل + حذف الحفظ
  widgets/common.dart          # Panel/IndicatorBar/StatChip مشتركة
test/                          # 25 اختبارًا
tool/balance_sim.dart          # محاكاة التوازن
.github/workflows/android-build.yml  # بناء APK على GitHub
```

### قواعد ثابتة

- كل رقم توازن في `engine/balance_config.dart` فقط.
- صفر ملفات صور (PNG/JPG).
- صفر شبكة: الـ release manifest يحذف `INTERNET` عبر `tools:node="remove"`.
- الحفظ بخرائط عادية بلا `build_runner`.
- `minSdk = 35` صريح، Java 17، Flutter stable حديث.

---

## التشغيل

```bash
flutter pub get
flutter run
flutter test                      # 25 اختبارًا
flutter analyze
dart run tool/balance_sim.dart 120 42
```

## بناء APK

```bash
flutter build apk --release
# build/app/outputs/flutter-apk/app-release.apk
```

**البناء الرسمي يتم على GitHub Actions** (انظر أدناه). لا يُبنى APK محليًا
ثم يُرفع — GitHub نفسه هو بيئة البناء.

## GitHub Actions

- الملف: `.github/workflows/android-build.yml`
- Runner: `ubuntu-latest` (GitHub-hosted)
- Flutter: `3.47.5` stable — Java: Temurin `17`
- الخطوات: checkout ← setup-java ← setup-flutter ← pub get ← analyze ←
  test ← build apk --release ← verify ← upload artifact ← release عند tag
- Artifact: **Termgn-Android-APK** (`app-release.apk`)
- Release: يُنشأ تلقائيًا عند push tag `v*` ويُرفق به الـ APK (عبر `GITHUB_TOKEN`)
- التشغيل: push على `main` / pull request / تشغيل يدوي / tag

الحصول على الـ APK: صفحة Actions ← آخر run أخضر ← Artifacts ←
`Termgn-Android-APK`، أو صفحة Releases عند وجود tag.

---

## التوازن (محاكاة 120 دورًا، بذرة 42)

- التقييم ~43 → ~82 مع لعب معقول، بلا انهيار وبلا تشبّع مبكر.
- الخزينة موجبة والدين صفر مع إدارة معقولة (فائض ~14.8M/دور في المحاكاة).
- الجواهر نادرة: فقط التقارير السنوية والإنجازات.

## Known Limitations

- الأيقونة لا تزال قالب Flutter الافتراضي.
- توقيع الـ Release بمفاتيح debug (تجريبي — التوقيع الإنتاجي عند مرحلة Play).
- لا SVG/Painters مخصصة بعد — الرايات presets لونية.
- لا طبقة ترجمة منفصلة — النصوص عربية داخل الشاشات.

# MGN — محاكاة إدارة دولة (Kotlin + Jetpack Compose)

محاكاة إدارة دولة وبناء حضارة — **إنتاج MGN STUDIO**.

- المنصة: **Android 15+ فقط** (`minSdk = targetSdk = compileSdk = 35`).
- **أوفلاين بالكامل**: لا `INTERNET` في الـ release (يُتحقق منه في CI عبر `aapt`).
- الاتجاه: Landscape. اللغة: عربي RTL. الثيم: أسود فاخر + ذهبي.
- الكود القديم (Flutter) محفوظ للمرجع فقط في `legacy_flutter/`.

## الحالة: الإصدار الأول (v0.4.0)

لعبة كاملة قابلة للعب: تأسيس ← قيادة ← اقتصاد/دبلوماسية/معالم ←
أحداث وقرارات ← مهام وإنجازات ← تقارير سنوية واستفتاء ← 3 نهايات.

## البنية

```
app/                  # التطبيق: تنقل، قائمة، إعدادات، شاشات الميزات
core/model/           # Kotlin JVM صافي: GameState غير قابل للتعديل
core/engine/          # المحركات النقية: Balance/Event/Diplomacy/Construction/
                      #   Turn/Mission (Random قابل للحقن)
core/data/            # Room (حفظ + backup) + DataStore (إعدادات) + Repository
core/audio/           # Media3 موسيقى + SoundPool مؤثرات + اهتزاز (صمت بلا ملفات)
design/               # MgnTheme، المكونات، GameAnimation، الخطوط
content/              # JSON: أحداث/معالم/دول/إنجازات/مهام/مواضع + Validator
feature/setup         # معالج التأسيس المصوّر
feature/command       # شاشة القيادة + القرارات + السجل + النهايات
feature/economy       # القطاعات والقروض والمخطط
feature/diplomacy     # خريطة العالم والمعاهدات والحرب
feature/development   # كتالوج المعالم والبناء
tools/balance-sim/    # محاكاة التوازن + البوت الآلي (5 seeds × 3 سلوكيات)
benchmark/            # Macrobenchmark (يحتاج جهازًا، لا يعمل على CI)
legacy_flutter/       # مرجع القراءة فقط
```

قاعدة ثابتة: **كل أرقام التوازن في `BalanceConfig` فقط**.

## التشغيل

```bash
./gradlew :core:model:test :core:engine:test :content:test :core:data:test :app:testDebugUnitTest
./gradlew :tools:balance-sim:run --args="120 42"
./gradlew :tools:balance-sim:balanceBot
./gradlew :app:assembleRelease   # APK (يحتاج Android SDK)
./gradlew :app:bundleRelease     # AAB
python3 tools/check_assets.py [--apk app.apk] [--max-mb 150]
```

## GitHub Actions

`.github/workflows/android-build.yml` على `ubuntu-latest`:
asset gate ← tests ← lint ← sims ← إصدار من الـ tag ← keystore من Secrets ←
`assembleRelease` + `bundleRelease` ← فحص `aapt` ← فحص الحجم والتراخيص ←
Artifacts (**Termgn-Android-APK**/**AAB**) ← Release عند tag `v*` (موقّع عند
توفر الأسرار، وإلا بمفاتيح debug مع تحذير).

## المحتوى

| العنصر | العدد |
|---|---|
| أحداث (30 كلاسيكية + 29 حديثة + 10 أزمات) | 69 |
| معالم (7 ثقافية + 8 حديثة) | 15 |
| دول AI | 10 |
| إنجازات | 18 |
| مهام | 16 |

## التوازن (bot playthrough)

| السلوك | التقييم (120 دورًا) | الدين | الحروب |
|---|---|---|---|
| متحفظ | ~52–58 | 0 | 0 |
| متوازن | ~95–98 | 0 | 0 |
| عدواني | ~64–76 | ≤570K | 1–3 |
| تخريب شامل | انهيار مؤكد | انفجار دَين | 10 |

النصر يُحرز باللعب المتقن (5/15)، والانهيار محجوز للتخريب الشامل.

## النشر

انظر `RELEASE.md` (المفتاح والمسارات) و`PLAY_STORE.md` (الوصف والسياسة)
و`assets/README.md` (خط الأصول) و`assets/LICENSES.md` (التراخيص)
و`docs/perf.md` (الأداء).

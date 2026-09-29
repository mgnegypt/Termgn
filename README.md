# MGN — Kotlin rebuild (PLAN 1 of 5)

محاكاة إدارة دولة وبناء حضارة — **إنتاج MGN STUDIO** — معاد بناؤها بـ
**Kotlin + Jetpack Compose** من الصفر.

> **الحالة:** PLAN 1 — الأساس + الـ Engine + المحتوى.
> التطبيق يفتح على شاشة فارغة داكنة ("MGN"). الشاشات الحقيقية في الخطط اللاحقة.

- المنصة: **Android 15+ فقط** (`minSdk = targetSdk = compileSdk = 35`).
- **أوفلاين بالكامل**: لا `INTERNET` في الـ release (يُتحقق منه في CI عبر `aapt`).
- الاتجاه: Landscape. اللغة: عربي RTL.
- الكود القديم (Flutter) محفوظ للمرجع فقط في `legacy_flutter/` — لا يُعدَّل ولا يُبنى منه.

## البنية

```
app/                  # Android app: شاشة Compose فارغة + Manifest
core/model/           # Kotlin JVM صافي: GameState غير قابل للتعديل + كل الموديلات
core/engine/          # Kotlin JVM صافي: BalanceConfig + Economy/Event/Diplomacy/
                      #   Construction/Turn engines (دوال نقية، Random قابل للحقن)
core/data/            # هيكل فقط (يُملأ في خطط لاحقة)
design/               # هيكل فقط (نظام التصميم في خطط لاحقة)
content/              # JSON: أحداث/معالم/دول/إنجازات + المحمّل + Content Validator
tools/balance-sim/    # محاكاة التوازن JVM (120 دور، seed 42)
tools/convert_content.py  # مولّد JSON من legacy (يُشغَّل عند تغيّر المرجع)
legacy_flutter/       # مرجع القراءة فقط
```

قاعدة ثابتة: **كل أرقام التوازن في `BalanceConfig` فقط** — لا أرقام سحرية elsewhere.

## التشغيل

```bash
./gradlew :core:model:test :core:engine:test :content:test   # اختبارات JVM
./gradlew :tools:balance-sim:run --args="120 42"             # المحاكاة
./gradlew :app:assembleRelease                                # APK (يحتاج Android SDK)
```

## GitHub Actions

`.github/workflows/android-build.yml` على `ubuntu-latest`:
checkout ← Temurin 17 ← Gradle cache ← JVM tests ← `lintRelease` ←
balance-sim ← `assembleRelease` ← فحص `aapt` (min/target=35، بلا INTERNET) ←
رفع **Termgn-Android-APK** ← Release عند tag `v*`.

الحصول على الـ APK: صفحة Actions ← آخر run أخضر ← Artifacts، أو صفحة Releases.

## المحتوى

| العنصر | العدد |
|---|---|
| أحداث (30 كلاسيكية + 29 حديثة + 10 أزمات) | 69 |
| معالم (7 ثقافية + 8 حديثة) | 15 |
| دول AI | 10 |
| إنجازات | 18 |

ملاحظة: المرجع القديم ذكر 68 حدثًا (29+29+10) لكن العدّ الفعلي لملفاته
30 كلاسيكية — الإجمالي الحقيقي **69**. أسماء الدول القديمة محفوظة كما هي.

## التوازن المرجعي (محاكاة 120 دورًا، seed 42)

التقييم ~43 → ~82–87 مع لعب معقول، خزينة موجبة، دين صفر،
فائض ~150–165K/دور، جواهر نادرة (تقارير سنوية وإنجازات فقط).
الأرقام الدقيقة للقديم/الجديد في تقرير PLAN 1 (commit message).

# MGN performance notes

Target: 60fps on a mid-range device, no jank on the command screen.

## What was done (static)

- **Animation budget:** at most 2 animated layers per screen (city clouds +
  light flicker; menu sky + clouds). All `rememberInfiniteTransition`s are
  created only when `reduceMotion == false` **and** the lifecycle is RESUMED.
- **Strong skipping** (Kotlin 2.4 default): all state holders are immutable
  data classes, so unchanged subtrees skip recomposition by value equality.
- **Stable keys** in every `LazyColumn` / `LazyVerticalGrid` (`key = { it.id }`).
- **No high-frequency state reads high in the tree:** deltas are computed
  from `previous` snapshots in the ViewModel, not by polling; count-up
  animations are 500–600ms tweens that settle.
- **Images:** no bitmap assets ship yet; when WebP art lands it must decode
  lazily per screen (see `assets/README.md`) with no static caches.
- **Memory over long sessions:** chronicle capped at 400 entries, chart
  series at 30 points, save payload is a single small JSON blob. The
  120-turn bot runs show no growth pathology on JVM.

## Baseline Profile / Macrobenchmark

Module `:benchmark` holds a cold-startup `MacrobenchmarkRule` test.
It needs a real device or emulator and never runs on CI. To produce a
Baseline Profile later: add a `BaselineProfileRule` test to `:benchmark`,
run its `collectBaselineProfile` task on a device, and copy the output to
`app/src/main/baselineProfiles/baseline-prof.txt` (picked up automatically
on the next release build thanks to `profileinstaller`).

`androidx.profileinstaller` is already a dependency so the Play-installed
build applies the profile automatically.

## Numbers

| Metric | Before | After | How measured |
|---|---|---|---|
| Cold start | — | — | `:benchmark` on device (pending hardware) |
| Command screen open | — | — | `:benchmark` on device (pending hardware) |
| End turn (engine, JVM) | ~3ms | ~3ms | bot playthrough wall time / turns |
| 120-turn bot memory | flat | flat | history caps verified in tests |
| APK size | 27MB (plan 4) | see CI artifact | `check_assets.py` budget ≤150MB |

No device or emulator is available in this environment, so device numbers
are honest placeholders until hardware testing (post-handoff step 1).

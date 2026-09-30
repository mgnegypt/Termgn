# MGN asset pipeline

All final art/sound is delivered by the owner — never invented. Code loads
every asset **by key**; a missing file renders a named placeholder (and a
`logcat` warning in debug builds only). The game must run silent and complete
with zero asset files present.

## Where everything goes

| Type | Location | Format | Naming |
|---|---|---|---|
| Lottie animations | `app/src/main/assets/animations/<key>.json` | Lottie JSON | key = usage, e.g. `splash_logo` |
| Music tracks | `app/src/main/assets/audio/music/<key>.ogg` | OGG Vorbis | `menu`, `calm`, `tension`, `war`, `victory`, `collapse` |
| Sound effects | `app/src/main/assets/audio/sfx/<key>.ogg` | OGG Vorbis | `click`, `panel`, `achievement`, `reward`, `danger`, `build`, `turn_end`, `error` |
| City backgrounds | `app/src/main/assets/images/city_<variant>.webp` | WebP | `city_day`, `city_sunset`, `city_night` |
| Event thumbnails | `app/src/main/assets/images/events/<imageKey>.webp` | WebP | matches `imageKey` in `events_*.json` |
| Landmark art | `app/src/main/assets/images/landmarks/<id>.webp` | WebP | matches landmark `id` |
| Ruler/flag art | composed in code (shape + symbol + color) | — | presets in `SetupViewModel.FLAG_PRESETS` |
| Icons | vector `ImageVector` in code (see below) | Kotlin | `design/.../icons` |

## Animation keys (`GameAnimation(key)`)

`splash_logo` · `achievement_unlock` · `turn_report` · `turn_transition` ·
`ending_victory` · `ending_collapse` · `ending_continuation` · `city_smoke`

## Image budgets

- WebP only, max width 1280px for backgrounds, 256px for thumbnails/icons.
- Images load lazily (`AsyncImage`-style on-demand decode where used) and are
  released when leaving the screen (no static bitmap caches).
- **APK budget ≤ 150MB** — enforced in CI (`check_apk_size.py`).

## Icons: SVG → ImageVector

Icons are checked in as SVG under `assets/svg/`, then converted once with the
official importer (Android Studio: *File → New → Vector Asset → Local file*,
or the command line below) into `ImageVector` Kotlin files. No SVG ships in
the APK.

```bash
# Example: convert one SVG with the androidx vector tooling
# (Android Studio Vector Asset Studio is the supported path;
#  svgs are kept in assets/svg/ as the single source of truth.)
ls assets/svg/
```

## Flags

Flags are never images: `FlagPreview` composes field + stripe + geometric
symbol from the preset colors. New symbols are code (Canvas paths).

## Licenses

Every file under `app/src/main/assets/` and every font must have a row in
`assets/LICENSES.md`. CI (`check_assets.py`) fails the build otherwise.

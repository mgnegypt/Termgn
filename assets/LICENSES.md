# Asset licenses

Every shipped asset file must have exactly one row here. CI fails otherwise.
"Commercial use" must be an explicit yes — anything else blocks release.

| Asset path | Source | Author / tool | License | Commercial use |
|---|---|---|---|---|
| `design/src/main/res/font/cairo.ttf` | google/fonts (github) | Cairo project (Guevara/Provenza) | SIL OFL 1.1 | yes |
| `design/src/main/res/font/reem_kufi.ttf` | google/fonts (github) | Reem Kufi project (Khaled Hosny) | SIL OFL 1.1 | yes |
| `design/src/main/res/font/tajawal_regular.ttf` | google/fonts (github) | Tajawal project (Boutros/Hamdi) | SIL OFL 1.1 | yes |
| `design/src/main/res/font/tajawal_bold.ttf` | google/fonts (github) | Tajawal project (Boutros/Hamdi) | SIL OFL 1.1 | yes |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` | generated placeholder | MGN STUDIO (temp) | proprietary-temp | n/a (replace before release) |
| `app/src/main/res/drawable/ic_launcher_foreground.xml` | generated placeholder | MGN STUDIO (temp) | proprietary-temp | n/a (replace before release) |
| `app/src/main/res/drawable/ic_launcher_monochrome.xml` | generated placeholder | MGN STUDIO (temp) | proprietary-temp | n/a (replace before release) |
| *(no game art/sound shipped yet — all runtime loads fall back to placeholders)* | — | — | — | — |

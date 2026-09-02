# CountUp — Technical Handoff

Purpose of this doc: let another engineer (or agent) pick up the project and understand what it is, how it's built, what decisions were made, where the tricky bits are, and what's still open — without re-reading everything.

---

## 1. What this is

**CountUp** is a small, fully offline Android app + home-screen widgets that track calendar days since user-defined anchor dates ("days since last haircut", a habit, an anniversary, etc.) or countdown days until upcoming events.

- Any number of items; each has a **name**, a custom or randomly-assigned **icon** (Material + Phosphor sets), an **anchor date**, optional **2-line notes**, and **custom surface styling**.
- App: zen-paper styled list (warm off-white, serif/sans/mono typography, 1px borders), add/edit/delete/reset, instant search & 1-tap sorting, 30 rotatable Chinese ink wash landscape themes.
- Widgets:
  - **Count-ups Grid Widget:** A 3-column / 2-column grid of item cells on dynamic ink wash backgrounds; **double-tap resets an item to today**.
  - **Hero Milestone Widget:** A dedicated single-milestone widget with **2x1 Poetic Card** layout, configuration picker on placement, milestone gold accent indicator, and safe two-tap in-place reset.

Threat/scope model is deliberately minimal: **no accounts, no network, no analytics, no database, no background scheduler, zero permissions, backup disabled.**

---

## 2. Tech stack (pinned — do NOT upgrade casually)

Versions live in `gradle/libs.versions.toml`.

| Component | Version | Note |
|---|---|---|
| Android Gradle Plugin | `9.4.0` | Uses **built-in Kotlin** (no `org.jetbrains.kotlin.android` plugin) |
| Gradle | `9.7.1` | Wrapper checked in |
| JDK | `17` | Temurin; `JAVA_HOME` set |
| Kotlin | `2.3.21` | Provided via `buildscript` classpath in root `build.gradle.kts` (above AGP's bundled KGP) |
| Compose BOM | `2026.06.00` | |
| RemoteViews (platform) | — | Home-screen widgets; pure platform RemoteViews for instant battery-efficient updates |
| compileSdk / targetSdk / minSdk | 37 / 37 / 26 | |

**Key build gotcha:** AGP 9 has built-in Kotlin — do not re-add `kotlin-android`. The `kotlin { compilerOptions {} }` block was also removed; `compileOptions` JDK 17 covers the JVM target. Release builds have R8 minification (`isMinifyEnabled = true`) and resource shrinking enabled.

Local SDK at `D:\Android\Sdk` (see `local.properties`).

---

## 3. Architecture — files & responsibilities

Production Kotlin is flat under `app/src/main/java/com/countup/app/`:

| File | Responsibility |
|---|---|
| `MainActivity.kt` | Lightweight Compose Activity host: collects `CountUpViewModel` state with lifecycle, binds UI effects to snackbars & widget updates |
| `CountUpViewModel.kt` | MVI ViewModel: manages atomic `_state.update` transitions, instant search/sort pipeline, dialog state, and effect emissions |
| `CountUpContract.kt` | Unidirectional MVI contract: `@Immutable CountUpUiState`, `CountUpUiEvent`, and `CountUpUiEffect` |
| `CountUpContent.kt` | Stateless root composable: full-bleed edge-to-edge ink background, odometer digit roll animations, search popover, dialogs |
| `ZenTheme.kt` | Mid-Century Modern Zen Paper token system via `CompositionLocalProvider(LocalZenColors)`, spring physics `pressScale`, a11y standards |
| `CountUpRepository.kt` | Clean repository abstraction with `DefaultCountUpRepository` backed by zero-data-loss `CountUpStore` |
| `AbstractBackgrounds.kt` | 30 rotatable Chinese ink wash abstract background themes + auto-daily rotation |
| `CountUpItem.kt` | `@Immutable` data class (`id`, `name`, `epochDay`, `comment`, `icon`, `futureFlag`, `showInWidget`, `cardColor`) + JSON `encodeItems`/`decodeItems` + balanced-brace stream `salvageItems` |
| `CountUpStore.kt` | Zero-data-loss persistence: 5-tier fail-safe hierarchy (Primary Prefs -> JSON Salvage -> Atomic Disk Backup `countup_backup.json` -> Legacy Migration -> Timestamped Quarantine) + Hero widget preference bindings |
| `DaysSince.kt` | Pure `daysSince(last, today)` using `ChronoUnit.DAYS` (supports negative future counts) |
| `DateConversion.kt` | UTC-safe picker millis → `LocalDate`; localized date formatter; dynamic "SINCE" / "UNTIL" sub-labeling |
| `ItemIcons.kt` | Curated Material + Phosphor icon registry with Category metadata |
| `WidgetBackgroundRenderer.kt` | Native procedural Canvas/Path vector renderer for Chinese ink wash themes on warm paper (< 180 KB memory) |
| `CountUpWidget.kt` | Ultra-minimalist Zen RemoteViews multi-item grid widget: dynamic ink background, full-width grid, `ResetCountReceiver` (in-place double-tap reset confirmation), `pushWidgetUpdate()` imperative refresh |
| `HeroWidgetReceiver.kt` | Dedicated single-item Hero Milestone widget receiver (2x1 Poetic Card), safe two-tap direct in-place reset (`ResetHeroCountReceiver`) |
| `HeroWidgetConfigureActivity.kt` | Interactive launcher widget configuration activity to select and pin a counter to a Hero widget instance |
| (debug) `WidgetHostActivity.kt` | Debug-only activity to render the widget for screenshots (not in release) |

Tests:
- `app/src/test/...` (JVM): `CountUpViewModelTest` (Turbine), `HeroWidgetTest` (2x1 layout & reset verification), `CountUpStressAndBoundaryTest` (1k items, unicode, leap years), `CountUpRepositoryTest`, `CountUpItemTest`, `CountUpStoreTest`, `EdgeCaseMatrixTest`, `AbstractBackgroundTest`, `DateConversionTest`, `DaysSinceTest`, `ItemIconsTest`, `SortOrderTest`, `WidgetRowTest`, `WidgetBackgroundTest`, `WidgetDimensionTest`, `CountUpContractAndFlowTest` → **160 JVM unit tests** (100% green)
- `app/src/androidTest/...` (device): `ComposeUiSmokeTest` (stateless UI & a11y semantics), `CountUpStoreInstrumentedTest` (CRUD, migration, recovery)

Resources: `res/values/strings.xml`, `themes.xml`, `colors.xml`; `res/drawable/ic_*.xml` (Material & Phosphor icons + `hero_milestone_dot`); `res/xml/haircut_widget_info.xml`, `res/xml/hero_widget_info.xml`, `data_extraction_rules.xml`, `backup_rules.xml`.

---

## 4. Data model & storage

One value per item, stored as a JSON array string under key `items_v1` in private prefs file `countup_prefs` with dual-write atomic backup snapshot `countup_backup.json`:

```json
[{"id":"uuid","name":"Meditation","epochDay":20667,"comment":"Daily morning calm","icon":"person","futureFlag":false,"showInWidget":true,"cardColor":""}]
```

- `epochDay` = `LocalDate.toEpochDay()`.
- **Zero Data Loss Guarantee:** If SharedPreferences is wiped or corrupted, `CountUpStore` automatically self-heals from `countup_backup.json`. If a payload is truncated mid-write by OS power cutoff, `salvageItems` extracts all intact items and preserves the raw broken payload in a timestamped quarantine key.
- **Hero Widget Binding:** Key `hero_widget_item_<appWidgetId>` stores the bound item UUID for each Hero Milestone widget instance.
- **Writes use `commit()` and `fd.sync()`** synchronously before updating the widget (write-before-update ordering).

---

## 5. Design & behavior decisions

- **Zen-paper theme & Chinese Ink Wash Backgrounds:** Warm paper background `#F7F6F3`, cards white with `1px #EAEAEA` border, radius 12, ink `#2F3437`, muted `#787774`; serif for headings/counts, sans for labels. 30 authentic Chinese ink wash landscape themes anchored to borders with negative space.
- **Dynamic Anchor Sub-labels:** Count $\ge 0$ renders `SINCE <date>`; count $< 0$ (future event) renders `UNTIL <date>`.
- **Scale-on-press & Motion:** `0.96` buttons / `0.99` cards via `pressScale()`; list add/remove uses `Modifier.animateItem()`, disabled under system reduce-motion.
- **Hero Milestone Widget (2x1 Poetic Card):**
  - **2x1 Poetic Card:** Wide horizontal card format (`minWidth="110dp"`, `minResizeWidth="110dp"`, `targetCellWidth="2"`, `targetCellHeight="1"`) with prominent count, icon badge, milestone dot, and anchor date sub-label.
  - **Safe Two-Tap Reset:** Direct in-place reset armed with 1.5-second timeout and hardware haptics without opening the app.

---

## 6. Build / test / run

```bash
export JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot"
./gradlew clean
./gradlew assembleDebug            # debug APK
./gradlew assembleRelease          # signed release APK + AAB bundle
./gradlew test                     # 139 JVM unit tests (100% green)
./gradlew connectedDebugAndroidTest # device tests (emulator/device online)
./gradlew lintDebug                # 0 errors
```

Install + launch:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.countup.app/.MainActivity
```

---

## 7. Security posture

- **Zero `<uses-permission>`** in merged debug & release manifests.
- `allowBackup="false"` + `dataExtractionRules`/`backup_rules` (backup disabled on all API levels).
- No network, no WebView, no `Log.*`, no secrets in source.
- `MainActivity` and `HeroWidgetConfigureActivity` are `exported=true` (launcher/config); widget receivers are `exported=false`.
- R8 minification and resource shrinking enabled for production release.

---

## 8. Open / known items

1. **Widget screenshot (debug builds only):** Renderable via `WidgetHostActivity.kt` for visual auditing.
2. **Interactive Design Lab:** `artifacts/prototype_hero_1x1_exploration.html` provides a standalone interactive testbed for testing 1x1 widget typography and layouts across multiple screen densities.
3. **Icons:** Google Material Icons (Apache 2.0) and Phosphor Icons (MIT).

---

## 9. Icon Registry & Addition Architecture

Icons are defined using a single-source-of-truth `@Immutable IconEntry` model in [`ItemIcons.kt`](file:///d:/Github/countUp/app/src/main/java/com/countup/app/ItemIcons.kt):
1. Drop the 24dp tintable vector drawable into `app/src/main/res/drawable/ic_<name>.xml`.
2. Add its descriptive TalkBack accessibility string `cd_icon_<name>` to both `app/src/main/res/values/strings.xml` and `app/src/main/res/values-zh/strings.xml`.
4. (Optional) Add deterministic bilingual keyword auto-styling rules to `KEYWORD_STYLE_RULES`.




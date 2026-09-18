# CountUp — Technical Handoff

Purpose of this doc: let another engineer (or agent) pick up the project and understand what it is, how it's built, what decisions were made, where the tricky bits are, and what's still open — without re-reading everything.

---

## 1. What this is

**CountUp** is an intentionally small, fully offline Android app and home-screen widget suite that tracks calendar days since user-defined anchor dates ("days since last haircut", a habit streak, sobriety, an anniversary) or countdown days until upcoming events.

- Any number of items; each has a **name**, a custom or randomly-assigned **icon** (100+ curated vector icons from Material + Phosphor sets), an **anchor date**, optional **2-line notes**, and **custom surface styling**.
- App: zen-paper styled list (warm off-white, serif/sans/mono typography, 1px borders), add/edit/delete/reset, instant search & 1-tap bidirectional sorting (tap to toggle Asc ⇄ Desc), 1-tap theme cycling (⚡ System / ☀️ Light / 🌙 Dark), calibrated dark mode palettes (Washi, Earth, Sumi) with luminous border brushes, dedicated Data & Backup settings dialog, in-card undo whispers, 30 rotatable Chinese ink wash landscape themes.
- Widgets:
  - **Count-ups Grid Widget:** A 3-column / 2-column grid of item cells on dynamic ink wash backgrounds; double-tap resets an item to today; widget toolbar includes 1-tap Voice Quick Add.
  - **Voice Quick Add:** Microphone trigger button on widget toolbar delegating speech recognition out-of-process (`RecognizerIntent.ACTION_RECOGNIZE_SPEECH`) with zero permissions, instant auto-anchor to today, and transient floating Undo/Edit card.
  - **Hero Milestone Widget:** Dedicated single-milestone widget in **2x1 Poetic Card** layout, configuration picker on placement, milestone gold accent indicator, and safe two-tap in-place reset.
  - **Zen Horizon Ribbon:** 4x1 & 2x1 minimalist horizon ribbon with direct on-widget unit cycling (days, weeks, months, years) on tap.
  - **Solar Rhythm Widget:** 4x2 & 2x2 seasonal canvas aligning tracked milestones with the 24 traditional Chinese Solar Terms (24 节气).
  - **Zen Pebble Widget:** 1x1 ultra-compact tactile tile hardened for all physical OEM launchers (Samsung One UI, Xiaomi HyperOS, Vivo OriginOS, OPPO ColorOS) with `resizeMode="none"` and bounded 16dp corners.

Threat/scope model is deliberately minimal: **no accounts, no network, no analytics, no database, no persistent background services, zero dangerous runtime permissions. Automated encrypted OS backup + self-sovereign offline SAF file portability.**

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

**Key build gotcha:** AGP 9 uses built-in Kotlin — do not re-add `kotlin-android`. Release builds have R8 minification (`isMinifyEnabled = true`) and resource shrinking enabled. Release signing in `app/build.gradle.kts` resolves release keystore passwords via a 3-tier fallback: `COUNTUP_KEYSTORE_PASS` env var -> `local.properties` (`countup.keystore.pass`) -> `../keystore/keystore-pass.txt` fallback file, gracefully falling back to debug signing only when release keystores are absent in CI.

SDK configured via `local.properties` (`sdk.dir`) or standard `ANDROID_HOME` / `ANDROID_SDK_ROOT` environment variables.

---

## 3. Architecture — files & responsibilities

Production Kotlin is organized under `app/src/main/java/com/countup/app/`:

| File | Responsibility |
|---|---|
| `MainActivity.kt` | Lightweight Compose Activity host: collects `CountUpViewModel` state with lifecycle, binds UI effects to snackbars & widget updates, manages SAF launcher intents |
| `VoiceAddActivity.kt` | Lightweight translucent activity for widget voice quick-add; delegates speech out-of-process (`RecognizerIntent.ACTION_RECOGNIZE_SPEECH`) with zero permissions and transient floating Zen confirmation pill |
| `VoiceTextSanitizer.kt` | Utility for voice recognition transcript cleaning, punctuation normalization, and whitespace sanitization |
| `CountUpViewModel.kt` | MVI ViewModel: manages atomic `_state.update` transitions, instant search/sort pipeline, dialog state, delegates file I/O to `BackupCoordinator`, and manages `ResetWhisperTracker` |
| `CountUpContract.kt` | Unidirectional MVI contract: `@Immutable CountUpUiState`, `CountUpUiEvent`, and `CountUpUiEffect` |
| `CountUpContent.kt` | Stateless root composable: full-bleed edge-to-edge ink background, decoupled subheader (sort pill, 1-tap theme cycle, settings gear), odometer digit roll animations, search popover, item cards with in-situ reset whispers |
| `CountUpDialogs.kt` | Focused modal dialog composables: `DataBackupSettingsDialog` (offline export/restore, version footer) and `RestorePreviewDialog` (merge vs. replace strategies) |
| `BackupCoordinator.kt` | Domain coordinator for Storage Access Framework (SAF) JSON export, pre-validation inspection, and restore workflows |
| `ResetWhisperTracker.kt` | Session-scoped tracker for accidental counter resets; handles in-situ card undo whispers with 10-second auto-dismiss and manual dismissal |
| `SortOrder.kt` | Bidirectional sort order enum (`SortCriteria` × `SortDirection`: Days, Date, Name; Asc ⇄ Desc) with `toggle()` support |
| `ZenTheme.kt` | Mid-Century Modern Zen Paper token system via `CompositionLocalProvider(LocalZenColors)`, spring physics `pressScale`, a11y standards |
| `ItemColors.kt` | MCM color palettes, dark mode luminous border brushes, calibrated contrast ratios, and theme token mappings across Washi, Earth, and Sumi suites |
| `ItemIcons.kt` | Centralized registry of 100+ tactile vector icons, bilingual TalkBack strings, and deterministic keyword auto-styling rules |
| `AbstractBackgrounds.kt` | Generative Chinese ink wash landscape rendering for light and dark modes (silver moonlight tones) |
| `CountUpRepository.kt` | Clean repository abstraction with `DefaultCountUpRepository` backed by zero-data-loss `CountUpStore` |
| `BackupRepository.kt` | Storage Access Framework (SAF) JSON backup export and interactive restore pipeline with pre-validation, preview extraction, and UUID-authoritative merge/replace strategies |
| `CountUpStore.kt` | Zero-data-loss persistence: 5-tier fail-safe hierarchy (Primary Prefs -> JSON Salvage -> Atomic Disk Backup `countup_backup.json` -> Legacy Migration -> Timestamped Quarantine) + widget preference bindings with orphaned ID cleanup |
| `CountUpItem.kt` | `@Immutable` data class (`id`, `name`, `epochDay`, `comment`, `icon`, `futureFlag`, `showInWidget`, `cardColor`) + JSON `encodeItems`/`decodeItems` + balanced-brace stream `salvageItems` |
| `MidnightAlarmReceiver.kt` | Battery-friendly AlarmManager RTC alarm receiver (`setAndAllowWhileIdle` with `RTC_WAKEUP`) advancing day counts without persistent background services, re-registered on widget updates post-reboot |
| `WidgetNavigationContract.kt` | Partitioned pending intent request codes and navigation routing for widget cell taps, resets, voice add, and unit cycling |
| `ZenWidgetReducer.kt` | Centralized state reduction and palette resolution for all widget families |
| `WidgetThemeTokens.kt` | Visual styling tokens, contrast definitions, and dimensions for widget canvases |
| `CountUpWidget.kt` | Zen RemoteViews multi-item grid widget (Count-ups): dynamic ink background, full-width grid, voice add trigger, `ResetCountReceiver` (in-place double-tap reset confirmation) |
| `HeroWidgetReceiver.kt` | Dedicated single-item Hero Milestone widget receiver (2x1 Poetic Card), safe two-tap direct in-place reset (`ResetHeroCountReceiver`) |
| `HeroWidgetConfigureActivity.kt` | Interactive launcher widget configuration activity with package verification to select and pin a counter to a Hero widget instance |
| `ZenHorizonWidgetReceiver.kt` | Minimalist Zen Horizon ribbon receiver (4x1 & 2x1) with in-place unit cycling |
| `ZenHorizonConfigureActivity.kt` | Configuration activity with package verification for Zen Horizon widget instances |
| `SolarRhythmWidgetReceiver.kt` | Seasonal canvas widget receiver integrating with the 24 traditional Chinese Solar Terms |
| `SolarRhythmConfigureActivity.kt` | Configuration activity with package verification for Solar Rhythm widget instances |
| `ZenPebbleWidgetReceiver.kt` | Ultra-compact 1x1 pebble tile receiver engineered for OEM launcher resilience |
| `ZenPebbleConfigureActivity.kt` | Configuration activity with package verification for Zen Pebble widget instances |
| (debug) `WidgetHostActivity.kt` | Debug-only activity to render widgets on-device for automated screenshot capture (excluded in release) |

Tests:
- `app/src/test/...` (JVM): **402 JVM unit tests** (100% green) covering `CountUpViewModelTest` (Turbine), `BackupCoordinatorTest`, `ResetWhisperTrackerTest`, `SortOrderTest`, `CountUpRepositoryTest`, `BackupRepositoryTest`, `HeroWidgetTest`, `ZenPebbleTest`, `SolarRhythmConfigurationTest`, `WidgetContractInvariantsTest`, `WidgetMemoryBudgetGateTest`, `CountUpStressAndBoundaryTest`, `DateConversionTest`, `DaysSinceTest`, `MidnightAlarmReceiverTest`, `CountUpContractAndFlowTest`, `ItemIconsTest`, `DatePickerContractTest`, `WidgetRefreshDebounceTest`, `ItemColorsTest`, and `EdgeCaseMatrixTest`.
- `app/src/androidTest/...` (device): `ComposeUiSmokeTest` (stateless UI & a11y semantics), `CountUpStoreInstrumentedTest` (CRUD, migration, recovery).
- `app/src/androidTest/...` (device): `ComposeUiSmokeTest` (stateless UI & a11y semantics), `CountUpStoreInstrumentedTest` (CRUD, migration, recovery).

---

## 4. Data model, storage & backup

One value per item, stored as a JSON array string under key `items_v1` in private prefs file `countup_prefs` with dual-write atomic backup snapshot `countup_backup.json`:

```json
[{"id":"uuid","name":"Meditation","epochDay":20667,"comment":"Daily morning calm","icon":"person","futureFlag":false,"showInWidget":true,"cardColor":""}]
```

- `epochDay` = `LocalDate.toEpochDay()`.
- **Zero Data Loss Guarantee:** If SharedPreferences is wiped or corrupted, `CountUpStore` automatically self-heals from `countup_backup.json`. If a payload is truncated mid-write by OS power cutoff, `salvageItems` extracts all intact items and preserves the raw broken payload in a timestamped quarantine key.
- **Widget Instance Bindings:** Per-instance bindings (`hero_widget_item_<id>`, `zen_horizon_widget_item_<id>`, `solar_rhythm_widget_item_<id>`, `zen_pebble_widget_item_<id>`) store bound item UUIDs. Deleting an item automatically cleans up all associated bindings across all widget families via `CountUpStore.purgeWidgetBindingsForItem(itemId)`. Stale IDs are cleaned up in `onDeleted` and sanitized on new-device restore.
- **Writes use `commit()` and `fd.sync()`** synchronously before updating widgets (write-before-update ordering).
- **Two-Tier Backup Architecture:**
  1. **Tier 1 (Automated OS Sync):** `android:allowBackup="true"` with `backup_rules.xml` and `data_extraction_rules.xml` synchronizing `countup_prefs.xml` and `countup_backup.json` to encrypted cloud storage (GMS) or D2D transfer tools (Mi Mover, Phone Clone).
  2. **Tier 2 (Offline SAF Export/Import):** User-triggered unencrypted JSON archive using Android's system document picker without requesting runtime storage permissions. Pre-validation preview screen offers Merge (UUID deduplicated, preserving distinct items with identical names) or Clean Replace strategies.

---

## 5. Design & behavior decisions

- **Zen-paper theme & Chinese Ink Wash Backgrounds:** Warm paper background `#F7F6F3`, cards white with `1px #EAEAEA` border, radius 12, ink `#2F3437`, muted `#787774`; serif for headings/counts, sans for labels. 30 authentic Chinese ink wash landscape themes anchored to borders with negative space.
- **Calibrated Dark Mode & Luminous Borders:** Overhauled dark mode palettes across Washi, Earth, and Sumi suites with luminous border brushes and calibrated contrast. Landscape washes rendered with silver moonlight tones. Preserves 16dp outer padding on `ItemCard` without inner clipping to prevent bottom-left text cutoff.
- **Dynamic Anchor Sub-labels:** Count $\ge 0$ renders `SINCE <date>`; count $< 0$ (future event) renders `UNTIL <date>`.
- **Decoupled Control Surface & Layout Density:**
  - The subheader separates list filtering from theme configuration and system backup.
  - **Sort Pill:** Displays active criterion and direction (`Days ↓`, `Date ↑`, `Name ↓`), toggles direction on repeated tap, and opens the search & sort popover.
  - **1-Tap Theme Cycle:** Adjacent 26dp action button cycling System (⚡) -> Light (☀️) -> Dark (🌙) with immediate tactile haptic feedback.
  - **Dedicated Settings Gear:** 26dp button (`ic_settings`) opening `DataBackupSettingsDialog` for offline export/restore and app version inspection.
  - **Button Sizing & Spacing Guardrail:** Subheader buttons maintain an exact 26dp circular bounding box with 1–2dp visual gap, matching `ItemCard` action rows. Avoid `.minimumInteractiveComponentSize()` on these subheader actions as it expands layout bounds to 48dp and blows out horizontal spacing.
- **Voice Quick Add (Microphone Action):** 1-tap microphone icon in the widget toolbar launches dedicated translucent `VoiceAddActivity`. Speech recognition is delegated out-of-process via `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` (`RecognitionService`), requiring zero audio permissions in CountUp. Spoken transcripts are sanitized, anchored to today, stored idempotently in `CountUpStore`, and presented on an edge-to-edge floating Zen confirmation card with Undo and Edit affordances while refreshing all widgets in real time.
- **In-Situ Reset Whispers:** Counter resets display an in-card recovery whisper (`ResetWhisperTracker`) with a 10-second window and one-tap undo, avoiding intrusive screen-wide banners.
- **Scale-on-press & Motion:** `0.96` buttons / `0.99` cards via `pressScale()`; list add/remove uses `Modifier.animateItem()`, disabled under system reduce-motion. Mechanical reset button features a subtle tooltip tap-hint.
- **Complete Zen Widget Suite:**
  - **Count-ups Multi-Grid:** 3x2 and 2x2 grid with double-tap direct reset and voice quick-add toolbar button.
  - **Hero Milestone (2x1):** Poetic card format with prominent count, icon badge, milestone gold dot, anchor date, and two-tap direct reset.
  - **Zen Horizon Ribbon (4x1 & 2x1):** Horizontal ribbon with on-widget unit cycling (days -> weeks -> months -> years).
  - **Solar Rhythm (4x2 & 2x2):** Seasonal art matching the current Chinese solar term with milestone countdown/countup, calmed with 3 breathing cycles settling to equilibrium.
  - **Zen Pebble (1x1):** Ultra-compact pebble tile with `resizeMode="none"`, `reconfigurable`, and corner radius $\le 16$dp to ensure universal OEM launcher drop targeting and instant card picker on placement.

---

## 6. Build / test / run

```bash
export JAVA_HOME="/path/to/jdk-17"   # or set via Android Studio / system PATH
./gradlew clean
./gradlew assembleDebug             # debug APK
./gradlew assembleRelease           # signed release APK + AAB bundle (R8 minified)
./gradlew test                      # 402 JVM unit tests (100% green)
./gradlew connectedDebugAndroidTest # device tests (emulator/device online)
./gradlew lintDebug                 # 0 errors
```

Install + launch:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.countup.app/.MainActivity
```

---

## 7. Security posture

- **Zero `<uses-permission>` declared**: The app requests zero Android permissions across both debug and release builds. `WAKE_LOCK`, `ACCESS_NETWORK_STATE`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`, and `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` are explicitly stripped via manifest removal rules. Tactile haptics operate through standard system view haptic channels without requiring `android.permission.VIBRATE`.
- **Zero-Permission Voice Recognition**: Voice Quick Add delegates speech recognition out-of-process to platform speech recognizers via `RecognizerIntent.ACTION_RECOGNIZE_SPEECH`, declaring package visibility via `<queries>`. Zero microphone or audio recording permissions are requested.
- **Safe Intent Handling & Calling Package Verification**: Widget configuration activities (`HeroWidgetConfigureActivity`, `ZenHorizonConfigureActivity`, `SolarRhythmConfigureActivity`, `ZenPebbleConfigureActivity`) verify calling package ownership and validate intent extras to prevent Intent Redirection.
- **Encrypted OS Backup:** Native backup governed by `dataExtractionRules` and `backup_rules` targeting only local preferences and JSON snapshots.
- **Scoped Storage:** Manual file backup uses system Storage Access Framework (`ACTION_CREATE_DOCUMENT`, `ACTION_OPEN_DOCUMENT`) with zero storage permission requests.
- No network, no WebView, no analytics, no `Log.*`, no secrets in source.
- `MainActivity` and widget configure activities are `exported=true` with strict intent filters; `VoiceAddActivity` is `exported=false` with translucent theme; all widget broadcast receivers are `exported=false`.
- R8 minification and resource shrinking enabled for production release builds with explicit `-keep` rules for domain data models and reflection targets.

---

## 8. Open / known items

1. **Widget screenshot (debug builds only):** Renderable via `WidgetHostActivity.kt` for visual auditing.
2. **Interactive Design Lab:** `prototype_zen_widgets.html` and `artifacts/prototype_hero_1x1_exploration.html` provide standalone interactive testbeds for exploring widget typography and layouts across multiple screen densities.
3. **Icons:** 100+ curated vector icons from Google Material Icons (Apache 2.0) and Phosphor Icons (MIT).
4. **CI Automation:** [`.github/workflows/ci.yml`](.github/workflows/ci.yml) validates `lintDebug`, `test`, and `assembleRelease` on Ubuntu runners on every push and PR to `main`.
5. **Keystore Management & Rotation:** [`docs/CREDENTIAL_ROTATION.md`](docs/CREDENTIAL_ROTATION.md) outlines production release key rotation and history scrubbing commands via `git-filter-repo`.
6. **Zero-Permission Reboot Rollover:** `MidnightAlarmReceiver` uses battery-friendly RTC alarms (`setAndAllowWhileIdle` with `RTC_WAKEUP`), re-registering on reboot whenever widget providers are updated or enabled.
7. **Decoupled Header Specification:** Full requirements, visual hierarchy, and before/after comparisons documented in [`docs/spec-decoupled-header-controls.md`](docs/spec-decoupled-header-controls.md).

---

## 9. Icon Registry & Addition Architecture

Icons are defined using a single-source-of-truth `@Immutable IconEntry` model in [`ItemIcons.kt`](file:///d:/Github/countUp/app/src/main/java/com/countup/app/ItemIcons.kt):
1. Drop the 24dp tintable vector drawable into `app/src/main/res/drawable/ic_<name>.xml`.
2. Add its descriptive TalkBack accessibility string `cd_icon_<name>` to both `app/src/main/res/values/strings.xml` and `app/src/main/res/values-zh/strings.xml`.
3. (Optional) Add deterministic bilingual keyword auto-styling rules to `KEYWORD_STYLE_RULES`.


# CountUp

[![CI](https://github.com/IvanYang007/CountUp-Android/actions/workflows/ci.yml/badge.svg)](https://github.com/IvanYang007/CountUp-Android/actions/workflows/ci.yml)

## 1. What the app does

An intentionally small, fully offline Android app and home-screen widget suite that tracks
the **days since a set of anchor dates** (e.g. last haircut, a habit streak, sobriety, an anniversary) or **days until upcoming events**.

- **Calm, Mindful Aesthetic & Dark Mode Overhaul:** Mid-century modern tactile styling with warm paper background, rich card drop shadows, calibrated dark mode palettes (Washi, Earth, Sumi) with luminous border brushes, and 30 rotating classical Chinese ink wash landscape themes.
- **100+ Curated Tactile Icons:** Rich built-in vector icon catalog (Material + Phosphor sets) covering habits, fitness, health, sobriety, finance, milestones, and mindfulness with bilingual TalkBack accessibility descriptions and deterministic keyword auto-styling.
- **Decoupled Subheader & Bidirectional Sorting:** Instant search, 1-tap bidirectional sorting (tap again to toggle Asc ⇄ Desc by Days, Date, or Name), direct 1-tap Theme Mode cycling (⚡ System / ☀️ Light / 🌙 Dark), and dedicated Settings access.
- **Dedicated Data & Backup Settings:** Standalone modal dialog (`ic_settings`) for offline SAF JSON export, pre-validation preview, and Merge/Replace restore strategies.
- **In-Card Undo Whispers:** Unobtrusive in-situ recovery alerts directly on item cards when counters are reset accidentally, replacing disruptive top-screen banners.
- **Habit Notes & Countdowns:** 2-line custom notes and automatic "UNTIL" sub-labeling for future target dates.
- **Full Zen Widget Suite (5 Home-Screen Widgets + Voice Quick Add):**
  - **Count-ups (Multi-Item Grid):** Full-width 3-column / 2-column grid widget displaying active milestones on dynamic ink wash and paper substrate backgrounds, featuring in-situ style suite cycling (All ➔ Washi ➔ Earth ➔ Sumi ➔ All) directly on the home screen, suite signature dots, 7-color MCM palettes, and widget toolbar.
  - **Voice Quick Add:** 1-tap microphone button on the widget toolbar triggering zero-permission out-of-process speech delegation (`RecognizerIntent.ACTION_RECOGNIZE_SPEECH`) with transient undo/edit confirmation pill.
  - **Hero Milestone Widget (2x1 Poetic Card):** Dedicated single-milestone widget with counter picker, ambient milestone gold accents, and safe two-tap direct in-place reset.
  - **Zen Horizon Ribbon (4x1 & 2x1):** Minimalist horizon ribbon featuring on-widget unit cycling (days, weeks, months, years) directly on tap.
  - **Solar Rhythm (4x2 & 2x2 Seasonal Canvas):** Harmonious seasonal tracker aligning your milestone with the 24 traditional Chinese Solar Terms (24 节气).
  - **Zen Pebble (1x1 Tactile Tile):** Ultra-compact pebble tile engineered for zero-compromise drop targeting across OEM launchers (Samsung One UI, Xiaomi HyperOS, Vivo OriginOS, OPPO ColorOS).
- **Dual-Tier Cross-Device Migration & Offline SAF Backup:**
  - **Tier 1 (Automated OS Sync):** Platform-native Android 12+ encrypted Auto Backup and Device-to-Device (D2D) migration (Mi Mover, Phone Clone, GMS) with automatic widget ID remapping across device restorations (`onRestored`).
  - **Tier 2 (Self-Sovereign JSON Portability):** Offline export and interactive restore via Android's Storage Access Framework (SAF) featuring a pre-restore preview screen and Merge / Replace conflict strategies.
- **Silent Midnight Rollover:** Battery-friendly RTC alarm (`MidnightAlarmReceiver`) advances day counts at `00:00:01` local time without persistent background services or battery drain.
- **Adaptive Layout & 120Hz Rendering:** Responsive centered column constraints (`widthIn(max = 640.dp)`) for foldables, tablets, and large screens; zero-allocation `GraphicsLayer` caching for 120Hz fluid scrolling across all 30 Chinese ink wash landscape themes; and an ahead-of-time ART Baseline Profile pre-compiling critical startup entry points and initial Compose rendering paths.
- **Accessibility First:** Full TalkBack screen reader semantics, descriptive action labels, and tactile haptic feedback.
- **Privacy First:** 100% offline, zero runtime permissions, no accounts, no ads, no trackers, no cloud servers, and no background daemon.

## 2. Build requirements

Versions are pinned in `gradle/libs.versions.toml`.

| Component | Version |
|---|---|
| Android Gradle Plugin | `9.3.0` (built-in Kotlin) |
| Gradle | `9.5.0` |
| JDK | `17` |
| Kotlin | `2.3.21` |
| Compose BOM | `2026.06.00` |
| compileSdk / targetSdk / minSdk | `37` / `37` / `26` |

Prerequisites:

- JDK 17 (`JAVA_HOME` set, or detected via Gradle).
- Android SDK (`local.properties` → `sdk.dir=<path_to_sdk>` or `ANDROID_HOME` / `ANDROID_SDK_ROOT` environment variable).
- SDK packages: `platforms;android-37`, `build-tools;37.0.0`, `platform-tools`.

## 3. How to build and run

```bash
./gradlew clean
./gradlew assembleDebug             # debug APK
./gradlew assembleRelease           # signed release APK & bundle (R8 minified)
./gradlew test                      # 444 JVM unit tests (100% pass)
./gradlew connectedDebugAndroidTest # instrumented tests (emulator/device online)
./gradlew lintDebug                 # Android Lint (0 errors)
```

Install and launch on a connected device/emulator:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.countup.app/.MainActivity
```

Existing single-item installs are migrated automatically: the legacy
`last_haircut_epoch_day` value becomes a "Haircut" item on first launch.

## 4. How to add and use the widgets

### 1. Count-ups (Multi-Item Grid Widget)
Long-press home screen → **Widgets** → **Count-ups** → drag to a slot.
Lists enabled items in a clean multi-column grid with dynamic ink wash backgrounds.
- **In-Situ Style Suite Cycling:** Tap the widget header title or suite signature dot directly on the home screen to cycle through color suites: **All** ➔ **Washi** ➔ **Earth** ➔ **Sumi** ➔ **All**. The grid and its subtle paper substrate background tint instantly filter in place without opening configuration screens.
- **Independent Multi-Widget Isolation:** Each overview grid instance maintains its own filter state independently using unique data URIs (`countup://widget/overview/$appWidgetId`), allowing multiple widgets across home screen pages to display distinct category views simultaneously.
- **Quick-Add Intent Continuity:** Tapping the `+` button on a filtered widget opens the Add Item dialog with that style suite pre-selected.
- **In-place reset:** Tapping the number arms the direct reset confirmation ("0?"), and a second tap within 1.5 seconds zeroes that counter to today without opening the app.

### 2. Hero Milestone Widget (2x1 Poetic Card)
Long-press home screen → **Widgets** → **Hero Milestone** → drag to a slot.
- **Configuration Picker:** Opens automatically to pick which counter to feature.
- **Poetic 2x1 Layout:** Prominent count, icon badge, milestone gold dot, and anchor date sub-label.
- **Safe Two-Tap Reset:** Tap count to arm confirmation ("0?"), tap again within 1.5 seconds to zero to today. Tapping the background opens the app directly to that habit.

### 3. Zen Horizon Ribbon (4x1 & 2x1)
Long-press home screen → **Widgets** → **Zen Horizon** → drag to a slot.
- **On-Widget Unit Cycling:** Tap the unit pill on the widget to dynamically cycle display between **days**, **weeks**, **months**, and **years** in real-time.

### 4. Solar Rhythm (4x2 & 2x2 Seasonal Canvas)
Long-press home screen → **Widgets** → **Solar Rhythm** → drag to a slot.
- **Seasonal Alignment:** Integrates tracked count with the 24 traditional Chinese Solar Terms (24 节气), rendering seasonal micro-art and atmospheric landscape themes.

### 5. Zen Pebble (1x1 Tactile Tile)
Long-press home screen → **Widgets** → **Zen Pebble** → drag to a slot.
- **Universal OEM Grid Resilience:** Hardened specifically for OEM launchers (Samsung One UI, Xiaomi HyperOS, Vivo OriginOS, OPPO ColorOS) with `resizeMode="none"`, uniform auto-sizing text, and bounded 16dp corners to prevent slot-0 drop snapping.

### 6. Voice Quick Add (1-Tap Widget Microphone)
- **Zero-Permission Speech Delegation:** Tap the microphone icon button in the widget toolbar to immediately speak a milestone name.
- **Out-of-Process Speech Recognition:** Speech recognition is delegated out-of-process via Android's native `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` (`RecognitionService`), requiring zero audio permissions in CountUp.
- **Instant Transient Confirmation:** The spoken text is sanitized, anchored to today, committed idempotently, and displayed in a transient edge-to-edge floating card with instant Undo and Edit affordances while refreshing all widgets in real time.

### 7. Reset Recovery & Accidental Tap Protection
- **Two-Tap Arming Protection:** Directly on the home screen, tapping a counter numeral displays `"0?"`. A second tap within 1.5 seconds confirms the reset; otherwise it disarms safely.
- **In-Card Undo Whispers & Recovery:** If an item is reset accidentally (from a widget or inside the app), opening the CountUp app immediately presents an in-situ undo whisper directly on the reset card with a 10-second window to restore your previous anchor date and historical streak metrics with a single tap.

### 8. Tested OEM Launcher Compatibility Matrix

All 5 widgets are tested across primary Android OEM launcher implementations to ensure exact placement, no drop snapping, and reliable configuration activity invocation:

| Launcher / OEM | Typical 1x1 Cell | Grid Density | Drop Targeting | Configuration Flow |
|---|---|---|---|---|
| Google Pixel Launcher | ~80dp | 5×5 | Verified | Auto-launches configure picker |
| Samsung One UI (Galaxy) | ~68dp | 4×6 / 5×6 | Verified (`resizeMode="none"`) | Auto-launches configure picker |
| Xiaomi HyperOS / MIUI | ~60–68dp | 4×6 / 5×6 | Verified (16dp bounded corners) | Auto-launches configure picker |
| Vivo OriginOS / Funtouch | ~56–64dp | 5×6 / 5×9 | Verified (autoSizeText uniform) | Auto-launches configure picker |
| OPPO ColorOS / Realme UI | ~60–66dp | 4×6 / 5×6 | Verified | Auto-launches configure picker |
| Nova Launcher / Lawnchair | Configurable | Dynamic | Verified | Auto-launches configure picker |

**Widget screenshot (debug builds only):** Renderable on-device via debug-only host activity:

```bash
adb shell am start -n com.countup.app/.WidgetHostActivity
adb exec-out screencap -p > artifacts/widget.png
```

## 5. Cross-Device Data Migration & Offline SAF Backup

CountUp protects user streaks and history across device upgrades and factory resets through two complementary, privacy-first tiers:

- **Tier 1: Platform-Native OS Backup & Phone Cloning**
  - Configured via `backup_rules.xml` and `data_extraction_rules.xml`.
  - Backs up primary preferences and the atomic snapshot `countup_backup.json` to encrypted Google Drive backup (on GMS devices) and authorizes Device-to-Device migration tools (Mi Mover, Phone Clone, EasyShare).
  - Automatically remaps launcher widget IDs via `AppWidgetProvider.onRestored(oldIds, newIds)` on device-to-device restores without purging valid configurations.
- **Tier 2: Self-Sovereign JSON Portability via Storage Access Framework (SAF)**
  - Accessible directly in the app via the dedicated **Settings** gear icon (`ic_settings`) in the subheader under **Data & Backup**.
  - **Export:** Generates a clean, UTF-8 encoded, unencrypted `.json` backup file using Android's system document creation picker (`ACTION_CREATE_DOCUMENT`) without requesting storage permissions.
  - **Import & Preview:** Selects a `.json` backup file via `ACTION_OPEN_DOCUMENT`. Displays a pre-restore preview showing item counts, anchor dates, and notes, letting you choose between **Merge** (deduplicates by immutable UUID, preserving distinct milestones that share names) or **Clean Replace**.
  - **Resilience:** Defensive exception handling ensures no crashes on stripped custom ROMs or file managers that misreport JSON MIME types.

## 6. Midnight Rollover (Zero-Battery Inexact RTC Alarm)

Widgets automatically advance at midnight without requiring battery-draining background services or `WorkManager`:
- `MidnightAlarmReceiver` registers a battery-friendly, low-power RTC alarm (`setAndAllowWhileIdle` with `RTC_WAKEUP`) targeting `00:00:01` local time.
- Preserves the zero-permission model by avoiding `SCHEDULE_EXACT_ALARM` or `WAKE_LOCK`. Alarms respect Android Doze mode and advance widget counters reliably during Doze maintenance windows or immediately upon screen wake.
- Automatically re-registers idempotently across device reboots whenever home-screen widgets update or become enabled.
- Handles time zone changes (`ACTION_TIMEZONE_CHANGED`) and manual clock adjustments (`ACTION_TIME_SET`) to recalculate and refresh immediately.

## 7. Verification performed

- **444 JVM Unit Tests** (100% passing) across data models, repository fail-safes, MVI ViewModel, JSON salvage parsing, widget reducers, navigation contracts, backup merge/replace strategies, reset whisper lifecycles, bidirectional sorting, date picker contracts, widget IPC debouncing, and 100+ icon registry invariants.
- Clean debug and release builds with R8 minification and resource shrinking enabled (`isMinifyEnabled = true`, `isShrinkResources = true`).
- Automated release bundle signing with keystore password resolution (`COUNTUP_KEYSTORE_PASS` -> `local.properties`).
- Android Lint (`lintDebug`): **0 errors**.
- Automated GitHub Actions CI workflow running test, lint, and assemble on all pull requests.
- Strict zero-permission architecture: the app requests zero Android permissions across both debug and release builds. The manifest explicitly strips `WAKE_LOCK`, `ACCESS_NETWORK_STATE`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`, and `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. Voice recognition delegates out-of-process to system speech providers via `<queries>`, requiring zero microphone or audio permissions. Tactile haptic feedback operates via standard system view haptic channels without requiring `android.permission.VIBRATE`.
- Physical device & emulator verification on Android 8.0 (API 26) through Android 15/16 (API 36/37).

## 8. Licensing

CountUp is open source licensed under the [Apache License 2.0](LICENSE).

Item icons are a curated set from **Google Material Icons** (fonts.google.com/icons)
and **Phosphor Icons**, licensed under the **Apache License 2.0** and **MIT License**, which permit
free commercial use. A copy of the Apache license is provided in the repository root [LICENSE](LICENSE) and at https://www.apache.org/licenses/LICENSE-2.0.

# CountUp

## 1. What the app does

An intentionally small, fully offline Android app and home-screen widget suite that tracks
the **days since a set of anchor dates** (e.g. last haircut, a habit streak, sobriety, an anniversary) or **days until upcoming events**.

- **Calm, Mindful Aesthetic:** Mid-century modern tactile styling with warm paper background, rich card drop shadows, and 30 rotating classical Chinese ink wash landscape themes.
- **Instant Search & 1-Tap Sorting:** Live query filtering and sorting by days elapsed, anchor date, or alphabetical name.
- **Habit Notes & Countdowns:** 2-line custom notes and automatic "UNTIL" sub-labeling for future target dates.
- **Full Zen Widget Suite (5 Home-Screen Widgets):**
  - **Count-ups (Multi-Item Grid):** Full-width 3-column / 2-column grid widget displaying active milestones on dynamic ink wash backgrounds with 7-color MCM palettes.
  - **Hero Milestone Widget (2x1 Poetic Card):** Dedicated single-milestone widget with counter picker, ambient milestone gold accents, and safe two-tap direct in-place reset.
  - **Zen Horizon Ribbon (4x1 & 2x1):** Minimalist horizon ribbon featuring on-widget unit cycling (days, weeks, months, years) directly on tap.
  - **Solar Rhythm (4x2 & 2x2 Seasonal Canvas):** Harmonious seasonal tracker aligning your milestone with the 24 traditional Chinese Solar Terms (24 节气).
  - **Zen Pebble (1x1 Tactile Tile):** Ultra-compact pebble tile engineered for zero-compromise drop targeting across OEM launchers (Samsung One UI, Xiaomi HyperOS, Vivo OriginOS, OPPO ColorOS).
- **Dual-Tier Cross-Device Migration & Offline SAF Backup:**
  - **Tier 1 (Automated OS Sync):** Platform-native Android 12+ encrypted Auto Backup and Device-to-Device (D2D) migration (Mi Mover, Phone Clone, GMS) with an automatic launcher widget ID sanitizer.
  - **Tier 2 (Self-Sovereign JSON Portability):** Offline export and interactive restore via Android's Storage Access Framework (SAF) featuring a pre-restore preview screen and Merge / Replace conflict strategies.
- **Silent Midnight Rollover:** Battery-friendly RTC alarm (`MidnightAlarmReceiver`) advances day counts at `00:00:01` local time without persistent background services or battery drain.
- **Accessibility First:** Full TalkBack screen reader semantics, descriptive action labels, and tactile haptic feedback.
- **Privacy First:** 100% offline, zero runtime permissions, no accounts, no ads, no trackers, no cloud servers, and no background daemon.

## 2. Build requirements and pinned versions

Versions are pinned in `gradle/libs.versions.toml` and deliberately **not** upgraded.

| Component | Version |
|---|---|
| Android Gradle Plugin | `9.4.0` (built-in Kotlin) |
| Gradle | `9.7.1` |
| JDK | `17` |
| Kotlin | `2.3.21` |
| Compose BOM | `2026.06.00` |
| compileSdk / targetSdk / minSdk | `37` / `37` / `26` |

> AGP 9 uses built-in Kotlin, so no `kotlin-android` plugin is applied; Kotlin
> `2.3.21` is provided via a `buildscript` classpath (above AGP's bundled KGP).

Requirements on this machine:

- JDK 17 (`JAVA_HOME` set to the Temurin 17 install).
- Android SDK at `D:\Android\Sdk` (`local.properties` → `sdk.dir`).
- SDK packages: `platforms;android-37.1`, `platforms;android-26`, `build-tools;37.0.0`,
  `platform-tools`, `emulator`, and system images `android-26;google_apis;x86_64`
  and `android-36;google_apis;x86_64` (AVDs `countUp_api26`, `countUp_api36`).

## 3. How to build and run

```bash
./gradlew clean
./gradlew assembleDebug             # debug APK
./gradlew assembleRelease           # signed release APK & bundle (R8 minified)
./gradlew test                      # 335 JVM unit tests (100% pass)
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
  - Includes an automatic **Launcher Widget Sanitizer** on startup that purges invalid widget IDs left behind by previous device launchers.
- **Tier 2: Self-Sovereign JSON Portability via Storage Access Framework (SAF)**
  - Accessible directly in the app settings under **Data & Backup**.
  - **Export:** Generates a clean, UTF-8 encoded, unencrypted `.json` backup file using Android's system document creation picker (`ACTION_CREATE_DOCUMENT`) without requesting storage permissions.
  - **Import & Preview:** Selects a `.json` backup file via `ACTION_OPEN_DOCUMENT`. Displays a pre-restore preview showing item counts, anchor dates, and notes, letting you choose between **Merge** (detects duplicates by ID and name) or **Clean Replace**.
  - **Resilience:** Defensive exception handling ensures no crashes on stripped custom ROMs or file managers that misreport JSON MIME types.

## 6. Midnight Rollover (Zero-Battery RTC Alarm)

Widgets automatically advance at midnight without requiring battery-draining background services or `WorkManager`:
- `MidnightAlarmReceiver` registers a single, idempotent alarm with Android's `AlarmManager` targeting `00:00:01` local time.
- Handles time zone changes (`ACTION_TIMEZONE_CHANGED`) and manual clock adjustments (`ACTION_TIME_SET`) to recalculate and refresh immediately.

## 7. Verification performed

- **335 JVM Unit Tests** (100% passing) across data models, repository fail-safes, MVI ViewModel, JSON salvage parsing, widget reducers, navigation contracts, and backup merge/replace strategies.
- Clean debug and release builds with R8 minification and resource shrinking enabled (`isMinifyEnabled = true`, `isShrinkResources = true`).
- Android Lint (`lintDebug`): **0 errors**.
- Strict zero-permission guard: manifest explicitly strips `WAKE_LOCK`, `ACCESS_NETWORK_STATE`, `RECEIVE_BOOT_COMPLETED`, and `FOREGROUND_SERVICE`. Only `VIBRATE` is declared for tactile haptic feedback.
- Physical device & emulator verification on Android 8.0 (API 26) through Android 15/16 (API 36/37).

## 8. Icons & licensing

Item icons are a curated set from **Google Material Icons** (fonts.google.com/icons)
and **Phosphor Icons**, licensed under the **Apache License 2.0** and **MIT License**, which permit
free commercial use. A copy of the Apache license is at https://www.apache.org/licenses/LICENSE-2.0.

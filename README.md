# CountUp

## 1. What the app does

An intentionally small, fully offline Android app + home-screen widgets that track
the **days since a set of anchor dates** (e.g. last haircut, a habit, sobriety, an anniversary) or **days until upcoming events**.

- **Calm, Mindful Aesthetic:** Mid-century modern tactile styling with warm paper background, rich card drop shadows, and 30 rotating classical Chinese ink wash landscape themes.
- **Instant Search & 1-Tap Sorting:** Live query filtering and sorting by days elapsed, anchor date, or alphabetical name.
- **Habit Notes & Countdowns:** 2-line custom notes and automatic "UNTIL" sub-labeling for future target dates.
- **Dual Home-Screen Widgets:**
  - **Count-ups (Multi-Item Grid):** Full-width 3-column / 2-column grid widget displaying active milestones on dynamic ink wash backgrounds with 7-color MCM palettes.
  - **Hero Milestone Widget:** Dedicated single-milestone widget in **2x1 Poetic Card** layout, complete with counter picker on placement, ambient milestone gold accents, and safe two-tap direct in-place reset.
- **Privacy First:** 100% offline, zero permissions, no accounts, no analytics, no ads, no background scheduler.

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
./gradlew assembleRelease           # signed release APK & bundle
./gradlew test                      # JVM unit tests (143 tests)
./gradlew connectedDebugAndroidTest # instrumented tests (emulator/device online)
./gradlew lintDebug                 # lint (0 errors)
```

Install and launch on a connected device/emulator:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.countup.app/.MainActivity
```

Existing single-item installs are migrated automatically: the old
`last_haircut_epoch_day` value becomes a "Haircut" item on first launch.

## 4. How to add and use the widgets

### 1. Count-ups (Multi-Item Grid Widget)
Long-press the home screen → **Widgets** → find **Count-ups** → drag to a slot.
The widget lists all enabled items in a clean grid with quantity-aware day counts and dynamic ink wash backgrounds.
- **In-place reset:** Tapping the number arms the direct reset confirmation ("0?"), and a second tap within 1.5 seconds zeroes that counter to today without opening the app.

### 2. Hero Milestone Widget (2x1 Poetic Card)
Long-press the home screen → **Widgets** → find **Hero Milestone** → drag to a slot.
- **Configuration Picker:** Automatically opens a picker dialog to choose which specific milestone to pin.
- **Poetic 2x1 Card Layout:** Displays icon badge, habit name, large bold day count, and anchor date sub-label with milestone gold accents.
- **Safe Two-Tap Reset:** Tap the count or badge once to arm confirmation ("0?"), tap again within 1.5 seconds to reset to today. Tapping the background opens the app directly to that habit.

**Widget screenshot (debug builds only):** the widget can be rendered on-device
with the debug-only host activity (present in debug builds, not in release):

```bash
adb shell am start -n com.countup.app/.WidgetHostActivity
adb exec-out screencap -p > artifacts/widget.png
```

**Receiver `exported` value:** `false` (secure default).

## 5. Verification performed

- Clean debug and release builds succeed with R8 minification and resource shrinking enabled (`isMinifyEnabled = true`, `isShrinkResources = true`).
- **139 JVM unit tests** and instrumented tests pass, including: multi-item
  create/read/update/delete, migration of the legacy single value into one item,
  Hero widget configuration and 1x1/2x1 rendering, two-tap in-place reset protocols,
  corrupt-data recovery, JSON round-trips, and widget row derivation.
- `lintDebug`: **0 errors**.
- Merged debug and release manifests request **zero permissions**; backup disabled.
- On-device (API 26 + API 36): verified UI interactions, data persistence across reboots,
  and both Multi-Item Grid and Hero Milestone widgets on launcher home screens.

## 6. Known limitation: best-effort widget refresh after midnight

The widgets refresh when the app resumes, when an item is added/edited/deleted/reset,
when the launcher or system asks, and on `updatePeriodMillis` (30 minutes) as a
fallback. **Counts may remain stale after midnight** until the launcher/system
refreshes or the app is opened — documented rather than hidden behind
AlarmManager/WorkManager, which are intentionally excluded.

## 7. Icons & licensing

Item icons are a curated set from **Google Material Icons** (fonts.google.com/icons)
and **Phosphor Icons**, licensed under the **Apache License 2.0** and **MIT License**, which permit
free commercial use. A copy of the Apache license is at https://www.apache.org/licenses/LICENSE-2.0.



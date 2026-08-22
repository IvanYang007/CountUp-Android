# CountUp

## 1. What the app does

An intentionally small, fully offline Android app + home-screen widget that tracks
the **days since a set of anchor dates** (e.g. last haircut, a habit, an anniversary).

- Create, rename, and delete any number of count-up items; each has a name, a randomly
  assigned icon from the Material "Social" category, and one anchor date.
- A mid-century-modern styled list shows each item's day count with calm add/edit/delete animations.
- A home-screen widget shows all items in a **compact 2-per-row grid**, each with a quantity-aware day count and its anchor date.
- No accounts, no network, no analytics, no database, no background scheduler.

## 2. Build requirements and pinned versions

Versions are pinned in `gradle/libs.versions.toml` and deliberately **not** upgraded.

| Component | Version |
|---|---|
| Android Gradle Plugin | `9.3.0` (built-in Kotlin) |
| Gradle | `9.5.0` |
| JDK | `17` |
| Kotlin | `2.3.21` |
| Compose BOM | `2026.06.00` |
| Glance AppWidget | `1.1.1` |
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
./gradlew assembleRelease           # release APK
./gradlew test                      # JVM unit tests
./gradlew connectedDebugAndroidTest # instrumented tests (emulator/device online)
./gradlew lintDebug                 # lint (0 errors)
```

Install and launch on a connected device/emulator:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.ivanyang.countup/.MainActivity
```

Existing single-item installs are migrated automatically: the old
`last_haircut_epoch_day` value becomes a "Haircut" item on first launch.

## 4. How to add and use the widget

Long-press the home screen → **Widgets** → find **Count-ups** → drag to a slot.
The widget lists every item in a compact 2-column grid, each with its day count.

Managing counts:

- **App:** each card has **Reset** (with a confirmation) and **Delete**; tapping the card edits it.
- **Widget:** each item cell has a small **reset to today** action that zeroes that
  item's count directly, without opening the app. Tapping anywhere else opens the app.

**Widget screenshot (debug builds only):** the widget can be rendered on-device
with the debug-only host activity (present in debug builds, not in release):

```bash
adb shell am start -n com.ivanyang.countup/.WidgetHostActivity
adb exec-out screencap -p > artifacts/widget.png
```

On a (windowless) headless emulator, widget placement/binding can be blocked
("Couldn't add widget") — use an interactive emulator or a physical device, or
place it through the launcher and screenshot the home screen.

**Receiver `exported` value:** `false` (secure default). If a specific launcher
cannot reach the receiver, flip to `true` and re-run the widget checks.

## 5. Verification performed

- Clean debug and release builds succeed.
- Unit tests (15) and instrumented tests (12) pass, including: multi-item
  create/read/update/delete, migration of the legacy single value into one item
  (without re-import after delete), corrupt-data recovery, JSON round-trips, and
  widget row derivation.
- `lintDebug`: **0 errors**, 21 warnings — all deliberate, listed below.
- Merged debug and release manifests request **zero permissions** (Glance's
  transitive permissions are stripped via `tools:node="remove"`); backup disabled.
- On-device (API 26 + API 36): added multiple items via the UI, counts verified
  (e.g. Haircut 17 days, Plant 84 days); migration verified end-to-end; data
  survives force-stop and **reboot**; the multi-item widget placed on the home
  screen rendering both items with counts.
- Widget receiver is registered and handles `APPWIDGET_UPDATE` without crashing.

Remaining warnings (all deliberate):

- `ApplySharedPref` / `UseKtx`: `commit()` is intentional — writes must be
  confirmed before the widget is updated.
- `UnusedAttribute` (`targetCellWidth/Height`): sizing attributes for API 31+, ignored below.
- `AndroidGradlePluginVersion` / `GradleDependency` / `NewerVersionAvailable`:
  the baseline is pinned; no upgrades are part of this project.
- `ObsoleteSdkInt` (`mipmap-anydpi-v26`): adaptive icons require the `-v26` qualifier regardless of minSdk.
- `VectorRaster` (widget preview drawable): a widget preview, not a launcher icon.

## 6. Known limitation: best-effort widget refresh after midnight

The widget refreshes when the app resumes, when an item is added/edited/deleted,
when the launcher or system asks, and on `updatePeriodMillis` (30 minutes) as a
fallback. **Counts may remain stale after midnight** until the launcher/system
refreshes or the app is opened — documented rather than hidden behind
AlarmManager/WorkManager, which are intentionally excluded.

## 7. Icons & licensing

Item icons are a curated set from **Google Material Icons** (fonts.google.com/icons),
"Social" category. They are licensed under the **Apache License 2.0**, which permits
free commercial use. A copy of the license is at https://www.apache.org/licenses/LICENSE-2.0.


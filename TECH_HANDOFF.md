# CountUp — Technical Handoff

Purpose of this doc: let another engineer (or agent) pick up the project and understand what it is, how it's built, what decisions were made, where the tricky bits are, and what's still open — without re-reading everything.

---

## 1. What this is

**CountUp** is a small, fully offline Android app + home-screen widget that counts calendar days since user-defined anchor dates ("days since last haircut", a habit, an anniversary, etc.).

- Any number of items; each has a **name**, a randomly-assigned **icon** (Material "Social" set), and one **anchor date**.
- App: zen-paper styled list (warm off-white, serif/sans/mono typography, 1px borders), add/edit/delete/reset.
- Widget: a toolbar + a 2-column-by-3-per-row grid of item cells (name above a circle containing the day count); **tapping a cell resets that item to today**.

Threat/scope model is deliberately minimal: **no accounts, no network, no analytics, no database, no background scheduler, zero permissions, backup disabled.**

---

## 2. Tech stack (pinned — do NOT upgrade casually)

Versions live in `gradle/libs.versions.toml`.

| Component | Version | Note |
|---|---|---|
| Android Gradle Plugin | `9.3.0` | Uses **built-in Kotlin** (no `org.jetbrains.kotlin.android` plugin) |
| Gradle | `9.5.0` | Wrapper checked in |
| JDK | `17` | Temurin; `JAVA_HOME` set |
| Kotlin | `2.3.21` | Provided via `buildscript` classpath in root `build.gradle.kts` (above AGP's bundled KGP 2.2.10) |
| Compose BOM | `2026.06.00` | |
| Glance AppWidget | `1.1.1` | Home-screen widget |
| compileSdk / targetSdk / minSdk | 37 / 37 / 26 | |

**Key build gotcha:** AGP 9 has built-in Kotlin — do not re-add `kotlin-android`. The `kotlin { compilerOptions {} }` block was also removed; `compileOptions` JDK 17 covers the JVM target.

Local SDK at `D:\Android\Sdk` (see `local.properties`).

---

## 3. Architecture — files & responsibilities

Production Kotlin is flat under `app/src/main/java/com/ivanyang/countup/`:

| File | Responsibility |
|---|---|
| `MainActivity.kt` | Compose app: list screen, add/edit dialog (name + date picker), reset/delete confirmation, header with `+`, theme, and per-item press/`animateItem()` motion |
| `CountUpItem.kt` | `CountUpItem` data class (`id`, `name`, `epochDay`, `icon`) + JSON `encodeItems`/`decodeItems` (org.json) |
| `CountUpStore.kt` | Persistence: items as one JSON array in private `SharedPreferences` (`countup_prefs`); CRUD; one-time migration of legacy single value |
| `DaysSince.kt` | Pure `daysSince(last, today)` using `ChronoUnit.DAYS` clamped ≥ 0 |
| `DateConversion.kt` | UTC-safe picker millis → `LocalDate`; localized date formatter |
| `ItemIcons.kt` | The 20-icon registry; `iconRes(name)` map + `SOCIAL_ICON_NAMES` + `DEFAULT_ICON` |
| `HaircutWidget.kt` | Glance widget: toolbar, 3-column `LazyVerticalGrid`, per-cell tap-to-reset (`ResetCountAction`), theme-aware colors |
| (debug) `WidgetHostActivity.kt` | Debug-only activity to render the widget for screenshots (not in release) |

Tests:
- `app/src/test/...` (JVM): `CountUpItemTest` (8), `DaysSinceTest` (6), `WidgetRowTest` (3) → **17 unit tests**
- `app/src/androidTest/...` (device): `CountUpStoreInstrumentedTest` (16) — CRUD, migration, recovery, icon assignment

Resources: `res/values/strings.xml`, `plurals.xml` (`days`, `days_unit`), `themes.xml`, `colors.xml`; `res/drawable/ic_*.xml` (20 Material icons + `ic_zen_enso` + `ic_solid_circle` + `ic_launcher_foreground`); `res/xml/haircut_widget_info.xml`, `data_extraction_rules.xml`, `backup_rules.xml`.

---

## 4. Data model & storage

One value per item, stored as a JSON array string under key `items_v1` in private prefs file `countup_prefs`:

```json
[{"id":"uuid","name":"Haircut","epochDay":20667,"icon":"person"}]
```

- `epochDay` = `LocalDate.toEpochDay()`.
- **Migration:** the old single-value format (`haircut_prefs/last_haircut_epoch_day`) is imported as one "Haircut" item on first read, guarded by a `migrated_v1` flag so it doesn't re-import after the user deletes everything.
- **Robustness:** missing/corrupt JSON recovers to a migrated-or-empty list (never crashes). `addItem` assigns a random icon from `SOCIAL_ICON_NAMES`.
- Writes use `commit()` (synchronous) deliberately — the app waits for persistence before updating the widget (write-before-update ordering).
- No encryption: the stored data is just dates; encryption was explicitly ruled out as unjustified.

---

## 5. Design & behavior decisions

- **Zen-paper theme** (from `minimalist-ui` + a parchment/serif take): warm paper background `#F7F6F3`, cards white with `1px #EAEAEA` border, radius 12, ink `#2F3437`, muted `#787774`; serif for headings/counts, sans for labels, monospace for meta. No gradients, no heavy shadows. Dark scheme variants defined in `MainActivity`.
- **Scale-on-press** (`0.96` buttons / `0.99` cards) via `pressScale()`; list add/remove uses `Modifier.animateItem()`, disabled under system reduce-motion.
- **Widget:** 3-column `LazyVerticalGrid`; cells show name (top) + a light circle (`ic_solid_circle`, faint ink rim) with a dark number. Tapping a cell runs `ResetCountAction` which resets only **that** item (per-id `ActionParameters`) and refreshes the widget. The circle number stays dark ink even in dark mode because the circle fill is fixed light.

### Known Glance 1.1.1 limitations (important)
- **No shape/rounded background** — the "circle" is a vector drawable; the shadow/radius for the widget can't be rounded; chips are square-ish.
- **No `weight`, no `align`, no `SpaceBetween`** in layouts — centering is done via `horizontalAlignment`/`contentAlignment`, and "push to the right" via `Modifier.weight(1f)` on names.
- **`ColorProvider` exists in `androidx.glance.unit`** (type) and the **day/night factory is `androidx.glance.color.ColorProvider(day: Color, night: Color)`** (aliased as `dayNightColorProvider`).
- `actionRunCallback` schedules via an async broadcast — reset is ~0.4 s, not instant (verified by timestamps).

---

## 6. Build / test / run

```bash
export JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot"
./gradlew clean
./gradlew assembleDebug            # debug APK
./gradlew assembleRelease          # unsigned release
./gradlew test                     # 17 JVM unit tests
./gradlew connectedDebugAndroidTest # device tests (emulator/device online)
./gradlew lintDebug                # 0 errors (warnings are intentional)
```

Install + launch:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.ivanyang.countup/.MainActivity
```

Emulators available: `countUp_api26` (API 26) and `countUp_api36` (API 36). Running AVD port 5556. Widget placement on the API 36 launcher works but **drag-and-drop placement via blind `adb` is unreliable** — place it manually to inspect the widget.

---

## 7. Security posture

- **Zero `<uses-permission>`** in merged debug & release manifests (Glance/WorkManager's transitive permissions are stripped via `tools:node="remove"` in the main manifest).
- `allowBackup="false"` + `dataExtractionRules`/`backup_rules` (backup disabled on all API levels).
- No network, no WebView, no `Log.*`, no secrets in source.
- Only `MainActivity` is `exported=true` (launcher); the widget receiver is `exported=false`. The other `exported=true` components are library-internal (Glance/WorkManager/profileinstaller) and required.
- No automated dependency-CVE scan is set up; release is not R8-minified (both intentional/LOW, noted in the security review).

---

## 8. Open / known items

1. **Widget dark-mode render not screenshot-verified** — the theme-aware dark-paper code is implemented and compiles, and a night-mode toggle was confirmed to keep text legible, but the placed widget couldn't be reliably screenshot in this session. Verify visually on a device by re-adding the widget and toggling dark mode.
2. **Widget reset "last item" report not reproducible** — tested all cells (first/last/scrolled-recycled) on the current build; all reset correctly. Likely causes of the report were a stale widget or a SystemUI ANR (emulator load). Re-add the widget / restart emulator if it recurs.
3. **Release hardening (LOW):** enable `isMinifyEnabled` (R8) and/or add OWASP dependency-check before any store release. Deliberately deferred (plan pinned obfuscation off).
4. **`WidgetHostActivity.kt`** (debug-only) exists to render the widget for screenshots; remember it is **not** part of release.
5. **Icons:** 20 Material "Social" icons embedded, **Apache 2.0 license** — commercial use OK; attribution credited in `README.md` §7.

---

## 9. How to make changes safely

- Keep the code flat (7 main files). Don't add a DB, DI, navigation lib, or background scheduler.
- After any change: `./gradlew test lintDebug assembleDebug` and (if device touched) `connectedDebugAndroidTest`.
- To add an icon: drop a Material Social SVG as `ic_<name>.xml`, add `"<name>"` to `SOCIAL_ICON_NAMES` and a case to `iconRes()` in `ItemIcons.kt`.
- Widget color changes: keep the count number dark (the circle fill is fixed light); use `dayNightColorProvider(day=…, night=…)` for theme-aware text.
- Don't bump pinned versions without re-running the whole suite.

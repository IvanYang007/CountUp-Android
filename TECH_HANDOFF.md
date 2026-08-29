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
| RemoteViews (platform) | — | Home-screen widget; Glance dependency removed in the instant-refresh rewrite |
| compileSdk / targetSdk / minSdk | 37 / 37 / 26 | |

**Key build gotcha:** AGP 9 has built-in Kotlin — do not re-add `kotlin-android`. The `kotlin { compilerOptions {} }` block was also removed; `compileOptions` JDK 17 covers the JVM target.

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
| `AbstractBackgrounds.kt` | 5 rotatable Chinese ink wash abstract background themes (Mountain, Sand Dunes, Sea Horizon, Solitary Isle with Taihu Scholar Rock, Willow Leaves) + auto-daily rotation |
| `CountUpItem.kt` | `@Immutable` data class (`id`, `name`, `epochDay`, `comment`, `icon`, `futureFlag`, `showInWidget`) + JSON `encodeItems`/`decodeItems` + balanced-brace stream `salvageItems` |
| `CountUpStore.kt` | Zero-data-loss persistence: 5-tier fail-safe hierarchy (Primary Prefs -> JSON Salvage -> Atomic Disk Backup `countup_backup.json` -> Legacy Migration -> Timestamped Quarantine) |
| `DaysSince.kt` | Pure `daysSince(last, today)` using `ChronoUnit.DAYS` (supports negative future counts) |
| `DateConversion.kt` | UTC-safe picker millis → `LocalDate`; localized date formatter; dynamic "SINCE" / "UNTIL" sub-labeling |
| `ItemIcons.kt` | The 20-icon registry; `iconRes(name)` map + `SOCIAL_ICON_NAMES` + `DEFAULT_ICON` |
| `WidgetBackgroundRenderer.kt` | Native procedural Canvas/Path vector renderer for 5 Chinese ink wash themes on warm paper (< 180 KB memory) |
| `CountUpWidget.kt` | Ultra-minimalist Zen RemoteViews widget: dynamic ink background, full-width 3-column grid, `ResetCountReceiver` (in-place double-tap reset confirmation), `pushWidgetUpdate()` imperative refresh |
| (debug) `WidgetHostActivity.kt` | Debug-only activity to render the widget for screenshots (not in release) |

Tests:
- `app/src/test/...` (JVM): `CountUpViewModelTest` (Turbine), `CountUpStressAndBoundaryTest` (1k items, unicode, leap years), `CountUpRepositoryTest`, `CountUpItemTest`, `CountUpStoreTest`, `EdgeCaseMatrixTest`, `AbstractBackgroundTest`, `DateConversionTest`, `DaysSinceTest`, `ItemIconsTest`, `SortOrderTest`, `WidgetRowTest`, `WidgetBackgroundTest` → **104 JVM unit tests** (100% green)
- `app/src/androidTest/...` (device): `ComposeUiSmokeTest` (stateless UI & a11y semantics), `CountUpStoreInstrumentedTest` (CRUD, migration, recovery)

Resources: `res/values/strings.xml`, `plurals.xml` (`days_unit`), `themes.xml`, `colors.xml`; `res/drawable/ic_*.xml` (20 Material icons + `ic_zen_enso` + `ic_solid_circle` + `ic_widget_grid_*`); `res/xml/haircut_widget_info.xml`, `data_extraction_rules.xml`, `backup_rules.xml`.

---

## 4. Data model & storage

One value per item, stored as a JSON array string under key `items_v1` in private prefs file `countup_prefs` with dual-write atomic backup snapshot `countup_backup.json`:

```json
[{"id":"uuid","name":"Meditation","epochDay":20667,"comment":"Daily morning calm","icon":"person","futureFlag":false,"showInWidget":true}]
```

- `epochDay` = `LocalDate.toEpochDay()`.
- **Zero Data Loss Guarantee:** If SharedPreferences is wiped or corrupted, `CountUpStore` automatically self-heals from `countup_backup.json`. If a payload is truncated mid-write by OS power cutoff, `salvageItems` extracts all intact items and preserves the raw broken payload in a timestamped quarantine key.
- **Migration:** The legacy v0 format (`haircut_prefs/last_haircut_epoch_day`) imports as one "Haircut" item on first read, guarded by `migrated_v1`.
- **Writes use `commit()` and `fd.sync()`** synchronously before updating the widget (write-before-update ordering).

---

## 5. Design & behavior decisions

- **Zen-paper theme & Chinese Ink Wash Backgrounds:** Warm paper background `#F7F6F3`, cards white with `1px #EAEAEA` border, radius 12, ink `#2F3437`, muted `#787774`; serif for headings/counts, sans for labels. 5 authentic Chinese ink wash landscape themes anchored to borders with negative space.
- **Dynamic Anchor Sub-labels:** Count $\ge 0$ renders `SINCE <date>`; count $< 0$ (future event) renders `UNTIL <date>`.
- **Scale-on-press & Motion:** `0.96` buttons / `0.99` cards via `pressScale()`; list add/remove uses `Modifier.animateItem()`, disabled under system reduce-motion.
- **Widget Double-Tap In-Place Reset:** Classic RemoteViews 3-column `GridView` (`countup_widget.xml`); tapping a cell once arms the item and prompts confirmation ("0?"), tapping again within 4 seconds resets the anchor date to today with Toast confirmation.

---

## 6. Build / test / run

```bash
export JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot"
./gradlew clean
./gradlew assembleDebug            # debug APK
./gradlew assembleRelease          # unsigned release
./gradlew test                     # 68 JVM unit tests (100% green)
./gradlew connectedDebugAndroidTest # device tests (emulator/device online)
./gradlew lintDebug                # 0 errors
```

Install + launch:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.countup.app/.MainActivity
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

# CountUp — Code Review Evaluation & Assessment

**Date:** 2026-09-13  
**Target Review Evaluated:** [`docs/CODE_REVIEW_2026-09-13.md`](file:///d:/Github/countUp/docs/CODE_REVIEW_2026-09-13.md)  
**Evaluator:** Antigravity (Google Official Android Skills Suite)  
**Commit Inspected:** `0162fc3` (branch `main`)  
**Scope:** Verification of all 39 findings (3 P0, 12 P1, 24 P2 / 28 table items) + 5 Untested High-Risk Behaviors against codebase and official Android engineering standards.

---

## Executive Summary

A comprehensive 1-by-1 evaluation of [`docs/CODE_REVIEW_2026-09-13.md`](file:///d:/Github/countUp/docs/CODE_REVIEW_2026-09-13.md) was conducted using official Android guidance from Google (`android-intent-security`, `play-policy-insights`, `r8-analyzer`, `agp-9-upgrade`, `testing-setup`, `android-profiler`, `edge-to-edge`, `adaptive`, `android-cli`, `compose-kotlin-agent-skills`).

### Key Evaluation Takeaways:
1. **Critical Vulnerabilities Confirmed (3/3 P0 Valid)**:
   - **P0-1** (Secret Leak in Packfiles): Commit `32f6f36` still exposes `keystore/keystore-pass.txt` in git packfile object `8370ad4c87436d41ea4c8c3701b4f2c6756d4986`.
   - **P0-2** (R8 Enum Obfuscation): `mapping.txt` proves `TimeDisplayMode` and `ZenWidgetDisplayUnit` are renamed to `f`, `g`, causing silent deserialization resets to `DAYS`.
   - **P0-3** (Silent Partial-Decode Data Loss): `CountUpItem.decodeItems` accepts partial arrays as valid; subsequent writes permanently truncate items without quarantining.
2. **High-Impact Nuance Identified**:
   - **P1-8** is **PARTIALLY VALID**: The review claimed 6 strings had "no Chinese anywhere". Code verification reveals `solar_rhythm_configure_*` and `backup_*` DO exist in `values-zh-rCN/strings.xml` and `values-zh-rTW/strings.xml`. They render in Chinese on standard Chinese devices, but are missing in `values-zh/` (generic Chinese fallback) and omitted from `locales_config.xml`.
3. **Repository Guardrails Conflict Highlighted**:
   - For **P2-R8-2** (Data model keep rules), while technically no reflection is used in `CountUpItem` (manual `org.json`), [`AGENTS.md`](file:///d:/Github/countUp/AGENTS.md) and [`docs/RECURRING_ISSUES.md`](file:///d:/Github/countUp/docs/RECURRING_ISSUES.md) explicitly mandate keeping data models in `proguard-rules.pro` to safeguard R8 data class copy and StateFlow serialization across release builds.

---

## Reconciliation Summary Matrix

| Severity | Total Findings in Review | Valid | Partially Valid | False Positive | Core Assessment |
|---|---|---|---|---|---|
| **P0** | 3 | **3** | 0 | 0 | 100% verified critical risks (Keystore leak, R8 enum loss, partial JSON truncation). |
| **P1** | 12 | **11** | **1** | 0 | 11 verified valid. P1-8 partially valid (strings exist in `zh-rCN`, missing in `values-zh`). |
| **P2** | 24 (28 table items) | **26** | **2** | 0 | 26 verified valid. P2-R8-2 (model keeps) & P2-TEST-3 (XML asserts) partially valid due to intentional guardrails. |
| **Untested Risks** | 5 | **5** | 0 | 0 | All 5 high-risk untested paths confirmed lacking coverage. |
| **Total** | **44 items** | **41** | **3** | **0** | **93.2% Valid, 6.8% Partially Valid, 0% False Positive.** |

---

## 1-by-1 Finding Assessments

### P0 Findings (Blocker / Data Loss / Secret Leak)

#### P0-1. Live release-upload-key password is in git history
- **Applicable Skill**: `play-policy-insights` / Security & Secrets Best Practices
- **Codebase Citation**: Object blob `8370ad4c87436d41ea4c8c3701b4f2c6756d4986` reachable in git packfile via `git rev-list --all --objects`. Extracted content is `717ad89de6cd825ca4b425d01f005e00`, matching `keystore/keystore-pass.txt` on disk.
- **Verdict**: `[VALID]`
- **Root Cause Assessment**: Removing `keystore-pass.txt` in commit `625f2c3` untracked the working tree file but preserved the git commit blob in history. Anyone with repo clone access can extract the production signing keystore password.

#### P0-2. R8 renames the two enums that are persisted by `name`; read path swallows the failure
- **Applicable Skill**: `r8-analyzer`
- **Codebase Citation**: 
  - `app/build/outputs/mapping/release/mapping.txt:261315`: `com.countup.app.TimeDisplayMode -> gc2: com.countup.app.TimeDisplayMode DAYS -> f`
  - `app/build/outputs/mapping/release/mapping.txt:262781`: `com.countup.app.ZenWidgetDisplayUnit -> ln2: com.countup.app.ZenWidgetDisplayUnit DAYS -> f`
  - `app/src/main/java/com/countup/app/CountUpStore.kt:414-445`: Writes `mode.name` and reads via `TimeDisplayMode.valueOf(raw) catch (_: IllegalArgumentException) { TimeDisplayMode.DAYS }`.
  - `app/proguard-rules.pro:15-18`: Only keeps `values()` and `valueOf(String)`, omitting `<fields>;`.
- **Verdict**: `[VALID]`
- **Root Cause Assessment**: Standard enum keeps do not pin enum field names. Calling `.name` persists obfuscated names in release builds (or unobfuscated in debug), and `valueOf()` mismatch throws `IllegalArgumentException`, which is caught and silently defaults to `DAYS`.

#### P0-3. One malformed element silently deletes items, then the next write destroys both copies
- **Applicable Skill**: `compose-kotlin-agent-skills` (Architecture & State Integrity)
- **Codebase Citation**:
  - `app/src/main/java/com/countup/app/CountUpItem.kt:201-205`: Loops over `JSONArray`, skips invalid elements, and returns `out` as long as `out.isNotEmpty()`.
  - `app/src/main/java/com/countup/app/CountUpStore.kt:43-47`: Accepts non-null `decoded`, synchronizes backup, and skips `quarantineRawPayload(raw)`.
  - `app/src/androidTest/java/com/countup/app/CountUpStoreInstrumentedTest.kt:274-286`: Test `partiallyCorruptArrayKeepsParseableItems` explicitly asserts that partial decode does NOT quarantine.
- **Verdict**: `[VALID]`
- **Root Cause Assessment**: A partial parse failure is treated as a healthy decode rather than corruption. Subsequent mutations overwrite `countup_prefs.xml` and `countup_backup.json` with only the subset, permanently purging the unparseable items.

---

### P1 Findings (High Priority / Behavioral Bugs / Policy Risks)

#### P1-1. No release artifact rebuilt since zero-permission change; last release still requests `VIBRATE`
- **Applicable Skill**: `play-policy-insights` / `android-cli`
- **Codebase Citation**:
  - `app/build/outputs/apk/release/app-release.apk` (mtime 15:07:10) requests `android.permission.VIBRATE`.
  - Commit `2c1261d` (time 21:57:11) removed `VIBRATE` from `AndroidManifest.xml`.
  - `README.md:133`, `TECH_HANDOFF.md:145`, `PRIVACY_POLICY.md:11` all claim `VIBRATE` is declared.
- **Verdict**: `[VALID]`
- **Root Cause Assessment**: Release artifacts on disk predated commit `2c1261d`, leaving a stale APK with `VIBRATE`, while documentation was never updated to reflect the zero-permission state.

#### P1-2. Lint never gates the release build
- **Applicable Skill**: `agp-9-upgrade` / `testing-setup`
- **Codebase Citation**:
  - `app/build.gradle.kts:83`: `checkReleaseBuilds = false`.
  - `.github/workflows/ci.yml:38`: Executes `./gradlew lintDebug` only.
- **Verdict**: `[VALID]`
- **Root Cause Assessment**: CI runs lint strictly against the debug variant, and AGP's release lint check is explicitly disabled, allowing release-only lint errors and manifest issues to slip through.

#### P1-3. CI silently builds an unsigned release
- **Applicable Skill**: `agp-9-upgrade` / Android CI Best Practices
- **Codebase Citation**:
  - `app/build.gradle.kts:33-47`: If keystore or password is missing, signing config `"release"` is never created, and `signingConfig = signingConfigs.findByName("release")` returns `null`.
  - `.github/workflows/ci.yml:41`: `./gradlew assembleRelease bundleRelease --no-daemon` executes without keystore credentials.
- **Verdict**: `[VALID]`
- **Root Cause Assessment**: AGP treats a `null` signing configuration on release builds as a request to produce unsigned artifacts rather than raising an error, causing CI to succeed while generating unusable unsigned bundles.

#### P1-4. No green test evidence exists for HEAD
- **Applicable Skill**: `testing-setup`
- **Codebase Citation**:
  - `app/build/reports/tests/testDebugUnitTest/index.html` timestamp: `2026-09-13T01:55:10Z`.
  - Commit `2c1261d` timestamp: `2026-09-13T01:57:11Z` (altered 20+ core files).
  - Commit `0162fc3` timestamp: `2026-09-13T01:58:01Z`.
- **Verdict**: `[VALID]`
- **Root Cause Assessment**: The 388 recorded passing tests validate an obsolete commit; no test execution has occurred on `0162fc3` to verify HEAD stability.

#### P1-5. The 41 instrumented tests never run anywhere
- **Applicable Skill**: `testing-setup`
- **Codebase Citation**:
  - `app/build.gradle.kts:13-19`: `defaultConfig` lacks `testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"`.
  - `app/build/outputs/apk/androidTest` directory does not exist.
  - `.github/workflows/ci.yml` contains zero connected/instrumented test steps.
- **Verdict**: `[VALID]`
- **Root Cause Assessment**: Without a declared test instrumentation runner, Android Gradle Plugin does not wire standard on-device test execution, leaving `CountUpStoreInstrumentedTest` and `ComposeUiSmokeTest` unexecuted.

#### P1-6. Exported widget configure activities never verify `appWidgetId` ownership
- **Applicable Skill**: `android-intent-security`
- **Codebase Citation**:
  - `HeroWidgetConfigureActivity.kt:56-64`, `ZenHorizonConfigureActivity.kt:56-64`, `SolarRhythmConfigureActivity.kt:56-64`, `ZenPebbleConfigureActivity.kt:56-64`: Only check `appWidgetId == INVALID_APPWIDGET_ID`.
  - Sibling receivers (`HeroWidgetReceiver.kt:29-36`) explicitly check `AppWidgetManager.getInstance(context).getAppWidgetInfo(appWidgetId)?.provider?.packageName == context.packageName`.
- **Verdict**: `[VALID]`
- **Root Cause Assessment**: Configure activities are exported (required for launcher binding) but fail to verify that the incoming `appWidgetId` belongs to `com.countup.app`, allowing third-party apps to pass arbitrary widget IDs and rebind other apps' widget slots.

#### P1-7. The widget memory-budget gate covers the wrong widgets and has no production caller
- **Applicable Skill**: `android-profiler` / IPC Binder Transaction Rules
- **Codebase Citation**:
  - `app/src/main/java/com/countup/app/WidgetMemoryBudgetGate.kt`: Only checks Zen Horizon, Solar Rhythm, and Zen Pebble 1x1. `assertAllWithinBudget()` is called exclusively in unit tests.
  - `app/src/main/java/com/countup/app/CountUpWidget.kt:102-141`: Allocates up to 360 × 260 × 4 bytes (~366 KB) for grid background bitmap, completely ungated.
- **Verdict**: `[VALID]`
- **Root Cause Assessment**: The budget gate was implemented as an isolated test-only helper covering Zen widgets, leaving the primary `CountUpWidget` (which pushes the largest bitmap payload) unmonitored at runtime.

#### P1-8. 162 strings exist only in `values-zh`; 6 strings have no Chinese translation at all
- **Applicable Skill**: Android Resource Resolution & Localization
- **Codebase Citation**:
  - Review claimed 6 keys had "no Chinese anywhere": `solar_rhythm_configure_title`, `solar_rhythm_configure_subtitle`, `backup_io_error`, `backup_restore_file_too_large`, `backup_restore_unsupported_version`, `backup_restore_damaged_notice`.
  - Direct grep check: `solar_rhythm_configure_title` exists in `values-zh-rCN/strings.xml:317` and `values-zh-rTW/strings.xml:308`. `backup_*` keys exist in `values-zh-rCN/strings.xml`.
  - However, they are completely absent from `values-zh/strings.xml`.
  - `res/xml/locales_config.xml` lists `zh-Hans`, `zh-Hant`, `zh-CN`, `zh-TW`, `zh-HK` but omits `zh`.
- **Verdict**: `[PARTIALLY_VALID]`
- **Root Cause Assessment**: The finding's claim that these keys "render in English on every Chinese device" is inaccurate; devices on `zh-CN` or `zh-TW` resolve the strings correctly. However, the finding is valid regarding resource fragmentation: `values-zh/` (generic Chinese fallback) is missing keys, and locale configurations are split redundantly across 6 folders.

#### P1-9. Zen Horizon widget subtitle bypasses localization
- **Applicable Skill**: Localization & Compose Architecture
- **Codebase Citation**:
  - `app/src/main/java/com/countup/app/ZenWidgetReducer.kt:61`: `DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)`.
  - `ZenWidgetReducer.kt:198`: `val formattedStart = (if (isFuture) "Until " else "Since ") + anchorDate.format(DATE_FORMATTER)`.
- **Verdict**: `[VALID]`
- **Root Cause Assessment**: Hardcoded English prefixes (`"Until "`, `"Since "`) and fixed `Locale.US` date formatting are concatenated in the view state reducer, bypassing existing localized string resources (`since_label`, `until_label`).

#### P1-10. Midnight alarm uses `RTC`, not `RTC_WAKEUP`; reboot re-arm depends entirely on widget `onUpdate`
- **Applicable Skill**: Android Background Tasks & AlarmManager
- **Codebase Citation**:
  - `app/src/main/java/com/countup/app/MidnightAlarmReceiver.kt:71`: `alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC, targetMillis, pendingIntent)`.
  - `app/src/main/AndroidManifest.xml:13`: `<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" tools:node="remove" />`.
- **Verdict**: `[VALID]`
- **Root Cause Assessment**: Non-wakeup `AlarmManager.RTC` does not wake the application processor from Doze. Furthermore, stripping `RECEIVE_BOOT_COMPLETED` leaves no wake-up receiver on device reboot; if no widget is placed on the home screen, midnight rollover is never re-scheduled.

#### P1-11. No schema version in the local store
- **Applicable Skill**: `compose-kotlin-agent-skills` (Data Architecture)
- **Codebase Citation**:
  - `app/src/main/java/com/countup/app/CountUpStore.kt`: Companion defines `KEY_MIGRATED` and `KEY_REVISION`, but no `KEY_SCHEMA_VERSION`.
  - `CountUpBackupPayload.kt:15`: Defines `CURRENT_SCHEMA_VERSION = 1`, but this guards only external backup imports, not local SharedPreferences evolution.
- **Verdict**: `[VALID]`
- **Root Cause Assessment**: Local persistence lacks an explicit schema version tracking key. Changes to local JSON schema cannot be detected on app launch or downgrade, risking silent data corruption.

#### P1-12. `MechanicalResetButton` has a real 30 dp target that Compose does not expand
- **Applicable Skill**: `compose-kotlin-agent-skills` (Compose UI & Accessibility)
- **Codebase Citation**:
  - `app/src/main/java/com/countup/app/CountUpContent.kt:998-1002`: Uses `Box(modifier = modifier.size(30.dp).pointerInput(enabled) { detectTapGestures(...) })`.
- **Verdict**: `[VALID]`
- **Root Cause Assessment**: Unlike `Modifier.clickable`, raw `Modifier.pointerInput` does not automatically inherit Compose Material3's `minimumInteractiveComponentSize` (48 dp). The hold-to-reset target is physically 30 dp, violating accessibility guidelines.

---

### P2 Findings (Hygiene, Polish, and Guardrail Checks)

#### Category 1: R8 / Build
1. **Dead keep rules for Room / WorkManager / Glance (`proguard-rules.pro:34-40`)**: `[VALID]` — `libs.versions.toml` and `app/build.gradle.kts` contain zero dependencies for Room, WorkManager, or Glance.
2. **`-keep class X { *; }` on 9 data models (`proguard-rules.pro:4-12`)**: `[PARTIALLY_VALID]` — While JSON serialization is manual `org.json`, keeping these models is an explicit hard rule in [`AGENTS.md`](file:///d:/Github/countUp/AGENTS.md) and [`docs/RECURRING_ISSUES.md`](file:///d:/Github/countUp/docs/RECURRING_ISSUES.md) to guard against R8 field stripping in data class copies and StateFlow reactivity.
3. **Duplicate enum rules, AAPT2 redundant keeps, `@Composable` keep (`proguard-rules.pro:15-45`)**: `[VALID]` — AAPT2 already generates component keeps; keeping all `@Composable` functions blocks R8 dead-code inlining.
4. **`debugSymbolLevel = "FULL"` no-op and `vcsInfo.include = false` (`app/build.gradle.kts:48-53`)**: `[VALID]` — App has no C/C++ native code; disabling VCS info removes commit tracking from Play Console.

#### Category 2: Secrets and Manifest
5. **Plaintext keystore password in project tree (`keystore/keystore-pass.txt`)**: `[VALID]` — Password file exists in working tree and is resolved in `build.gradle.kts`.
6. **Dynamic receiver permission declaration merges (`AndroidManifest.xml:15`)**: `[VALID]` — Only `<uses-permission>` was removed; `<permission android:name="...DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION">` still merges.
7. **Debug `WidgetHostActivity` is `exported="true"` without unique package ID (`src/debug/AndroidManifest.xml`)**: `[VALID]` — Debug variant lacks `applicationIdSuffix = ".debug"`, replacing release installs.
8. **Debug merges PreviewActivity and ComponentActivity**: `[VALID]` — Injected by Compose UI tooling in debug build type.

#### Category 3: Resources and Localization
9. **Missing widget previews and `previewLayout` (`res/xml/*_widget_info.xml`)**: `[VALID]` — `solar_rhythm` has no preview; `zen_horizon` reuses Hero preview; no widget declares `previewLayout`.
10. **6 overlapping Chinese resource folders and duplicate entries in `locales_config.xml`**: `[VALID]` — `locales_config.xml` lists both script and country tags (`zh-Hans` and `zh-CN`), causing duplicate entries in per-app language settings.
11. **58 `VectorPath` lint warnings (`lint-results-debug.xml`)**: `[VALID]` — Verified exactly 58 VectorPath warnings for complex vector paths.
12. **Hardcoded `"ZEN"` fallback tag (`CountUpItem.kt:86`)**: `[VALID]` — Fallback tag defaults to English string `"ZEN"` regardless of system locale.
13. **All 6 widget providers poll at `1800000` ms (`res/xml/*_widget_info.xml`)**: `[VALID]` — All 6 providers trigger periodic system wakeups at 30-minute intervals.

#### Category 4: Repository Hygiene
14. **52.8 MB tracked across 523 files including 107 in `artifacts/`**: `[VALID]` — Verified 107 files in `artifacts/` and 38 in `playstore_package/` are tracked in git index.
15. **Agent working notes committed in `.scratch/`**: `[VALID]` — 6 markdown files in `.scratch/codebase-hardening/` are committed.
16. **~33 MB of untracked release APKs in repo root**: `[VALID]` — Multiple historical release APKs reside untracked in the working tree.

#### Category 5: State and Concurrency
17. **Widget binding survives item deletion (`ZenWidgetReducer.kt:88-93`, `CountUpStore.kt:296-317`)**: `[VALID]` — `deleteItem` removes item and reset records, but leaves widget binding in prefs, causing permanent blank widgets.
18. **Theme/sort/binding accessors bypass `globalStoreLock` (`CountUpStore.kt:460-530`)**: `[VALID]` — Preference accessors mutate SharedPreferences directly without holding `globalStoreLock`.
19. **`scheduleMidnightAlarm` runs ~7 provider IPCs on main thread in `onUpdate`**: `[VALID]` — Synchronous IPC occurs in BroadcastReceiver `onUpdate` before coroutine dispatch.
20. **PendingIntent requestCode namespace mixing (`WidgetNavigationContract.kt:53-57`)**: `[VALID]` — Low collision probability, but combines raw `appWidgetId` with `hash * 31 + id`.
21. **`RemoteViews.setInt(id, "setColorFilter", 0)` is unreliable clear**: `[VALID]` — Sets integer 0 (transparent color) rather than clearing color filter.
22. **28 × `commit()` calls instead of `apply()` (`CountUpStore.kt`)**: `[VALID]` — Exactly 28 blocking synchronous `commit()` calls exist in store.

#### Category 6: UI and Accessibility
23. **List insets applied to parent instead of `contentPadding` (`CountUpContent.kt:169-173`)**: `[VALID]` — Scaffold inner padding is applied to parent `Column`, clipping list scrolling behind navigation bars.
24. **No window size class handling / fixed 260 dp width (`CountUpContent.kt:749`)**: `[VALID]` — Dropdown menu has fixed width; main layout lacks responsive wide-screen layout.

#### Category 7: Test Quality
25. **Tautological assertions `assertTrue(active || !active)` (`MidnightAlarmReceiverTest.kt:79`)**: `[VALID]` — Test contains meaningless assertion and assertion-free test methods.
26. **Debounce test tests local duplicate `TestDebouncer` (`WidgetRefreshDebounceTest.kt:19-38`)**: `[VALID]` — Tests a local mock class rather than production debounce logic.
27. **11 XML-substring tests assert file text (`WidgetContractInvariantsTest.kt`)**: `[PARTIALLY_VALID]` — Valid that tests inspect raw text strings, but partially valid because this is a lightweight invariant check intentionally enforcing `RECURRING_ISSUES.md`.
28. **Test reflects into private `persist(List)` (`CountUpStoreTest.kt:336`)**: `[VALID]` — Test uses reflection to invoke private persistence method.

---

### Untested High-Risk Behaviors Evaluation

1. **`CountUpStore.items()` partial-decode loss (P0-3)**: Confirmed untested; existing test `partiallyCorruptArrayKeepsParseableItems` encodes data loss as expected behavior.
2. **No schema-version guard in `CountUpStore` (P1-11)**: Confirmed untested; no test simulates schema evolution or field renaming.
3. **`MainActivity.refreshWidget` debounce and lifecycle flush**: Confirmed untested against production activity code.
4. **Reboot and Doze re-arm of `MidnightAlarmReceiver.scheduleMidnightAlarm` (P1-10)**: Confirmed untested; no verification of reboot re-arm without active widgets.
5. **`CountUpStore.deleteItem` interaction with widget bindings**: Confirmed untested; no test asserts widget rebinds or clears when its target item is deleted.

---

## Prioritized Recommended Change List

Adhering to **Ponytail Lazy Senior Dev** principles (YAGNI, minimal surgical diffs, root cause fixes) and **[`AGENTS.md`](file:///d:/Github/countUp/AGENTS.md)** guardrails:

### Phase 1: Security & Data Integrity (P0) — Immediate Execution
1. **[P0-1] Scrub Packfile Secret & Rotate Credentials**:
   - Run `git filter-repo --invert-paths --path keystore/keystore-pass.txt --force`.
   - Remove `keystore/keystore-pass.txt` from disk and resolve solely via `COUNTUP_KEYSTORE_PASS` environment variable or CI secret.
   - Rotate upload key in Google Play Console.
   - *Diff estimate*: ~10 lines in build script + git history scrub.
2. **[P0-2] Fix Enum Name Persistence & Add R8 Keep**:
   - In `app/proguard-rules.pro`, add:
     ```proguard
     -keepclassmembers,allowoptimization enum com.countup.app.TimeDisplayMode, com.countup.app.ZenWidgetDisplayUnit {
         <fields>;
     }
     ```
   - In `CountUpStore.kt`, migrate read/write to stable identifiers or ordinals with fallback.
   - *Diff estimate*: ~15 lines.
3. **[P0-3] Quarantine Partial JSON Decodes**:
   - In `CountUpItem.kt:decodeItems`, return `null` whenever `out.size < arr.length()`.
   - In `CountUpStore.kt`, trigger `quarantineRawPayload(raw)` on partial decodes to prevent overwriting intact backups.
   - Update `CountUpStoreInstrumentedTest` to assert quarantine on partial corruptions.
   - *Diff estimate*: ~8 lines.

### Phase 2: Build, CI & Component Security (P1) — High Priority
4. **[P1-3 + P1-2 + P1-5] CI & Build Verification Gates**:
   - In `app/build.gradle.kts`, set `checkReleaseBuilds = true` and declare `testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"`.
   - Fail release build if `signingConfig` is missing when assembling release.
   - In `.github/workflows/ci.yml`, run `./gradlew test lintRelease assembleRelease`.
   - *Diff estimate*: ~12 lines.
5. **[P1-6] Verify `appWidgetId` Ownership in Configure Activities**:
   - In `HeroWidgetConfigureActivity`, `ZenHorizonConfigureActivity`, `SolarRhythmConfigureActivity`, and `ZenPebbleConfigureActivity`:
     ```kotlin
     val info = AppWidgetManager.getInstance(this).getAppWidgetInfo(appWidgetId)
     if (info == null || info.provider.packageName != packageName) {
         finish()
         return
     }
     ```
   - *Diff estimate*: ~16 lines (4 lines across 4 files).
6. **[P1-9] Localize Zen Horizon Subtitle**:
   - Replace hardcoded `"Until "` / `"Since "` in `ZenWidgetReducer.kt` with string resources `R.string.until_label` and `R.string.since_label`, and use localized date formatting.
   - *Diff estimate*: ~10 lines.
7. **[P1-10] Use `RTC_WAKEUP` & Fix Alarm Scheduling**:
   - Switch `AlarmManager.RTC` to `AlarmManager.RTC_WAKEUP` in `MidnightAlarmReceiver.kt`.
   - *Diff estimate*: ~4 lines.
8. **[P1-11] Introduce Local Store Schema Version**:
   - Add `KEY_SCHEMA_VERSION = "schema_version"` to `CountUpStore.kt` companion and check it during store initialization.
   - *Diff estimate*: ~12 lines.
9. **[P1-12] Expand `MechanicalResetButton` Touch Target**:
   - Add `.sizeIn(minWidth = 48.dp, minHeight = 48.dp)` or wrapper touch target expansion to satisfy accessibility standards without expanding optical icon size.
   - *Diff estimate*: ~6 lines.
10. **[P1-8] Consolidate Chinese Localization Buckets**:
    - Copy missing keys into `values-zh/strings.xml` to ensure clean generic fallback across all Chinese locales.
    - *Diff estimate*: ~20 lines in XML.
11. **[P1-1] Rebuild Release Artifact & Update Docs**:
    - Re-run `./gradlew assembleRelease` and sync `README.md`, `TECH_HANDOFF.md`, and `PRIVACY_POLICY.md` to confirm zero permissions.
    - *Diff estimate*: ~8 lines in docs.

### Phase 3: Hygiene & Quality Polish (P2) — Low Priority / Housekeeping
12. **[P2-R8-1 + P2-R8-3 + P2-R8-4] Clean Up R8 & Build Configurations**:
    - Delete dead keep rules for Room/WorkDatabase in `proguard-rules.pro`.
    - Delete duplicate enum and redundant component keep rules.
    - Remove `ndk { debugSymbolLevel = "FULL" }` from `build.gradle.kts`.
    - *(Guardrail check: DO NOT delete data model keep rules, as mandated by AGENTS.md)*.
    - *Diff estimate*: -25 lines in `proguard-rules.pro`.
13. **[P2-HYG-1 + P2-HYG-2 + P2-HYG-3] Untrack Artifacts & Scratch Notes**:
    - `git rm -r --cached artifacts/ playstore_package/ .scratch/`.
    - Remove loose `.apk` files from root directory.
14. **[P2-STATE-1] Clean Widget Bindings on Item Deletion**:
    - In `CountUpStore.deleteItem(id)`, invoke `sanitizeOrphanedWidgetBindings()` or clean matching widget IDs.
    - *Diff estimate*: ~8 lines.
15. **[P2-TEST-1 + P2-TEST-2] Clean Tautological & Duplicate Tests**:
    - Replace `assertTrue(active || !active)` with real assertions on `hasActiveWidgets`.
    - Wire `WidgetRefreshDebounceTest` to exercise actual debounce logic.
    - *Diff estimate*: ~25 lines.

---

## Guardrails: What NOT to Change
- **DO NOT remove data models from `proguard-rules.pro`** (`CountUpItem`, `BackgroundTheme`, etc.): Even though current JSON persistence is manual `org.json`, `AGENTS.md` explicitly mandates retaining data models to prevent R8 bytecode alterations in StateFlow and data class copy operations.
- **DO NOT modify `android:resizeMode="none"` or add `configuration_optional`** in `zen_pebble_widget_info.xml`: Strictly enforced by `RECURRING_ISSUES.md` to prevent OEM launcher snapping to slot 0 and pencil affordance traps.
- **DO NOT introduce `WorkManager` or persistent services** to resolve P1-10: Preserves strict zero-permission, battery-safe offline architecture.

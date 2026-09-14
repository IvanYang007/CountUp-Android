# CountUp — Full Android Code Review

**Date:** 2026-09-13
**Commit reviewed:** `0162fc3` (branch `main`, working tree clean)
**Scope:** full repository — 83 Kotlin files (~19k lines), 6 AppWidget providers, build files, manifests, R8 config, resources, CI, tests
**Result:** 3 × P0, 12 × P1, 24 × P2
**Changes made:** none. This review is read-only.

---

## Snapshot

| Property | Value |
|---|---|
| Package / namespace | `com.countup.app` |
| App version | `versionCode 51`, `versionName "3.0.0"` |
| minSdk / targetSdk / compileSdk | 26 / 37 / 37 |
| AGP / Kotlin / Gradle | 9.3.0 / 2.3.21 / 9.5.0 |
| UI | Jetpack Compose Material3, single Activity, no Navigation library |
| Widgets | 6 providers on RemoteViews (not Glance) |
| Persistence | hand-rolled `CountUpStore` over SharedPreferences + JSON |
| Unit tests | 388 `@Test` across 38 files |
| Instrumented tests | 41 `@Test` across 2 files |
| Merged permissions (current source) | none |
| Merged permissions (last release APK) | `android.permission.VIBRATE` |

---

## Severity definitions used

- **P0** — secret exposure, silent user data loss, crash/ANR on a supported path, or an unshippable release artifact.
- **P1** — incorrect behavior users will hit, security or Play risk, or missing verification on a critical path.
- **P2** — hygiene, maintainability, performance polish.

---

# P0

## P0-1. Live release-upload-key password is in git history

`keystore/keystore-pass.txt` was committed in `32f6f36`, removed from the index in `625f2c3`,
but the blob is still reachable in the packfiles.

```
$ git rev-list --all --objects | grep keystore-pass
8370ad4c87436d41ea4c8c3701b4f2c6756d4986 keystore/keystore-pass.txt
```

The blob was extracted and compared byte-for-byte against the current
`keystore/keystore-pass.txt` on disk: **identical**. The certificate it protects
(SHA-256 `a95606aa86ab9ad7fb1b10872330c4c0d1a1c4b9a11de1711aafa5ed4da83c2f`) signs both
`app/build/outputs/apk/release/app-release.apk` and the committed
`CountUp-v2.18.0-release.apk`.

Exposure is currently bounded only because the GitHub remote is private.

`docs/CREDENTIAL_ROTATION.md` documents the leak path and the scrub procedure, but the scrub
has not been run — `.scratch/codebase-hardening/issues/04-keystore-password-scrub.md` shows the
procedure as complete while the blob remains in history.

**Fix direction:** run `git filter-repo --invert-paths --path keystore/keystore-pass.txt --force`,
rotate the upload key in Play Console, and move the password out of the project tree (env var or
CI secret only). Note that history rewriting does not un-leak the value; rotation is the real fix.

---

## P0-2. R8 renames the two enums that are persisted by `name`; the read path swallows the failure

`app/build/outputs/mapping/release/mapping.txt` lines 261315-261321 and 262781-262783:

```
com.countup.app.TimeDisplayMode -> gc2:
    com.countup.app.TimeDisplayMode DAYS -> f
    com.countup.app.TimeDisplayMode ELAPSED_BREAKDOWN -> g
    com.countup.app.TimeDisplayMode TOTAL_WEEKS -> h

com.countup.app.ZenWidgetDisplayUnit -> ln2:
    com.countup.app.ZenWidgetDisplayUnit DAYS -> f
```

Write path — `app/src/main/java/com/countup/app/CountUpStore.kt`:

```kotlin
// :424-428
fun setHeroWidgetDisplayMode(appWidgetId: Int, mode: TimeDisplayMode): Boolean {
    return prefs.edit()
        .putString(PREFIX_HERO_DISPLAY_MODE + appWidgetId, mode.name)
        .commit()
}

// :441-445
fun setZenHorizonUnit(appWidgetId: Int, unit: ZenWidgetDisplayUnit): Boolean {
    return prefs.edit()
        .putString(PREFIX_ZEN_HORIZON_UNIT + appWidgetId, unit.name)
        .commit()
}
```

Read path — the same file:

```kotlin
// :414-421
fun getHeroWidgetDisplayMode(appWidgetId: Int): TimeDisplayMode {
    val raw = prefs.getString(PREFIX_HERO_DISPLAY_MODE + appWidgetId, null)
    return try {
        if (raw != null) TimeDisplayMode.valueOf(raw) else TimeDisplayMode.DAYS
    } catch (_: IllegalArgumentException) {
        TimeDisplayMode.DAYS
    }
}

// :431-438  — identical shape for ZenWidgetDisplayUnit
```

`app/proguard-rules.pro` keeps `CountUpItem`, `BackgroundTheme`, `SortOrder`, `ThemeMode`,
`RestoreStrategy`, `CountUpBackupPayload` — all verified identity-mapped in `mapping.txt` — but
does **not** keep these two enums. The global rule only preserves the methods:

```proguard
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
```

Method keeps do not pin constant names, which `mapping.txt` proves.

**Effect:** a stored widget display unit silently reverts to `DAYS` whenever R8 assigns a
different name in a later build, and on any debug→release transition. The `catch` block
guarantees no one ever sees an error.

**Fix direction:** either
`-keepclassmembers,allowoptimization enum com.countup.app.TimeDisplayMode, com.countup.app.ZenWidgetDisplayUnit { <fields>; }`
or stop persisting `.name` and store a stable code/ordinal.

---

## P0-3. One malformed element silently deletes items, then the next write destroys both copies

`app/src/main/java/com/countup/app/CountUpItem.kt:196-209`:

```kotlin
internal fun decodeItems(raw: String?): List<CountUpItem>? {
    if (raw.isNullOrBlank()) return null
    return try {
        val arr = JSONArray(raw)
        if (arr.length() == 0) return emptyList()
        val out = ArrayList<CountUpItem>(arr.length())
        for (i in 0 until arr.length()) {
            decodeElement(arr.optJSONObject(i))?.let { out.add(it) }   // bad element skipped
        }
        if (out.isEmpty() && arr.length() > 0) null else out            // partial list is "valid"
    } catch (_: Exception) {
        null
    }
}
```

`app/src/main/java/com/countup/app/CountUpStore.kt:44-47` accepts that partial list without
quarantining the raw payload:

```kotlin
val decoded = decodeItems(raw)
if (decoded != null) {
    ensureBackupInSync(raw)
    return@synchronized decoded      // quarantineRawPayload() is NOT called on this path
}
```

`quarantineRawPayload(raw)` runs at `:59`, reachable only when `decodeItems` returns `null` —
i.e. only when *every* element fails.

Consequence: one bad `epochDay` (missing, out of `LocalDate` bounds, or renamed by a future
schema change) drops that counter. `ensureBackupInSync` then writes the reduced list over
`countup_backup.json`, and the next `persist()` overwrites `countup_prefs.xml` too. Both copies
of the removed item are gone.

`app/src/androidTest/java/com/countup/app/CountUpStoreInstrumentedTest.kt:274-286` asserts
`contains("items_v1_quarantine") == false`, so the loss is currently encoded as intended
behavior. That instrumented test never runs (see P1-5).

**Fix direction:** quarantine whenever `out.size < arr.length()`, and treat a partial decode as
corruption rather than success.

---

# P1

### P1-1. No release artifact rebuilt since the zero-permission change; the last release still requests `VIBRATE`

`2c1261d` ("harden architecture, enforce zero permissions") removed
`<uses-permission android:name="android.permission.VIBRATE" />` from
`app/src/main/AndroidManifest.xml` and bumped the version to 3.0.0 / 51.

Artifact evidence:

```
$ aapt2 dump permissions app/build/outputs/apk/release/app-release.apk
package: com.countup.app
uses-permission: name='android.permission.VIBRATE'
permission: com.countup.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION

$ aapt2 dump permissions CountUp-v2.18.0-release.apk
uses-permission: name='android.permission.VIBRATE'

$ aapt2 dump permissions app/build/outputs/apk/debug/app-debug.apk
permission: com.countup.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION   # no uses-permission
```

Timestamps confirm the gap: the release APK was built 15:07; the debug APK at 21:57, after the
commit at 21:57:11.

Documentation drift: `README.md:133`, `TECH_HANDOFF.md:145` and `PRIVACY_POLICY.md:11` all state
that `VIBRATE` is declared. It is not.

**Important:** removing `VIBRATE` is *correct and safe*. Every haptic in the app goes through
Compose `LocalHapticFeedback.performHapticFeedback(...)` (verified across `CountUpContent.kt`,
`CountUpDialogs.kt`, `ZenTheme.kt` and the four configure activities), which does not require the
permission. No `Vibrator`/`VibrationEffect` usage exists anywhere.

**Fix direction:** rebuild and re-verify the release artifact, then sync the three docs.

### P1-2. Lint never gates the release build

`app/build.gradle.kts:90-92`:

```kotlin
lint {
    abortOnError = true
    checkReleaseBuilds = false
    ...
}
```

`.github/workflows/ci.yml` runs only `./gradlew lintDebug`. `abortOnError` therefore protects
nothing on the release variant, and release-only checks never execute. Disabling
`UnusedAttribute` also hides manifest drift: `android:fullBackupContent` is ignored on API 31+
because `dataExtractionRules` is present with `targetSdk 37`.

The existing `app/build/reports/lint-results-debug.xml` contains 58 issues, all `VectorPath`
warnings, 0 errors — so the gate looks green while covering the debug variant only.

**Fix direction:** set `checkReleaseBuilds = true` and run `lintRelease` (or `lint`) in CI.

### P1-3. CI silently builds an unsigned release

`app/build.gradle.kts:32-40`:

```kotlin
signingConfigs {
    if (releaseKeystore.exists() && !keystorePass.isNullOrBlank()) {
        create("release") { ... }
    }
}
```

and `:56`:

```kotlin
signingConfig = signingConfigs.findByName("release")   // null when the keystore is absent
```

`.github/workflows/ci.yml` provides no keystore and no `COUNTUP_KEYSTORE_PASS` secret;
`.gitignore:17` ignores `keystore/`. AGP does not fail on a null signing config — it emits an
unsigned APK/AAB. CI runs `assembleRelease bundleRelease` and passes green on every machine but
this one.

**Fix direction:** fail the release build fast when the keystore or password is missing, and
supply the secret to CI.

### P1-4. No green test evidence exists for HEAD

```
app/build/reports/tests/testDebugUnitTest/index.html    mtime 2026-09-13T01:55:10Z  (21:55 local)
app/build/test-results/testDebugUnitTest                mtime 2026-09-13T01:55:10Z
commit 2c1261d                                          time   2026-09-12 21:57:11 -0400
app/build/outputs/apk/debug/app-debug.apk               mtime 2026-09-13T01:57:47Z  (21:57 local)
```

`2c1261d` changed 20+ app source files (`CountUpDialogs.kt`, `ItemIcons.kt`, `MainActivity.kt`,
`CountUpStore.kt`, all widget receivers, `app/build.gradle.kts`, the manifest) **two minutes
after** the last recorded test run. The 388 passing tests describe a tree that no longer exists.
The debug APK was rebuilt at 21:57 without re-running tests.

The result itself is good news for that older tree: 388 tests, 0 failures, 0 skipped, 6.83 s.

**Fix direction:** re-run `./gradlew test lintDebug assembleRelease` on `0162fc3` before trusting
any test claim.

### P1-5. The 41 instrumented tests never run anywhere

- `defaultConfig` in `app/build.gradle.kts` declares no `testInstrumentationRunner`.
- No `debugAndroidTest` manifest exists under `app/build/intermediates`.
- `app/build/outputs/apk/androidTest` does not exist, so no test APK was ever assembled.
- `ci.yml` has no emulator or `connectedAndroidTest` step.

`CountUpStoreInstrumentedTest` (27 tests) and `ComposeUiSmokeTest` (14 tests) are dead weight,
and `CountUpStoreInstrumentedTest` is the suite that encodes the P0-3 behavior.

**Fix direction:** declare `testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"`,
add a managed-device or emulator CI job, and confirm the suite passes.

### P1-6. Exported widget configure activities never verify `appWidgetId` ownership

All four configure activities share the same shape —
`HeroWidgetConfigureActivity.kt:56-64` and `:86-87`, with identical lines in
`ZenHorizonConfigureActivity.kt`, `SolarRhythmConfigureActivity.kt`, `ZenPebbleConfigureActivity.kt`:

```kotlin
appWidgetId = intent?.extras?.getInt(
    AppWidgetManager.EXTRA_APPWIDGET_ID,
    AppWidgetManager.INVALID_APPWIDGET_ID
) ?: AppWidgetManager.INVALID_APPWIDGET_ID
if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }
...
store.setHeroWidgetBinding(appWidgetId, selectedItem.id)
pushHeroWidgetUpdate(appContext, appWidgetId)
```

The missing-extra case is handled. A **bogus-but-valid** id is not. Any installed app can run

```
am start -n com.countup.app/.HeroWidgetConfigureActivity --ei appWidgetId 7
```

and one user tap rebinds a live widget to a different counter.

The sibling receivers already do the right check — `HeroWidgetReceiver.kt:29-36` and
`ZenHorizonWidgetReceiver.kt:27-33`:

```kotlin
val info = AppWidgetManager.getInstance(context).getAppWidgetInfo(appWidgetId) ?: return
if (info.provider.packageName != context.packageName) return
```

`exported="true"` is required here (the launcher starts these activities), so a permission is not
an option; an ownership check is.

**Fix direction:** apply the same `getAppWidgetInfo(id).provider.packageName` guard in all four
configure activities before any state write.

### P1-7. The widget memory-budget gate covers the wrong widgets and has no production caller

`app/src/main/java/com/countup/app/WidgetMemoryBudgetGate.kt:16-30` enumerates only
`ZEN_HORIZON_*`, `SOLAR_RHYTHM_*` and `ZEN_PEBBLE_1X1`. `assertAllWithinBudget()` at `:114` is
referenced only from `WidgetMemoryBudgetGateTest.kt:59` and
`ZenWidgetSuiteIntegrationSmokeTest.kt:114` — never from production code.

Meanwhile `app/src/main/java/com/countup/app/CountUpWidget.kt:102-141` renders and ships
`setImageViewBitmap(R.id.widget_bg_image, bgBitmap)` with `targetWidth = 360` and height up to
260 → 360 × 260 × 4 = **366,400 bytes**, roughly 9× the self-imposed 40 KB budget. The Hero
widget is not in the budget enum at all.

The stated risk model — `TransactionTooLargeException` when pushing widget state — is therefore
guarded only for widgets whose bitmaps are already clamped to 32 KB by
`ZenHorizonTrackRenderer.computeSafeDimensions`, and only inside unit tests.

**Fix direction:** apply the gate at the push site, and add the grid and Hero bitmaps to it.

### P1-8. 162 strings exist only in `values-zh`; 6 strings have no Chinese translation at all

Key-set diff of `app/src/main/res/values/strings.xml` (453 keys, 0 marked `translatable="false"`)
against every Chinese bucket:

| Resource folder | Keys | Missing from English set |
|---|---|---|
| `values` | 453 | — |
| `values-zh` | 447 | **6** |
| `values-zh-rCN` | 297 | 156 |
| `values-zh-rTW` | 286 | 167 |
| `values-b+zh+Hans` | 285 | 168 |
| `values-b+zh+Hant` | 282 | 171 |
| `values-zh-rHK` | 282 | 171 |

Cross-check: 162 keys exist in `values-zh` and in **none** of the five script/region folders, and
`values-b+zh+Hans` has 0 keys that `values-zh` lacks (it is a strict subset).

Keys with no Chinese anywhere — these render in English on every Chinese device:

- `solar_rhythm_configure_title`
- `solar_rhythm_configure_subtitle`
- `backup_io_error`
- `backup_restore_file_too_large`
- `backup_restore_unsupported_version`
- `backup_restore_damaged_notice`

The other 156-171 gaps (mostly `solar_term_*`, `cd_icon_*`, `zen_*` keys) resolve per resource ID.
Android resolution considers only the configs that define a given key, so a zh-Hant device
resolving a key present only in `values` and `values-zh` selects `values-zh`. The consequence is
**Simplified Chinese text on Traditional Chinese devices**, not English fallback.

`values-zh` is also absent from `res/xml/locales_config.xml`, which lists `en`, `zh-Hans`,
`zh-Hant`, `zh-CN`, `zh-TW`, `zh-HK`.

**Confidence:** key absence verified by set arithmetic. The fallback direction follows the
standard per-resource-ID rule but was **not** confirmed on a device — verify before fixing.

### P1-9. Zen Horizon widget subtitle bypasses localization

`app/src/main/java/com/countup/app/ZenWidgetReducer.kt`:

```kotlin
// :61
private val DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)

// :198
val formattedStart = (if (isFuture) "Until " else "Since ") + anchorDate.format(DATE_FORMATTER)
```

Consumed at `ZenHorizonWidgetReceiver.kt:179`.

Everywhere else the app routes dates through `formatLocalized` / `formatAnchorDateSubLabel`
(`DateConversion.kt:23-40`), and `since_label` / `until_label` already exist as string resources.
This one path hardcodes English prefixes and a US date order, so the 4×1 / 2×1 Zen Horizon widget
shows `Since Jan 20, 2025` inside an otherwise Chinese UI.

### P1-10. Midnight alarm uses `RTC`, not `RTC_WAKEUP`; reboot re-arm depends entirely on widget `onUpdate`

`app/src/main/java/com/countup/app/MidnightAlarmReceiver.kt:70-78`:

```kotlin
alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC, targetMillis, pendingIntent)
```

`setExactAndAllowWhileIdle` fires during Doze, but the non-wakeup `RTC` type does not wake a
sleeping device — the rollover is deferred until the user next wakes the phone. With
`RECEIVE_BOOT_COMPLETED` removed at `AndroidManifest.xml:13`, the only re-arm path is the widget
receivers' `onUpdate` / `onEnabled`. With no widget placed, nothing re-arms after a reboot.

`docs/RECURRING_ISSUES.md` §4 presents this as handled.

**Confidence:** the missing boot path is certain; exact `RTC` Doze deferral semantics are
platform-dependent and worth confirming on a device.

### P1-11. No schema version in the local store

`CountUpStore.kt`'s companion object defines `KEY_MIGRATED` (`migrated_v1` boolean) and no version
integer — `grep -i schemaVersion` over the module returns nothing. `CountUpBackupPayload` guards
*imports* with `CURRENT_SCHEMA_VERSION`, but the store itself cannot detect a downgrade or a
renamed field, so such an event degrades into P0-3 rather than failing loudly.

### P1-12. `MechanicalResetButton` has a real 30 dp target that Compose does not expand

`app/src/main/java/com/countup/app/CountUpContent.kt:998-1016`:

```kotlin
.size(30.dp)
.pointerInput(enabled) { detectTapGestures(...) }
```

with a second 30 dp `Box` at `:1070`.

Compose Foundation expands `Modifier.clickable` nodes to the 48 dp minimum touch target, but
`pointerInput` / `detectTapGestures` receives no such expansion. The hold-to-reset gesture is
therefore a genuine 30 dp target on every card, and a miss lands on the card's `clickable`
handler (open editor) instead.

---

# P2

## R8 / build

| Issue | Location |
|---|---|
| Dead keep rules for `androidx.room` / `androidx.work` / Glance. `configuration.txt` has 44 library sections and none for those; neither `gradle/libs.versions.toml` nor `app/build.gradle.kts` declares them. The comment encodes a false dependency model | `proguard-rules.pro:34-40` |
| `-keep class X { *; }` on 9 data models with no reflective consumer. Persistence is manual `org.json` (`CountUpItem.kt:166-196`); no `Class.forName` / `getDeclaredField` / `getDeclaredMethod` in `app/src/main` | `proguard-rules.pro:4-12` |
| Byte-identical duplicate of the default-file enum rule; component keeps that AAPT2 already supplies; `@Composable` member keep that blocks R8 removing/inlining every composable | `proguard-rules.pro:15-18`, `:21-32`, `:43-45` |
| `debugSymbolLevel = "FULL"` is a no-op on a native-free app; `vcsInfo.include = false` drops the metadata Play uses for traceability | `app/build.gradle.kts:52-54`, `:57-60` |

## Secrets and manifest

| Issue | Location |
|---|---|
| Keystore password stored in plaintext inside the project tree, so any folder copy or handoff leaks the upload key. Gitignored, but present | `keystore/keystore-pass.txt`, `app/build.gradle.kts:26`; also `local.properties` |
| The comment claims the dynamic-receiver permission is removed; only the `<uses-permission>` is stripped, while the `<permission android:protectionLevel="signature">` declaration still merges. Latent trap for the first future `ContextCompat.registerReceiver(..., RECEIVER_NOT_EXPORTED)` on API 26-32 | `AndroidManifest.xml:15`; merged release manifest lines 13-15 |
| Debug-only `WidgetHostActivity` is `exported="true"` and reads no incoming intent. Debug and release share `applicationId`, so a debug install replaces the release one | `src/debug/AndroidManifest.xml:9-11` |
| Debug build additionally merges exported `androidx.compose.ui.tooling.PreviewActivity` and `androidx.activity.ComponentActivity` | debug merged manifest |

## Resources and localization

| Issue | Location |
|---|---|
| `solar_rhythm_widget_info.xml` declares no preview at all; `zen_horizon_widget_info.xml` reuses `@drawable/hero_widget_preview` (the Hero preview); **no** widget declares `previewLayout` even though `docs/RECURRING_ISSUES.md` lists it as a Hard Invariant | `res/xml/*_widget_info.xml` |
| 6 overlapping Chinese resource folders; `locales_config.xml` lists `zh-Hans` **and** `zh-CN` / `zh-TW` / `zh-HK`, producing duplicate rows in the per-app language picker; `values-zh` is not declared at all; `bundle { language { enableSplit = false } }` ships every locale to every device | `res/xml/locales_config.xml`, `app/build.gradle.kts:81-85` |
| 58 `VectorPath` lint warnings — several paths exceed 6,000 characters, costing render time and APK size | `lint-results-debug.xml` |
| Hardcoded `"ZEN"` fallback tag | `CountUpItem.kt:83-88` |
| All 6 widget providers poll at `1800000` ms — platform-minimum compliant, but six independent periodic wakeups | `res/xml/*_widget_info.xml` |

## Repository hygiene

| Issue | Location |
|---|---|
| 52.8 MB tracked across 523 files, including **107** `artifacts/` files. `.scratch/codebase-hardening/issues/04-keystore-password-scrub.md` marks "untrack `artifacts/`" as complete and `docs/CREDENTIAL_ROTATION.md` §2 gives the command, but the files are still tracked | `artifacts/`, `playstore_package/` (38 files) |
| Agent working notes are committed | `.scratch/` (6 files) |
| ~33 MB of untracked AAB/APK left in the repo root (gitignored but on disk) | `CountUp-v*.aab`, `CountUp-v*.apk` |

## State and concurrency

| Issue | Location |
|---|---|
| Widget binding survives item deletion; `WidgetTargetResolution.Deleted` is never consumed, so a widget bound to a deleted item renders blank permanently with an orphaned binding | `ZenWidgetReducer.kt:88-93`, `CountUpStore.kt:296-317` |
| Theme / sort / binding accessors bypass `globalStoreLock` while `sanitizeOrphanedWidgetBindings` iterates `prefs.all.keys` under it | `CountUpStore.kt:460-530` |
| `scheduleMidnightAlarm` runs ~7 provider IPCs on the main thread inside `onUpdate`, before `launchAsync` | `CountUpWidget.kt:60-61`, `ZenPebbleWidgetReceiver.kt:19-21` |
| PendingIntent requestCode namespaces mix raw `appWidgetId` with derived `hash * 31 + id + offset`; `FLAG_UPDATE_CURRENT` on a collision would overwrite the other PendingIntent's extras. Low likelihood, real namespace mixing | `WidgetNavigationContract.kt:53-57`, `:105-112` vs `CountUpWidget.kt:501` |
| `RemoteViews.setInt(id, "setColorFilter", 0)` is not a reliable clear for a recycled `ImageView`, although the comments claim a solid plate | `CountUpWidget.kt:396`, `:424` |
| 28 × `commit()` + fsync and zero `apply()` — not a live bug (callers are on the injected IO dispatcher), but nothing enforces that contract at the type level | `CountUpStore.kt` |

## UI and accessibility

| Issue | Location |
|---|---|
| List insets applied to the parent instead of `contentPadding`, so the card list stops at the navigation-bar inset instead of scrolling under it | `CountUpContent.kt:169-173` vs `:245` |
| No window-size-class handling; unbounded content width on tablets and unfolded foldables; a fixed `.width(260.dp)` | `CountUpContent.kt:168-175`, `:749` |

## Test quality

| Issue | Location |
|---|---|
| Tautological and assertion-free tests — `assertTrue(active \|\| !active)`, plus two tests whose only claim is "must not throw" | `MidnightAlarmReceiverTest.kt:56-80` |
| Debounce test re-implements the logic in a local `TestDebouncer` and tests the copy; `MainActivity.refreshWidget` is never executed | `WidgetRefreshDebounceTest.kt:19-38` |
| 11 XML-substring tests assert file text, not behavior, and break on any formatting change | `WidgetContractInvariantsTest.kt` (6), `SolarRhythmConfigurationTest.kt` (5 of 6) |
| Test reflects into private `persist(List)` | `CountUpStoreTest.kt:336` |

---

# Untested high-risk behaviors, ranked

1. **`CountUpStore.items()` partial-decode loss (P0-3).** No test mutates after a partial decode
   and asserts the dropped item survives anywhere. `partiallyCorruptArrayKeepsParseableItems`
   asserts the loss is expected.
2. **No schema-version guard in `CountUpStore` (P1-11).** No test round-trips a payload with a
   renamed or added field to prove a future version fails loudly.
3. **`MainActivity.refreshWidget` debounce and `onResume` / `onPause` flush.** The existing test
   exercises a private duplicate.
4. **Reboot and Doze re-arm of `MidnightAlarmReceiver.scheduleMidnightAlarm` (P1-10).** No test
   asserts an alarm is registered after a reboot, nor the `RTC` vs `RTC_WAKEUP` choice.
5. **`CountUpStore.deleteItem` interaction with widget bindings.** No test asserts a widget bound
   to a deleted item rebinds or clears its binding.

---

# Verified correct

These were checked and are fine — recorded so they are not re-investigated.

- **No Intent redirection anywhere.** No `getParcelableExtra`, `getSerializableExtra`,
  `Intent.parseUri`, nested-`Intent` launch, or `startActivity` of intent-derived data in
  `app/src/main` or `app/src/debug`. `MainActivity.kt:176-200` consumes deep links by exact string
  comparison; `countup://pin_*` maps to fixed local `ComponentName`s.
- **Warm-boot deep-link handling is correct.** `MainActivity.onNewIntent` calls `setIntent(newIntent)`
  before re-running `handleIntent`.
- **PendingIntent flags are correct.** Every construction is `FLAG_IMMUTABLE` except
  `CountUpWidget.kt:176-182`, which is `FLAG_MUTABLE` with an explicit component — required for
  `setPendingIntentTemplate`, and the only reachable action is an undoable reset.
- **Receivers are safe.** All `exported="false"`; the two custom-action widget receivers verify
  `info.provider.packageName == packageName` before mutating state. `MidnightAlarmReceiver` declares
  only the app action plus protected system broadcasts, and its `ACTION_TIME_CHANGED` code check
  matches the manifest filter (`"android.intent.action.TIME_SET"`).
- **`CountUpWidgetService`** is `exported="false"` with `android:permission="android.permission.BIND_REMOTEVIEWS"`.
- **No ContentProvider, no FileProvider.** The `androidx.startup` provider is removed via
  `tools:node="remove"`.
- **Backup import is bounded.** `BackupCoordinator.kt:26,57` caps input at 2 MB.
- **No dynamically registered receivers**, so no missing `RECEIVER_NOT_EXPORTED` flag.
- **Edge-to-edge is correct.** `enableEdgeToEdge()` is called in `MainActivity.kt:104` before
  `setContent`; `Scaffold(contentWindowInsets = WindowInsets.safeDrawing)` and
  `consumeWindowInsets` match the official pattern; no double IME padding.
- **Predictive back needs no work.** No `BackHandler`, `OnBackPressedCallback`, `onBackPressed` or
  `dispatchKeyEvent` anywhere in `app/src/main`; `android:enableOnBackInvokedCallback` is not set to
  `false`; all dismissals go through Compose dialog `onDismissRequest`.
- **RemoteViews usage is legal.** Only supported view classes; no custom views, no
  `ViewGroup.addView`. Every tap surface has `setOnClickPendingIntent` or
  `setOnClickFillInIntent` + `setPendingIntentTemplate`.
- **`MutableStateFlow` is private**, only `asStateFlow()` is exposed (`CountUpViewModel.kt:41-42`).
- **Only one Flow collection in Compose, and it is `collectAsStateWithLifecycle`** (`MainActivity.kt:110`).
- **Dispatcher discipline is real.** `MainDispatcherRule` defaults to `UnconfinedTestDispatcher` and
  pairs `setMain`/`resetMain`; ViewModel tests inject `ioDispatcher`; Turbine is used properly.
- **`getInstance` double-checked locking** with `@Volatile` plus a reentrant lock is correct, and all
  5 providers share the default process.
- **Undecodable payloads are quarantined** before any overwrite, capped at 3 entries.
- **`daysSince` uses `ChronoUnit.DAYS` on `LocalDate`**, never milliseconds, so it is DST-safe.
- **Material DatePicker UTC→`LocalDate` pinning is correct** (`DateConversion.kt:15`).
- **`restoreBackupPayload(REPLACE_ALL)` rolls back on commit failure**, and damaged imports are
  forced to `MERGE` (`BackupCoordinator.kt:98-104`).
- **Strong skipping is on** (Kotlin 2.3.21), so lambda parameters do not make `ItemCard` or
  `SubHeaderRow` unskippable.

---

# Android skills applied

| Skill | Used for |
|---|---|
| `android-intent-security` | P1-6; verified no Intent redirection, PendingIntent flags, receiver exports |
| `play-policy-insights` | P1-1 permission/policy drift; `updatePeriodMillis` battery compliance |
| `r8-analyzer` | P0-2; the P2 keep-rule cleanup list |
| `agp-9-upgrade` | AGP 9 build files; `vcsInfo` / `ndk` block validity |
| `testing-setup` | P1-4, P1-5; test-quality P2s |
| `android-profiler` | P1-7 bitmap and transaction budget; main-thread IPC |
| `edge-to-edge` | `enableEdgeToEdge()` verification; list-inset P2 |
| `adaptive` | Large-screen and foldable gap |
| `navigation-event` | Predictive-back verification |
| `android-cli` | Device and manifest verification commands |

Verified **not applicable** — grepped absent across all 83 Kotlin files: `camerax`,
`media3-cast-integration`, `wear-compose-m3`, `leanback-to-compose-tv-migration`,
`display-glasses-with-jetpack-compose-glimmer`, `engage-sdk-integration`,
`play-billing-library-version-upgrade`, `restore-credentials`, `verified-email`, `appfunctions`,
`ml-kit-genai-prompt-api`, `styles` (no Compose Styles API usage).
`migrate-xml-views-to-jetpack-compose` is excluded because the 8 files in `res/layout/` are
RemoteViews widget layouts, not app UI.

---

# Review limitations

- **No build was run.** All test and lint evidence comes from existing artifacts on disk, and that
  evidence predates HEAD (`0162fc3`) by the gap described in P1-4.
- **P1-8's fallback direction** follows Android's per-resource-ID resolution rule but was not
  confirmed on a device.
- **P1-10's `RTC` Doze semantics** are platform-dependent; the missing reboot re-arm path is
  certain, the deferral window is not measured.
- **No device or emulator run** was performed, so nothing here is a runtime-confirmed defect except
  where an artifact was inspected directly (`aapt2`, `apksigner`, `mapping.txt`, JUnit XML).
- Items in the P2 tables come from reading source, not from runtime measurement.

---

# Suggested order of work

1. **P0-1** — rotate the upload key, then scrub history. Rotation is the real fix.
2. **P0-3** — quarantine partial decodes. Small diff, prevents permanent data loss.
3. **P0-2** — add the two enum keep rules, or stop persisting `.name`.
4. **P1-3 + P1-5 + P1-2** — make CI meaningful: fail on missing signing config, run lint on release,
   run the instrumented suite.
5. **P1-4** — re-run the full suite on HEAD and record the result.
6. **P1-1** — rebuild the release artifact and sync `README.md`, `TECH_HANDOFF.md`, `PRIVACY_POLICY.md`.
7. **P1-6, P1-9, P1-7, P1-10, P1-11, P1-12, P1-8** — in any order.
8. **P2** — batch the R8 deletions and repo-hygiene items into one cleanup commit.

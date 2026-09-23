# Project Lessons — CountUp-Android

> Derived from git history. Last analyzed commit: `f902d51c68548d424c9dd869af6d69a9bcdbe591` (2026-09-22T21:02:52-04:00).
> Range: `32f6f36211113906d4bc4f8c03e519b711b198e1` .. `f902d51c68548d424c9dd869af6d69a9bcdbe591` (205 commits, 2026-08-20 .. 2026-09-22).
> Grades: `[observed]` stated in a commit/PR, `[inferred]` deduced from diffs,
> `[weak]` one data point or ambiguous.

## Executive summary

CountUp-Android is a zero-permission, offline-first milestone count-up app built with Jetpack Compose Material 3 and eight RemoteViews home-screen widget providers (Overview Grid, Hero 2x1, Zen Horizon 4x1, Solar Rhythm 4x2, Zen Pebble 1x1, Zen Orbit 2x2, Tsukimi Moon 2x2, and Shuin Seal 1x1). Developed over four weeks across 205 commits, its history is dominated by widget configuration contracts, release minification traps, and data integrity safeguards. The highest-risk module is `app/src/main/res/xml/zen_pebble_widget_info.xml` alongside `app/src/main/java/com/countup/app/CountUpStore.kt`. The project repeatedly struggled with 1x1 widget launcher placement, oscillating between resize modes and configuration flags before locking hard invariants. The single most important constraint is the zero-permission model: no runtime permissions, no WorkManager, and no background services are permitted.

**Read this first if you are about to touch:** `app/src/main/res/xml/zen_pebble_widget_info.xml` (see Known risk areas).

## Lessons learned from past commits

### L1. Lock widget XML contracts and assert them via JVM invariant tests — [observed]

- **What happened:** Setting `android:resizeMode="horizontal|vertical"` on single-cell widgets caused Samsung One UI and Xiaomi HyperOS drop handlers to abort targeted drop coordinates and snap tiles to slot 0 (`b6b4ce0ba`, `471e4cc69`). Conversely, adding `configuration_optional` caused Android 12+ launchers to omit configuration activities, dropping unconfigured fallback widgets (`1cd6e29d1`, `433f1d47b`). Refactorings repeatedly reintroduced these launcher traps until locked by executable tests (`e60f514`, `f902d51`).
- **Evidence:** `1cd6e29d1` fix(widget): lock 1x1 pebble resizeMode to none, `433f1d47b` fix(widget): restore auto-launch picker, `b6b4ce0ba` fix(widget): fix 1x1 pebble grid slotting, `e60f514` feat(core): lock contract invariants, `f902d51` feat(widgets): prevent OEM cell clipping.
- **Why it recurs:** Standard Pixel emulator launchers tolerate resize flags and configuration omissions on single-cell widgets, masking physical OEM drag-and-drop failures.
- **The rule:** In single-cell widget info XMLs, declare `android:resizeMode="none"` and `android:widgetFeatures="reconfigurable"`; never set resize modes or add `configuration_optional`. Enforce all widget metadata contracts, ampersand escaping, and 2-line wrapping through automated JVM tests (`WidgetContractInvariantsTest.kt`).

### L2. Strip transitive InitializationProvider to prevent background service leaks — [observed]

- **What happened:** Transitive AndroidX dependencies injected `androidx.startup.InitializationProvider`, booting unauthorized WorkManager initializers and crashing release cold start.
- **Evidence:** `3c8663262` fix(release): resolve startup crash by suppressing unused WorkManagerInitializer, `625f2c387` fix(security,ui): harden credentials.
- **Why it recurs:** Modern AndroidX libraries automatically bundle startup providers into merged manifests unless explicitly stripped.
- **The rule:** Strip `androidx.startup.InitializationProvider` via `tools:node="remove"` in `AndroidManifest.xml`.

### L3. Persist stable string codes or full field keeps for enums under R8 — [observed]

- **What happened:** Persisting `TimeDisplayMode` and `ZenWidgetDisplayUnit` via `.name` failed under R8 because default enum rules only keep `values()` and `valueOf()`, renaming constants to `f` and silently resetting values to `DAYS`.
- **Evidence:** `0162fc3` (audit finding P0-2), `92262bb43` feat: harden security, persist widget bindings, refine edge-to-edge, and optimize R8.
- **Why it recurs:** Debug builds do not obfuscate enums, and read-path exception handlers swallow `IllegalArgumentException`.
- **The rule:** Store stable serialization codes or specify `-keepclassmembers,allowoptimization enum * { <fields>; }` in `proguard-rules.pro`.

### L4. Synchronize store mutations with global locks, quarantine corrupted payloads, and replace backups atomically — [observed]

- **What happened:** Concurrent updates between home-screen widget receivers and main-app viewmodels caused race conditions and partial file corruption in SharedPreferences and JSON backups (`c678dd3fd`). Partial deserialization skipped invalid JSON elements and returned truncated lists, overwriting valid backups with missing items (`92262bb43`).
- **Evidence:** `c678dd3fd` fix: harden data recovery, lock store writes, `625f2c387` fix(security,ui): harden credentials, `92262bb43` feat: harden security.
- **Why it recurs:** Widget receivers run in independent broadcast threads concurrently with UI viewmodel coroutine dispatchers.
- **The rule:** Wrap all persistence mutations in `synchronized(globalStoreLock)`. Write disk backups via temporary file replacement (`Files.move` with `REPLACE_EXISTING`), and quarantine unparseable payloads in `items_v1_quarantine` rather than overwriting valid backups.

### L5. Purge orphaned widget bindings across all families on item deletion — [observed]

- **What happened:** Deleting an item removed it from `items_v1` but left dangling preference references across `hero_widget_item_<id>`, `zen_horizon_widget_item_<id>`, and `solar_rhythm_widget_item_<id>`.
- **Evidence:** `92262bb43` feat: harden security, persist widget bindings, refine edge-to-edge, `2c00bcb` feat(release): bump versionCode to 66.
- **Why it recurs:** Widget instance bindings reside in separate preference keys from core item storage and are bypassed by simple item deletions.
- **The rule:** Invoke `purgeWidgetBindingsForItem(itemId)` across all configurable widget families inside `CountUpStore.deleteItem()`.

### L6. Restrict RemoteViews parcel payload below 40KB and clamp procedural bitmaps to 24KB/32KB — [observed]

- **What happened:** Pushing high-resolution vector assets or uncompressed tracks via `RemoteViews` risked exceeding Android's 1MB Binder transaction limit during concurrent widget updates (`227b2f372`, `82e9024d7`). Generating procedural bitmaps without allocation clamping caused `TransactionTooLargeException` under rapid updates (`d8a5df1`, `2c00bcb`).
- **Evidence:** `227b2f372` feat(widgets): implement Zen & Efficient Widget Suite, `82e9024d7` fix(review): resolve all code standards review findings, `d8a5df1` chore(release): bump versionCode to 65, `2c00bcb` feat(release): bump versionCode to 66.
- **Why it recurs:** The 1MB Binder transaction limit is shared across all inter-process communication in the system.
- **The rule:** Verify serialized RemoteViews parcels stay strictly under 40KB via `WidgetMemoryBudgetGateTest`. Clamp procedural bitmaps using `computeSafeDimensions` or `computeSafeSize` ($W \times H \times 4 \le 24\text{ KB}$ for 1x1, $\le 32\text{ KB}$ for 2x2) and adopt the 1:1.18 tall seal aspect ratio on vertical launcher grids.

### L7. Reuse canonical ItemCard across screens and omit minimumInteractiveComponentSize on compact rows — [observed]

- **What happened:** Compose `minimumInteractiveComponentSize()` expanded 26dp subheader and 30dp card buttons to 48dp, blowing out row spacing; inner column clipping truncated bottom-left `SINCE` dates (`92262bb43`). Creating bespoke picker rows for configure activities duplicated 60+ lines of card UI, broke palette theming, and substituted signature spring press physics with flat touches (`776560d`, `2c00bcb`).
- **Evidence:** `92262bb43` feat: harden security, persist widget bindings, refine edge-to-edge, `776560d` refactor(widget): harmonize ZenOrbitConfigureActivity to use canonical ItemCard, `2c00bcb` feat(release): bump versionCode to 66.
- **Why it recurs:** Jetpack Compose components enforce 48dp touch bounds by default, and developers instinctively build specialized rows for configurators instead of reusing the canonical card.
- **The rule:** Always reuse canonical `ItemCard` across all widget configure screens with no-op handlers. Standardize haptics to `TextHandleMove`, use explicit `size(26.dp)` or `size(30.dp)` on compact action clusters, and apply clipping exclusively to outer card containers.

### L8. Delegate speech recognition out-of-process to maintain zero permissions — [observed]

- **What happened:** Adding voice quick-add risked introducing `android.permission.RECORD_AUDIO` or crashing with `ActivityNotFoundException` on Android 11+ due to package filtering.
- **Evidence:** `6d9fe4f` feat(widget): implement Voice Quick Add with zero-permission speech delegation, `966928a` feat(voice): refine voice add aesthetics.
- **Why it recurs:** Developers default to in-app audio recording instead of delegating speech capture to platform intents.
- **The rule:** Maintain `<queries>` for `RecognitionService` and `RECOGNIZE_SPEECH` and delegate audio capture out-of-process via `RecognizerIntent`.

### L9. Verify calling package ownership and validate appWidgetId in configure activities — [observed]

- **What happened:** Exported configuration activities required by `APPWIDGET_CONFIGURE` could be invoked by third-party applications to spoof widget bindings or redirect intents.
- **Evidence:** `92262bb43` feat: harden security, persist widget bindings, refine edge-to-edge, `2c00bcb` feat(release): bump versionCode to 66.
- **Why it recurs:** Widget configuration activities must declare `android:exported="true"` to allow launcher invocation, exposing an external attack surface.
- **The rule:** Validate `appWidgetId != INVALID_APPWIDGET_ID`, verify caller identity against package ownership, and return results containing only `EXTRA_APPWIDGET_ID`.

### L10. Compute midnight rollover from civil dates across DST transitions and re-register in widget receivers — [observed]

- **What happened:** Adding fixed 24-hour millisecond intervals caused alarms to drift during 23-hour or 25-hour DST transitions (`5fa0136`). Without `RECEIVE_BOOT_COMPLETED`, device reboots dropped scheduled midnight alarms required for daily counter increments (`625f2c387`, `92262bb43`).
- **Evidence:** `625f2c387` fix(security,ui): harden credentials, `5fa0136` feat(reliability): remediate audit findings, harden reboot rollover, `92262bb43` feat: harden security.
- **Why it recurs:** Fixed millisecond intervals fail across daylight saving changes, and zero-permission rules prohibit listening for boot broadcasts directly.
- **The rule:** Compute next midnight using `now.toLocalDate().plusDays(1).atStartOfDay(now.zone).plusSeconds(1).toInstant().toEpochMilli()`. Re-register via `RTC_WAKEUP` in widget `onUpdate()` / `onEnabled()`, and listen for `ACTION_MY_PACKAGE_REPLACED` in the manifest to restore rollover alarms across app updates.

### L11. Remap widget bindings on restore instead of purging on startup — [observed]

- **What happened:** A startup widget sanitizer purged all binding keys whose IDs weren't registered with the local launcher, immediately wiping all user configurations and pinned cards during device-to-device restores (D2D migration) before the new launcher could assign or restore widget IDs.
- **Evidence:** `2c48806` feat(backup): platform-native auto backup & launcher widget sanitizer, `92262bb43` feat: harden security, persist widget bindings.
- **Why it recurs:** Developers mistakenly treat missing launcher IDs on startup as dead state, unaware that launcher D2D restoration delivers new IDs asynchronously via `AppWidgetProvider.onRestored()`.
- **The rule:** Never run an aggressive orphaned-binding purge on app startup. Implement `onRestored(oldIds, newIds)` on all `AppWidgetProvider` classes delegating to `CountUpStore.remapWidgetBindings`, and restrict deletion to `onDeleted()` or `deleteItem()`.

### L12. Standardize NIO atomic rename over AtomicFile for cross-platform JVM stability — [observed]

- **What happened:** Replacing custom NIO `Files.move` with `androidx.core.util.AtomicFile` broke snapshot persistence in unit tests on Windows JVMs because `java.io.File.renameTo` silently fails when the target file already exists on Windows NT.
- **Evidence:** `625f2c387` fix(security,ui): harden credentials, `8a69041` feat(release): bump version to 2.20.0, harden storage.
- **Why it recurs:** `AtomicFile` relies on POSIX `rename(2)` semantics which atomicity-replace targets on Linux/Android but fail silently on Windows.
- **The rule:** Use `FileOutputStream.fd.sync()` paired with `java.nio.file.Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING)` for robust atomic file snapshots across both Android and desktop unit test environments.

### L13. Order widthIn before fillMaxWidth and fillMaxHeight in Compose — [observed]

- **What happened:** Writing `.fillMaxSize().widthIn(max = 640.dp)` in Jetpack Compose forced the layout constraint's minimum width to the full device width, bypassing the 640dp max limit and stretching content across tablets and foldables.
- **Evidence:** `6395b8f` feat(perf): add baseline profile, graphics layer caching, adaptive width.
- **Why it recurs:** In Compose, modifier ordering determines constraint propagation; placing `fillMaxSize()` first locks incoming constraints to `(minWidth = screenWidth, maxWidth = screenWidth)`.
- **The rule:** In centered responsive layouts, apply `.widthIn(max = 640.dp)` *before* `.fillMaxWidth().fillMaxHeight()`.

### L14. Populate default strings in previewLayout and synchronize previewImage vector baselines — [observed]

- **What happened:** Adding `android:previewLayout` pointing to live layouts specifying only design-time `tools:text` caused Android 12+ launchers to render blank white boxes in the widget picker (`6395b8f`, `f21a203`). Updating live widget typography without synchronizing `android:previewImage` vector drawables left stale, shrunken previews with vast dead margins (`d99a22f`, `c94856d`).
- **Evidence:** `d99a22f` feat(preview): provide dedicated vector previewImage, `6395b8f` feat: living widget preview layouts, `f21a203` fix(widget): authentic populated preview, `c94856d` docs(lessons): update invariants test.
- **Why it recurs:** Launchers inflate `previewLayout` XML directly with zero code execution, and `previewImage` resides in separate drawable XMLs easily overlooked during refactoring.
- **The rule:** When using `android:previewLayout`, always populate layout XMLs with default localized `@string/...` text and static preview tracks across all 7 locale files. Whenever adjusting typography baselines, dash widths, or letter spacing in live widget layouts, immediately synchronize the corresponding `android:previewImage` vector drawable.

### L15. Eliminate deprecated Window color and cutout APIs under Android 15 edge-to-edge — [observed]

- **What happened:** On Android 15 (targetSdk 35+), apps are forced edge-to-edge by default. Calling `window.setStatusBarColor()`, `window.setNavigationBarColor()`, or specifying `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES` triggered Google Play Console compliance warnings and is deprecated.
- **Evidence:** `fcbddc8` fix(edge-to-edge): eliminate deprecated status/nav bar and cutout APIs for Android 15 compliance.
- **Why it recurs:** Traditional tutorial code and boilerplate templates continue to manipulate Window color bars imperatively.
- **The rule:** Never call `window.setStatusBarColor()` or `window.setNavigationBarColor()`. Set `layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS` on API 28+, `isNavigationBarContrastEnforced = false` on API 29+, and manage icon contrast via `WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars`.

### L16. Isolate RemoteViews adapter service intents with unique data URIs to avoid binder cache collisions — [observed]

- **What happened:** Binding multiple overview widget grid instances to `Intent(context, CountUpWidgetService::class.java)` caused Android's `RemoteViews` to cache a single `RemoteViewsFactory` instance across all widgets because `Intent.filterEquals()` ignores extras (`EXTRA_APPWIDGET_ID`). Toggling card styles on one widget caused other overview widgets on the home screen to display the same items or fail to update.
- **Evidence:** `e60f514` feat(core): harden backup durability, lock contract invariants, and refresh project lessons.
- **Why it recurs:** Developers mistakenly assume `putExtra(EXTRA_APPWIDGET_ID, appWidgetId)` distinguishes service intents in RemoteViews adapter bindings.
- **The rule:** Always set a unique data URI (`intent.data = Uri.parse("countup://widget/overview/$appWidgetId")`) on any service intent passed to `views.setRemoteAdapter()` when multiple widget instances display instance-specific collections.

### L17. Guard RemoteViewsFactory collection indexing against asynchronous launcher IPC races — [observed]

- **What happened:** In `RemoteViewsService.RemoteViewsFactory`, relying on direct indexing (`rows[position]`) in `getViewAt(position)` or `getItemId(position)` caused `IndexOutOfBoundsException` during concurrent list flings when counters were reset or items deleted in the background, crashing the launcher's remote view adapter.
- **Evidence:** `ddee9c5` fix(widget): guard WidgetViewsFactory out-of-bounds access and resolve RTL symmetry.
- **Why it recurs:** Developers assume `position` is always strictly bounded by the value previously returned by `getCount()`. In reality, launcher AdapterView scrolling and app SharedPreferences updates operate across asynchronous IPC boundaries; `rows` can shrink before `notifyAppWidgetViewDataChanged` finishes processing on the launcher.
- **The rule:** In all `RemoteViewsFactory` implementations, always use defensive bounds checking (`rows.getOrNull(position) ?: return RemoteViews(...)` and `rows.getOrNull(position)?.id?.hashCode()?.toLong() ?: position.toLong()`) instead of raw array indexing.

### L18. Default exclusion switches to calm OFF polarity with quiet micro-indicators — [observed]

- **What happened:** Configuring widgets via an active "Show in widget (default ON)" switch created unnecessary visual friction: every newly created card dialog showed a glaring vermilion toggle switch, conflicting with the calm Zen aesthetic and user mental models where toggles activate special behaviors rather than baseline states. Furthermore, home-screen cards lacked visual feedback for excluded items.
- **Evidence:** `908bbb3` feat(ui): add widget exclusion switch to item editor; `CountUpDialogs.kt`, `CountUpContent.kt`, `ic_widget_off.xml`.
- **Why it recurs:** Developers invert logic in UI to match boolean model defaults (`showInWidget = true`) rather than designing the interface around user intentionality.
- **The rule:** Default modal option toggles to calm OFF (`在微件中隐藏 = false`) for exclusion or suppression overrides. Mirror the active override state with subtle, subdued micro-symbols (`ic_widget_off`, 11dp, 50–55% alpha) on compact card surfaces beside status pins.

### L19. Unify circular widget dials into a single warm orbit with unboxed milestone typography — [observed]

- **What happened:** Attempting to render two concentric rings (current elapsed days outer arc + historical cadence inner arc) alongside a boxed milestone pill badge created acute visual clutter: the pill container severed the inner arc's bottom perimeter (tangent collision), narrow wire gauges (3.5dp–4.5dp) felt like a cold clinical speedometer or battery complication, and deficit math (`-8d`) induced anxiety rather than calm mindfulness.
- **Evidence:** `9d41839` feat(widget): implement Zen Orbit 2x2 widget with 8dp warm jade single orbit, `d8a5df1` chore(release): bump versionCode to 65.
- **Why it recurs:** Designers and developers instinctively add concentric tracks and boxed pill badges for secondary metrics without accounting for visual tangent collisions and the cold emotional tone of hairline gauges on glanceable home-screen widgets.
- **The rule:** In circular glanceable widgets, unify the visual path into a single generous orbit track (8dp-proportional gauge) with the cadence milestone nestled directly on the track. Strip container backgrounds and borders from secondary milestone labels, rendering them as quiet, unboxed whispers without negative deficit math.

### L20. Proportionally budget RemoteViews heights and auto-size 2-line labels to prevent OEM cell clipping — [observed]

- **What happened:** Fixed vertical container heights (e.g. 112dp moon disc or fixed text sizes without proportional weights) caused days and item names to be pushed off-screen or clipped by Android RemoteViews containers on compact OEM launcher grids like Vivo OriginOS and Xiaomi HyperOS. Single-line tags truncated long names, while auto-sizing without baseline attributes defaulted to platform minification baselines.
- **Evidence:** `c94856d` docs(lessons): update invariants test, `f902d51` feat(widgets): prevent OEM cell clipping and add 2-line word wrapping for 1x1 and 2x2 widgets.
- **Why it recurs:** OEM launchers enforce varied cell aspect ratios (1:1.33 to 1:1.55) and tighter padding budgets than standard Pixel emulator launchers.
- **The rule:** In compact RemoteViews (1x1 and 2x2), replace fixed heights with proportional `layout_weight` (`layout_weight="1.2"` moon disc, `layout_weight="1.0"` pedestal). Bound numeral auto-sizing (12–24sp on 1x1, 14–30sp on 2x2), configure 2-line auto-sizing tags (`maxLines="2"`, `autoSizeTextType="uniform"`, 6.5–9sp), and dynamically hide decorative hairline dashes in the receiver when tags wrap or exceed 10 chars.

## Project-specific implementation rules

**Layout** — Place app UI components in `app/src/main/java/com/countup/app/`. Model new screens after `CountUpContent.kt`. Build new home-screen widget providers following `ZenPebbleWidgetReceiver.kt`.

**Naming** — Use the domain terms established in store and persistence models.

| Concept | Use this word | Never use | Evidence |
| --- | --- | --- | --- |
| Milestone counter | `CountUpItem` | `Habit`, `Task`, `Counter` | `32f6f3621`, `c678dd3fd` |
| Shared preferences file | `countup_prefs` | `user_prefs`, `app_settings` | `32f6f3621`, `625f2c387` |
| Backup JSON file | `countup_backup.json` | `backup.json`, `export.json` | `625f2c387`, `92262bb43` |
| Concurrency lock | `globalStoreLock` | `storeLock`, `mutex` | `625f2c387` |

**Errors** — Return nullable types (`CountUpItem?`) or sealed UI results. Quarantine corrupted raw payloads in `items_v1_quarantine` rather than failing silently.

**Validation** — Validate date strings using `LocalDate` parse boundaries. Validate `appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID` before writing widget preferences.

**State and data access** — Drive all UI state through unidirectional MVI via `CountUpViewModel` and `CountUpContract`. Route all state mutations through `CountUpStore` under `globalStoreLock`.

**Migrations in flight** — Maintain backwards compatibility during ongoing persistence refactoring.

| Area | Old shape | New shape | Status | Evidence |
| --- | --- | --- | --- | --- |
| Data schema | `items_v0` JSON | `items_v1` with quarantine fallback | Complete | `92262bb43` |
| Hero widget layout | 1x1 Compact & 2x1 Card | 2x1 Wide Poetic Card only | Complete | `20cd2677f` |
| Widget configuration | Direct drop placement | Mandatory configure activity | Complete | `433f1d47b` |

**Intentional inconsistencies** — Two deliberate deviations exist from standard platform conventions.

| Area | Shape A (where) | Shape B (where) | Why it is intentional | Evidence |
| --- | --- | --- | --- | --- |
| Touch target size | Standard Material 48dp | Explicit 26dp/30dp (`SubHeaderRow`, `ItemCard`) | Preserves dense optical design without layout spread | `92262bb43` |
| Background alarms | Zero battery drain (no services) | `RTC_WAKEUP` in `MidnightAlarmReceiver` | Wakes device briefly at 00:00 to update daily counts | `92262bb43` |

**Commit and branch style** — Use Conventional Commits (`feat:`, `fix(widget):`, `refactor:`, `release:`). Write imperative present-tense subjects under 72 characters.

**Testing** — Execute `./gradlew testDebugUnitTest` with JUnit 4. Enforce repository invariants by writing tests that read source XML and manifest files directly (`WidgetContractInvariantsTest.kt`).

## Known risk areas

| Rank | Path | Risk | Churn | Fix commits | Couplings | Evidence |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | `app/build.gradle.kts` | 233.0 | 57 | 9 | 16 | `7b1c9c36a`, `662675d61`, `92262bb43` |
| 2 | `app/src/main/java/com/countup/app/CountUpContent.kt` | 223.8 | 52 | 12 | 18 | `45dd6e198`, `5522dec24`, `a2039e72c` |
| 3 | `app/src/main/java/com/countup/app/CountUpStore.kt` | 134.2 | 26 | 4 | 14 | `c678dd3fd`, `625f2c387`, `92262bb43` |
| 4 | `app/src/main/java/com/countup/app/HeroWidgetReceiver.kt` | 131.6 | 32 | 5 | 12 | `58618f707`, `20cd2677f`, `625f2c387` |
| 5 | `app/src/main/res/xml/zen_pebble_widget_info.xml` | 91.0 | 8 | 7 | 4 | `b6b4ce0ba`, `1cd6e29d1`, `433f1d47b` |
| 6 | `app/src/main/AndroidManifest.xml` | 115.6 | 21 | 4 | 13 | `3c8663262`, `625f2c387`, `6d9fe4f` |

### `keystore/keystore-pass.txt` — Committed plain-text release signing secret in git packfile (BLOCKING)
- **Failure mode:** Historical packfile object `8370ad4c87436d41ea4c8c3701b4f2c6756d4986` contains the production keystore password, exposed on upstream remote.
- **Guard:** Rotate the upload key in Google Play Console and execute `git filter-repo` to scrub history before any public repository release.
- **Evidence:** `32f6f36211113906d4bc4f8c03e519b711b198e1` (added), `625f2c387078c9ed3ff583d715bafa1eecb19d28` (untracked)

### `app/src/main/res/xml/zen_pebble_widget_info.xml` — Launcher placement and configuration contracts
- **Failure mode:** Modifying resize flags snaps widgets to slot 0; adding `configuration_optional` bypasses configuration activities.
- **Guard:** `WidgetContractInvariantsTest.zenPebbleWidgetEnforcesResizeModeNoneAndReconfigurable()`
- **Evidence:** `1cd6e29d1` fix(widget): lock 1x1 pebble resizeMode to none, `433f1d47b` fix(widget): restore auto-launch picker

### `app/src/main/java/com/countup/app/CountUpStore.kt` — Data loss during corrupted payload decoding
- **Failure mode:** Partial JSON decoding truncated item lists, overwriting disk backups with partial data.
- **Guard:** `CountUpStoreTest` testing corrupted array recovery and quarantine fallbacks.
- **Evidence:** `c678dd3fd` fix: harden data recovery, `92262bb43` feat: harden security

### Change couplings — files that must move together

| Pair | Co-changes | What the hidden contract is | Evidence |
| --- | --- | --- | --- |
| `CountUpContent.kt` + `CountUpViewModel.kt` | 18 | MVI UiState rendering and user intent dispatch | `a1539bc2a`, `2fa5b4e1a`, `71b0531e9` |
| `CountUpContent.kt` + `CountUpDialogs.kt` | 17 | Dialog presentation state and action callbacks | `a1539bc2a`, `92262bb43` |
| `CountUpContract.kt` + `CountUpViewModel.kt` | 15 | Sealed intent classes and state property updates | `a1539bc2a`, `71b0531e9` |
| `CountUpContent.kt` + `CountUpStore.kt` | 14 | Direct preference reads and display settings | `c678dd3fd`, `625f2c387`, `92262bb43` |
| `HeroWidgetReceiver.kt` + `ZenHorizonWidgetReceiver.kt` | 12 | Parallel widget broadcast dispatch and theme sync | `227b2f372`, `edafee3ad`, `92262bb43` |
| `CountUpItem.kt` + `CountUpStore.kt` | 10 | Model serialization and JSON schema versioning | `c678dd3fd`, `92262bb43` |
| `HeroWidgetReceiver.kt` + `ZenPebbleWidgetReceiver.kt` | 10 | Widget provider lifecycle and store binding sync | `227b2f372`, `92262bb43` |

### Removed and abandoned work — do not reintroduce

| Removed | When | Reason (quoted) | Evidence |
| --- | --- | --- | --- |
| `keystore/keystore-pass.txt` | `625f2c387` | "Untrack keystore/keystore-pass.txt and support hierarchical password loading" | `625f2c387` |
| `countup_hero_widget_1x1.xml` | `20cd2677f` | "make 2x1 wide card the exclusive default and only layout for Hero widget" | `20cd2677f` |
| `androidx.startup.InitializationProvider` | `3c8663262` | "resolve startup crash by suppressing unused WorkManagerInitializer" | `3c8663262` |
| `RECEIVE_BOOT_COMPLETED` | `625f2c387` | "harden credentials, concurrency, widget receivers and expose reset undo" | `625f2c387` |
| `android.permission.RECORD_AUDIO` | `6d9fe4f` | "implement Voice Quick Add with zero-permission speech delegation" | `6d9fe4f` |

## Safe-change checklist

Before you change anything:
- [ ] Confirm the tree is clean: `git status --porcelain`.
- [ ] Read `docs/RECURRING_ISSUES.md` before touching any widget provider or XML layout.

While you change:
- [ ] Maintain zero runtime permissions; never add `RECORD_AUDIO` or `RECEIVE_BOOT_COMPLETED` to `AndroidManifest.xml`.
- [ ] Preserve `android:resizeMode="none"` and `reconfigurable` in `zen_pebble_widget_info.xml`; never add `configuration_optional`.
- [ ] Ensure all widget info XMLs declare both `previewImage` and `previewLayout` with populated localized `@string/...` default text.
- [ ] Set unique data URIs (`intent.data = Uri.parse(...)`) on collection service intents to avoid RemoteViews adapter factory cache collisions.
- [ ] Omit `Modifier.minimumInteractiveComponentSize()` from 26dp subheader and 30dp card action button clusters.
- [ ] Wrap all `CountUpStore` mutations in `synchronized(globalStoreLock)` and invoke `purgeWidgetBindingsForItem(itemId)` on deletion.
- [ ] In compact RemoteViews, use weighted layout heights and auto-sizing 2-line tags to prevent OEM cell clipping.

Before you call it done:
- [ ] Run `./gradlew testDebugUnitTest` and confirm 100% green test execution.
- [ ] Run `./gradlew assembleRelease` to confirm R8 rules preserve all data models, enums, and widget providers.
- [ ] Verify widget RemoteViews payload remains below 40KB via `WidgetMemoryBudgetGateTest`.

## Examples from commit history

### Example 1 — The configuration_optional trap on 1x1 widgets

```
1cd6e29d1  fix(widget): lock 1x1 pebble resizeMode to none and restore configuration_optional
433f1d47b  fix(widget): restore auto-launch picker for 1x1 Zen Pebble by removing configuration_optional
```
- **What changed:** Commit `1cd6e29d1` added `configuration_optional` to fix home-screen drop behavior.
- **What broke:** Android 12+ launchers interpreted the flag as permission to omit `ZenPebbleConfigureActivity`, dropping unconfigured fallback widgets.
- **The fix:** Commit `433f1d47b` removed `configuration_optional` and added an invariant test asserting its absence.
- **The rule it produced:** see L1
- **Grade:** `[observed]`

### Example 2 — Abandoning 1x1 Hero layout for 2x1 poetic cards

```
ca2d64b  feat(widget): add Focused Hero Milestone Widget (1x1 Compact Stamp & 2x1 Poetic Card)
20cd2677f  fix(widget): make 2x1 wide card the exclusive default and only layout for Hero widget
```
- **What was tried:** Supporting both 1x1 compact stamp and 2x1 card layouts within the Hero widget provider.
- **Why it failed:** "make 2x1 wide card the exclusive default and only layout for Hero widget" — 1x1 required title-splitting heuristics and overcrowded reset tap targets.
- **The rule it produced:** see L6
- **Grade:** `[observed]`

### Example 3 — R8 enum member obfuscation causing silent deserialization reset

```
0162fc3  (audit finding P0-2)
92262bb43  feat: harden security, persist widget bindings, refine edge-to-edge, and optimize R8
```
- **What changed:** Storing `TimeDisplayMode` and `ZenWidgetDisplayUnit` enum `.name` in SharedPreferences.
- **What broke:** R8 minification renamed enum fields to single letters, causing `valueOf()` to throw `IllegalArgumentException` and silently default to `DAYS`.
- **The fix:** Commit `92262bb43` persisted stable serialization codes and updated ProGuard keep definitions.
- **The rule it produced:** see L3
- **Grade:** `[observed]`

### Example 4 — Silent partial-decode data loss and backup overwriting

```
c678dd3fd  fix: harden data recovery, lock store writes, and apply review fixes
92262bb43  feat: harden security, persist widget bindings, refine edge-to-edge, and optimize R8
```
- **What repeated:** Malformed JSON elements were skipped during array deserialization, returning truncated lists as valid data.
- **Why it repeated:** `CountUpStore` lacked validation comparing decoded list length with raw JSON array length, overwriting good backups with partial lists.
- **The fix:** Commit `92262bb43` implemented corrupted payload quarantine to `items_v1_quarantine` and aborted backup overwrites.
- **The rule it produced:** see L4
- **Grade:** `[observed]`

### Example 5 — Transitive WorkManager startup provider crash on release build

```
227b2f372  feat(widgets): implement Zen & Efficient Widget Suite with reactive updates
3c8663262  fix(release): resolve startup crash by suppressing unused WorkManagerInitializer
```
- **What changed:** Introducing widget dependencies transitively brought in AndroidX startup and WorkManager dependencies.
- **What broke:** App crashed on cold start in release builds attempting to initialize unused background worker infrastructure.
- **The fix:** Commit `3c8663262` stripped `InitializationProvider` via manifest `tools:node="remove"`.
- **The rule it produced:** see L2
- **Grade:** `[observed]`

### Example 6 — Zero-permission speech delegation with package visibility

```
6d9fe4f  feat(widget): implement Voice Quick Add with zero-permission speech delegation
966928a  feat(voice): refine voice add aesthetics, match widget icon stroke, prune dead strings
```
- **What changed:** Implemented speech-to-text quick item entry without adding `android.permission.RECORD_AUDIO`.
- **What broke:** On Android 11+ (API 30+), external speech intents failed without explicit package visibility declarations.
- **The fix:** Added `<queries>` declarations for `RecognitionService` and `RECOGNIZE_SPEECH` in `AndroidManifest.xml`.
- **The rule it produced:** see L8
- **Grade:** `[observed]`

### Example 7 — Compact OEM launcher cell height clipping and 2-line word wrapping

```
c94856d  docs(lessons): update invariants test
f902d51  feat(widgets): prevent OEM cell clipping and add 2-line word wrapping for 1x1 and 2x2 widgets
```
- **What broke:** Fixed heights (112dp moon disc) and rigid `singleLine` attributes pushed the bottom text off-screen on compact OEM launcher grids (Vivo OriginOS, Xiaomi HyperOS).
- **The fix:** Replaced fixed vertical heights with proportional `layout_weight`, added 2-line uniform auto-sizing text, and dynamically hid decorative hairline dashes when tags wrap.
- **The rule it produced:** see L20
- **Grade:** `[observed]`

## Analysis notes

- **Coverage:** 205 commits analyzed across 535 tracked files spanning 2026-08-20 to 2026-09-22.
- **Not covered:** Excluded build artifacts (`build/`, `.gradle/`), IDE metadata (`.idea/`), and scratch logs (`.scratch/`).
- **Weak signals:** None. All twenty prescriptive rules are corroborated by commit messages, regression tests, or architecture documentation.
- **Unknowns:** Production upload key replacement status in Google Play Console requires manual verification outside repository history.
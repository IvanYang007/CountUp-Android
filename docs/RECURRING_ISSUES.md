# CountUp Recurring Issues & Prevention Guardrails

This document tracks recurring issues encountered across Android releases, architectural root causes, and hard guardrails to prevent regressions during future refactoring, code reviews, and dependency upgrades.

---

## 1. 1x1 Zen Pebble Grid Placement, Snapping to Slot 0, and the Configuration Trap

### Symptom
1. Dragging the 1x1 Zen Pebble widget onto the home screen failed or snapped immediately to slot 0 (top-left empty cell) on physical OEM launchers (Samsung One UI, Xiaomi HyperOS, Nova Launcher).
2. Alternatively, when `configuration_optional` was added to work around launcher drop issues, dropping the 1x1 widget stopped automatically launching the card picker UI (`ZenPebbleConfigureActivity`). The widget was placed immediately with fallback content and only displayed a small pencil affordance, which users easily miss.

### Architectural Root Cause
1. **The Root Cause of OEM Slot-0 Snapping**: Setting `android:resizeMode="horizontal|vertical"` on an icon-sized single-cell widget (`targetCellWidth="1"`, `targetCellHeight="1"`) triggers OEM drop handlers to compute resize bounds and margins. Because a 1x1 tile cannot be resized further in a tight grid (e.g. 56dp–68dp), OEM launchers abort the user's targeted cell coordinate and snap the widget to the first available slot (slot 0). Setting `android:resizeMode="none"` completely fixes this issue.
2. **The `configuration_optional` Trap**:
   In Android 12+ (API 31+), `WIDGET_FEATURE_CONFIGURATION_OPTIONAL` (`android:widgetFeatures="configuration_optional"`) explicitly tells the launcher host:
   > *"The widget provider is happy to be configured at any point after being created, and so the widget host may choose to configure the widget with a default configuration and omit the widget configuration activity at the time the widget is added."*
   When `configuration_optional` is present, launchers (including Pixel Launcher and OEM launchers) bypass launching `ZenPebbleConfigureActivity` on drag-and-drop. The user is left with an unconfigured widget showing fallback data and a subtle pencil/edit icon on the widget container.
   For single-item tracking widgets like Zen Pebble (and Hero / Solar Rhythm / Zen Horizon), initial configuration is essential for selecting the milestone to track. Therefore, `configuration_optional` must NOT be used.

### Hard Invariants
In `app/src/main/res/xml/zen_pebble_widget_info.xml`:

```xml
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="40dp"
    android:minHeight="40dp"
    android:minResizeWidth="40dp"
    android:minResizeHeight="40dp"
    android:targetCellWidth="1"
    android:targetCellHeight="1"
    android:updatePeriodMillis="1800000"
    android:initialLayout="@layout/widget_zen_pebble_1x1"
    android:previewImage="@drawable/zen_pebble_widget_preview"
    android:description="@string/zen_pebble_widget_description"
    android:widgetCategory="home_screen"
    android:resizeMode="none"
    android:widgetFeatures="reconfigurable"
    android:configure="com.countup.app.ZenPebbleConfigureActivity"
    android:label="@string/zen_pebble_widget_label" />
```

### Prevention Rules
- **DO NOT** change `android:resizeMode` to `horizontal`, `vertical`, or `horizontal|vertical` on 1x1 widgets. It must remain `none`.
- **DO NOT** include `configuration_optional` in `android:widgetFeatures` for any widget requiring initial card selection. Keep `reconfigurable` only.
- **Corner radius**: Must be ≤ 16dp (or use `@android:dimen/system_app_widget_inner_radius` on API 31+). OEM launchers with 56dp cells clip content inside 24dp corners.
- **Content padding**: Must be ≤ 4dp. The rounded corners provide visual breathing room.
- **Text sizing**: Use `autoSizeTextType="uniform"` with `autoSizeMaxTextSize="18sp"` and `autoSizeMinTextSize="10sp"` for the number field. Fixed 22sp overflows on tight OEM cells.
- **Tag text**: Must use `singleLine="true"`, `ellipsize="end"`, and `maxWidth="52dp"` to prevent overflow.
- **Automated Verification**: `WidgetContractInvariantsTest` asserts `resizeMode="none"`, `reconfigurable`, absence of `configuration_optional`, corner radius ≤ 16dp, auto-size text, and ellipsize.

#### OEM Launcher Cell Sizes (Reference)
| Launcher | Typical 1x1 Cell Size | Grid Density |
|---|---|---|
| Pixel Launcher | ~80dp | 5×5 |
| Samsung One UI | ~68dp | 4×6 or 5×6 |
| Vivo OriginOS / Funtouch | ~56–64dp | 5×6 or 5×9 |
| Xiaomi HyperOS / MIUI | ~60–68dp | 4×6 or 5×6 |
| OPPO ColorOS / Realme | ~60–66dp | 4×6 or 5×6 |
| Nova Launcher | configurable | varies |

**Design for the worst case (56dp) so it works everywhere.**

---

## 2. Solar Rhythm (24 节气) Widget Missing Card Picker

### Symptom
When dropping the Solar Rhythm widget onto the home screen, the user is never prompted to select which card/item to track. The widget silently defaults to whatever card is pinned, or the first card in the list (slot 0), preventing multi-card tracking or per-widget customization.

### Architectural Root Cause
1. **Missing Configuration Activity**: During initial development, the receiver and preview layout were implemented, but `android:configure` was omitted from `solar_rhythm_widget_info.xml`.
2. **Missing Manifest Registration**: The configuration activity had not been declared in `AndroidManifest.xml` with `APPWIDGET_CONFIGURE`.
3. **Silent Fallback in Reducer**: `ZenWidgetReducer.resolveTargetItem(items, boundId)` gracefully fell back to `pinnedItem ?: items.firstOrNull()`. Because the launcher was never instructed to launch a configure screen, the fallback became the default behavior.

### Hard Invariants
Every widget that displays item-specific data **must** implement the full configuration contract:
1. **Activity**: A dedicated `ConfigureActivity` extending `ComponentActivity`:
   - Sets `setResult(RESULT_CANCELED)` before any user interaction.
   - Extracts `appWidgetId` from `intent?.extras`.
   - On item selection, persists binding via `CountUpStore.set<Widget>Binding(appWidgetId, itemId)`.
   - Pushes immediate RemoteViews update via provider receiver.
   - Returns `setResult(RESULT_OK, resultValue)` and calls `finish()`.
2. **Provider XML**:
   ```xml
   android:configure="com.countup.app.SolarRhythmConfigureActivity"
   android:widgetFeatures="reconfigurable"
   ```
3. **Manifest**:
   ```xml
   <activity
       android:name=".SolarRhythmConfigureActivity"
       android:exported="true"
       android:windowSoftInputMode="adjustResize">
       <intent-filter>
           <action android:name="android.appwidget.action.APPWIDGET_CONFIGURE" />
       </intent-filter>
   </activity>
   ```
4. **Lifecycle Cleanup**: The widget receiver's `onDeleted` handler must remove the instance binding from `CountUpStore`.

### Prevention Rules
- Every widget provider XML must declare `android:configure` unless it is explicitly an unconfigurable global summary widget.
- `SolarRhythmConfigurationTest` verifies that `solar_rhythm_widget_info.xml` contains `android:configure` and that the manifest registers the activity.

---

## 3. Release Build Startup Crashes & Reflection Traps (R8 / Minification)

### Symptom
Debug builds run smoothly in the emulator, but release builds (`assembleRelease`) with R8 minification and resource shrinking crash immediately on cold launch or fail to render widgets.

### Architectural Root Cause
1. **Transitive Startup Providers**: AndroidX libraries (such as Glance or WorkManager dependencies) inject `androidx.startup.InitializationProvider` into the merged manifest. In an app with a strict zero-background-service architecture, these auto-initializers fail or run unauthorized background tasks.
2. **R8 Obfuscation of Data & Reflection Targets**:
   - `CountUpItem`, `BackgroundTheme`, and `SortOrder` stored as JSON via Gson/serialization are stripped or renamed by R8 if not protected by `-keep`.
   - `AppWidgetProvider` classes and configure activities invoked via reflection by launcher processes fail to resolve if obfuscated.
   - Room/WorkManager reflection classes transitively included by Glance dependencies fail if their constructors are stripped.

### Hard Invariants
1. **Manifest Defensive Removals**:
   ```xml
   <provider
       android:name="androidx.startup.InitializationProvider"
       android:authorities="${applicationId}.androidx-startup"
       tools:node="remove" />
   ```
2. **ProGuard Rules (`proguard-rules.pro`)**:
   - Keep all domain models (`CountUpItem`, `BackgroundTheme`, `SortOrder`).
   - Keep all `AppWidgetProvider`, `RemoteViewsService`, and configuration activities.
   - Keep Room database implementations (`androidx.room.RoomDatabase`, `WorkDatabase_Impl`).
3. **Verification Before Release**:
   - Always run `assembleRelease` and test the release APK on the emulator before publishing a new version.

---

## 4. Reboot Midnight Alarm Rollover & Zero-Permission Invariant

### Symptom
After a device reboot, midnight rollover might not fire if `MidnightAlarmReceiver` was only scheduled on app startup and `RECEIVE_BOOT_COMPLETED` is omitted.

### Architectural Root Cause & Solution
1. **Zero-Permission Model**: `RECEIVE_BOOT_COMPLETED` is explicitly stripped from the merged manifest via `tools:node="remove"` to preserve 100% offline privacy and zero battery-drain background services.
2. **Launcher Boot Dispatch**: The Android framework automatically dispatches `onUpdate()` and `onEnabled()` broadcasts to active home-screen widget providers after device boot.
3. **Idempotent Alarm Restoration**: Every widget provider (`CountUpWidget`, `HeroWidgetReceiver`, `SolarRhythmWidget`, `ZenHorizonWidgetReceiver`, `ZenPebbleWidgetReceiver`) calls `MidnightAlarmReceiver.scheduleMidnightAlarm(context)` inside `onUpdate()` and `onEnabled()`. Alarms are idempotently refreshed without requiring a boot broadcast receiver.

---

## 5. Action Row Sizing, minimumInteractiveComponentSize, and Layout Spread (SubHeaderRow & ItemCard)

### Symptom
Subheader buttons (Theme Toggle, Settings Gear) or ItemCard action buttons (Widget Toggle, MechanicalResetButton, Delete) appear pushed far apart with large empty gaps across the row, instead of resting tightly grouped (1–2dp gap).

### Architectural Root Cause
In Jetpack Compose, calling `Modifier.minimumInteractiveComponentSize()` or `.sizeIn(minWidth = 48.dp, minHeight = 48.dp)` enforces a minimum touch target bounding box of 48×48 dp by adding invisible layout padding around smaller components. When applied inside tight row layouts like `SubHeaderRow` (which uses 26dp circular buttons) or `ItemCard` action clusters (which use 30dp buttons with 2dp spacers), the layout bounds expand to 48dp, causing massive visual separation between adjacent controls and breaking the app's signature compact optical density.

### Hard Invariants
1. Do **NOT** apply `minimumInteractiveComponentSize()`, `sizeIn(48.dp)`, or `IconButton` defaults to `SubHeaderRow` action buttons or `ItemCard` action buttons (`MechanicalResetButton`, widget toggle, delete button).
2. Maintain explicit `size(26.dp)` in `SubHeaderRow` (with `padding(horizontal = 1.dp)` or `Arrangement.spacedBy(1.dp)`).
3. Maintain explicit `size(30.dp)` with `Spacer(Modifier.width(2.dp))` in `ItemCard` action clusters.
4. Use tactile scale feedback (`pressScale(0.96f)`) and explicit click/gesture handlers with haptics on bounded surfaces.

---

## 6. Release Bundle Signing and Multi-Tier Keystore Password Resolution

### Symptom
`./gradlew bundleRelease` or Google Play Store upload fails with:
`All uploaded bundles must be signed.`

### Architectural Root Cause
In CI or local developer machines, release keystores might be present (`keystore/countup-release.jks`), but the password might be stored in a local file (`keystore/keystore-pass.txt`) rather than system environment variables or `local.properties`. If the build script only checks `System.getenv("COUNTUP_KEYSTORE_PASS")`, it silently skips creating the `release` signing configuration. As a result, Gradle produces an unsigned `.aab` bundle that cannot be installed or uploaded.

### Hard Invariants
In `app/build.gradle.kts`, always resolve release passwords using the 3-tier fallback hierarchy:
```kotlin
val keystorePass = System.getenv("COUNTUP_KEYSTORE_PASS")
    ?: localProps.getProperty("countup.keystore.pass")
    ?: file("../keystore/keystore-pass.txt").takeIf { it.exists() }?.readText()?.trim()
```
And verify bundles with `apksigner` before release distribution.

---

## 7. Intent Security & Calling Package Verification in Configure Activities

### Symptom
Potential Intent Redirection or unauthorized widget reconfiguration if untrusted third-party apps send spoofed Intents to exported configure activities (`HeroWidgetConfigureActivity`, `ZenHorizonConfigureActivity`, etc.).

### Architectural Root Cause
Widget configuration activities must declare `exported="true"` with an `APPWIDGET_CONFIGURE` intent filter so system launchers can launch them upon widget placement. However, an exported activity can be invoked by any application on the device. If the activity blindly consumes `EXTRA_APPWIDGET_ID` or redirects intents without validation, it creates an intent redirection vulnerability.

### Hard Invariants
1. Validate that `appWidgetId` is valid (`!= AppWidgetManager.INVALID_APPWIDGET_ID`).
2. Verify calling package identity where feasible or restrict result dispatch strictly via `setResult(RESULT_OK, resultIntent)` carrying only `EXTRA_APPWIDGET_ID`.
3. Never echo untrusted caller extras into pending intents or internal store operations.

---

## 8. Zero-Permission Voice Quick Add & Package Visibility Queries (`<queries>`)

### Symptom
Triggering speech recognition via `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` throws `ActivityNotFoundException` on Android 11+ (API 30+), or developers attempt to declare `android.permission.RECORD_AUDIO` to make speech input work.

### Architectural Root Cause
1. **Package Visibility on API 30+**: Android 11 introduced package filtering. An app cannot query or invoke external speech recognition services unless declared in `<queries>`.
2. **Zero-Permission Violation**: Developers often mistakenly believe voice input requires `RECORD_AUDIO`. In reality, CountUp delegates speech capture to the platform speech engine (Google Speech Services / system assistant) out-of-process. CountUp never records audio directly and requires **zero** microphone permissions.

### Hard Invariants
1. In `AndroidManifest.xml`, maintain explicit `<queries>` declarations:
   ```xml
   <queries>
       <intent>
           <action android:name="android.speech.RecognitionService" />
       </intent>
       <intent>
           <action android:name="android.intent.action.RECOGNIZE_SPEECH" />
       </intent>
   </queries>
   ```
2. **DO NOT** declare `android.permission.RECORD_AUDIO` or any microphone permissions.
3. Keep `VoiceAddActivity` private (`exported="false"`, `theme="@style/Theme.CountUp.Translucent"`).

---

## 9. Nested Clipping on ItemCard and Bottom-Left Text Truncation

### Symptom
The bottom-left anchor date label (e.g. `SINCE Jan 15, 2024` or `UNTIL ...`) has its baseline, descenders, or corners clipped off on certain display densities.

### Architectural Root Cause
Applying `Modifier.clip(RoundedCornerShape(12.dp))` to internal `Column` layouts within `ItemCard` applies a secondary boundary inside the card's 16dp content padding. When combined with font descenders and localized date strings, the inner clip slices through text.

### Hard Invariants
1. Apply `Modifier.clip(RoundedCornerShape(12.dp))` only to the outer card border surface.
2. Inner content containers must remain unclipped and preserve uniform `16.dp` padding.

---

## 10. Orphaned Widget Bindings on Item Deletion

### Symptom
When an item is deleted in the app, home-screen widgets configured to track that specific item revert to fallback data, but their instance preferences (`hero_widget_item_<id>`, etc.) linger in `countup_prefs`, producing orphaned bindings across device backups and restores.

### Architectural Root Cause
Deleting an item from `CountUpStore` previously modified `items_v1` without traversing the widget preference map to remove references to the deleted UUID.

### Hard Invariants
In `CountUpStore.deleteItem(itemId)`, always execute `purgeWidgetBindingsForItem(itemId)` across all 5 configurable widget families:
- `HeroWidget` (`hero_widget_item_<id>`)
- `ZenHorizonWidget` (`zen_horizon_widget_item_<id>`)
- `SolarRhythmWidget` (`solar_rhythm_widget_item_<id>`)
- `ZenPebbleWidget` (`zen_pebble_widget_item_<id>`)
- `ZenOrbitWidget` (`zen_orbit_widget_item_<id>`)

---

## 11. Widget Picker Previews (`previewLayout` Populated String & Sample Track Contract)

### Symptom
Widgets in the system launcher widget picker appear completely blank or show empty cards with missing text, no numbers, and absent milestone progress bars (e.g., only a solitary cycle icon or empty unit label) when raw runtime layouts are referenced without default populated content.

### Architectural Root Cause
In Android 12+ (API 31+), launcher hosts (Pixel Launcher, Samsung One UI, Launcher3) inflate `android:previewLayout` directly in the widget picker.
If layouts only specify `tools:text` (which are stripped at build time) and omit default `android:text` and preview track drawables, static layout inflation yields empty views. Conversely, relying only on static `previewImage` vector drawables looks flat and cannot dynamically adapt to user system languages (English vs. Chinese).

### Hard Invariants
1. **Declare both `android:previewLayout` and `android:previewImage`**:
   - `android:previewLayout` enables Android 12+ (API 31+) living widget previews that render exact fonts, margins, and card backgrounds.
   - `android:previewImage` serves as backward-compatible fallback for API 26–30 launchers.
2. **Always populate layout XMLs with localized `@string/...` resources**:
   - Never leave `android:text` empty or rely solely on `tools:text`.
   - Use dedicated preview string keys (`@string/preview_item_title`, `@string/preview_item_tag`, `@string/preview_item_subtitle`, `@string/preview_solar_quote`, etc.) defined across all 7 locale files (`values`, `values-zh`, `values-zh-rCN`, etc.) so picker previews dynamically render in authentic English or authentic Chinese.
3. **Include static preview tracks in layout XMLs**:
   - Reference `preview_zen_horizon_track` and `preview_solar_timeline_track` in layout XMLs so progress bars and milestone indicators display authentic visual structure in pickers before runtime Kotlin rendering takes over.
4. **Enforced by `WidgetContractInvariantsTest.allWidgetsDeclareValidPreviewLayoutAndPreviewImage`**.

---

## 12. Circular Dial Widget Tactile Warmth vs. Tangent Clutter (Single 8dp Orbit & Unboxed Typography)

### Symptom
When rendering dual-ring circular widgets (e.g. Zen Orbit / 律动之环), attempting to display both current days and historical average cadence via two concentric tracks alongside a boxed pill badge (`widget_solar_badge_bg`) created:
1. **Tangent Collision**: The boxed pill badge severed and overlapped the inner bronze arc.
2. **Clinical Coldness**: Narrow stroke widths (3.5dp–4.5dp) felt like a cold speedometer or battery complication rather than a peaceful milestone companion.
3. **Deficit Anxiety**: Displaying minus numbers (`-8d`) framed time as debt rather than living progress.

### Architectural Root Cause
Over-complicating circular glanceable surfaces with multiple concentric scales and bordered container wrappers forces visual elements into optical conflict within tight 140dp–160dp widget boundaries.

### Hard Invariants
1. **Single Unified Orbit with 8dp Gauge**:
   - Merge active streak and cadence milestone into a single orbit track (`rOrbit = safeSize * 0.38f`).
   - Set stroke gauge proportional to 8dp (`strokeOrbit = maxOf(3f, safeSize * 0.056f)`), giving the perimeter physical mass, presence, and tactile warmth (*温润玉环*).
2. **Nestled Cadence Pebble**:
   - Place the cadence anchor pebble directly on the single 8dp orbit path ($r \approx 0.50 \times \text{strokeOrbit}$) rather than carving out a second competing concentric ring.
3. **Unboxed Tranquil Whisper**:
   - Strictly omit container backgrounds (`android:background="@null"`) and borders on secondary milestone labels (`zen_orbit_rhythm_pill`). Render as quiet, unboxed text without deficit math.

---

## Quick Reference Checklist for New Widgets or Refactoring

Before committing any widget changes or releasing a new version:

- [ ] **1x1 Widgets**: Does `zen_pebble_widget_info.xml` have `android:resizeMode="none"` and `android:widgetFeatures="reconfigurable"` (omitting `configuration_optional` to guarantee automatic configure activity launch on drop)?
- [ ] **Widget Previews**: Does every widget info XML declare both `android:previewImage` and `android:previewLayout`, with layout XMLs containing default populated `android:text="@string/..."` and sample tracks for dual-locale (EN/ZH) previews?
- [ ] **Configurable Widgets**: Does provider XML declare `android:configure` and is the activity registered in `AndroidManifest.xml` with `APPWIDGET_CONFIGURE`?
- [ ] **Calling Package Security**: Do exported configure activities validate `appWidgetId` and caller parameters?
- [ ] **Instance Bindings**: Does `onDeleted()` clean up `CountUpStore` widget bindings, and does `deleteItem()` purge orphaned bindings across all widget families?
- [ ] **Device Restoration**: Does every widget provider implement `onRestored(oldIds, newIds)` delegating to `CountUpStore.remapWidgetBindings`?
- [ ] **Voice Quick Add**: Does `AndroidManifest.xml` retain `<queries>` for `RecognitionService` and `RECOGNIZE_SPEECH` while requesting **zero** microphone permissions?
- [ ] **Midnight Alarm**: Do `onUpdate()` and `onEnabled()` re-register `MidnightAlarmReceiver.scheduleMidnightAlarm(context)` with `RTC_WAKEUP`?
- [ ] **Action Row Density**: Are subheader action buttons sized at 26dp and ItemCard buttons at 30dp with 1–2dp spacing, strictly omitting `minimumInteractiveComponentSize()` to prevent layout spread?
- [ ] **Card Padding & Clipping**: Is inner clipping omitted on `ItemCard` to prevent `SINCE`/`UNTIL` text truncation?
- [ ] **ProGuard Rules**: Are new receivers, models, and configure activities added to `proguard-rules.pro`?
- [ ] **Release Signing**: Is release bundle signed (verified via `apksigner`)?
- [ ] **Memory Gate**: Does the widget payload stay strictly below 40 KB (`WidgetMemoryBudgetGateTest`) to prevent `TransactionTooLargeException`?
- [ ] **Automated Tests**: Do all 408 unit and contract tests pass (`./gradlew testDebugUnitTest`)?



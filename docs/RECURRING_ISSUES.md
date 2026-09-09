# CountUp Recurring Issues & Prevention Guardrails

This document tracks recurring issues encountered across Android releases, architectural root causes, and hard guardrails to prevent regressions during future refactoring, code reviews, and dependency upgrades.

---

## 1. 1x1 Zen Pebble Grid Placement & Snapping to Slot 0

### Symptom
On physical Android devices (Samsung One UI, Xiaomi HyperOS, Nova Launcher, etc.), dragging the 1x1 Zen Pebble widget onto a specific home screen cell fails or snaps immediately to slot 0 (the top-left empty cell). On standard Google Pixel emulator launchers, dragging appears to work, masking the bug during basic emulator testing.

### Architectural Root Cause
1. **Mandatory Configuration Trap**: Without `configuration_optional` in `android:widgetFeatures`, OEM launchers treat widget configuration as mandatory *before* placement can complete. When the user drags the widget to an arbitrary cell, the launcher aborts the targeted drop coordinate and forces placement into the default slot 0 while waiting for configuration.
2. **Resize Mode Confusion on 1x1 Tiles**: Setting `android:resizeMode="horizontal|vertical"` on an icon-sized single-cell widget (`targetCellWidth="1"`, `targetCellHeight="1"`) triggers custom OEM drop handlers to compute resize bounds and padding. This conflicts with single-cell icon grids and breaks launcher drop targeting.

### Hard Invariants
In `app/src/main/res/xml/zen_pebble_widget_info.xml`, the following attributes are **immutable**:

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
    android:previewLayout="@layout/widget_zen_pebble_1x1"
    android:description="@string/zen_pebble_widget_description"
    android:widgetCategory="home_screen"
    android:resizeMode="none"
    android:widgetFeatures="reconfigurable|configuration_optional"
    android:configure="com.countup.app.ZenPebbleConfigureActivity"
    android:label="@string/zen_pebble_widget_label" />
```

### Prevention Rules
- **DO NOT** change `android:resizeMode` to `horizontal`, `vertical`, or `horizontal|vertical` on 1x1 widgets. It must remain `none`.
- **DO NOT** remove `configuration_optional` from `android:widgetFeatures` on 1x1 pebble widgets.
- **Corner radius**: Must be ≤ 16dp (or use `@android:dimen/system_app_widget_inner_radius` on API 31+). OEM launchers with 56dp cells clip content inside 24dp corners.
- **Content padding**: Must be ≤ 4dp. The rounded corners provide visual breathing room.
- **Text sizing**: Use `autoSizeTextType="uniform"` with `autoSizeMaxTextSize="18sp"` and `autoSizeMinTextSize="10sp"` for the number field. Fixed 22sp overflows on tight OEM cells.
- **Tag text**: Must use `singleLine="true"`, `ellipsize="end"`, and `maxWidth="52dp"` to prevent overflow.
- **Automated Verification**: `WidgetContractInvariantsTest` must assert `resizeMode="none"`, `configuration_optional`, corner radius ≤ 16dp, auto-size text, and ellipsize.

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

## Quick Reference Checklist for New Widgets or Refactoring

Before committing any widget changes or releasing a new version:

- [ ] **1x1 Widgets**: Does `zen_pebble_widget_info.xml` have `android:resizeMode="none"` and `android:widgetFeatures="reconfigurable|configuration_optional"`?
- [ ] **Configurable Widgets**: Does provider XML declare `android:configure` and is the activity registered in `AndroidManifest.xml` with `APPWIDGET_CONFIGURE`?
- [ ] **Instance Bindings**: Does `onDeleted()` clean up `CountUpStore` widget bindings?
- [ ] **Midnight Alarm**: Do `onUpdate()` and `onEnabled()` re-register `MidnightAlarmReceiver.scheduleMidnightAlarm(context)`?
- [ ] **ProGuard Rules**: Are new receivers and configure activities added to `proguard-rules.pro`?
- [ ] **Memory Gate**: Does the widget payload stay strictly below 40 KB (`WidgetMemoryBudgetGateTest`) to prevent `TransactionTooLargeException`?
- [ ] **Automated Tests**: Do all unit and contract tests pass (`./gradlew testDebugUnitTest`)?

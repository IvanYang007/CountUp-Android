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
    android:previewLayout="@layout/widget_zen_pebble_1x1"
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

## Quick Reference Checklist for New Widgets or Refactoring

Before committing any widget changes or releasing a new version:

- [ ] **1x1 Widgets**: Does `zen_pebble_widget_info.xml` have `android:resizeMode="none"` and `android:widgetFeatures="reconfigurable"` (omitting `configuration_optional` to guarantee automatic configure activity launch on drop)?
- [ ] **Configurable Widgets**: Does provider XML declare `android:configure` and is the activity registered in `AndroidManifest.xml` with `APPWIDGET_CONFIGURE`?
- [ ] **Instance Bindings**: Does `onDeleted()` clean up `CountUpStore` widget bindings?
- [ ] **Midnight Alarm**: Do `onUpdate()` and `onEnabled()` re-register `MidnightAlarmReceiver.scheduleMidnightAlarm(context)`?
- [ ] **ProGuard Rules**: Are new receivers and configure activities added to `proguard-rules.pro`?
- [ ] **Memory Gate**: Does the widget payload stay strictly below 40 KB (`WidgetMemoryBudgetGateTest`) to prevent `TransactionTooLargeException`?
- [ ] **Automated Tests**: Do all unit and contract tests pass (`./gradlew testDebugUnitTest`)?

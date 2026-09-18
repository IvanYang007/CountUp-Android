# CountUp — Agent Guidelines & Instructions

## Emulator & Manual Testing Rule (CRITICAL)

When the user asks to **"show emulator"**, **"run in emulator"**, **"test manually"**, or **"show me in emulator"**:

1. **DO NOT** attempt blind `Start-Process emulator.exe` or `cmd.exe /c start ...` without the desktop specifier.
   - On Windows, agent shell processes execute in an isolated desktop (`exebox-...`), causing the emulator GUI window to spawn invisibly away from the user's screen.
2. **ALWAYS** run the pre-configured 1-try launcher script:
   ```powershell
   powershell -ExecutionPolicy Bypass -File .\scripts\launch_emulator.ps1 -Deploy -OpenDialog
   ```
   This script:
   - Resolves the Android SDK and targets `countUp_api36`.
   - Checks if an emulator is already online (preventing redundant boot cycles).
   - If not running, spawns `emulator.exe` directly onto the user's primary interactive desktop (`WinSta0\Default`) via Win32 `CreateProcess`.
   - Waits for boot completion.
   - Builds `assembleDebug`, installs the APK, and launches `MainActivity`.
   - Taps to open and expand the dialog so the feature under review is instantly on screen.

## Recurring Issues & Widget Guardrails (CRITICAL)

To prevent known regressions when modifying widget providers, configurations, or release builds, consult and adhere strictly to [`docs/RECURRING_ISSUES.md`](docs/RECURRING_ISSUES.md):

1. **1x1 Zen Pebble Grid Placement & Auto-Picker Contract**:
   - In `zen_pebble_widget_info.xml`, keep `android:resizeMode="none"` and `android:widgetFeatures="reconfigurable"`.
   - Never set `resizeMode` to `horizontal|vertical` (causes physical OEM launchers like Samsung One UI, HyperOS to cancel drag-and-drop targeting and force-snap to slot 0).
   - **DO NOT** add `configuration_optional` to `android:widgetFeatures`. Setting `configuration_optional` instructs the launcher to skip opening `ZenPebbleConfigureActivity` upon widget drop and show a pencil icon instead.
2. **Widget Configuration Contracts**:
   - Every widget that displays item-specific data must declare `android:configure` pointing to a valid `ConfigureActivity` registered with `APPWIDGET_CONFIGURE` in `AndroidManifest.xml`.
   - Widgets must clean up instance bindings in receiver `onDeleted`.
3. **Zero Background Work & Release Stability**:
   - No `WorkManager` or persistent background services are permitted.
   - Strip `androidx.startup.InitializationProvider` via manifest `tools:node="remove"`.
   - Retain data models and reflection targets in `proguard-rules.pro`.
4. **Zero-Permission Speech Delegation (Voice Quick Add)**:
   - NEVER add `android.permission.RECORD_AUDIO` or any microphone permissions. CountUp is strictly zero-permission.
   - Speech recognition is delegated out-of-process via `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` to the system's speech recognition service.
   - Maintain `<queries>` declarations in `AndroidManifest.xml` for `android.speech.RecognitionService` and `android.intent.action.RECOGNIZE_SPEECH` to preserve package visibility on Android 11+ (API 30+).
5. **Intent Security & Calling Package Verification**:
   - Exported widget configure activities must verify calling package ownership and validate intent extras (`appWidgetId`) before binding items or returning results to prevent Intent Redirection.
6. **Orphaned Widget Binding Cleanup**:
   - Deleting an item in `CountUpStore` must call `purgeWidgetBindingsForItem(itemId)` across all widget providers (`hero`, `zen_horizon`, `solar_rhythm`, `zen_pebble`) to prevent stale or orphaned bindings.
7. **Action Row Density & Card Clipping Guardrails**:
   - NEVER add `.minimumInteractiveComponentSize()` or `IconButton` defaults to `SubHeaderRow` (26dp) or `ItemCard` (30dp) action clusters — it causes layout spread.
   - NEVER add nested `clip(RoundedCornerShape)` to inner column content on `ItemCard` — it truncates bottom-left "SINCE" text. Keep clipping on the outer container only.

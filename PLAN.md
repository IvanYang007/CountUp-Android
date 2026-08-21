# REVISED_PLAN.md

## 1. Objective

Build a small, offline Android app and home-screen widget that display the number of calendar days since the user’s last haircut.

The implementation must remain intentionally small:

- One stored date
- One primary app screen
- One date picker
- One confirmation dialog
- One home-screen widget
- No accounts, network access, analytics, background scheduler, or unnecessary architecture

---

## 2. User behavior

### First launch

1. If no valid date is stored, initialize the last-haircut date to the device’s current local date.
2. Display:
   - Number of days since the stored date
   - Localized stored date
   - Button to choose a different date
   - Button to record a haircut today
3. Refresh all widget instances after initialization.

### Choose a date

1. Open a Material date picker.
2. Do not allow the user to select a future date.
3. Convert the picker result from UTC milliseconds using:

   ```kotlin
   Instant.ofEpochMilli(value)
       .atZone(ZoneOffset.UTC)
       .toLocalDate()
   ```

4. Persist the selected date.
5. Update the app and all widget instances after the write succeeds.

### Record a haircut today

1. Show a confirmation dialog.
2. On confirmation, store the current local date.
3. Update the app and all widgets after the write succeeds.
4. On cancellation, make no change.

### Widget interaction

1. Display:
   - Day count
   - Correct singular or plural text
   - Localized last-haircut date
2. Tapping the widget opens the app and requests the confirmation dialog.
3. The intent extra may only open the dialog; it must never mutate the stored date directly.
4. Consume and remove the extra after handling it so the dialog does not return after rotation, process restoration, or reopening from recents.

---

## 3. Scope boundaries

### Included

- Android 8.0/API 26 and later
- Kotlin
- Jetpack Compose app UI
- Glance app widget
- Material date picker
- `SharedPreferences`
- Calendar-day arithmetic with `java.time`
- Unit, instrumentation, and focused manual verification
- Light and dark theme support
- Localized date and quantity formatting

### Explicitly excluded

- Internet access
- Accounts or authentication
- Analytics, advertising, or telemetry
- Database or repository abstraction
- Dependency injection
- Navigation framework
- Services
- AlarmManager
- WorkManager
- Notifications
- Encrypted preferences
- Biometrics
- Obfuscation work beyond build defaults
- Multiple haircut records or history
- Cloud or device backup
- Exact-at-midnight widget refresh guarantees

---

## 4. Pinned build baseline

Use a checked-in version catalog and Gradle wrapper. Do not use dynamic versions or ask the implementation agent to discover the “latest mutually compatible” combination.

Initial pinned baseline:

| Component | Version |
|---|---:|
| Android Gradle Plugin | `9.3.0` |
| Gradle | `9.5.0` |
| JDK | `17` |
| Kotlin | `2.3.21` |
| Compose BOM | `2026.06.00` |
| Glance AppWidget | `1.1.1` |
| Compile SDK | `37` |
| Target SDK | `37` |
| Minimum SDK | `26` |

AGP 9.3 supports API 37 and uses Gradle 9.5.0 and JDK 17 as its compatibility baseline. The Compose BOM and Glance versions above are stable pinned releases rather than dynamic dependencies.

### Dependency rule

Before feature work:

```bash
./gradlew help
./gradlew assembleDebug
```

If the baseline does not build:

1. Capture the exact failure.
2. Change only the incompatible component.
3. Prefer a stable downgrade over an alpha, beta, or release candidate.
4. Record the final working versions in `libs.versions.toml` and the README.
5. Do not continue with an unverified dependency combination.

No dependency upgrades are part of this project after the baseline passes.

---

## 5. Project structure

Keep production Kotlin code flat and limited to approximately five files:

```text
app/src/main/java/<package>/
├── MainActivity.kt
├── HaircutStore.kt
├── DaysSince.kt
├── HaircutWidget.kt
└── DateConversion.kt
```

Suggested responsibilities:

### `MainActivity.kt`

- Compose screen
- Date picker
- Confirmation dialog
- Intent-extra handling
- App and widget refresh coordination

### `HaircutStore.kt`

- Read and write the stored epoch day
- First-run initialization
- Defensive validation and recovery
- No interface and no second implementation

### `DaysSince.kt`

- Pure calendar-day subtraction
- Negative-result clamping

### `DateConversion.kt`

- Pure UTC picker-millisecond conversion
- Localized date formatter helper if useful

### `HaircutWidget.kt`

- `GlanceAppWidget`
- `GlanceAppWidgetReceiver`
- Widget UI
- Widget-to-app action

Resource files may remain in the normal Android resource directories.

---

## 6. Data model and persistence

Store one value:

```text
last_haircut_epoch_day: Long
```

Use a private `SharedPreferences` file.

### Write behavior

Use `commit()`, not `apply()`, for this single small value.

The required order is:

1. Write the date with `commit()`.
2. Confirm that the write succeeded.
3. Update in-memory UI state.
4. Call the widget’s `updateAll()`.

Do not request a widget update before persistence completes.

If `commit()` fails:

- Keep the previous displayed value.
- Do not report success.
- Do not update the widget with an unpersisted value.
- Show a short non-destructive error message.

### Read behavior

A stored value is valid when:

- It can be converted with `LocalDate.ofEpochDay()`.
- It is not earlier than January 1, 1970.
- It is not later than tomorrow in the device’s current local calendar.

If the preference is missing, malformed, or outside that defensive range:

1. Initialize it to the current local date.
2. Persist the replacement.
3. Continue without crashing.

### Clock rollback and future dates

If an otherwise readable stored date is later than the current local date:

- Display `0 days`.
- Do not overwrite the stored date merely because the clock moved backward.
- A normal user-selected date remains limited to today or earlier.

The defensive “today plus one” storage bound allows small clock or timezone changes without immediately treating the preference as corrupt.

---

## 7. Date calculation

Use calendar dates, not elapsed milliseconds:

```kotlin
fun daysSince(lastHaircut: LocalDate, today: LocalDate): Long =
    ChronoUnit.DAYS.between(lastHaircut, today).coerceAtLeast(0)
```

Rules:

- Same date returns `0`.
- Yesterday returns `1`.
- Future dates display `0`.
- DST transitions must not affect the result.
- Leap days follow `LocalDate` calendar arithmetic.
- Reading the count must never mutate the stored date.

The app and widget must call the same pure function.

---

## 8. App UI

Use one screen with no navigation framework.

Display:

- Large day count
- Quantity-aware label from `plurals.xml`
- “Last haircut” followed by the localized stored date
- “Record haircut today” primary action
- “Choose date” secondary action

Use:

```kotlin
DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
```

Do not hardcode English date formats or manually concatenate `day`/`days`.

### Intent handling

Declare `MainActivity` with:

```xml
android:launchMode="singleTop"
```

Handle the widget request in both:

- `onCreate()`
- `onNewIntent()`

After recognizing the boolean extra:

1. Remove it from the intent.
2. Call `setIntent()` with the cleaned intent.
3. Set Compose state to open the confirmation dialog.

The extra must not contain or set a date.

---

## 9. Widget

Use a passive, stateless `GlanceAppWidget`.

The widget reads application state from `HaircutStore` whenever rendered; it must not rely on in-memory state. Glance widgets are recreated as needed, so application data must remain in persistent storage and explicit updates must be requested after data changes.

### Widget content

Display:

```text
17 days
Last haircut: Aug 2, 2026
```

For zero and one:

```text
0 days
1 day
```

The date remains visible so users can recognize a stale count without adding a background scheduler.

### Refresh policy

Refresh when:

- The date is initialized
- The user chooses a date
- The user confirms a haircut today
- The app resumes
- The launcher requests a widget update
- A manual or system periodic widget update occurs

Set `updatePeriodMillis` to `1800000` as a best-effort fallback. Android does not support a shorter periodic interval, and execution timing is not exact.

Do not add AlarmManager or WorkManager for the MVP. The README must explain that the widget may remain stale after midnight until the launcher/system refreshes it or the app is opened.

Do not depend on a manifest-registered `ACTION_DATE_CHANGED` receiver. Modern Android restricts many implicit manifest broadcasts, making that approach unsuitable as an exact refresh guarantee.

### Widget sizing and preview

The provider XML must include:

- `minWidth`
- `minHeight`
- `targetCellWidth`
- `targetCellHeight`
- `previewImage`
- Label or description resources
- Resize configuration only if the widget actually adapts

Treat `minWidth` and `minHeight` as the cross-version sizing contract. The cell-based attributes apply on newer Android versions, while `previewImage` supports older widget pickers.

### Widget receiver export behavior

Start with:

```xml
android:exported="false"
```

Then verify on the target emulator:

1. Widget appears in the picker.
2. Widget can be placed.
3. Launcher-triggered updates work.
4. Forced updates work.
5. Widget still works after reboot.

If those checks fail because the launcher cannot reach the receiver:

- Change it to `android:exported="true"`.
- Repeat the checks.
- Record the compatibility reason in the README.

Regardless of the final value, the receiver must:

- Remain stateless
- Hold no secrets
- Perform no direct date mutation
- Accept only required app-widget actions

### Pending intent

The widget launch action must produce an immutable pending intent.

Verify that the Glance-generated action uses immutable semantics. If a custom `PendingIntent` is introduced, require `FLAG_IMMUTABLE`.

---

## 10. Manifest and privacy

The final merged manifest must satisfy:

- No `<uses-permission>` entries
- `android:allowBackup="false"`
- No services
- No content providers
- No unrelated receivers
- No network security configuration
- Only the launcher activity and required widget receiver are exposed as needed

Do not add `INTERNET`, exact-alarm, notification, boot, storage, or calendar permissions.

The stored date does not justify encryption or authentication. Avoid adding key management and restoration failure modes to protect a non-sensitive integer.

---

## 11. Implementation sequence

### Phase 1 — Build gate

- Create the project and pinned version catalog.
- Configure the Gradle wrapper.
- Set SDK levels.
- Run `help`, `assembleDebug`, and unit tests.
- Resolve the build baseline before adding features.

### Phase 2 — Pure date logic

Implement:

- `daysSince()`
- UTC picker-millisecond conversion
- Localized date formatting

Add focused tests immediately.

### Phase 3 — Persistence

Implement:

- Missing-value initialization
- Defensive reads
- Synchronous writes
- Corruption recovery
- Future-date preservation

### Phase 4 — App screen

Implement:

- Count and date display
- Quantity resources
- Date picker
- Confirmation dialog
- Intent-extra consumption

### Phase 5 — Widget

Implement:

- Widget UI
- Stored-date read
- App launch action
- Provider XML
- Static preview
- Update-after-write behavior

### Phase 6 — Verification

Run automated checks, emulator checks, manifest inspection, persistence checks, and screenshot capture.

Do not add new architecture or features during verification.

---

## 12. Automated tests

Keep the test suite focused.

### JVM tests

1. Today produces `0`.
2. A known past date produces the expected positive count.
3. A future date produces `0`.
4. Date-picker UTC milliseconds convert to the intended `LocalDate`.
5. A leap-day boundary produces the expected calendar-day count.

### Instrumentation tests

6. Missing preference initializes to today.
7. Corrupt or out-of-range preference recovers without crashing.

Do not build a large suite that merely retests `java.time`.

Verify singular and plural resources through one UI/instrumentation assertion if already convenient; otherwise include them in the manual checklist rather than adding a new testing framework.

---

## 13. Manual verification

Use an API 26 Google APIs emulator and one current API emulator.

Prefer a non-Play Google APIs image when root access is needed for clock and preference inspection.

Perform these ten checks:

1. Clean install initializes the date to today and displays `0 days`.
2. Selecting yesterday displays `1 day`.
3. Selecting an older date displays the expected plural count.
4. Future dates cannot be selected.
5. Confirming “today” persists the value and refreshes the widget.
6. Cancelling confirmation makes no change.
7. Widget tap opens exactly one confirmation dialog.
8. Rotation, recents, and process recreation do not reopen a consumed dialog.
9. Data survives force-stop, reboot, and reinstall with `-r`.
10. Advancing the emulator by one calendar day changes the app from `0 days` to `1 day`, and a forced widget update produces the same result.

For the rollover check:

- Disable automatic time.
- Set the haircut date to the current emulator date.
- Confirm `0 days`.
- Advance the emulator clock by one calendar day.
- Relaunch the app and confirm `1 day`.
- Force a widget update and confirm `1 day`.

Restore automatic time afterward.

---

## 14. Verification commands

Run at minimum:

```bash
./gradlew clean
./gradlew test
./gradlew connectedDebugAndroidTest
./gradlew lintDebug
./gradlew assembleDebug
```

Lint errors block completion. Warnings must be reviewed and listed in the final report; they must not be silently dismissed.

### Persistence

```bash
adb shell am force-stop <package>
adb shell monkey -p <package> 1

adb reboot

adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Screenshot

```bash
adb exec-out screencap -p > widget.png
```

The screenshot must show the placed widget with both the day count and anchor date.

### Permission inspection

Locate and inspect the merged debug manifest:

```bash
find app/build/intermediates \
  -name AndroidManifest.xml \
  -path '*merged*' \
  -print
```

Then assert that the applicable merged manifest contains no permissions:

```bash
grep -n "<uses-permission" <merged-manifest-path>
```

Expected result: no matches.

Also inspect the packaged APK where tooling is available:

```bash
apkanalyzer manifest permissions \
  app/build/outputs/apk/debug/app-debug.apk
```

Expected result: no requested permissions.

---

## 15. README

Keep the README to six sections:

1. What the app does
2. Build requirements and pinned versions
3. How to build and run
4. How to add and use the widget
5. Verification performed
6. Known limitation: best-effort widget refresh after midnight

Do not duplicate this implementation plan in the README.

---

## 16. Deliverables

Required output:

```text
project/
├── app/
├── gradle/
├── gradlew
├── gradlew.bat
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── README.md
├── REVISED_PLAN.md
└── artifacts/
    └── widget.png
```

Include:

- Buildable source
- Pinned version catalog
- Gradle wrapper
- Tests
- Widget preview asset
- Home-screen widget screenshot
- Short verification report
- Any lint warnings that remain
- Final receiver `exported` value and the compatibility test result

---

## 17. Completion criteria

The project is complete only when:

- Debug APK builds from a clean checkout.
- Automated tests pass.
- Lint has no errors.
- The app and widget show the same count.
- Date-picker conversion is UTC-safe.
- Writes complete before widget updates.
- Corrupt preferences do not crash the app.
- Widget intent extras are consumed once.
- Persistence survives force-stop, reboot, and upgrade install.
- The merged manifest requests zero permissions.
- Backup is disabled.
- The widget works on API 26 and a current API emulator.
- Midnight staleness is documented rather than hidden by unnecessary scheduling.
- No excluded feature or architectural layer has been added.																																																																																																																																															                
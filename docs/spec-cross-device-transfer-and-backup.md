# Spec: Cross-Device Data & Settings Migration (Automated OS Sync & Offline SAF Backup)

## Problem Statement

CountUp is an intentionally small, 100% offline habit and milestone tracker. Users rely on the app to track meaningful multi-year life streaks, anchor dates, and habit notes. However, because `android:allowBackup` is currently disabled and no manual export mechanism exists, all items, streaks, cycle statistics, and theme preferences are permanently lost whenever a user upgrades to a new phone, reinstalls the app, or factory resets their device.

At the same time, CountUp users choose the app specifically for its zero-permission, privacy-first, and zero-account architecture. Any solution that introduces cloud user logins, remote backend servers, or intrusive device permissions would violate the core trust contract of the application. 

Furthermore, the user base spans diverse Android ecosystems worldwide: standard global devices with Google Mobile Services (GMS), de-Googled privacy ROMs (GrapheneOS, CalyxOS), and domestic Chinese Android devices (Xiaomi HyperOS, Huawei HarmonyOS, OPPO ColorOS, Vivo OriginOS, Honor MagicOS) that operate without GMS and feature customized OEM file managers. The migration architecture must work reliably across all these environments, failing gracefully and silently without ever throwing uncaught exceptions or error dialogs.

## Solution

A dual-tier migration architecture that combines automated native platform synchronization with self-sovereign offline file portability, hardened for global and domestic Chinese Android ecosystems:

1. **Tier 1: Invisible Platform-Native Auto Backup & Device-to-Device (D2D) Transfer**:
   - Re-enable standard Android backup support governed by strict cloud and D2D data extraction rules.
   - **GMS Devices**: During standard new phone setup (via cable or local transfer), the Android operating system migrates the user's primary items database and preferences directly between devices. For users with Google account backup enabled, an encrypted snapshot syncs securely to their private Google Drive backup storage and restores on reinstall.
   - **Non-GMS & Chinese OEM Devices**: On devices without Google Play Services, cloud backup is silently bypassed at the OS level with zero errors or crashes. Setting `android:allowBackup="true"` explicitly authorizes domestic OEM migration tools (Xiaomi Mi Mover / 换机克隆, Huawei Phone Clone, Vivo EasyShare) to migrate the app's local database during device-to-device phone cloning.
   - A startup sanitizer automatically detects and purges stale home-screen widget launcher IDs from the previous device, preventing orphaned widget states on new launchers.

2. **Tier 2: Self-Sovereign Offline Backup & Restore via Storage Access Framework (SAF)**:
   - Provide a peaceful, non-intrusive "Data & Backup" interface in the app settings.
   - **Export**: The user can create a standardized, unencrypted, human-readable `.json` backup file using Android's system document creation picker (`ACTION_CREATE_DOCUMENT`), saving it to their local storage, Downloads, an SD card, external drive, or personal cloud storage.
   - **Import**: The user can select a `.json` backup file via the system document picker (`ACTION_OPEN_DOCUMENT`). The app validates the file, presents a summary preview of the discovered items, and allows the user to choose between merging new items into their current list or performing a clean full restore.
   - **OEM Resilience ("Never Error Out")**:
     - System file picker launches are wrapped in defensive exception guards (`ActivityNotFoundException`) to handle stripped or customized OEM ROMs without crashing.
     - Document picker queries use permissive MIME type matching (`application/json`, `text/plain`, `*/*`) to bypass OEM file manager filtering bugs that grey out `.json` files.
     - All stream serialization explicitly enforces `UTF-8` encoding, ensuring flawless fidelity for Chinese characters, localized dates, and custom notes.
   - Zero permissions requested: all file operations execute through scoped content URIs granted by the system file picker without requiring storage permissions.

## User Stories

1. As a habit tracker getting a new phone, I want my active count-ups and accumulated streaks to transfer automatically during device setup, so that I do not lose months of progress.
2. As a privacy-focused user, I want the app to migrate my data without requiring me to create an account, register an email, or log into a server, so that my personal habits remain private.
3. As an offline utility user, I want my data backup to work without internet permissions, so that I can verify the app cannot transmit my habit names to third parties.
4. As a user upgrading devices, I want my custom appearance settings (sort order, theme mode, and ink wash background) to be restored alongside my items, so that the app looks familiar immediately.
5. As a home-screen widget user, I want restoring a backup on a new device to clean up invalid widget bindings, so that placing new widgets on the new launcher does not produce broken or mismatched counters.
6. As a cautious user preparing for a device factory reset, I want to export my items and settings to a standalone JSON file, so that I have a local archival copy independent of cloud services.
7. As an advanced Android user on a de-Googled ROM, I want a manual file-based backup and restore option, so that I can migrate my data without relying on Google Play Services.
8. As a user of a domestic Chinese Android phone (Xiaomi, Huawei, OPPO, Vivo), I want the app to operate smoothly without Google Play Services, so that I never see errors or crashes caused by missing cloud frameworks.
9. As a Chinese phone user using an OEM migration utility (Mi Mover, Phone Clone), I want my count-up data to be included in device-to-device migration, so that my new phone is ready immediately after cloning.
10. As a user with Chinese habit names and notes, I want characters such as "理发", "健身", and Solar Terms ("清明", "立春") to be preserved perfectly across backups, so that none of my records are garbled.
11. As a user on a phone with an unusual or customized file manager, I want the file picker to recognize my backup file even if the OEM system misclassifies JSON as plain text, so that my file is not greyed out or unselectable.
12. As a user on a stripped custom ROM lacking a system file picker, I want the app to show a gentle in-app notice instead of crashing, so that my app experience remains stable.
13. As a user with multiple devices, I want to share my backup file to another phone and import it, so that I can replicate my tracking list on secondary hardware.
14. As a user restoring an old backup file, I want to see a clear preview of how many items were found in the file before confirming, so that I do not accidentally restore an unintended archive.
15. As a user with existing items on my new phone, I want the choice to merge imported items instead of replacing everything, so that I do not overwrite recently added counters.
16. As a user merging a backup, I want the app to detect duplicate items by identifier and name, so that merging does not create duplicate entries for the same habit.
17. As a user importing a partially damaged or truncated backup file, I want the app's salvage engine to recover as many valid items as possible, so that minor file corruption does not cause complete data loss.
18. As a user selecting backup files, I want the app to use the native Android system file picker, so that I never have to grant broad storage permissions to the app.
19. As a user who accidentally selects a non-backup file, I want an informative message explaining that the file format is invalid, so that I understand what went wrong without the app crashing.
20. As a user completing a successful restore, I want my home-screen widgets to update immediately with the restored items, so that my glanceable counters reflect my restored data right away.
21. As a minimalist user, I want the backup and restore options quietly nested within the existing settings menu, so that the primary screen remains serene and uncluttered.
22. As a TalkBack / screen reader user, I want all backup actions, dialogs, and restore previews to be fully accessible with descriptive semantic labels, so that I can manage my backups non-visually.

## Implementation Decisions

### 1. Platform-Native Backup Configuration (Tier 1)
- The application manifest will set `android:allowBackup="true"` and delegate file inclusion/exclusion to dedicated resource files (`data_extraction_rules.xml` and `backup_rules.xml`).
- Inclusion rules will target `countup_prefs.xml` and the atomic secondary snapshot `countup_backup.json`.
- Legacy migration files (`haircut_prefs.xml`) and temporary write files (`countup_backup.json.tmp`) are explicitly excluded.
- On non-GMS devices, Android's OS-level backup daemon remains inactive with zero app-level overhead or errors. Setting `allowBackup="true"` allows domestic Chinese OEM migration tools (Xiaomi Mi Mover, Huawei Phone Clone, etc.) to copy private app files during device setup.
- A widget binding sanitizer will run during application startup: it queries `AppWidgetManager` for active widget IDs across all installed providers and purges any stored binding key whose associated ID no longer exists on the current device.

### 2. Standalone Backup Payload Schema (Tier 2)
- A decoupled, versioned transfer container will encapsulate portable application state.
- Device-specific launcher widget IDs and transient undo stacks will be excluded from the export envelope.
- Prototype type shape:
  ```kotlin
  data class CountUpBackupPayload(
      val schemaVersion: Int,
      val exportTimestamp: Long,
      val appVersion: String,
      val sortOrder: SortOrder,
      val themeMode: ThemeMode,
      val backgroundTheme: BackgroundTheme,
      val items: List<CountUpItem>,
  )
  ```
- JSON serialization will produce human-readable, formatted output bound strictly to `Charsets.UTF_8` to guarantee character integrity for Chinese and international text.
- Deserialization will integrate with the app's existing fault-tolerant parser: if standard array decoding encounters syntax issues, it delegates to the token scanner to salvage all self-contained valid items.

### 3. Non-GMS & Domestic Chinese OEM Defensive Guards
- **Defensive Picker Launch**: Calls to `registerForActivityResult` launchers (`ACTION_CREATE_DOCUMENT`, `ACTION_OPEN_DOCUMENT`) will be wrapped in `try-catch` handling `ActivityNotFoundException` and `SecurityException`. If an OEM ROM lacks a functional system document picker, the app emits an in-app error snackbar rather than crashing.
- **Permissive MIME Type Matching**: To prevent OEM file managers (such as older MIUI / ColorOS file pickers) from greying out `.json` files, the document picker will query multiple MIME types:
  ```kotlin
  arrayOf("application/json", "text/plain", "text/*", "*/*")
  ```
  Payload validation occurs downstream via the JSON parser, regardless of the MIME classification applied by the OEM file manager.
- **Strict Character Encoding**: All streams (`InputStream`, `OutputStream`, `BufferedReader`, `BufferedWriter`) will explicitly specify `Charsets.UTF_8` to avoid OEM system default charset variances.

### 4. Restore Strategy Behavior
- The restore engine will support two explicit strategies:
  - **Replace All**: Clears existing items and replaces them entirely with the payload items. Also restores the exported sort order, theme mode, and background theme.
  - **Merge Keep Existing**: Retains all existing items on the current device. Examines imported items and appends only those with novel IDs and distinct normalized names. Preserves the current device's user preferences.

### 5. Architecture & State Management
- Follow unidirectional MVI data flow with state hoisting.
- All backup payload generation, JSON parsing, and stream I/O will execute on background I/O coroutine dispatchers.
- The UI state will reflect dialog visibility, parsing/processing indicators, and any pending candidate payload awaiting user strategy selection.
- Completing a restore will update repository state, commit to disk, notify the UI via a feedback message, and dispatch a widget update effect.

## Testing Decisions

### What Makes a Good Test
- Tests must verify external behavior, contracts, and data resilience without asserting on internal private methods.
- Every restore path must demonstrate zero data loss: valid items must survive round trips, and malformed files must be handled gracefully without exceptions or corrupted states.

### Modules to Test
1. **Payload Serialization & UTF-8 Round-Trip**: Verify that encoding a full backup payload containing Chinese characters ("理发", "健身", "清明", "立春"), emoji icons, and multi-line notes survives a full decode round-trip with zero loss or corruption.
2. **Salvage & Malformed Input Handling**: Verify that feeding truncated, syntactically broken, or foreign JSON payloads into the decoder safely yields either a salvaged partial list of valid items or a safe null, without throwing uncaught exceptions.
3. **Restore Strategy Mechanics**: Verify that "Replace All" overwrites state as expected and that "Merge Keep Existing" correctly ignores duplicates by ID and normalized name while inserting novel items.
4. **Widget Binding Sanitization**: Verify that passing a simulated list of active system widget IDs purges dead launcher bindings from storage while preserving valid active bindings and items.
5. **Non-GMS & Defensive Error Handling**: Verify that missing file picker activities or invalid file formats trigger appropriate user feedback without throwing uncaught exceptions or application crashes.
6. **Contract & Manifest Invariants**: Verify that backup XML configurations correctly target the expected files and that release builds retain zero permissions.

### Prior Art
- Builds directly upon `CountUpStoreTest`, `CountUpItemTest`, and `WidgetContractInvariantsTest`.

## Out of Scope

- Remote cloud synchronization servers, WebDAV, or third-party cloud SDK integrations.
- QR-code camera-based air-gapped data transfer (which would require unwanted camera permissions).
- Direct peer-to-peer Wi-Fi or Bluetooth local socket streaming (which would require local network permissions).
- Automated cross-device widget placement (Android launchers do not permit programmatic widget placement across different devices).

## Further Notes

- By combining OS-level auto-backup with an explicit SAF JSON export hardened with OEM defensive guards, the application solves both passive user upgrades (seamless new phone unboxing or OEM phone clone) and intentional user migrations (manual archiving and cross-account movement) while strictly maintaining its zero-permission, offline-first promise.

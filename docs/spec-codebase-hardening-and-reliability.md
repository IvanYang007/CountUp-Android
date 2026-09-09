# Spec: Codebase Hardening, Reliability & Audit Remediation

## Problem Statement

A thorough audit of the CountUp codebase identified 12 critical and reliability findings (P0-1 through P0-4 and P1-1 through P1-8) spanning system alarm lifecycles, licensing, credential hygiene, widget state volatility, build reproducibility, merge integrity, and documentation accuracy:

1. **Alarm Precision & Permission Contradiction (P0-1)**: The documentation asserts a `00:00:01` midnight rollover, but modern Android (API 31+) denies exact alarms by default without explicit permissions. While the app defensively avoids crashing by falling back to inexact execution, non-wakeup RTC alarms allow rollover to slip during Doze mode, contradicting promotional claims of exact rollover.
2. **Missing Repository License (P0-2)**: The repository lacks an open-source `LICENSE` file. While third-party icon licenses are noted, the core codebase defaults to "All Rights Reserved", legally blocking forks, community contributions, and redistribution.
3. **Historical Keystore Password Exposure & Artifact Clutter (P0-3)**: Keystore credentials (`keystore/keystore-pass.txt`) were historically committed to Git history (commit `32f6f36`) before being untracked in `625f2c3`, meaning release signing credentials remain exposed in repository packfiles. Furthermore, 90+ binary artifacts and screenshots remain tracked in Git despite `.gitignore` rules.
4. **Streak Reset Discovery & In-Place Recovery (P0-4)**: Accidental two-tap resets from the launcher can zero long-standing streaks. Although an undo mechanism exists in the data layer and in-app UI, the widget provides no visual or interactive recovery, and the user documentation completely omits undo availability.
5. **Reboot Alarm Amnesia (P1-1)**: Because `RECEIVE_BOOT_COMPLETED` is explicitly stripped for zero-permission compliance, device reboots wipe all system alarms. None of the five widget providers reschedule the alarm in their update cycles, leaving widget counters stale after a reboot until the user manually launches the main application.
6. **Binder Payload & IPC Resilience (P1-2)**: While the multi-item grid widget bounds its dynamic ink wash canvas to ~374 KB, passing high-resolution bitmaps across Binder IPC presents memory pressure on OEM launchers (Samsung One UI, Xiaomi HyperOS).
7. **Fragile AGP 9 Kotlin Buildscript Override (P1-3)**: Overriding AGP 9's built-in Kotlin compiler by injecting `org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.21` via the buildscript classpath is an unsupported configuration that risks build pipeline failures on minor Gradle or AGP patches.
8. **Absence of Continuous Integration (P1-4)**: The repository claims 335 passing tests and 0 lint errors, but lacks automated GitHub Actions workflows to verify pull requests and guarantee release builds.
9. **Machine-Coupled Build Documentation (P1-5)**: Build guides mandate personal Windows workstation directories (`D:\Android\Sdk`, local Temurin paths, and named AVDs) rather than portable Gradle toolchains and standard environment variables.
10. **Volatile Widget Arming Across Process Death (P1-6)**: The two-tap reset arming state is held in static JVM memory keyed only by item ID. Process termination between taps wipes the armed state, and identical items displayed across multiple widgets unintentionally cross-arm each other.
11. **Stale Pinned Dependencies (P1-7)**: Dependencies are pinned with no automated dependency update tooling (Dependabot/Renovate), leaving security patches and bugfixes unnoticed.
12. **Destructive Name-Colliding Backup Merge (P1-8)**: The backup merge algorithm rejects incoming records if their normalized name matches an existing item, even when their unique IDs and anchor dates represent completely distinct events (e.g., separate "Haircut" or "Anniversary" trackers).

## Solution

Deliver a comprehensive hardening and reliability upgrade to resolve all 12 findings without compromising CountUp's defining core principles: **100% offline, zero runtime permissions, no background daemons, and calm Zen aesthetics**:

1. **Clarify Midnight Rollover & Battery Lifecycle**: Align documentation and code to explicitly describe the power-efficient, battery-friendly inexact non-wakeup RTC alarm strategy that respects Android Doze mode while refreshing reliably upon device wake.
2. **Establish Open-Source Licensing**: Add an Apache 2.0 / MIT dual license to the root repository, formally liberating the project for community fork and contribution.
3. **Cleanse Repository History & Credentials**: Rotate the production keystore, generate a fresh signing key, purge credential residue from Git history, and untrack committed artifacts from repository index.
4. **Expose Widget Reset Recovery & Undo Transparency**: Document the multi-tier undo system in user guides, and introduce a prominent recovery cue that directs users to immediate streak restoration.
5. **Idempotent Reboot Rescheduling via Widget Lifecycle**: Equip all widget provider update cycles with idempotent midnight alarm re-registration so counters automatically regain midnight updates after reboot without requiring `RECEIVE_BOOT_COMPLETED`.
6. **Vector & Resource Optimization for Binder IPC**: Transition widget background rendering toward pre-sized vector drawable resources and strict memory budgets, eliminating large bitmap IPC parcel overhead.
7. **Standardize AGP 9 & Kotlin Configuration**: Align Kotlin Gradle Plugin usage with supported AGP 9 conventions using declarative plugins DSL and AGP bundled compiler versions.
8. **Establish Automated CI Workflow**: Deploy a zero-maintenance GitHub Actions workflow running `./gradlew test lintDebug assembleRelease` on every push and pull request.
9. **Portable Toolchains & Universal Build Docs**: Adopt Gradle Java Toolchains (Java 17) and replace workstation-specific paths with standard environment variable references.
10. **Persist Widget Transient State Keyed by Widget Instance**: Move reset-arming and interactive transient states to lightweight, persistent key-value storage scoped strictly to `(appWidgetId, itemId)` with automatic expiration and cleanup.
11. **Automate Dependency Security Monitoring**: Introduce Dependabot configuration to monitor Compose BOM, AGP, and core libraries for security patches.
12. **UUID-Authoritative Backup Merge**: Revise the backup merge deduplication strategy to rely strictly on immutable UUIDs, allowing legitimately shared names across different counters while offering smart duplicate suggestions.

## User Stories

1. As a contributor, I want a clear open-source LICENSE file in the repository root, so that I can legally fork, inspect, and contribute improvements.
2. As a security-conscious user, I want the project's release signing keys and passwords to be secure and never exposed in repository history, so that I can trust release APK integrity.
3. As an everyday user, I want my widget counters to advance reliably across midnight without draining my battery or requiring background location/wake permissions.
4. As a device owner, I want my widget counters to resume daily advancement automatically after my phone reboots, even if I have not opened the app yet.
5. As a habit tracker, I want to understand exactly how midnight updates work in low-power Doze mode, so that I know what to expect when waking up in the morning.
6. As a person tracking sobriety or major life milestones, I want accidental widget taps to be instantly recoverable, so that I never lose years of progress from an unintentional screen tap.
7. As a user who accidentally reset a streak from a widget, I want clear instructions in the app and documentation showing me how to restore my previous anchor date.
8. As a user with multiple widgets on my home screen, I want tapping a reset on one widget to only affect that specific widget instance, so that other widgets showing the same milestone do not enter an armed state unexpectedly.
9. As a user on a low-memory device, I want widget reset confirmations to survive brief process termination between double taps, so that my confirmation tap is never dropped.
10. As a user on a Samsung One UI or Xiaomi HyperOS device, I want widgets to update smoothly without failing or throwing Binder memory errors, so that my launcher stays fast and responsive.
11. As a developer, I want the Gradle build to use supported AGP and Kotlin plugin configurations, so that future tooling updates do not break project compilation.
12. As an open-source contributor, I want GitHub Actions to run automated tests and lint checks on every pull request, so that code quality and passing assertions are publicly verifiable.
13. As a developer on macOS or Linux, I want the build documentation and Gradle scripts to build cleanly using Java toolchains without hardcoded Windows drive letters.
14. As a project maintainer, I want automated dependency security pull requests via Dependabot, so that upstream security patches in Compose and AndroidX are never missed.
15. As a user restoring data from another device, I want the merge feature to preserve multiple counters that share the same name (such as separate habits or family milestones), so that distinct records are never silently dropped.
16. As an accessibility user, I want TalkBack to clearly announce widget reset states and undo possibilities, so that I have full confidence in my milestone tracking.
17. As a user with multiple widgets of different sizes, I want unit cycling on horizon ribbons to persist across device reboots and launcher restarts.
18. As an offline-first advocate, I want all reliability fixes to strictly preserve zero-permission, zero-network, and zero-daemon operation.

## Implementation Decisions

### 1. Alarm Lifecycle & Zero-Permission Clarification
- Maintain strict zero-permission policy: do not request `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`, or `WAKE_LOCK`.
- Retain `setAndAllowWhileIdle(AlarmManager.RTC, ...)` fallback on Android 12+.
- Update documentation and technical handoff to accurately state that midnight rollover is a battery-friendly, low-power RTC alarm that aligns with maintenance windows during Doze, updating immediately upon device screen wake.

### 2. Legal & Open-Source Licensing
- Place a standard `LICENSE` file (Apache License 2.0) in the root of the repository.
- Ensure all source headers and documentation clearly reference the license terms.

### 3. Credential Rotation & History Cleansing
- Generate a new release signing keystore and update local build properties.
- Purge `keystore/keystore-pass.txt` from Git history using `git-filter-repo` or BFG Repo-Cleaner before public publishing.
- Remove tracked binary files from the Git index in `artifacts/` while keeping them ignored by `.gitignore`.

### 4. Streak Protection & Undo UX Transparency
- Update `README.md` and user-facing documentation to explicitly explain the 3-slot widget reset recovery banner and in-card undo whisper.
- Add an in-app visual prompt that triggers when a widget reset occurs, offering a 1-tap undo action.
- Ensure the snapshot data structure (`ResetSnapshot`) retains the complete pre-reset state (`epochDay`, `resetCount`, `totalResetDays`, `futureFlag`).

### 5. Idempotent Post-Reboot Alarm Restoration
- Add an idempotent alarm registration call inside `onUpdate()` and `onEnabled()` across all five widget providers (`CountUpWidgetReceiver`, `HeroWidgetReceiver`, `ZenHorizonWidgetReceiver`, `SolarRhythmWidgetReceiver`, `ZenPebbleWidgetReceiver`).
- When the launcher refreshes widgets after device boot, the alarm is automatically and idempotently re-armed without requiring `RECEIVE_BOOT_COMPLETED`.

### 6. Widget IPC Memory Budget Hardening
- Enforce strict byte budget limits across all widgets via the existing `WidgetMemoryBudgetGate`.
- For the multi-item grid widget (`CountUpWidget`), investigate migrating dynamic ink wash backgrounds to pre-rendered vector drawable resources or compressing bitmaps to WebP/palette indices to reduce IPC parcel size from ~375 KB to < 50 KB.

### 7. AGP 9 & Kotlin Tooling Stabilization
- Refactor `build.gradle.kts` to remove the manual `buildscript` classpath injection of `kotlin-gradle-plugin`.
- Standardize on AGP 9's native built-in Kotlin compiler integration and verify compatibility with the Compose compiler plugin.

### 8. GitHub Actions Continuous Integration
- Add `.github/workflows/ci.yml` targeting `ubuntu-latest` with Temurin JDK 17.
- Automate execution of `./gradlew test lintDebug assembleRelease` on every pull request and push to `main`.
- Upload test reports and lint results on failure.

### 9. Build Portability & Toolchain Integration
- Add `java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }` to `app/build.gradle.kts`.
- Rewrite `README.md` and `TECH_HANDOFF.md` setup sections to instruct developers to configure `ANDROID_HOME` / `sdk.dir` in `local.properties` rather than hardcoding personal machine paths.

### 10. Instance-Scoped Persistent Widget State
- Replace volatile in-memory static variables in `ResetCountReceiver` with persistent key-value storage (SharedPreferences) keyed by compound identifier `(appWidgetId, itemId)`.
- Enforce a 1500 ms timestamp threshold on stored arming records.
- Clean up transient states in `onDeleted()` across widget receivers to prevent orphaned keys.

### 11. Automated Dependency Security Alerts
- Create `.github/dependabot.yml` configured to check Gradle dependencies weekly.
- Target Compose BOM, AGP, Kotlin, and AndroidX core libraries.

### 12. UUID-Authoritative Deduplication in Backup Merge
- Modify `CountUpStore.restoreBackupPayload` for `RestoreStrategy.MERGE_KEEP_EXISTING`:
  - Rely exclusively on immutable `id` (UUID) to identify duplicates.
  - If an incoming item has an existing name but a different UUID, import it with a disambiguated label (or preserve original name) rather than silently dropping the milestone.

## Testing Decisions

### Test Boundary & High Seam Selection
- **Highest Seam**: Test through `CountUpRepository` and `CountUpViewModel` using Turbine and Coroutines Test Dispatchers for business and state logic.
- **Widget Seam**: Test through `AppWidgetProvider` lifecycle events (`onUpdate`, `onReceive`, `onDeleted`) and verify state persistence and `RemoteViews` integrity.
- **Deduplication Seam**: Test `CountUpStore` merge operations with duplicate names, duplicate UUIDs, and cross-device datasets.

### Quality Standards for Good Tests
- Tests must verify observable behavioral outcomes, not internal private helper calls.
- Concurrency tests must verify atomic file operations under parallel threads.
- IPC memory tests must assert serialized byte sizes against Android Binder thresholds.

### Prior Art
- Builds directly upon `CountUpViewModelTest`, `CountUpStoreTest`, `WidgetMemoryBudgetGateTest`, and `EdgeCaseMatrixTest`.

## Out of Scope

- Introducing network permissions or cloud synchronization accounts.
- Introducing `WorkManager` or persistent foreground background services.
- Adding third-party crash reporting or analytics SDKs.
- In-place multi-step widget configuration wizards on the home screen.

## Further Notes

- The project remains committed to zero runtime permissions and zero tracking.
- Every architectural adjustment must maintain compatibility across Android 8.0 (API 26) through Android 15/16 (API 36/37).

# Feature Specification: Decoupled Control Surface (Focused Sort/Search, External 1-Tap Theme Toggle, and Dedicated Data Settings)

## 1. Problem Statement & Motivation

CountUp's previous sub-header control grouped four logically distinct features into a single composite trigger (`Sort:Days ↓ · ☀️ ▾ 🔍`) and an oversized "junk drawer" popup menu:
1. Instant keyword search (transient list filter).
2. Sort order selection (list view configuration).
3. Appearance theme toggle (global environmental preference).
4. Data backup export and restore (administrative storage I/O).

This composite architecture introduced three critical user-experience and cognitive defects:
- **Category Confusion:** Users searching for data export or dark mode do not expect these controls to be nested inside a dropdown labeled "Sort".
- **Visual & Tap Clutter:** The trigger button crammed three separate glyphs (`Sort:`, `☀️`, `▾`, `🔍`) into a single touch target, creating visual noise and ambiguous affordances.
- **Cognitive Overload in the Popup:** The popup menu was tall and crowded, combining ephemeral search input with permanent system settings and destructive restore actions.

---

## 2. Requirements & Desired Architecture

Based on the architectural review and design requirements:

1. **Focused Sort & Search Pop-up Menu:**
   - Retain **both** live keyword search and sort order selection inside the pop-up menu.
   - **Remove the magnifying glass (`🔍`) icon** from the sub-header trigger pill.
   - Strip the `APPEARANCE` and `DATA BACKUP` sections out of the pop-up menu completely, leaving a clean, focused, and fast list-filtering surface.

2. **External 1-Tap Theme Toggle:**
   - Move the theme toggle outside the menu, placed directly adjacent to the sort pill in the sub-header.
   - Tapping the icon cycles sequentially through the three theme modes:
     $$\text{Auto (System ⚡)} \longrightarrow \text{Light (☀️)} \longrightarrow \text{Dark (🌙)} \longrightarrow \text{Auto (System ⚡)}$$
   - The icon dynamically updates on every press to reflect the active mode.
   - Applies immediate theme updates and triggers tactile haptic feedback.

3. **Dedicated Settings Icon at Former Magnifying Glass Position:**
   - Place a dedicated Settings gear icon (`⚙`) in the sub-header control cluster at the exact position previously occupied by the magnifying glass (`🔍`).
   - Tapping the `⚙` icon opens a dedicated, elegant **Data & Backup Settings Dialog**.
   - The Settings dialog contains **only**:
     - **Header / Title:** "Data & Backup" with a clean dismiss action.
     - **Export Backup:** Title, description ("Save a JSON backup of items and appearance settings"), and export arrow indicator (`↗`).
     - **Restore Backup:** Title, description ("Import and restore items from a JSON backup file"), and import arrow indicator (`↙`).
     - **Footer:** Versioning and privacy assurance (`CountUp v%s · 100% Offline & Private`).

4. **Preserve Background Change Logic:**
   - Do **NOT** modify or interfere with the background wallpaper cycling mechanism.
   - The Enso circle button (`ic_zen_enso`) in the primary top header retains its exact existing `onCycleBackground` callback and behavior.

---

## 3. Visual Layout & UI Topology

### Main Screen Sub-Header Layout (Before vs. After)

#### Before (Composite Franken-Pill):
```
┌────────────────────────────────────────────────────────────────────────┐
│  3 items                                       [ Sort:Days ↓ · ☀️ ▾ 🔍 ]│
└────────────────────────────────────────────────────────────────────────┘
                                                         │
                                                         ▼ Tap opens:
                                           ┌───────────────────────────┐
                                           │  🔍 [ Search habits... ]  │
                                           │  SORT ORDER               │
                                           │  APPEARANCE (Auto/Sun/Moon│
                                           │  DATA BACKUP (Export/Imp) │
                                           └───────────────────────────┘
```

#### After (Decoupled, Focused Controls):
```
┌────────────────────────────────────────────────────────────────────────┐
│  3 items                        [ Sort: Days ↓ ▾ ]   ·   [ ☀️ ]   [ ⚙ ]│
└────────────────────────────────────────────────────────────────────────┘
                                          │                   │       │
                  ┌───────────────────────┘                   │       │
                  ▼                                           ▼       ▼
       ┌──────────────────────────┐                 Cycles:       Opens:
       │ 🔍 [ Search habits...  ] │                 ⚡ ➔ ☀️ ➔ 🌙   ┌───────────────────────────┐
       │                          │                               │ DATA & BACKUP             │
       │ SORT ORDER               │                               ├───────────────────────────┤
       │ ✓ Days elapsed           │                               │ Export Backup           ↗ │
       │   Anchor date            │                               │ Save JSON snapshot        │
       │   Name A–Z               │                               ├───────────────────────────┤
       │                          │                               │ Restore Backup          ↙ │
       └──────────────────────────┘                               │ Import from JSON file     │
                                                                  ├───────────────────────────┤
                                                                  │ CountUp v2.18 · 100% Off..│
                                                                  └───────────────────────────┘
```

---

## 4. Detailed Component Specifications

### 4.1 SubHeaderRow Component (`CountUpContent.kt`)

#### Component Signature:
```kotlin
@Composable
private fun SubHeaderRow(
    itemCount: Int,
    sortOrder: SortOrder,
    themeMode: ThemeMode,
    searchQuery: String,
    isMenuOpen: Boolean,
    onToggleMenu: (Boolean) -> Unit,
    onSearchChanged: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSelectSortOrder: (SortOrder) -> Unit,
    onCycleThemeMode: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
)
```

#### Controls Breakdown:
1. **Item Count Display:**
   - Left-aligned label: `"3 items"` (or localized singular/plural).
2. **Sort & Search Pill:**
   - Displays `Sort: ` (muted ink) + active sort label (e.g. `Days ↓` in primary accent) + `▾` caret.
   - Semantics: `"Sort and search options. Currently sorted by %s. Tap to search or change sort."`
   - Tactile feedback: `Modifier.pressScale()`.
   - Anchors `DropdownMenu` containing only Search input and `SortOrder` choices.
3. **Mid-Separator Dot:**
   - Subtle `·` glyph with `MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)`.
4. **Theme Mode Cycle Button:**
   - Displays the current mode's glyph:
     - `ThemeMode.SYSTEM` $\rightarrow$ `⚡`
     - `ThemeMode.LIGHT` $\rightarrow$ `☀️`
     - `ThemeMode.DARK` $\rightarrow$ `🌙`
   - Single tap invokes `onCycleThemeMode()`, cycling:
     ```kotlin
     val nextMode = when (themeMode) {
         ThemeMode.SYSTEM -> ThemeMode.LIGHT
         ThemeMode.LIGHT -> ThemeMode.DARK
         ThemeMode.DARK -> ThemeMode.SYSTEM
     }
     ```
   - Tactile feedback: `LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.LongPress)` or `TextHandleMove`.
   - Semantics: `"Theme mode: %s. Tap to cycle theme."`
5. **Settings Button (`⚙`):**
   - Positioned at the exact slot previously occupied by the search canvas drawing.
   - Circular touch target ($32\times 32\,\text{dp}$ with $48\times 48\,\text{dp}$ touch target padding).
   - Renders a clean 6-tooth gear vector or Material icon.
   - Tactile press scale + haptic click.
   - Single tap invokes `onOpenSettings()`.
   - Semantics: `"Settings and data backup"`.

---

### 4.2 Popover Dropdown Menu (`CountUpContent.kt`)

- **Container:** `DropdownMenu` with `zenColors.paperBackground` (Light) / `zenColors.paperSurface` (Dark), rounded corners (12dp), and hairline border.
- **Section 1: Search Field:**
  - `BasicTextField` with leading search icon (`Canvas`), hint text `"Search habits…"`, and clear button (`✕`).
  - Typing updates `searchQuery` immediately with live filtering on the main list.
- **Section 2: Sort Order List:**
  - Section header: `"SORT ORDER"`.
  - Three rows (`Days ↓`, `Date ↓`, `Name A–Z`).
  - Active item displays checkmark (`✓`) and accent tint.
  - Tapping an item persists `SortOrder` and closes the dropdown.
- **Removed Sections:**
  - `APPEARANCE` header and segmented control are **removed**.
  - `DATA BACKUP` header and Export/Restore rows are **removed**.

---

### 4.3 Data & Backup Settings Dialog (`CountUpDialogs.kt`)

#### Component Signature:
```kotlin
@Composable
fun DataBackupSettingsDialog(
    appVersion: String,
    onDismiss: () -> Unit,
    onExportBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    modifier: Modifier = Modifier,
)
```

#### Layout & Styling:
- **Container:** Standard `AlertDialog` utilizing `zenDialogStyle(RoundedCornerShape(22.dp))`.
- **Title Row:**
  - Label: `"DATA & BACKUP"` in bold sans-serif, letter-spaced.
  - Trailing dismiss button: `✕` circle button.
- **Body Content:**
  - **Export Card:**
    - Title: `"Export Backup"`.
    - Subtitle: `"Save a JSON backup of items and appearance settings"`.
    - Trailing arrow: `↗`.
    - On click: Closes dialog and triggers SAF export intent (`onExportBackup()`).
  - **Restore Card:**
    - Title: `"Restore Backup"`.
    - Subtitle: `"Import and restore items from a JSON backup file"`.
    - Trailing arrow: `↙`.
    - On click: Closes dialog and triggers SAF document open intent (`onRestoreBackup()`).
- **Footer:**
  - Muted secondary caption: `"CountUp v%s · 100% Offline & Private · Zero Trackers"`.

---

## 5. State Management & Flow (`CountUpViewModel.kt` & `MainActivity.kt`)

1. **State Properties:**
   - `isSearchSortMenuOpen: Boolean` — controls sort/search dropdown menu.
   - `isSettingsDialogOpen: Boolean` — controls Data & Backup dialog.
   - `themeMode: ThemeMode` — current active appearance mode.
2. **Event Handling:**
   - `Event.CycleThemeMode`: Computes next theme mode, persists to `CountUpStore.setThemeMode()`, and updates StateFlow.
   - `Event.SetSettingsDialogVisible(Boolean)`: Toggles settings dialog visibility.
   - `Event.ExportBackupRequested`: Dispatches SAF create document launcher.
   - `Event.ImportBackupRequested`: Dispatches SAF open document launcher.
3. **Background Cycling:**
   - `onCycleBackground` callback in `MainActivity.kt` and `CountUpContent.kt` remains completely untouched.

---

## 6. Accessibility & TalkBack Semantics

1. **Sort & Search Pill:**
   - `contentDescription = stringResource(R.string.cd_sort_search_pill, stringResource(sortOrder.labelRes))`
2. **Theme Cycle Button:**
   - `contentDescription = stringResource(R.string.cd_theme_cycle_button, stringResource(themeMode.labelRes), stringResource(nextThemeMode.labelRes))`
   - Clearly informs screen reader users of the current state and what will happen upon activation.
3. **Settings Button:**
   - `contentDescription = stringResource(R.string.cd_settings_button)`
4. **Touch Target Compliance:**
   - All interactive controls enforce `Modifier.minimumInteractiveComponentSize()` ($\ge 48\times 48\,\text{dp}$) to ensure zero accessibility touch target violations.

---

## 7. Verification & Testing Plan

1. **Unit Tests (`CountUpViewModelTest.kt` / `CountUpStoreTest.kt`):**
   - Verify `CycleThemeMode` transitions: `SYSTEM` $\rightarrow$ `LIGHT` $\rightarrow$ `DARK` $\rightarrow$ `SYSTEM`.
   - Verify theme persistence across store re-instantiations.
   - Verify `searchQuery` live filtering behaves identically with new sub-header controls.
2. **Lint & Static Analysis:**
   - Run `./gradlew lintDebug` to verify 0 errors, no missing string resources, and valid accessibility attributes.
3. **Automated JVM Test Suite:**
   - Run `./gradlew test` (verifying all 336 unit tests pass).
4. **Interactive Emulator Verification:**
   - Launch `./scripts/launch_emulator.ps1 -Deploy`.
   - Test Sort/Search dropdown: Type text, select sort mode, clear search.
   - Test 1-Tap Theme Toggle: Tap icon 3 times; verify smooth transition through Auto, Light, and Dark modes.
   - Test Settings Dialog: Tap `⚙` icon, verify Export Backup and Restore Backup trigger system pickers.
   - Test Background Cycle: Tap Enso logo; verify wallpaper cycles properly without regression.

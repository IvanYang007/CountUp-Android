# Feature Specification: Modern MVI Architecture, Compose UI Elevation & Testing Suite

## Problem Statement

As CountUp has grown from a single-item haircut counter into a rich multi-item habit tracker featuring Chinese ink wash backgrounds, search, and dynamic sorting, the codebase and UI architecture face three key scalability and polish challenges:

1. **Monolithic UI and State Coupling:** The primary activity currently manages all state variables (items list, search filter, sort order, background themes, dialog targets, deletion/reset confirmations, and widget IPC triggers) directly inside the activity lifecycle. This tight coupling impedes automated UI testing, prevents headless component previews in Compose tooling, and increases the risk of state race conditions during rapid user interactions.
2. **Static Counter and Layout Clippings:** When days increment, reset to zero, or rearrange upon sorting, day count numbers update abruptly without tactile feedback. Furthermore, the procedural Chinese ink wash artwork is constrained within inner scaffold padding rather than bleeding edge-to-edge behind the system status and navigation bars.
3. **Absence of Flow & UI Regression Automation:** State transitions, search/sort filtering logic, and dialog interactions lack fast, hermetic coroutine flow tests (Turbine) and headless Compose UI tests, making future UI enhancements susceptible to silent regressions.

---

## Solution

Modernize the CountUp application using 2026 Jetpack Compose engineering standards, Clean Architecture / MVI (Model-View-Intent) unidirectional data flow, and modern testing practices:

1. **Unidirectional MVI Architecture & Stateless UI:** Introduce an immutable `CountUpUiState`, a `CountUpUiEvent` dispatch contract, a one-shot `CountUpUiEffect` channel, and an isolated `CountUpViewModel` with atomic `_state.update` mutations. Decouple screen rendering into a stateful container and a pure, stateless `CountUpContent` composable.
2. **Tactile Zen Motion & Odometer Digit Roll:** Implement smooth mechanical/odometer digit roll animations using `AnimatedContent` with directional vertical sliding and spring physics for day count transitions, tactile spring-damped press feedback, and breathing Enso motions.
3. **Full-Bleed Edge-to-Edge Ink Canvas:** Render the 5 procedural Chinese ink wash themes across the entire viewport behind transparent system bars while protecting interactive elements with `WindowInsets.safeDrawing`.
4. **Structured Zen Design Token System:** Formalize the Mid-Century Modern Zen Paper palette into a `CompositionLocal`-driven token system with support for both warm paper and OLED obsidian ink themes.
5. **Comprehensive Testing Suite:** Introduce Turbine for millisecond ViewModel state pipeline validation, a hermetic in-memory repository fake, and headless Compose UI tests verifying visual semantics and empty states.

---

## User Stories

### A. Zen Visual Experience & Motion
1. As a mindful user, I want the Chinese ink wash background to extend completely edge-to-edge behind the system status bar and gesture navigation bar, so that the screen feels like an authentic, uninterrupted paper scroll.
2. As a user viewing my habits, I want the day count numbers to roll smoothly up or down when values change or when items are reset, so that the interface feels tactile and alive.
3. As a user tapping on cards, buttons, and the Enso theme switcher, I want tactile spring-damped press reactions, so that physical feedback feels organic and responsive.
4. As a user cycling through background themes, I want the Enso icon to smoothly acknowledge the change with a gentle brush-stroke rotation, so that theme transitions feel intentional.
5. As a user with system "Reduce Motion" enabled, I want animations to automatically collapse to instantaneous transitions, so that accessibility preferences are fully respected without visual stutter.
6. As a user in low-light environments, I want the Zen design token system to support a serene dark obsidian paper mode, so that I can track habits without harsh glare while retaining the ink wash aesthetic.

### B. Habit Management & Interaction Flow
7. As a user searching for an item, I want the search input in the Sort & Search popover to filter the visible list instantaneously on the same frame, so that I experience zero input lag.
8. As a user switching sort modes, I want the active sort order to persist reliably across app restarts, so that my preferred list hierarchy is always preserved.
9. As a user resetting an item to today, I want a clear confirmation dialog followed by an instant reset of the count to zero with immediate widget sync, so that my home screen is always up to date.
10. As a user deleting an item, I want the item card to animate out smoothly while remaining items glide into their new positions, so that the layout change is effortless to follow.
11. As a user toggling home-screen widget visibility on an item, I want to see a clear feedback snackbar and immediate widget update, so that I know exactly which items are displayed on my launcher.
12. As a TalkBack user, I want each item card to announce its name, days elapsed, and anchor date as a single merged semantic element, so that navigation is efficient and non-repetitive.
13. As a touch device user, I want all interactive controls (Enso button, Add button, Search clear icon, sort options) to have a minimum 48dp × 48dp touch target, so that controls are effortless to tap accurately.

### C. Developer Ergonomics & Reliability
14. As an engineer maintaining CountUp, I want all business logic, search filtering, and state transitions to live in a dedicated ViewModel, so that the UI layer remains purely declarative and free of state-mutation race conditions.
15. As an engineer writing UI tests, I want `CountUpContent` to be a pure stateless composable, so that I can run headless Compose UI tests without mocking Android activity lifecycles or IPC dependencies.
16. As an engineer validating state flows, I want Turbine unit tests for `CountUpViewModel`, so that I can verify complex multi-event pipelines (search + sort + CRUD) deterministically in under 50 milliseconds.
17. As an engineer modifying data classes, I want `@Immutable` stability guarantees on list items and UI states, so that the Compose compiler eliminates all redundant recompositions in `LazyColumn`.

---

## Implementation Decisions

### 1. Architectural Seams & Unidirectional MVI Contract
* **State Container & ViewModel:**
  - Create a single immutable UI state object encapsulating the entire screen state:
    ```kotlin
    @Immutable
    data class CountUpUiState(
        val items: List<CountUpItem> = emptyList(),
        val searchQuery: String = "",
        val sortOrder: SortOrder = SortOrder.DAYS_DESC,
        val backgroundTheme: BackgroundTheme = BackgroundTheme.AUTO_DAILY,
        val editorTarget: CountUpItem? = null,
        val isEditorOpen: Boolean = false,
        val isSearchSortMenuOpen: Boolean = false,
        val pendingDelete: CountUpItem? = null,
        val pendingReset: CountUpItem? = null,
    )
    ```
  - Dispatched actions flow strictly through a `sealed interface CountUpUiEvent` (e.g. `SearchQueryChanged`, `SortOrderSelected`, `CycleBackground`, `SaveItem`, `ConfirmDelete`, `ConfirmReset`, `ToggleWidgetVisibility`).
  - One-shot UI events (Snackbars, Widget Refresh IPC) flow through a `Channel<CountUpUiEffect>(Channel.BUFFERED)`.
  - State mutations inside `CountUpViewModel` are strictly atomic using `_state.update { it.copy(...) }`.

* **Container vs. Content Composable Structure:**
  - `MainActivity` / `CountUpScreen`: Stateful container that connects to `CountUpViewModel`, collects state via `collectAsStateWithLifecycle()`, and binds effects to `SnackbarHostState` and widget IPC.
  - `CountUpContent`: Pure, stateless composable taking `(state: CountUpUiState, onEvent: (CountUpUiEvent) -> Unit)`. Enables interactive Android Studio Previews and fast UI testing.

* **Repository Abstraction:**
  - Define a lightweight `CountUpRepository` interface exposing `Flow<List<CountUpItem>>` and suspending CRUD methods.
  - Back the repository with `CountUpStore` in production (maintaining the existing 5-tier zero-data-loss hierarchy and atomic disk backups) while providing an in-memory fake for hermetic testing.

### 2. Compose UI Elevation & Motion Design
* **Zen Odometer Digit Roll:**
  - Render day count numbers inside `AnimatedContent` with a vertical slide transition specification (`slideInVertically` + `slideOutVertically`) combined with fade transitions.
  - Directional awareness: when a count increments or resets, the animation direction reflects the numeric change.
* **Full-Bleed Edge-to-Edge Canvas:**
  - Place `Modifier.drawAbstractBackground(backgroundTheme)` on the root fullscreen container before insets are applied.
  - Apply `WindowInsets.safeDrawing` to interactive content areas (app header, sub-header, list viewport, and dialogs).
* **Compose Stability & LazyColumn Optimization:**
  - Annotate `CountUpItem` with `@Immutable`.
  - Specify `contentType = { "item_card" }` inside `items()` within `LazyColumn` for layout reuse.
* **Zen Design Token System:**
  - Define `ZenColorScheme` and provide it via `CompositionLocalProvider(LocalZenColors provides zenColors)`.
  - Maintain color roles for Chinese ink wash pigments: Ink Black (焦墨), Ink Muted (淡墨), Vermilion (朱砂), Ochre (赭石), Sage (柳绿), Indigo (花青), and Paper Surface (宣纸).

---

## Testing Decisions

### 1. ViewModel StateFlow Testing with Turbine
* **Scope:** Test state transitions, search substring filtering, sort order modifications, theme cycling, dialog visibility toggling, and error paths.
* **Test Tooling:** `app.cash.turbine:turbine` + `kotlinx-coroutines-test` (`runTest` and `MainDispatcherRule`).
* **Key Assertions:**
  - Emits initial state with persisted items.
  - Search query updates filter `displayItems` in real-time without modifying underlying `items`.
  - Sort order changes immediately reorder `displayItems`.
  - Deletion, reset, and widget toggles trigger corresponding `CountUpUiEffect` emissions.

### 2. Headless Compose UI Semantics Testing
* **Scope:** Test stateless `CountUpContent`, `ItemCard`, `ItemEditorDialog`, and `SortSearchPopover` using `createComposeRule()`.
* **Key Assertions:**
  - Verifies empty list state ("No items yet") and empty search state ("No matching items found").
  - Verifies dynamic "SINCE <date>" vs. "UNTIL <date>" labeling on item cards.
  - Verifies accessibility content descriptions and merged semantics.

### 3. Hermetic Fakes
* Implement `FakeCountUpRepository` implementing `CountUpRepository` for zero-IO, instant unit test execution.

---

## Out of Scope

1. **Network Sync & Cloud Accounts:** CountUp remains 100% local, private, and offline with zero Android permissions.
2. **Third-Party Database (Room/SQL):** Retain the lightweight, battle-tested zero-data-loss `CountUpStore` persistence hierarchy.
3. **Complex Navigation Graph Framework:** CountUp remains a single-screen focused habit tracker; dialogs and popovers remain overlay-driven.
4. **Custom AGSL Runtime Shaders on Legacy API:** Maintain procedural Canvas/Path rendering compatible across all supported Android versions (API 26+).

---

## Further Notes

* **Backward Compatibility:** All existing data stored in `countup_prefs` and `countup_backup.json` remains 100% compatible with no migration overhead.
* **Dependency Updates:** Add `turbine` and `kotlinx-coroutines-test` to `libs.versions.toml` under test dependencies.

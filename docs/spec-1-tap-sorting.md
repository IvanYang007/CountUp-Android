# Feature Spec: Minimalist 1-Tap List Sorting for CountUp

**Status:** Ready for Implementation  
**Target:** CountUp (Android / Jetpack Compose)  
**Scope:** Minimalist 1-Tap In-Place Sorting Control  
**Security & Permissions:** 0 Permissions, 100% Offline, Zero-Bloat  

---

## Problem Statement

As users add more count-up items (e.g., haircuts, plant watering, air filter changes, sobriety streaks, anniversaries), items currently appear in arbitrary insertion order. When a user has 4 to 12 items, finding a specific counter or identifying which maintenance item has accumulated the most days requires unnecessary cognitive scanning.

However, traditional sorting implementations in task and tracking apps introduce nested settings pages, popup action sheets, or multi-step dropdown menus. These patterns create visual clutter, increase interaction friction, and destroy the calm, offline "Zen-paper" aesthetic that defines CountUp.

---

## Solution

Introduce an ultra-minimalist, 1-tap cyclic sorting control that lives directly within the list surface. 

Tapping the subtle sorting indicator immediately cycles through three clear, useful orderings with tactile scale feedback and smooth, natural Jetpack Compose item animations (`Modifier.animateItem()`). The chosen sort preference is persisted locally across app restarts with zero background overhead, zero network requirements, and zero permission prompts.

---

## User Stories

1. As a user with multiple tracking items, I want to sort my count-ups by elapsed days in descending order, so that items with the highest day counts (the ones most in need of attention or representing my longest streaks) appear at the top.
2. As a user tracking frequent maintenance events, I want to sort my count-ups by anchor date (newest first), so that my most recently reset or updated items appear first.
3. As a user with a growing collection of count-ups, I want to sort my items alphabetically by name (A–Z), so that I can immediately locate a specific item by name without scanning the entire list.
4. As a minimalist user, I want the sorting mechanism to require only a single tap to cycle modes, so that I never have to navigate away from the main screen or open a settings modal.
5. As a user appreciating calm aesthetics, I want the sorting control to blend seamlessly into the warm Zen-paper typographic palette without competing visually with item cards or the Enso theme button.
6. As a user who values smooth motion, I want the list cards to glide gently into their new sorted positions using calm animations, so that the reordering feels physical, grounded, and non-jarring.
7. As an accessibility-conscious user with system "Reduce Motion" enabled, I want sorting reorders to apply instantly without animation, so that the interface respects my device accessibility preferences.
8. As a returning user, I want my selected sorting mode to persist across app relaunches and device reboots, so that my list stays organized exactly how I left it.
9. As a user with only 0 or 1 item, I want the sorting control to remain unobtrusive and graceful, so that the interface never feels empty or broken.
10. As a privacy-focused user, I want my sorting preferences to remain 100% offline and stored in private local preferences, so that zero tracking or telemetry is generated.
11. As a TalkBack / screen reader user, I want clear semantic content descriptions on the sort control (e.g., "Sorted by days elapsed, tap to sort by newest date"), so that I know the current state and what will happen when activated.
12. As a tactile user, I want the sort pill to scale down slightly (`0.96` scale) when pressed, matching the tactile press feedback of all other interactive elements in CountUp.

---

## Implementation Decisions

### 1. The Sort Modes & State Machine
The sort state cycles deterministically in a 3-step loop:

```
[ DAYS_DESC (Default: Most Days ↓) ] ──► [ DATE_DESC (Newest Anchor ↓) ] ──► [ NAME_ASC (Alphabetical A-Z) ] ──► [ DAYS_DESC ]
```

* **`DAYS_DESC` (Default):** Items with the largest number of elapsed days appear first. For future countdown items (negative days), positive count-ups precede upcoming events.
* **`DATE_DESC`:** Items anchored to the most recent calendar date (highest `epochDay`) appear first.
* **`NAME_ASC`:** Items are sorted alphabetically, case-insensitively (`localeCompare` / `String.compareTo(..., ignoreCase = true)`).

### 2. UI Placement & Zen-Paper Visual Harmony
* **Location:** Placed in the secondary sub-header region right above the `LazyColumn` and below the top title row, aligned horizontally to the right of the item count indicator (e.g., `4 items`).
* **Visual Style:** A compact, rounded pill styled with `McmPaperSurface` background (`#EBDCC3`), 1px `McmRule` border (`#E3D3B8`), and muted text (`#6B5D4F`) with an accent label in `McmOrange` (`#D97642`).
* **Micro-copy format:** `Sort: Days ↓`, `Sort: Date ↓`, `Sort: Name A–Z`.
* **Motion & Touch:** Bound to `rememberPressSource()` with `Modifier.pressScale(0.96f)` and an accessible touch target area ($\ge 48 \text{ dp}$).

### 3. Separation of Concerns & Modules
* **Domain & Ordering Logic:** A pure sorting comparator / function residing alongside `DaysSince.kt` / `CountUpItem.kt` that accepts a `List<CountUpItem>`, the target `SortOrder`, and the reference `today: LocalDate`, returning a newly ordered list.
* **Store Persistence:** Extended `CountUpStore` with a lightweight, synchronous `getSortOrder()` and `setSortOrder(order: SortOrder)` stored under key `sort_order_v1` in `countup_prefs`.
* **Main UI Integration:** `MainActivity` holds `sortOrder` in Compose state (`mutableStateOf`), passing sorted items to `CountUpList` and animating list item changes via `Modifier.animateItem()`.

### 4. Widget Independence
* The home-screen widget's 3-column / 2-column grid will continue to reflect items according to their visibility and order without being disrupted by UI sort cycling, or optionally mirror the stored sort order during `pushWidgetUpdate`.

---

## Testing Decisions

### What Makes a Good Test
Tests should verify deterministic external behavior, state persistence, edge case handling (future dates, tie-breakers, empty collections, case differences), and UI contract preservation—without coupling to internal layout composable trees.

### Test Coverage Plan
1. **Pure JVM Unit Tests (`app/src/test/...`):**
   * **Sort by Days (`DAYS_DESC`):** Verifies that an item with 100 days comes before an item with 5 days, and positive days precede future countdowns (e.g., $-5$ days).
   * **Sort by Anchor Date (`DATE_DESC`):** Verifies that an item anchored to `2026-02-28` comes before `2026-01-01`, regardless of name.
   * **Sort by Name (`NAME_ASC`):** Verifies case-insensitive ordering (`"air filter"`, `"Anniversary"`, `"Barber"`, `"car oil"`).
   * **Tie-Breaking Stability:** Verifies that items with identical sort values preserve stable relative ordering without flickering.
   * **State Machine Cycling:** Verifies that `SortOrder.DAYS_DESC.next()` produces `DATE_DESC`, which cycles to `NAME_ASC`, which cycles back to `DAYS_DESC`.
2. **Store Persistence Tests (`CountUpStoreTest`):**
   * Verifies saving and reading all `SortOrder` enum values.
   * Verifies fallback to `SortOrder.DAYS_DESC` when the preference key is missing or corrupted.
3. **Prior Art in Codebase:**
   * Aligns with the existing 72 JVM unit tests in `CountUpStoreTest.kt`, `DaysSinceTest.kt`, and `DateConversionTest.kt`.

---

## Out of Scope

* **Drag-and-Drop Manual Reordering:** Manual custom drag-sorting adds gesture conflicts, reorder persistence complexity, and extra libraries; 1-tap algorithmic sorting fulfills 100% of organization needs with zero friction.
* **Settings Page / Popup Menus:** No dedicated settings screen, overflow dots menu, or modal sheets will be added.
* **Multi-Tier Compound Filters:** No tagging, category filters, or search bars (unnecessary for a focused day counter).
* **Push Notifications / Alarms:** Explicitly excluded to protect the zero-permission and calm offline promise.

---

## Further Notes

* **Zero Permissions Guarantee:** This feature requires **0 Android permissions**, **0 new dependencies**, and **0 database changes**.
* **Prototype Reference:** The interactive prototype demonstrating this exact 1-tap cyclic pill interaction and animation feel is available in `artifacts/prototype_countup_design.html`.

# Feature Specification: Sort & Search Popover Menu with Magnifying Glass

## Problem Statement

When a user's collection of tracked habits and count-up items grows, they need an effortless way to both sort their list and quickly filter items by keyword. While a cyclic 1-tap sort button allows cycling through pre-set ordering rules, it does not allow a user to instantly jump to a specific sort mode or filter out items when they only want to see a specific habit (e.g., typing "bo" to instantly see only "Water Bonsai" and "Reading Book"). At the same time, adding a separate full-width search bar permanently onto the screen takes up valuable vertical real estate and disrupts the serene Mid-Century Modern Zen aesthetic. Users need an integrated, compact control that combines instant substring search with direct sort selection without adding permanent visual clutter.

## Solution

Transform the sub-header sort pill into an integrated **Sort & Search control** featuring a subtle magnifying glass icon and the active sort mode label (e.g., `🔍 Sort: Days ↓`). Tapping the pill opens a calm, lightweight popover directly anchored to the control.

The popover contains:
1. **An Instant Substring Search Field:** A focused, clean text input that filters the list in real-time ($<1\text{ms}$ in-memory matching) on every keystroke as the user types, matching against item names (case-insensitively).
2. **Direct Sort Mode Selection:** Clear selectable rows for each sort mode (*Days Elapsed $\downarrow$*, *Newest Date $\downarrow$*, *Alphabetical A–Z*) with active checkmark indicators. Selecting a sort mode updates the list order immediately, updates the pill label, and closes the popover.
3. **Instant Clear & Empty State:** An inline clear button (`✕`) when text is entered, and an organic Zen empty state ("No matching items found") when no habits match the query.
4. **Seamless Dismissal:** Tapping outside the popover, pressing the system back button, or selecting a sort option dismisses the popover smoothly.

---

## User Stories

1. As a habit tracker user, I want to see a magnifying glass icon next to the sort label in the sub-header, so that I immediately understand I can both search and sort from this control.
2. As a user with multiple habits, I want to tap the Sort & Search pill to reveal a compact popover, so that I can access filtering and sorting tools without leaving the main screen.
3. As a user searching for a habit, I want the search input in the popover to filter the visible list in real time with zero noticeable lag, so that I can find my habit within a couple of keystrokes.
4. As a user filtering items, I want substring matching to be case-insensitive, so that searching "bonsai", "Bonsai", or "BONSAI" matches "Water Bonsai" reliably.
5. As a user filtering items, I want the item count in the sub-header to update dynamically (e.g., "2 items" matching), so that I know exactly how many items matched my query.
6. As a user viewing search results, I want the matching text within the item card title to be visually highlighted with a subtle warm accent, so that I can see why an item matched.
7. As a user entering a query with no matches, I want to see an elegant, calm empty state with an icon and clear message, so that I know no items match without feeling like the app crashed.
8. As a user searching for items, I want an inline clear button (`✕`) inside the search input, so that I can reset my search string with a single tap.
9. As a user who wants to change sort order, I want to see all available sort modes listed clearly in the popover with their descriptions, so that I can pick my desired order in one tap rather than cycling through unwanted modes.
10. As a user selecting a new sort mode, I want the active mode to be clearly highlighted with a checkmark and background tint, so that I can visually verify which sort rule is currently active.
11. As a user who changed the sort order, I want the choice to be saved to local storage immediately, so that my preference persists across app restarts.
12. As a user with an active search filter, I want the sorted order to apply directly to the filtered results, so that matching items appear in the order I expect (e.g., matching habits ordered by most days elapsed).
13. As a user who is done searching or sorting, I want to tap anywhere outside the popover or press the Android back button, so that the popover closes without altering my list state.
14. As a user using TalkBack accessibility, I want the Sort & Search button to announce its current state and explain that tapping opens search and sort options, so that the control is accessible to screen readers.
15. As a mindful user, I want the popover to adhere to the warm Zen-paper palette and typography, so that the interface feels cohesive, soothing, and high quality.

---

## Implementation Decisions

### 1. State Model & Interaction Architecture
* **Single Source of Truth in Main UI:**
  - Maintain `searchQuery: String` (default `""`) and `sortOrder: SortOrder` (persisted in local preferences).
  - Maintain `isSearchSortMenuOpen: Boolean` (default `false`) controlling popover visibility.
* **Pure Transformation Pipeline:**
  - The displayed list is derived via pure in-memory calculation:
    $$\text{DisplayItems} = \text{sortItems}\Big(\text{filterItems}(\text{AllItems}, \text{searchQuery}), \text{sortOrder}\Big)$$
  - Filtering checks `name.contains(query, ignoreCase = true)` and optional `comment.contains(query, ignoreCase = true)`.
  - Sorting reuses the pure deterministic comparator with stable tie-breaking.

### 2. UI Component & Popover Design
* **Trigger Button (Sub-header):**
  - Styled with Mid-Century Modern tokens: `surfaceVariant` background, 1px `outline` border, 8dp rounded corners, and tactile press scale (`Modifier.pressScale(0.96f)`).
  - Layout: `[ "Sort: " ] [ Active Sort Mode Label ] [ ▾ Caret ] [ 🔍 Magnifying Glass Icon ]`.
* **Popover Overlay (`DropdownMenu` / Compact Zen Sheet):**
  - Rendered with `surfaceVariant` / paper-card background, rounded corners (12dp), and subtle elevation shadow.
  - Top section: Search text field with leading search icon, trailing clear icon (`✕` visible when `searchQuery.isNotEmpty()`), and soft placeholder ("Search habits...").
  - Divider / Section header: Subtle uppercase label "SORT ORDER" in muted ink.
  - Selection rows: Clickable items for `DAYS_DESC`, `DATE_DESC`, `NAME_ASC` displaying human-readable labels and short descriptions, with the selected item highlighted in olive tint.
* **Item Card Motion:**
  - Items enter and exit using `Modifier.animateItem()` so filtered items smoothly collapse and expand without jarring visual cuts.

### 3. Performance & Resource Constraints
* **Instant Execution:** Filtering an in-memory list of 5–100 items executes in $<0.5\text{ms}$, requiring no coroutine dispatchers or asynchronous debouncing. Keystrokes reflect on the screen on the exact same frame.
* **Zero Permissions & Offline:** Operates 100% locally with zero external network requests, zero database overhead, and 0 Android `<uses-permission>` tags.

---

## Testing Decisions

### 1. Pure Unit Testing (`Filter & Sort Logic`)
* **Test Module:** `app/src/test/java/com/countup/app/FilterAndSortTest.kt` (or extending `SortOrderTest.kt`).
* **Test Cases:**
  - Empty query returns all items in requested sort order.
  - Substring match filters correctly (e.g. "med" matches "Meditation").
  - Case-insensitivity verification ("AIR", "air", "Air").
  - Partial matches on comments/notes if applicable.
  - Multi-match filtering combined with different sort orders (`DAYS_DESC`, `DATE_DESC`, `NAME_ASC`).
  - Zero-match query returns empty list.
  - Query with leading/trailing whitespace is trimmed gracefully.

### 2. Store & State Persistence Testing
* **Test Module:** `CountUpStoreTest.kt`.
* **Test Cases:**
  - `getSortOrder()` and `setSortOrder()` persist reliably across instances.
  - Search query is deliberately transient (in-memory only, resets to empty on fresh launch).

### 3. Android Lint & Compose Verification
* Automated lint check via `./gradlew lintDebug` ensuring 0 accessibility, typography, or resource errors.

---

## Out of Scope

1. **Complex Regex or Fuzzy Matching Algorithms:** Only clean, predictable substring containment is needed; advanced Levenshtein distance or regex syntax is out of scope.
2. **Persistent Search History:** Past search strings will not be saved to disk or shown in a recent search history dropdown.
3. **Multi-tag Filtering / Category Chips:** Filtering is strictly keyword-based; no complex multi-facet query builder or tag selection chips.
4. **Cloud / Remote Search:** 100% offline, private, and local to device.

---

## Further Notes

* **Tactile Aesthetics:** The magnifying glass icon should use a clean vector outline matching the Zen icon stroke weight (1.5dp–2dp), tinted in `onSurfaceVariant` ink.
* **Keyboard Management:** When the popover opens, focus can cleanly land on the search input, and dismissing the popover hides the software keyboard cleanly.

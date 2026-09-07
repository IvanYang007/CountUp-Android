# Spec: Grounded Zen & Efficient Widget Suite (3 Form Factors)

## Problem Statement

Users of CountUp value intentionality, calm aesthetics, and digital well-being. However, the current widget offerings (the 2x1 Hero Milestone Widget and the 3-column Overview Grid Widget) leave significant ergonomic and sensory gaps on the Android home screen:

1. **Static Data Staleness**: Existing widgets only increment at midnight. Between 00:01 AM and 11:59 PM, they remain static picture frames that offer no interactivity or fresh temporal perspective.
2. **All-or-Nothing App Launches**: If a user wants to view their time in different units (months, weeks, or milestone percentages), they are forced to tap the widget and enter the main application, exposing them to notification distractions and phone rabbit holes.
3. **Disconnection from Natural Rhythms**: While CountUp features poetic themes and 24 Solar Terms (*节气*) within its design system, the widgets isolate personal time from natural time, missing an opportunity to ground personal progress within the turning of the seasons.
4. **Launcher Footprint Inflexibility**: Users of minimalist vertical launchers (such as Niagara Launcher or Olauncher) or dense desktop grids lack ultra-compact single-cell (`1x1`) and slim single-row (`2x1`/`4x1`) options that integrate cleanly into tight icon arrangements without dominating screen real estate.

## Solution

Introduce a cohesive **Zen & Efficient Widget Suite** composed of three distinct, grounded form factors designed with Jetpack Glance and Android RemoteViews architecture:

1. **🌊 The "Zen Horizon" (4x1 & 2x1 Ribbon)**: An ultra-slim horizon line showing the active counter with a 1dp hairline milestone track and in-place unit toggling (*Days ⇄ Months ⇄ Weeks ⇄ Hours*) directly on the home screen via Glance action callbacks without launching the app.
2. **🎋 The "Solar Rhythm" (4x2 & 2x2 Canvas)**: A rich, poetic canvas harmonizing personal count-ups with the 24 Solar Terms (*二十四节气*). Computed deterministically on-device using astronomical longitude math (zero network calls), the widget dynamically shifts its background palette and seasonal micro-poetry across Spring Jade, Summer Bamboo, Autumn Tea, and Winter Slate.
3. **🪨 The "Zen Pebble" (1x1 Minimalist Token)**: An ultra-compact single-icon grid widget displaying a bold serene numeral, micro-label, hairline ink dash, and a 1-word tag (*clean, zen, love, focus*). Designed for minimalist single-screen launchers and crowded home screen docks.

---

## User Stories

### General & Suite Ergonomics
1. As a minimalist Android user, I want widgets that consume zero background battery and require zero network permissions, so that my device battery health and privacy remain pristine.
2. As a phone user checking my home screen dozens of times a day, I want widgets rendered in high-contrast matte ink tones and breathable whitespace (*Ma*), so that my cognitive stress is reduced rather than elevated.
3. As an Android 12+ user, I want all widgets to respect system-rounded corners and dynamic theming, so that they look native across Pixel, OneUI, and third-party launchers.
4. As a dark mode user, I want widget backgrounds to adopt deep slate ink tones (`#1B1E22`) instead of pure OLED black, so that contrast is legible without causing eye strain at night.

### 🌊 The "Zen Horizon" (4x1 & 2x1 Ribbon)
5. As an efficiency-focused user, I want to tap the day count on the Zen Horizon widget to cycle units in-place (Days ➔ Months ➔ Weeks ➔ Hours ➔ Years), so that I can re-frame my progress without opening the full application.
6. As a commuter glancing at my phone, I want the Zen Horizon widget to fit into a single horizontal launcher row (`4x1` or `2x1`), so that it does not crowd out my calendar or clock.
7. As a goal-oriented user, I want the Zen Horizon widget to display a subtle hairline milestone progress bar with a calm pebble marker, so that I can intuitively see how close I am to my next milestone (e.g. 500 days, 1 year).
8. As a user with multiple tracked events, I want to bind specific Zen Horizon widgets to distinct tracked items via widget configuration, so that I can feature different events on different launcher pages.

#### 🎋 The "Solar Rhythm" (4x2 & 2x2 Canvas)
9. As an admirer of Eastern aesthetics, I want the widget to display the current traditional Solar Term (e.g., *White Dew, Major Snow, Grain Rain*), so that my personal journey feels connected to the macro-rhythm of nature.
10. As a seasonal observer, I want the widget background and typography tints to subtly adapt to the seasonal temperature (warm tea in Autumn, bamboo jade in Spring, crisp slate in Winter), so that my home screen breathes with the weather.
11. As an offline user, I want solar term calculations to occur completely on-device without internet access, so that the widget functions flawlessly in airplane mode and off-grid.
12. As a reader of poetry, I want the Solar Rhythm widget to present an understated, one-line poetic phrase reflecting the seasonal transition, so that unlocking my phone offers a brief pause of literary beauty.
13. As a planner, I want the Solar Rhythm widget to show a seasonal progress track (from the beginning of the solar term to the next transition), so that I understand where today sits within the seasonal quarter.

### 🪨 The "Zen Pebble" (1x1 Minimalist Token)
14. As a Niagara Launcher or minimalist launcher user, I want a 1x1 pebble widget that fits precisely inside a standard app icon cell, so that my single-column home screen remains uncluttered.
15. As a glance-oriented user, I want the Zen Pebble to show only the bold number, a micro-unit, and a one-word label (*clean, study, love*), so that I can absorb the state of my milestone in under 200 milliseconds.
16. As a user tracking multiple habits, I want to align three or four 1x1 pebbles side-by-side in a single launcher row, so that I have a tranquil dashboard taking minimal vertical space.
17. As an active user, I want tapping the 1x1 pebble to open directly to the specific event detail screen in CountUp, so that I have instant access when deeper journaling is desired.

---

## Implementation Decisions

### 1. Architecture & Seams
- **Single State Transformation Seam**: All 3 widgets will consume a shared, pure Kotlin reducer:
  ```kotlin
  fun resolveZenWidgetState(
      item: CountUpItem,
      today: LocalDate,
      displayMode: TimeDisplayMode,
      solarTerm: SolarTermInfo
  ): ZenWidgetViewState
  ```
  This guarantees that 100% of formatting, unit cycling, milestone math, and color token resolution is decoupled from Android `RemoteViews` / `Glance` and can be tested on the JVM.
- **Glance 1.1+ and RemoteViews Synergy**:
  - `ZenHorizonWidget` and `ZenPebbleWidget` will leverage lightweight RemoteViews providers to maximize launch speed and minimize IPC transaction payloads (< 40 KB).
  - `SolarRhythmWidget` will utilize Jetpack Glance with `actionRunCallback` for zero-activity background interactivity.

### 2. In-Place Unit Cycling
- `ZenHorizonWidget` cycles through `TimeDisplayMode`:
  `DAYS` ➔ `MONTHS` ➔ `TOTAL_WEEKS` ➔ `HOURS` ➔ `ELAPSED_BREAKDOWN` ➔ `DAYS`.
- Mode persistence is stored per widget instance ID in `CountUpStore` (`KEY_HERO_WIDGET_DISPLAY_MODE_<id>`).
- Tapping the numeric container fires an explicit broadcast with `EXTRA_APP_WIDGET_ID`, cycling the stored preference and triggering an immediate `pushWidgetUpdate` without opening any activity.

### 4. Deterministic On-Device Solar Term Engine
- Port the solar term algorithm from `prototype_solar_terms.html` into a pure Kotlin domain utility: `SolarTermCalculator`.
- Calculates the sun's apparent ecliptic longitude using standard astronomical epoch formulas (VSOP87-simplified approximation) with an error margin $< 1$ minute across years 1970–2100.
- Completely deterministic; zero network requests; computed only upon store updates and daily rollover alarms.

### 5. Midnight Rollover & Battery Preservation
- No background workers (`WorkManager`) or persistent services are permitted to run in the background.
- Rollovers are triggered once per 24 hours via `AlarmManager.setExactAndAllowWhileIdle` targeted at `00:00:01` local time.
- The `MidnightAlarmReceiver` wakes up, re-queries active widget IDs, updates the widget view hierarchy with the new epoch day, and schedules the next midnight alarm before immediately releasing the wake lock.

---

## Testing Decisions

### 1. What Makes a Good Test
- Tests must strictly verify **external behavior, mathematical correctness, and formatting output** rather than internal framework wiring.
- Tests must run on the local JVM without requiring an Android emulator or device runtime.
- RemoteViews bitmap allocations and Binder IPC payloads must be verified against size limits to prevent `TransactionTooLargeException`.

### 2. Modules to Test
- `ZenWidgetReducerTest`:
  - Unit cycling logic across all time intervals (verifying human-readable strings for 0 days, 1 day, 30 days, 365 days, 10,000 days).
  - Milestone progress calculations (asserting correct percentage fill and pebble position on the horizon track).
- `SolarTermCalculatorTest`:
  - Assert exact date boundaries for major solar terms (*Spring Equinox, Summer Solstice, Autumn Equinox, Winter Solstice, White Dew*) across past and future benchmark years.
  - Verify seasonal palette token mapping for every term.
- `ZenPebbleFormattingTest`:
  - Assert that large numbers (e.g. 1,200 days) format cleanly into compact glyphs (e.g. `1.2k`) to prevent text overflow in 1x1 cells.

### 3. Prior Art
- Builds directly upon `app/src/test/java/com/countup/app/HeroWidgetTest.kt` (which already tests `TimeDisplayMode`, milestone identify, and patina color resolving) and `app/src/test/java/com/countup/app/WidgetRowTest.kt`.

---

## Out of Scope

1. **Audio / Confetti Celebrations**: No sound effects, fireworks, vibrations beyond subtle haptic ticks, or gamification animations.
2. **Third-party Habit Sync**: No synchronization with Google Fit, Apple Health, or Todoist.
3. **Continuous Sub-Hour Timers**: No live ticking seconds or millisecond stopwatch displays, as this severely compromises Android battery life.
4. **Cloud / Webhook Push**: All widget configurations, day marks, and event counts remain strictly on the local device.

---

## Further Notes

- Interactive behaviors and visual styling have been validated in the local standalone artifact `prototype_zen_widgets.html`.
- Android 12+ widgets automatically receive system corner radii (`@android:dimen/system_app_widget_background_radius`), so inner paddings must account for 16dp to 28dp curves to avoid content clipping.


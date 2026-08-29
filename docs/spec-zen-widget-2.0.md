# Spec: Ultra-Minimalist Zen Widget 2.0 (Connected Ink Background & Streamlined Architecture)

## Problem Statement

The current CountUp home-screen widget suffers from visual and structural clutter that conflicts with the app's core Zen aesthetic and quick-glance goal:
1. A manual floating "refresh" button occupies over 10% of the widget's horizontal width, compressing habit names and making the card feel like a utility dashboard rather than a peaceful milestone plaque.
2. The widget background is a plain flat paper tone that lacks the tranquil Chinese ink wash landscape art (*远山、平沙、瀚海、孤石、杨柳*) present in the main app.
3. Users already have redundant date information displayed across their lock screens, status bars, and system clock widgets; having dates or excessive metadata on the CountUp widget creates cognitive noise and reduces glanceable reading speed.

## Solution

Transform the widget into an ultra-minimalist, tranquil **Zen Hanging Plaque (茶席雅牌)**:
1. **Eliminate All Refresh Clutter**: Remove the manual refresh button, its reserved margin, and all redundant refresh broadcast/gating code. The existing in-place double-tap reset and synchronous store updates provide instant, reliable synchronization without manual intervention.
2. **Unified Dynamic Ink Background**: Render the app's active Chinese ink landscape theme directly into the widget background as a gentle, low-opacity (15%) mineral wash anchored to the bottom-right corner using lightweight native Android Canvas drawing.
3. **Maximized Full-Width 3-Column Grid**: Expand the habit grid across the full width of the card, giving item titles more breathing room and making bold circular count plates instantly identifiable at a glance.
4. **Quiet Minimalist Branding**: Feature an understated, whisper-soft `CountUp` title on the top-left, omitting redundant current dates and clocks.

## User Stories

1. As a habit tracker, I want the home-screen widget to update automatically whenever I change data in the app, so that I never have to press a manual refresh button.
2. As a mindful user, I want the widget background to reflect the same Chinese ink wash landscape theme chosen in the main app, so that my home screen feels visually connected to the app experience.
3. As an everyday user, I want the widget to display full-width habit columns without wasted margin space, so that long habit names are not prematurely truncated.
4. As a commuter glancing quickly at my phone, I want habit day counts inside high-contrast matte colored plates, so that I can register my streak numbers in under 300 milliseconds.
5. As a minimalist, I want the widget free of redundant date and time timestamps, so that the widget remains calm, focused, and uncluttered.
6. As a user with limited phone battery and RAM, I want the widget background rendering to consume negligible system resources (< 180 KB memory, 0% battery drain), so that my launcher stays snappy and smooth.
7. As a user with accidental touches, I want cell taps on the widget to require a double-tap confirmation (`0?` prompt) before resetting, so that I never lose a 100-day habit streak by mistake.
8. As a user operating in dark mode or varying launcher themes, I want the widget colors to remain harmonious and legible regardless of wallpaper brightness.
9. As a foldable or compact phone owner, I want the widget layout to scale cleanly across small and wide widget placements without clipping.
10. As a privacy-focused individual, I want the widget to operate 100% locally and offline without background network sync or alarm managers.

## Implementation Decisions

### 1. Refresh Architecture Elimination
- Remove the manual refresh action (`ACTION_REFRESH`) from the widget provider receiver.
- Delete obsolete refresh-tracking methods and preference keys (`markWidgetRefreshed`, `widgetRefreshedOn`, `KEY_LAST_WIDGET_REFRESH_DAY`) from the store.
- Remove the refresh `ImageView` and right margin from the widget root layout, allocating 100% of horizontal width to the item grid.

### 2. Native Canvas Vector Background Renderer
- Construct a dedicated, lightweight procedural renderer that executes native `android.graphics.Canvas` and `android.graphics.Path` commands to draw the active theme (`MOUNTAIN`, `SAND_DUNES`, `SEA_HORIZON`, `SOLITARY_ISLE`, `WILLOW_LEAVES`, or `AUTO_DAILY`).
- Render at a compact resolution ($480 \times 280$ px) at 15% opacity on warm washi paper (`#F5E6D3`).
- Stream the rendered bitmap directly into the background `ImageView` inside `RemoteViews` during store updates, staying well below Android Binder's 1MB transaction threshold (< 180 KB payload).

### 3. Typography & Layout Hierarchy
- Place a quiet, compact `CountUp` title in warm ink tone (`#6B5D4F`) on the top-left margin (taking $\le 16\text{dp}$ vertical height).
- Omit all current date/clock text elements from the widget hierarchy.
- Maintain the 3-column cell structure with matte rotating circle plates (Olive, Terracotta, Ochre) and bold central day counts.

## Testing Decisions

- **Visual & Layout Parity Tests**: Verify that the widget background accurately renders each of the 5 Chinese ink wash themes and auto-daily rotation without throwing exceptions.
- **IPC Payload Safety Tests**: Assert that the generated background bitmap size remains strictly under 300 KB across high-density displays (avoiding `TransactionTooLargeException`).
- **Store & Synchronous Push Verification**: Confirm that `addItem`, `updateItem`, `deleteItem`, and `resetTo` trigger instant `pushWidgetUpdate` and update the launcher without requiring manual refresh.
- **Double-Tap State Machine Tests**: Verify that cell taps arm for 4 seconds, prompt with `0?`, and disarm safely without side effects.
- **Prior Art**: Builds on the existing `EdgeCaseMatrixTest` and `CountUpStoreTest` JVM suites.

## Out of Scope

- User-customizable per-widget background opacity sliders.
- Interactive multi-page widget swipe pagination (standard vertical scroll remains native).
- Cloud synchronization or web hooks.

## Further Notes

- The removal of the refresh button eliminates approximately 50 lines of redundant code and layout boilerplate while enhancing both visual symmetry and performance.

# Google Play Store Listing & Submission Package

This document contains everything needed to publish **CountUp** to the Google Play Store.

---

## 1. Store Listing Details

### App Title (max 30 characters)
```text
CountUp - Mindful Days Tracker
```

### Short Description (max 80 characters)
```text
Track days since or until habits, milestones, and events with calm widgets.
```

### Full Description (max 4,000 characters)
```text
CountUp is a calm, minimalist days-since and countdown tracker designed with a warm mid-century modern aesthetic. 

Whether you are tracking habits, days since a haircut, sobriety milestones, gym streaks, or counting down to an upcoming journey, CountUp keeps your most meaningful milestones front and center—without clutter, ads, or distractions.

✨ KEY FEATURES:

• Calm, Tactile Aesthetic & Dark Mode: Beautiful warm paper background, rich 3D card drop shadows with specular edge highlights, luminous dark mode palettes (Washi, Earth, Sumi), and thoughtful typography designed to bring mindfulness to daily tracking.

• 100+ Curated Tactile Icons: Handcrafted vector iconography covering habits, fitness, health, sobriety, finance, milestones, and mindfulness with automatic keyword styling and bilingual TalkBack support.

• 30 Classical Poetic Landscape Themes: Immerse your habit tracking in breathtaking Chinese ink wash landscapes that rotate daily or on-demand—from Mountain Peaks and Solitary Isle to Zen Bamboo and Falling Stars.

• Instant Search & 1-Tap Bidirectional Sorting: Find any habit effortlessly or sort by days elapsed, anchor date, or alphabetical name—tap again to toggle ascending and descending order.

• 5 Home Screen Widgets + Voice Quick Add:
  - Multi-Item Grid: Full-width live day counts on dynamic ink wash backgrounds.
  - Voice Quick Add: Tap the microphone on your widget to speak a habit name with zero microphone permissions (delegated out-of-process).
  - Hero Milestone (2x1): Dedicated single-milestone spotlight with gold accent dot and safe two-tap reset.
  - Zen Horizon Ribbon (4x1 & 2x1): Minimalist ribbon with on-widget unit cycling (days, weeks, months, years) directly on tap.
  - Solar Rhythm (4x2 & 2x2): Harmonious seasonal canvas aligning your count with the 24 traditional Chinese Solar Terms (24 节气).
  - Zen Pebble (1x1): Ultra-compact tactile tile hardened for all physical OEM launchers.

• In-Card Undo Whispers: Unobtrusive 10-second recovery alert directly on the reset card if a counter is reset accidentally, restoring your exact streak and anchor date with one tap.

• 2-Line Habit Notes & Countdowns: Add custom notes for quick context, with automatic "UNTIL" sub-labeling for future target dates.

• Offline SAF Backup & Transfer: Fully offline JSON export and restore with pre-validation preview, UUID deduplication, and merge/replace strategies.

🔒 100% PRIVATE & OFFLINE:
• Zero Permissions: CountUp requests zero runtime permissions. Voice input is delegated out-of-process to your platform assistant without requesting microphone access.
• Completely Offline: No internet connection required. Your data never leaves your device.
• No Ads, No Tracking: Pure, undisturbed tracking.
```

## 2. Package Name & Application ID

```text
com.countup.app
```

---

## 3. Store Categorization & Tags

| Attribute | Value |
|---|---|
| **Application Type** | App |
| **Category** | Productivity |
| **Tags** | Habit Tracker, Day Counter, Countdown, Minimalist, Offline, Zen, Widget |
| **Content Rating** | Everyone (PEGI 3 / ESRB Everyone) |
| **Target Audience** | All ages |
| **Contains Ads** | No |
| **In-App Purchases** | No |

---

## 4. Data Safety & Privacy Policy Declaration
 
 | Question | Play Console Answer |
 |---|---|
 | Does your app collect or share any user data? | **No** |
 | Does your app use an internet connection? | **No** (Zero network permissions in `AndroidManifest.xml`) |
 | Is your app intended for children? | **No** (General audience / Everyone) |
 | App Security | All data is stored in the local encrypted app sandbox. |
 
 ### Privacy Policy Text (For GitHub / Hosting)
 ```markdown
 # Privacy Policy for CountUp

 Last updated: September 2026

 CountUp ("we", "our", or "us") is committed to protecting your privacy.

 1. Information Collection and Use
 CountUp is a 100% offline application. We do not collect, transmit, store, or share any personal information, usage analytics, or device identifiers. All data you create (such as habit names, notes, and dates) remains strictly on your local device.

 2. Permissions
 CountUp requests zero device permissions. It does not require access to your internet connection, location, contacts, camera, or microphone. The optional Voice Quick Add feature delegates voice input out-of-process to the Android platform's native speech recognition service without requesting microphone permissions in CountUp.

 3. Third-Party Services
 CountUp does not integrate with any third-party SDKs, analytics providers, or advertising networks.

 4. Changes to This Privacy Policy
 Any future updates to this policy will be posted on this page.

 5. Contact Us
 If you have questions about this Privacy Policy, please contact the developer via the app support listing.
 ```

---

## 5. Package Artifacts Inventory

All release artifacts are located in `playstore_package/`:

| File | Description | Dimension / Size |
|---|---|---|
| [`app-release.aab`](file:///d:/Github/countUp/playstore_package/app-release.aab) | **Signed Production App Bundle** (Upload to Play Console) | **4.8 MB** *(Includes de-obfuscation mapping)* |
| [`app-release.apk`](file:///d:/Github/countUp/playstore_package/app-release.apk) | **Signed Release APK** (For physical device validation) | **2.76 MB** |
| [`icon_512x512.png`](file:///d:/Github/countUp/playstore_package/icon_512x512.png) | High-Res App Icon (32-bit RGB PNG, no alpha) | 512 x 512 px |
| [`feature_graphic_1024x500.png`](file:///d:/Github/countUp/playstore_package/feature_graphic_1024x500.png) | Feature Graphic Banner | 1024 x 500 px |
| [`screenshot_01_main_cards_light.png`](file:///d:/Github/countUp/playstore_package/screenshot_01_main_cards_light.png) | Screenshot 1: **Main List View & 3D Tactile Cards (Light)** | 1080 x 2400 px |
| [`screenshot_02_main_cards_dark.png`](file:///d:/Github/countUp/playstore_package/screenshot_02_main_cards_dark.png) | Screenshot 2: **Main List View & Luminous Palettes (Dark)** | 1080 x 2400 px |
| [`screenshot_03_future_countdown_light.png`](file:///d:/Github/countUp/playstore_package/screenshot_03_future_countdown_light.png) | Screenshot 3: **Future Event Countdowns & "UNTIL" Sub-labels** | 1080 x 2400 px |
| [`screenshot_04_edit_item_dialog_light.png`](file:///d:/Github/countUp/playstore_package/screenshot_04_edit_item_dialog_light.png) | Screenshot 4: **Tactile Milestone Editor & 2-Line Notes** | 1080 x 2400 px |
| [`screenshot_05_tactile_icon_picker_light.png`](file:///d:/Github/countUp/playstore_package/screenshot_05_tactile_icon_picker_light.png) | Screenshot 5: **100+ Curated Tactile Icon Picker** | 1080 x 2400 px |
| [`screenshot_06_solar_calendar_picker_light.png`](file:///d:/Github/countUp/playstore_package/screenshot_06_solar_calendar_picker_light.png) | Screenshot 6: **Zen Calendar Picker & Date Selection** | 1080 x 2400 px |
| [`screenshot_07_sort_search_filter_dark.png`](file:///d:/Github/countUp/playstore_package/screenshot_07_sort_search_filter_dark.png) | Screenshot 7: **Instant Search & 1-Tap Bidirectional Sorting** | 1080 x 2400 px |
| [`screenshot_08_theme_cycling_light.png`](file:///d:/Github/countUp/playstore_package/screenshot_08_theme_cycling_light.png) | Screenshot 8: **1-Tap Theme Cycling & Landscape Themes** | 1080 x 2400 px |
| [`screenshot_09_edit_item_dialog_dark.png`](file:///d:/Github/countUp/playstore_package/screenshot_09_edit_item_dialog_dark.png) | Screenshot 9: **Nighttime Milestone Editing & Customization** | 1080 x 2400 px |
| [`screenshot_10_privacy_backup_dialog_light.png`](file:///d:/Github/countUp/playstore_package/screenshot_10_privacy_backup_dialog_light.png) | Screenshot 10: **Offline SAF JSON Backup, Preview & Restore** | 1080 x 2400 px |
| [`screenshot_11_widgets_horizon_dark.png`](file:///d:/Github/countUp/playstore_package/screenshot_11_widgets_horizon_dark.png) | Screenshot 11: **Zen Horizon & Hero Milestone Widgets** | 1080 x 2400 px |
| [`screenshot_12_widgets_solar_multi_light.png`](file:///d:/Github/countUp/playstore_package/screenshot_12_widgets_solar_multi_light.png) | Screenshot 12: **Solar Rhythm & Multi-Item Grid Widgets** | 1080 x 2400 px |

*(Localized Chinese equivalents are also provided under `playstore_package/zh/`)*

---

## 6. Step-by-Step Play Console Submission Guide

1. **Sign in to Google Play Console**: Go to [play.google.com/console](https://play.google.com/console).
2. **Create App**: Click **Create app**, enter App name: `CountUp - Mindful Days Tracker`, Default language: `English (United States)`, Free app.
3. **Set up Store Presence**:
   - Navigate to **Grow** -> **Store presence** -> **Main store listing**.
   - Copy & paste the Short Description and Full Description from Section 1 above.
   - Upload `icon_512x512.png` to **App icon**.
   - Upload `feature_graphic_1024x500.png` to **Feature graphic**.
   - Upload all 12 screenshots (`screenshot_01` through `screenshot_12`) to **Phone screenshots** (and the Chinese set to Chinese localization if configuring multi-language store listings).
4. **Complete App Content Questionnaire**:
   - Privacy Policy: Complete offline note or link to privacy policy.
   - Ads: Select "No, my app does not contain ads".
   - App access: Select "All functionality is available without special access".
   - Data safety: Select "No user data is collected or shared".
   - Content rating: Start questionnaire -> Select "Utility / Productivity" -> Answer No to all content flags -> Rating will be "Everyone".
5. **Create Production Release**:
   - Go to **Release** -> **Production**.
   - Click **Create new release**.
   - Upload [`playstore_package/app-release.aab`](file:///d:/Github/countUp/playstore_package/app-release.aab) (or [`playstore_package/app-release-v56.aab`](file:///d:/Github/countUp/playstore_package/app-release-v56.aab)).
   - Enter Release notes:
     ```text
     Release 3.0.4 (v56):
     • Cold Startup Boost: Ahead-of-Time ART Baseline Profile pre-compiling critical startup paths for up to 40% faster cold launch.
     • Zero-Allocation 120Hz Scrolling: GPU-backed GraphicsLayer caching for all 30 Chinese ink wash themes.
     • Responsive Adaptive Layout: Centered column constraints for seamless scaling across foldables, tablets, and large screens.
     • Zero-Permission Voice Quick Add: Robust offline-friendly speech recognition with instant widget quick-add.
     • 100% Private & Zero Permissions: Zero background battery drain, no tracking, pure offline mindfulness.
     ```
   - Click **Review release** -> **Start rollout to Production**.

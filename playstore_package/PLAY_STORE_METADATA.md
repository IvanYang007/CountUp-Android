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

Whether you are tracking habits, days since a haircut, sobriety milestones, gym streaks, or counting down to an upcoming journey, CountUp keeps your most meaningful milestones front and center—without clutter, ads, or distractions.✨ KEY FEATURES:

• Calm, Tactile Aesthetic: Beautiful warm paper background, rich 3D card drop shadows with specular edge highlights, and thoughtful typography designed to bring mindfulness to daily tracking.

• 30 Classical Poetic Landscape Themes: Immerse your habit tracking in breathtaking Chinese ink wash landscapes that rotate daily or on-demand—from Mountain Peaks and Solitary Isle to Zen Bamboo and Falling Stars.

• Instant Search & 1-Tap Sorting: Find any habit effortlessly on the same frame or sort by days elapsed, anchor date, or alphabetical name.

• 2-Line Habit Notes & Comments: Add custom notes (e.g., "Trim sides short", "Morning 20-min mindfulness") to any counter for quick, elegant context.

• Full-Width 3-Column Home Screen Widget: Keep live day counts right on your launcher with a battery-efficient widget, dynamic ink wash backgrounds, and tactile color plates.

• Double-Tap In-Place Streak Reset: Reset any streak directly from your widget with safe 4-second confirmation without opening the app.

• Widget Visibility Toggle: Choose exactly which milestones appear on your home screen widget with a single tap of the modular widget grid icon.

• Future Event Countdowns: Set upcoming dates to track days until your trip, launch, or celebration arrives with automatic "UNTIL" sub-labeling.

🔒 100% PRIVATE & OFFLINE:
• Zero Permissions: CountUp requests no special Android permissions.
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

 Last updated: August 2026

 CountUp ("we", "our", or "us") is committed to protecting your privacy.

 1. Information Collection and Use
 CountUp is a 100% offline application. We do not collect, transmit, store, or share any personal information, usage analytics, or device identifiers. All data you create (such as habit names, notes, and dates) remains strictly on your local device.

 2. Permissions
 CountUp requests zero device permissions. It does not require access to your internet connection, location, contacts, camera, or microphone.

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
| [`app-release.aab`](file:///d:/Github/countUp/playstore_package/app-release.aab) | **Signed Production App Bundle** (Upload to Play Console) | **4.4 MB** *(Includes 2.6 MB de-obfuscation mapping)* |
| [`app-release.apk`](file:///d:/Github/countUp/playstore_package/app-release.apk) | **Signed Release APK** (For physical device validation) | **2.05 MB** *(Download size: ~1.6 MB)* |
| [`icon_512x512.png`](file:///d:/Github/countUp/playstore_package/icon_512x512.png) | High-Res App Icon (32-bit RGB PNG, no alpha) | 512 x 512 px |
| [`feature_graphic_1024x500.png`](file:///d:/Github/countUp/playstore_package/feature_graphic_1024x500.png) | Feature Graphic Banner | 1024 x 500 px |
| [`screenshot_1_main_list.png`](file:///d:/Github/countUp/playstore_package/screenshot_1_main_list.png) | Phone Screenshot 1: **Main List View & 3D Tactile Cards** | 1080 x 2400 px |
| [`screenshot_2_sort_search.png`](file:///d:/Github/countUp/playstore_package/screenshot_2_sort_search.png) | Phone Screenshot 2: **Instant Search & 1-Tap Sorting** | 1080 x 2400 px |
| [`screenshot_3_edit_item.png`](file:///d:/Github/countUp/playstore_package/screenshot_3_edit_item.png) | Phone Screenshot 3: **Habit Editor & Tactile Inputs** | 1080 x 2400 px |
| [`screenshot_4_date_picker.png`](file:///d:/Github/countUp/playstore_package/screenshot_4_date_picker.png) | Phone Screenshot 4: **Zen Material DatePicker** | 1080 x 2400 px |
| [`screenshot_5_themes_gallery.png`](file:///d:/Github/countUp/playstore_package/screenshot_5_themes_gallery.png) | Phone Screenshot 5: **30 Classical Landscape Themes** | 1080 x 2400 px |
| [`screenshot_6_home_widget.png`](file:///d:/Github/countUp/playstore_package/screenshot_6_home_widget.png) | Phone Screenshot 6: **Full-Width 3-Column Zen Widget** | 1080 x 2400 px |

---

## 6. Step-by-Step Play Console Submission Guide

1. **Sign in to Google Play Console**: Go to [play.google.com/console](https://play.google.com/console).
2. **Create App**: Click **Create app**, enter App name: `CountUp - Mindful Days Tracker`, Default language: `English (United States)`, Free app.
3. **Set up Store Presence**:
   - Navigate to **Grow** -> **Store presence** -> **Main store listing**.
   - Copy & paste the Short Description and Full Description from Section 1 above.
   - Upload `icon_512x512.png` to **App icon**.
   - Upload `feature_graphic_1024x500.png` to **Feature graphic**.
   - Upload all 6 screenshots (`screenshot_1` through `screenshot_6`) to **Phone screenshots**.
4. **Complete App Content Questionnaire**:
   - Privacy Policy: Complete offline note or link to privacy policy.
   - Ads: Select "No, my app does not contain ads".
   - App access: Select "All functionality is available without special access".
   - Data safety: Select "No user data is collected or shared".
   - Content rating: Start questionnaire -> Select "Utility / Productivity" -> Answer No to all content flags -> Rating will be "Everyone".
5. **Create Production Release**:
   - Go to **Release** -> **Production**.
   - Click **Create new release**.
   - Upload [`playstore_package/app-release.aab`](file:///d:/Github/countUp/playstore_package/app-release.aab).
   - Enter Release notes: `Release 1.4.6: Added curated Mid-Century Modern and Zen card color combinations with random Paper White defaults, 38 Phosphor line icons, instant search & 1-tap sorting, and ultra-minimalist home screen widgets.`
   - Click **Review release** -> **Start rollout to Production**.

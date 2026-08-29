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

• Calm, Tactile Aesthetic: Beautiful warm paper background, rich card drop shadows, and thoughtful typography designed to bring mindfulness to daily tracking.

• 2-Line Habit Notes & Comments: Add custom notes (e.g., "Trim sides short", "Better sleep quality") to any counter for quick, elegant context.

• Glanceable Home Screen Widget: Keep live day counts right on your Android home screen with a clean, battery-efficient widget.

• Widget Visibility Toggle: Choose exactly which milestones appear on your home screen widget with a single tap of the modular widget grid icon.

• Instant Reset & Custom Anchor Dates: Reset any streak to today with one tap, or pick any past or future calendar date using the integrated date picker.

• Future Event Countdowns: Set upcoming dates to track negative days until your trip, launch, or celebration arrives.

🔒 100% PRIVATE & OFFLINE:
• Zero Permissions: CountUp requests no special Android permissions.
• Completely Offline: No internet connection required. Your data never leaves your device.
• No Ads, No Tracking: Pure, undisturbed tracking.

Embrace simplicity and celebrate every day with CountUp.
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
| **Tags** | Habit Tracker, Day Counter, Countdown, Minimalist, Offline |
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
| [`app-release.aab`](file:///d:/Github/countUp/playstore_package/app-release.aab) | **Signed Production App Bundle** (Upload to Play Console) | ~8.0 MB |
| [`app-release.apk`](file:///d:/Github/countUp/playstore_package/app-release.apk) | **Signed Release APK** (For physical device validation) | ~8.3 MB |
| [`icon_512x512.png`](file:///d:/Github/countUp/playstore_package/icon_512x512.png) | High-Res App Icon (32-bit RGB PNG, no alpha) | 512 x 512 px |
| [`feature_graphic_1024x500.png`](file:///d:/Github/countUp/playstore_package/feature_graphic_1024x500.png) | Feature Graphic Banner | 1024 x 500 px |
| [`screenshot_1_main_list.png`](file:///d:/Github/countUp/playstore_package/screenshot_1_main_list.png) | Phone Screenshot 1: Main List View | 1080 x 2400 px |
| [`screenshot_2_edit_item.png`](file:///d:/Github/countUp/playstore_package/screenshot_2_edit_item.png) | Phone Screenshot 2: Edit Item & Notes | 1080 x 2400 px |
| [`screenshot_3_date_picker.png`](file:///d:/Github/countUp/playstore_package/screenshot_3_date_picker.png) | Phone Screenshot 3: Material DatePicker | 1080 x 2400 px |
| [`screenshot_4_home_widget.png`](file:///d:/Github/countUp/playstore_package/screenshot_4_home_widget.png) | Phone Screenshot 4: Home Screen Widget | 1080 x 2400 px |

---

## 6. Step-by-Step Play Console Submission Guide

1. **Sign in to Google Play Console**: Go to [play.google.com/console](https://play.google.com/console).
2. **Create App**: Click **Create app**, enter App name: `CountUp - Days Since Tracker`, Default language: `English (United States)`, Free app.
3. **Set up Store Presence**:
   - Navigate to **Grow** -> **Store presence** -> **Main store listing**.
   - Copy & paste the Short Description and Full Description from Section 1 above.
   - Upload `icon_512x512.png` to **App icon**.
   - Upload `feature_graphic_1024x500.png` to **Feature graphic**.
   - Upload `screenshot_1_main_list.png`, `screenshot_2_edit_item.png`, `screenshot_3_date_picker.png`, and `screenshot_4_home_widget.png` to **Phone screenshots**.
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
   - Enter Release notes: `Initial release of CountUp: track days since or countdown to events with a calm mid-century modern aesthetic and home screen widgets.`
   - Click **Review release** -> **Start rollout to Production**.

package com.countup.app

import android.os.Build
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat

/**
 * Modern, platform-compliant Edge-to-Edge initializer for Android 15 (API 35+) and backward-compatible to API 26.
 *
 * Eliminates all deprecated APIs flagged by Google Play Console:
 * - Omits android.view.Window.setStatusBarColor
 * - Omits android.view.Window.setNavigationBarColor
 * - Uses LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS instead of deprecated LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
 *
 * System bar appearance (light/dark icons) is managed dynamically by [ZenTheme]
 * via [WindowCompat.getInsetsController].
 * System bar background transparency is defined declaratively in [res/values/themes.xml].
 */
fun ComponentActivity.enableEdgeToEdge() {
    WindowCompat.setDecorFitsSystemWindows(window, false)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        window.attributes.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        window.isNavigationBarContrastEnforced = false
    }
}

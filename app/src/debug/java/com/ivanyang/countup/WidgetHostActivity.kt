package com.ivanyang.countup

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout

/**
 * Debug-only activity (never in release builds) that renders the real
 * [CountUpWidget] RemoteViews inside an AppWidgetHost so the widget can be
 * captured as a screenshot representing exactly what the launcher displays.
 *
 * Launch: adb shell am start -n com.ivanyang.countup/.WidgetHostActivity
 */
class WidgetHostActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val manager = AppWidgetManager.getInstance(this)
        val provider = ComponentName(this, CountUpWidgetReceiver::class.java)
        val host = AppWidgetHost(this, HOST_ID)

        val root = FrameLayout(this)
        root.setBackgroundColor(Color.WHITE)
        val card = FrameLayout(this)
        card.layoutParams = FrameLayout.LayoutParams(WIDGET_WIDTH_PX, WIDGET_HEIGHT_PX).apply {
            gravity = Gravity.CENTER
        }
        card.setBackgroundColor(Color.WHITE)
        root.addView(card)
        setContentView(root)

        val appWidgetId = host.allocateAppWidgetId()
        val bound = manager.bindAppWidgetIdIfAllowed(appWidgetId, provider)
        // startListening() is required for the host to receive widget updates;
        // without it the host view stays in its error state ("Couldn't add").
        host.startListening()
        val hostView = host.createView(this, appWidgetId, null)
        hostView.setAppWidget(appWidgetId, manager.getAppWidgetInfo(appWidgetId))
        card.addView(hostView)

        // Not a failure if binding was denied; hostView still renders the
        // provider's RemoteViews when bound.
        @Suppress("UNUSED_VARIABLE")
        val keep = bound
    }

    private companion object {
        const val HOST_ID = 2048
        const val WIDGET_WIDTH_PX = 760
        const val WIDGET_HEIGHT_PX = 180
    }
}

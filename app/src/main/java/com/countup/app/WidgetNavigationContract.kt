package com.countup.app

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/**
 * Family taxonomy for widgets, encapsulating launch intent partition offsets.
 */
enum class WidgetFamily(val launchOffset: Int) {
    HERO(101),
    ZEN_HORIZON(150),
    SOLAR_RHYTHM(202),
    ZEN_PEBBLE(303);
}

/**
 * Shared contract constants and navigation utilities for widget-to-app intents.
 */
object WidgetNavigationContract {
    const val EXTRA_TARGET_ITEM_ID = "EXTRA_TARGET_ITEM_ID"
    const val EXTRA_APP_WIDGET_ID = "EXTRA_APP_WIDGET_ID"

    // Action constants for widget broadcast receivers
    const val ACTION_CYCLE_HERO_DISPLAY_MODE = "com.countup.app.ACTION_CYCLE_HERO_DISPLAY_MODE"
    const val ACTION_CYCLE_ZEN_HORIZON_UNIT = "com.countup.app.ACTION_CYCLE_ZEN_HORIZON_UNIT"

    // Activity launch request code partition offsets per widget family
    const val HERO_PENDING_INTENT_OFFSET = 101
    const val ZEN_HORIZON_PENDING_INTENT_OFFSET = 150
    const val SOLAR_RHYTHM_PENDING_INTENT_OFFSET = 202
    const val ZEN_PEBBLE_PENDING_INTENT_OFFSET = 303

    // Broadcast pending intent request code offsets
    const val HERO_RESET_PENDING_INTENT_OFFSET = 4004
    const val ZEN_HORIZON_CYCLE_PENDING_INTENT_OFFSET = 8888
    const val HERO_CYCLE_PENDING_INTENT_OFFSET = 9009

    fun createLaunchIntent(context: Context, targetItemId: String? = null): Intent {
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (targetItemId != null) {
                putExtra(EXTRA_TARGET_ITEM_ID, targetItemId)
            }
        }
    }

    /**
     * Attaches a pending intent to [viewId] that opens MainActivity when a widget is in empty state.
     */
    fun attachEmptyStateLaunchIntent(
        views: RemoteViews,
        context: Context,
        appWidgetId: Int,
        viewId: Int,
    ) {
        val launchIntent = createLaunchIntent(context)
        val pendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(viewId, pendingIntent)
    }

    /**
     * Attaches a pending intent to [viewId] that opens MainActivity targeting [targetItemId].
     */
    fun attachItemLaunchIntent(
        views: RemoteViews,
        context: Context,
        appWidgetId: Int,
        targetItemId: String,
        family: WidgetFamily,
        viewId: Int,
    ) {
        val launchIntent = createLaunchIntent(context, targetItemId)
        val requestCode = resolveRequestCode(targetItemId, appWidgetId, family.launchOffset)
        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(viewId, pendingIntent)
    }

    /**
     * Attaches a broadcast pending intent to [viewId].
     */
    fun attachBroadcastPendingIntent(
        views: RemoteViews,
        context: Context,
        requestCode: Int,
        intent: Intent,
        viewId: Int,
    ) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(viewId, pendingIntent)
    }

    fun resolveRequestCode(targetItemId: String?, appWidgetId: Int, offset: Int): Int {
        val base = if (targetItemId != null) {
            targetItemId.hashCode() * 31 + appWidgetId
        } else {
            appWidgetId
        }
        return (base + offset) and 0x7FFFFFFF
    }
}

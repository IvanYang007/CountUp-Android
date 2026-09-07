package com.countup.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import java.time.LocalDate

/**
 * Zen Horizon Ribbon Widget provider (4x1 & 2x1).
 * Displays active counter with a 1dp hairline milestone progress track,
 * Patina Gold circular pebble indicator, and in-place unit cycling.
 */
class ZenHorizonWidgetReceiver : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == WidgetNavigationContract.ACTION_CYCLE_ZEN_HORIZON_UNIT) {
            val appWidgetId = intent.getIntExtra(WidgetNavigationContract.EXTRA_APP_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val info = try {
                    appWidgetManager.getAppWidgetInfo(appWidgetId)
                } catch (_: Exception) {
                    null
                }
                if (info == null || info.provider.packageName != context.packageName) {
                    return
                }
                val appContext = context.applicationContext
                launchAsync {
                    val store = CountUpStore(appContext)
                    val currentUnit = store.getZenHorizonUnit(appWidgetId)
                    val nextUnit = currentUnit.next()
                    store.setZenHorizonUnit(appWidgetId, nextUnit)
                    pushZenHorizonWidgetUpdate(appContext, appWidgetId)
                }
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val appContext = context.applicationContext
        launchAsync {
            for (appWidgetId in appWidgetIds) {
                pushZenHorizonWidgetUpdate(appContext, appWidgetId)
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle?,
    ) {
        val appContext = context.applicationContext
        launchAsync {
            pushZenHorizonWidgetUpdate(appContext, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val appContext = context.applicationContext
        launchAsync {
            val store = CountUpStore(appContext)
            for (id in appWidgetIds) {
                store.removeZenHorizonBinding(id)
            }
        }
    }
}

/** Pushes an update to all placed Zen Horizon widgets on the launcher. */
fun pushAllZenHorizonWidgetsUpdate(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val ids = manager.getAppWidgetIds(ComponentName(context, ZenHorizonWidgetReceiver::class.java))
    if (ids.isEmpty()) return
    for (id in ids) {
        pushZenHorizonWidgetUpdate(context, id)
    }
}

/** Renders and pushes RemoteViews for a single Zen Horizon widget. */
fun pushZenHorizonWidgetUpdate(context: Context, appWidgetId: Int) {
    val manager = AppWidgetManager.getInstance(context)
    val store = CountUpStore(context)
    val items = store.items()
    val boundItemId = store.getZenHorizonBinding(appWidgetId)
    val targetItem = ZenWidgetReducer.resolveTargetItem(items, boundItemId)

    val today = LocalDate.now()
    val isDark = isNightMode(context)
    val views = buildResponsiveZenHorizonRemoteViews(context, targetItem, today, appWidgetId, isDark)
    manager.updateAppWidget(appWidgetId, views)
}

/** Builds responsive RemoteViews handling 4x1 and 2x1 layouts. */
fun buildResponsiveZenHorizonRemoteViews(
    context: Context,
    item: CountUpItem?,
    today: LocalDate,
    appWidgetId: Int,
    isDark: Boolean,
): RemoteViews {
    val views4x1 = buildZenHorizonRemoteViews(context, item, today, appWidgetId, isDark, is2x1 = false)
    val views2x1 = buildZenHorizonRemoteViews(context, item, today, appWidgetId, isDark, is2x1 = true)

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        RemoteViews(
            mapOf(
                SizeF(110f, 40f) to views2x1,
                SizeF(250f, 40f) to views4x1,
            )
        )
    } else {
        val manager = AppWidgetManager.getInstance(context)
        val options = manager.getAppWidgetOptions(appWidgetId)
        val minWidth = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250) ?: 250
        if (minWidth < 200) views2x1 else views4x1
    }
}

/** Builds a single RemoteViews layout for either 4x1 or 2x1. */
fun buildZenHorizonRemoteViews(
    context: Context,
    item: CountUpItem?,
    today: LocalDate,
    appWidgetId: Int,
    isDark: Boolean,
    is2x1: Boolean,
): RemoteViews {
    val layoutRes = if (is2x1) R.layout.widget_zen_horizon_2x1 else R.layout.widget_zen_horizon_4x1
    val views = RemoteViews(context.packageName, layoutRes)

    if (item == null) {
        views.setViewVisibility(R.id.zen_horizon_empty, View.VISIBLE)
        views.setViewVisibility(R.id.zen_horizon_left_section, View.GONE)
        views.setViewVisibility(R.id.zen_horizon_right_section, View.GONE)

        val defaultBg = WidgetThemeTokens.resolve(isDark).canvasBg
        views.setInt(R.id.zen_horizon_root, "setBackgroundColor", defaultBg)

        val launchIntent = WidgetNavigationContract.createLaunchIntent(context)
        val pendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(R.id.zen_horizon_root, pendingIntent)
        return views
    }

    views.setViewVisibility(R.id.zen_horizon_empty, View.GONE)
    views.setViewVisibility(R.id.zen_horizon_left_section, View.VISIBLE)
    views.setViewVisibility(R.id.zen_horizon_right_section, View.VISIBLE)

    val store = CountUpStore(context)
    val unit = store.getZenHorizonUnit(appWidgetId)
    val state = ZenWidgetReducer.resolveZenWidgetState(
        item = item,
        today = today,
        unit = unit,
        isDarkMode = isDark,
    )

    // Set tranquil background color
    views.setInt(R.id.zen_horizon_root, "setBackgroundColor", state.palette.canvasBg)

    // Left section: Title and Subtitle
    views.setTextViewText(R.id.zen_horizon_title, state.title.uppercase())
    views.setTextColor(R.id.zen_horizon_title, state.palette.secondaryInk)

    if (!is2x1) {
        views.setTextViewText(R.id.zen_horizon_subtitle, state.startDateFormatted)
        views.setTextColor(R.id.zen_horizon_subtitle, state.palette.secondaryInk)
    }

    // Hairline Track with Patina Gold Pebble Indicator
    val trackBitmap = ZenHorizonTrackRenderer.renderTrack(
        progress = state.milestoneProgress,
        isDark = isDark,
    )
    if (trackBitmap != null) {
        views.setImageViewBitmap(R.id.zen_horizon_track, trackBitmap)
    }

    // Right section: Numeral and Unit
    views.setTextViewText(R.id.zen_horizon_number, state.primaryValueText)
    views.setTextColor(R.id.zen_horizon_number, state.palette.primaryInk)

    views.setTextViewText(R.id.zen_horizon_unit, state.unitLabelText)
    views.setTextColor(R.id.zen_horizon_unit, state.palette.secondaryInk)
    views.setContentDescription(R.id.zen_horizon_unit, context.getString(R.string.cd_zen_horizon_cycle_unit))

    // Left tap -> Open item in CountUp
    val launchIntent = WidgetNavigationContract.createLaunchIntent(context, item.id)
    val leftPendingIntent = PendingIntent.getActivity(
        context,
        WidgetNavigationContract.resolveRequestCode(item.id, appWidgetId, WidgetNavigationContract.ZEN_HORIZON_PENDING_INTENT_OFFSET),
        launchIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    views.setOnClickPendingIntent(R.id.zen_horizon_left_section, leftPendingIntent)

    // Right tap -> In-place unit cycling (#13)
    val cycleIntent = Intent(context, ZenHorizonWidgetReceiver::class.java).apply {
        action = WidgetNavigationContract.ACTION_CYCLE_ZEN_HORIZON_UNIT
        putExtra(WidgetNavigationContract.EXTRA_APP_WIDGET_ID, appWidgetId)
    }
    val cyclePendingIntent = PendingIntent.getBroadcast(
        context,
        appWidgetId + WidgetNavigationContract.ZEN_HORIZON_CYCLE_PENDING_INTENT_OFFSET,
        cycleIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    views.setOnClickPendingIntent(R.id.zen_horizon_right_section, cyclePendingIntent)

    return views
}

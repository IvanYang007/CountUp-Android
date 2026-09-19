package com.countup.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import java.time.LocalDate

/**
 * The Zen Pebble (1x1 极简原石) Widget provider.
 * Ultra-compact single-cell token optimized for 56x56dp to 80x80dp launcher cells.
 * Features a bold serene count glyph, micro-unit, hairline ink dash, and a one-word label.
 */
class ZenPebbleWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val appContext = context.applicationContext
        MidnightAlarmReceiver.scheduleMidnightAlarm(appContext)
        launchAsync {
            for (appWidgetId in appWidgetIds) {
                pushZenPebbleWidgetUpdate(appContext, appWidgetId)
            }
        }
    }

    override fun onEnabled(context: Context) {
        MidnightAlarmReceiver.scheduleMidnightAlarm(context.applicationContext)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val appContext = context.applicationContext
        launchAsync {
            val store = CountUpStore.getInstance(appContext)
            for (id in appWidgetIds) {
                store.removeZenPebbleBinding(id)
            }
        }
    }

    override fun onRestored(context: Context, oldWidgetIds: IntArray, newWidgetIds: IntArray) {
        val appContext = context.applicationContext
        CountUpStore.getInstance(appContext).remapWidgetBindings(oldWidgetIds, newWidgetIds)
        MidnightAlarmReceiver.scheduleMidnightAlarm(appContext)
        launchAsync {
            for (newId in newWidgetIds) {
                pushZenPebbleWidgetUpdate(appContext, newId)
            }
        }
    }
}

/** Pushes update to all placed Zen Pebble widgets on the launcher. */
fun pushAllZenPebbleWidgetsUpdate(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val ids = manager.getAppWidgetIds(ComponentName(context, ZenPebbleWidgetReceiver::class.java))
    if (ids.isEmpty()) return
    for (id in ids) {
        pushZenPebbleWidgetUpdate(context, id)
    }
}

/** Renders and pushes RemoteViews for a single Zen Pebble widget. */
fun pushZenPebbleWidgetUpdate(context: Context, appWidgetId: Int) {
    val manager = AppWidgetManager.getInstance(context)
    val store = CountUpStore.getInstance(context)
    val items = store.items()
    val boundItemId = store.getZenPebbleBinding(appWidgetId)
    val targetItem = ZenWidgetReducer.resolveTargetItem(items, boundItemId)
    val customTag = store.getZenPebbleTag(appWidgetId)

    val today = LocalDate.now()
    val isDark = isNightMode(context)
    val views = buildZenPebbleRemoteViews(context, targetItem, today, appWidgetId, isDark, customTag)
    manager.updateAppWidget(appWidgetId, views)
}

/** Builds the RemoteViews hierarchy for the 1x1 Zen Pebble widget. */
private fun buildZenPebbleRemoteViews(
    context: Context,
    item: CountUpItem?,
    today: LocalDate,
    appWidgetId: Int,
    isDark: Boolean,
    customTag: String? = null,
): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_zen_pebble_1x1)

    if (item == null) {
        views.setViewVisibility(R.id.zen_pebble_empty, View.VISIBLE)
        views.setViewVisibility(R.id.zen_pebble_content, View.GONE)

        val defaultBg = WidgetThemeTokens.resolve(isDark).canvasBg
        views.setInt(R.id.zen_pebble_bg, "setColorFilter", defaultBg)
        WidgetNavigationContract.attachEmptyStateLaunchIntent(views, context, appWidgetId, android.R.id.background)
        return views
    }

    views.setViewVisibility(R.id.zen_pebble_empty, View.GONE)
    views.setViewVisibility(R.id.zen_pebble_content, View.VISIBLE)

    val widgetState = ZenWidgetReducer.resolveZenWidgetState(
        item = item,
        today = today,
        isDarkMode = isDark,
    )
    val oneWordLabel = item.resolveOneWordLabel(customTag)

    // Set tranquil background with adaptive pebble corners (16dp / system radius on v31)
    views.setInt(R.id.zen_pebble_bg, "setColorFilter", widgetState.palette.canvasBg)

    // Unified TalkBack semantic description for accessibility
    val unitRes = when {
        widgetState.isFuture -> R.string.unit_until
        widgetState.daysCount == 1L -> R.string.unit_day_singular
        else -> R.string.unit_days
    }
    val unitDays = context.getString(unitRes)
    val a11yDesc = if (widgetState.isFuture) {
        val daysUnit = context.getString(if (widgetState.daysCount == 1L) R.string.unit_day_singular else R.string.unit_days)
        "${item.name}: ${widgetState.compactValueText} $daysUnit $unitDays"
    } else {
        "${item.name}: ${widgetState.compactValueText} $unitDays"
    }
    views.setContentDescription(android.R.id.background, a11yDesc)

    // Bold compact numeral
    views.setTextViewText(R.id.zen_pebble_number, widgetState.pebbleNumberText)
    views.setTextColor(R.id.zen_pebble_number, widgetState.palette.primaryInk)

    // Micro-unit label
    views.setTextViewText(R.id.zen_pebble_unit, unitDays)
    views.setTextColor(R.id.zen_pebble_unit, widgetState.palette.secondaryInk)

    // Hairline ink dash using semantic theme token
    views.setInt(R.id.zen_pebble_dash, "setBackgroundColor", widgetState.palette.microDivider)

    // Subtle 1-word tag
    views.setTextViewText(R.id.zen_pebble_tag, oneWordLabel)
    views.setTextColor(R.id.zen_pebble_tag, widgetState.palette.accentPrimary)

    // Tap anywhere on pebble opens specific event in CountUp
    WidgetNavigationContract.attachItemLaunchIntent(
        views = views,
        context = context,
        appWidgetId = appWidgetId,
        targetItemId = item.id,
        family = WidgetFamily.ZEN_PEBBLE,
        viewId = android.R.id.background,
    )

    return views
}

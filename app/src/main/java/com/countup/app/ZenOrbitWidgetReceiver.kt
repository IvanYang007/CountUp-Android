package com.countup.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import java.time.LocalDate

/**
 * The Zen Orbit (双环律动) Widget provider.
 * Concentric dual-ring dial harmonizing active current count
 * with the counter's historical average cadence.
 */
class ZenOrbitWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val appContext = context.applicationContext
        MidnightAlarmReceiver.scheduleMidnightAlarm(appContext)
        launchAsync {
            for (appWidgetId in appWidgetIds) {
                pushZenOrbitWidgetUpdate(appContext, appWidgetId)
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
                store.removeZenOrbitBinding(id)
            }
        }
    }

    override fun onRestored(context: Context, oldWidgetIds: IntArray, newWidgetIds: IntArray) {
        val appContext = context.applicationContext
        CountUpStore.getInstance(appContext).remapWidgetBindings(oldWidgetIds, newWidgetIds)
        MidnightAlarmReceiver.scheduleMidnightAlarm(appContext)
        launchAsync {
            for (newId in newWidgetIds) {
                pushZenOrbitWidgetUpdate(appContext, newId)
            }
        }
    }
}

/** Pushes update to all placed Zen Orbit widgets on the launcher. */
fun pushAllZenOrbitWidgetsUpdate(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val ids = manager.getAppWidgetIds(ComponentName(context, ZenOrbitWidgetReceiver::class.java))
    if (ids.isEmpty()) return
    for (id in ids) {
        pushZenOrbitWidgetUpdate(context, id)
    }
}

/** Renders and pushes RemoteViews for a single Zen Orbit widget. */
fun pushZenOrbitWidgetUpdate(context: Context, appWidgetId: Int) {
    val manager = AppWidgetManager.getInstance(context)
    val store = CountUpStore.getInstance(context)
    val items = store.items()
    val boundItemId = store.getZenOrbitBinding(appWidgetId)
    val targetItem = ZenWidgetReducer.resolveTargetItem(items, boundItemId)

    val today = LocalDate.now()
    val isDark = isNightMode(context)
    val views = buildZenOrbitRemoteViews(context, targetItem, today, appWidgetId, isDark)
    manager.updateAppWidget(appWidgetId, views)
}

/** Builds the RemoteViews hierarchy for the 2x2 Zen Orbit widget. */
private fun buildZenOrbitRemoteViews(
    context: Context,
    item: CountUpItem?,
    today: LocalDate,
    appWidgetId: Int,
    isDark: Boolean,
): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_zen_orbit_2x2)

    if (item == null) {
        views.setViewVisibility(R.id.zen_orbit_empty, View.VISIBLE)
        views.setViewVisibility(R.id.zen_orbit_content, View.GONE)

        val defaultBg = WidgetThemeTokens.resolve(isDark).canvasBg
        views.setInt(R.id.zen_orbit_root, "setBackgroundColor", defaultBg)
        WidgetNavigationContract.attachEmptyStateLaunchIntent(views, context, appWidgetId, R.id.zen_orbit_root)
        return views
    }

    views.setViewVisibility(R.id.zen_orbit_empty, View.GONE)
    views.setViewVisibility(R.id.zen_orbit_content, View.VISIBLE)

    val state = ZenWidgetReducer.resolveZenOrbitState(
        item = item,
        today = today,
        isDarkMode = isDark,
    )

    // Base tranquil background
    views.setInt(R.id.zen_orbit_root, "setBackgroundColor", state.palette.canvasBg)

    // Render Procedural Dial
    val dialBitmap = ZenOrbitTrackRenderer.renderOrbit(
        daysCount = state.daysCount,
        averageDays = state.averageDays,
        resetCount = state.resetCount,
        isDark = isDark,
    )
    if (dialBitmap != null) {
        views.setImageViewBitmap(R.id.zen_orbit_dial, dialBitmap)
    }

    // Event Tag
    views.setTextViewText(R.id.zen_orbit_tag, state.oneWordLabel)
    views.setTextColor(R.id.zen_orbit_tag, state.palette.secondaryInk)

    // Main Numeral
    views.setTextViewText(R.id.zen_orbit_number, state.daysCount.toString())
    views.setTextColor(R.id.zen_orbit_number, state.palette.primaryInk)

    // Unit Label
    val unitRes = when {
        state.isFuture -> R.string.unit_until
        state.daysCount == 1L -> R.string.unit_day_singular
        else -> R.string.unit_days
    }
    views.setTextViewText(R.id.zen_orbit_unit, context.getString(unitRes))
    views.setTextColor(R.id.zen_orbit_unit, state.palette.secondaryInk)

    // Rhythm Pill Badge
    val pillColor = if (state.isTranscended || state.isHarmonic) {
        state.palette.accentKintsugi
    } else {
        if (isDark) 0xFFD4B87C.toInt() else 0xFF9C8360.toInt()
    }
    val pillText = when {
        !state.hasHistory -> context.getString(R.string.zen_orbit_first_cycle)
        state.isHarmonic -> context.getString(R.string.zen_orbit_harmonic_badge)
        state.isTranscended -> context.getString(R.string.zen_orbit_beyond_badge, state.deltaDays)
        else -> context.getString(R.string.zen_orbit_avg_badge, state.averageDays)
    }
    views.setTextViewText(R.id.zen_orbit_rhythm_pill, pillText)
    views.setTextColor(R.id.zen_orbit_rhythm_pill, pillColor)

    // TalkBack Content Description
    val a11yText = "${state.title}: ${state.daysCount} ${context.getString(unitRes)} ($pillText)"
    views.setContentDescription(R.id.zen_orbit_root, a11yText)

    // Tap anywhere opens target item in MainActivity
    WidgetNavigationContract.attachItemLaunchIntent(
        views = views,
        context = context,
        appWidgetId = appWidgetId,
        targetItemId = item.id,
        family = WidgetFamily.ZEN_ORBIT,
        viewId = R.id.zen_orbit_root,
    )

    return views
}

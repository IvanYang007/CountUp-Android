package com.countup.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import java.time.LocalDate

/**
 * The Tsukimi (月相之镜 · 朔望之镜) 2x2 Widget provider.
 * Synodic lunar mirror harmonizing milestone count with celestial moon phases.
 */
class TsukimiWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val appContext = context.applicationContext
        MidnightAlarmReceiver.scheduleMidnightAlarm(appContext)
        launchAsync {
            for (appWidgetId in appWidgetIds) {
                pushTsukimiWidgetUpdate(appContext, appWidgetId)
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
                store.removeTsukimiBinding(id)
            }
        }
    }

    override fun onRestored(context: Context, oldWidgetIds: IntArray, newWidgetIds: IntArray) {
        val appContext = context.applicationContext
        CountUpStore.getInstance(appContext).remapWidgetBindings(oldWidgetIds, newWidgetIds)
        MidnightAlarmReceiver.scheduleMidnightAlarm(appContext)
        launchAsync {
            for (newId in newWidgetIds) {
                pushTsukimiWidgetUpdate(appContext, newId)
            }
        }
    }
}

/** Pushes update to all placed Tsukimi widgets on the launcher. */
fun pushAllTsukimiWidgetsUpdate(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val ids = manager.getAppWidgetIds(ComponentName(context, TsukimiWidgetReceiver::class.java))
    if (ids.isEmpty()) return
    for (id in ids) {
        pushTsukimiWidgetUpdate(context, id)
    }
}

/** Renders and pushes RemoteViews for a single Tsukimi widget. */
fun pushTsukimiWidgetUpdate(context: Context, appWidgetId: Int) {
    val manager = AppWidgetManager.getInstance(context)
    val store = CountUpStore.getInstance(context)
    val items = store.items()
    val boundItemId = store.getTsukimiBinding(appWidgetId)
    val targetItem = ZenWidgetReducer.resolveTargetItem(items, boundItemId)

    val today = LocalDate.now()
    val isDark = isNightMode(context)
    val views = buildTsukimiRemoteViews(context, targetItem, today, appWidgetId, isDark)
    manager.updateAppWidget(appWidgetId, views)
}

/** Builds the RemoteViews hierarchy for the 2x2 Tsukimi widget. */
private fun buildTsukimiRemoteViews(
    context: Context,
    item: CountUpItem?,
    today: LocalDate,
    appWidgetId: Int,
    isDark: Boolean,
): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_tsukimi_2x2)

    if (item == null) {
        views.setViewVisibility(R.id.tsukimi_widget_empty, View.VISIBLE)
        views.setViewVisibility(R.id.tsukimi_widget_content, View.GONE)

        val defaultBg = WidgetThemeTokens.resolve(isDark).canvasBg
        views.setInt(R.id.tsukimi_widget_root, "setBackgroundColor", defaultBg)
        WidgetNavigationContract.attachEmptyStateLaunchIntent(views, context, appWidgetId, R.id.tsukimi_widget_root)
        return views
    }

    views.setViewVisibility(R.id.tsukimi_widget_empty, View.GONE)
    views.setViewVisibility(R.id.tsukimi_widget_content, View.VISIBLE)

    val state = ZenWidgetReducer.resolveTsukimiState(
        item = item,
        today = today,
        isDarkMode = isDark,
    )

    // Base tranquil background
    views.setInt(R.id.tsukimi_widget_root, "setBackgroundColor", state.palette.canvasBg)

    // Render Procedural Moon Disc Bitmap
    val moonBitmap = TsukimiMoonRenderer.renderMoonDisc(
        lunarState = state.lunarState,
        isDark = isDark,
    )
    if (moonBitmap != null) {
        views.setImageViewBitmap(R.id.tsukimi_widget_moon_disc, moonBitmap)
    }

    // Milestone Numeral
    views.setTextViewText(R.id.tsukimi_widget_number, state.daysCount.toString())
    views.setTextColor(R.id.tsukimi_widget_number, state.palette.primaryInk)

    // Unit Label
    val unitRes = when {
        state.isFuture -> R.string.unit_until
        state.daysCount == 1L -> R.string.unit_day_singular
        else -> R.string.unit_days
    }
    views.setTextViewText(R.id.tsukimi_widget_unit, context.getString(unitRes))
    views.setTextColor(R.id.tsukimi_widget_unit, state.palette.secondaryInk)

    // Counter Item Name at the bottom
    val nameText = state.title.ifBlank { state.oneWordLabel }
    views.setTextViewText(R.id.tsukimi_widget_tag, nameText)
    views.setTextColor(R.id.tsukimi_widget_tag, state.palette.secondaryInk)

    // Accessibility Content Description
    val a11yText = "${nameText}: ${state.daysCount} ${context.getString(unitRes)}, ${state.phaseVerse}"
    views.setContentDescription(R.id.tsukimi_widget_root, a11yText)

    // Tap anywhere opens target item in MainActivity
    WidgetNavigationContract.attachItemLaunchIntent(
        views = views,
        context = context,
        appWidgetId = appWidgetId,
        targetItemId = item.id,
        family = WidgetFamily.TSUKIMI,
        viewId = R.id.tsukimi_widget_root,
    )

    return views
}

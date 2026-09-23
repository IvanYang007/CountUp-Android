package com.countup.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import java.time.LocalDate

/**
 * The Shuin (金石朱印 · 1x1) Cinnabar Seal Widget provider.
 * Unboxed, containerless milestone jewel floating directly on the wallpaper.
 */
class ShuinWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val appContext = context.applicationContext
        MidnightAlarmReceiver.scheduleMidnightAlarm(appContext)
        launchAsync {
            for (appWidgetId in appWidgetIds) {
                pushShuinWidgetUpdate(appContext, appWidgetId)
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
                store.removeShuinBinding(id)
            }
        }
    }

    override fun onRestored(context: Context, oldWidgetIds: IntArray, newWidgetIds: IntArray) {
        val appContext = context.applicationContext
        CountUpStore.getInstance(appContext).remapWidgetBindings(oldWidgetIds, newWidgetIds)
        MidnightAlarmReceiver.scheduleMidnightAlarm(appContext)
        launchAsync {
            for (newId in newWidgetIds) {
                pushShuinWidgetUpdate(appContext, newId)
            }
        }
    }
}

/** Pushes update to all placed Shuin widgets on the launcher. */
fun pushAllShuinWidgetsUpdate(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val ids = manager.getAppWidgetIds(ComponentName(context, ShuinWidgetReceiver::class.java))
    if (ids.isEmpty()) return
    for (id in ids) {
        pushShuinWidgetUpdate(context, id)
    }
}

/** Renders and pushes RemoteViews for a single Shuin widget. */
fun pushShuinWidgetUpdate(context: Context, appWidgetId: Int) {
    val manager = AppWidgetManager.getInstance(context)
    val store = CountUpStore.getInstance(context)
    val items = store.items()
    val boundItemId = store.getShuinBinding(appWidgetId)
    val targetItem = ZenWidgetReducer.resolveTargetItem(items, boundItemId)

    val today = LocalDate.now()
    val isDark = isNightMode(context)
    val views = buildShuinRemoteViews(context, targetItem, today, appWidgetId, isDark)
    manager.updateAppWidget(appWidgetId, views)
}

/** Builds the RemoteViews hierarchy for the 1x1 Shuin widget. */
private fun buildShuinRemoteViews(
    context: Context,
    item: CountUpItem?,
    today: LocalDate,
    appWidgetId: Int,
    isDark: Boolean,
): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_shuin_1x1)

    if (item == null) {
        views.setViewVisibility(R.id.shuin_empty, View.VISIBLE)
        views.setViewVisibility(R.id.shuin_content, View.GONE)
        WidgetNavigationContract.attachEmptyStateLaunchIntent(views, context, appWidgetId, android.R.id.background)
        return views
    }

    views.setViewVisibility(R.id.shuin_empty, View.GONE)
    views.setViewVisibility(R.id.shuin_content, View.VISIBLE)

    val state = ZenWidgetReducer.resolveShuinState(
        item = item,
        today = today,
        isDarkMode = isDark,
    )

    // Render Procedural Organic Cinnabar Seal Bitmap
    val sealBitmap = ShuinSealRenderer.renderSeal(isDark = isDark)
    if (sealBitmap != null) {
        views.setImageViewBitmap(R.id.shuin_seal_plate, sealBitmap)
    }

    // Milestone Numeral
    views.setTextViewText(R.id.shuin_number, state.compactNumberText)

    // Unit Label
    val unitRes = when {
        state.isFuture -> R.string.unit_until
        state.daysCount == 1L -> R.string.unit_day_singular
        else -> R.string.unit_days
    }
    val unitText = context.getString(unitRes).uppercase()
    views.setTextViewText(R.id.shuin_unit, unitText)

    // Item Name at the bottom
    val nameText = state.title.ifBlank { state.oneWordLabel }
    views.setTextViewText(R.id.shuin_name, nameText)

    // Accessibility Content Description
    val a11yText = "${state.title}: ${state.compactNumberText} $unitText"
    views.setContentDescription(android.R.id.background, a11yText)

    // Tap anywhere on seal opens target item in MainActivity
    WidgetNavigationContract.attachItemLaunchIntent(
        views = views,
        context = context,
        appWidgetId = appWidgetId,
        targetItemId = item.id,
        family = WidgetFamily.SHUIN,
        viewId = android.R.id.background,
    )

    return views
}

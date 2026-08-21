package com.ivanyang.countup

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import java.time.LocalDate

/**
 * Classic RemoteViews home-screen widget (the "Gmail method"): the app pushes
 * new RemoteViews imperatively via [pushWidgetUpdate] after every store write,
 * so a data change lands on the launcher immediately instead of waiting for a
 * Glance re-composition pass. Data lives in [CountUpStore]; the grid re-queries
 * it through [CountUpWidgetService] whenever the launcher is told the collection
 * changed ([AppWidgetManager.notifyAppWidgetViewDataChanged]).
 *
 * Interaction parity with the previous Glance widget:
 *  - tap the widget background -> open the app
 *  - tap a cell -> reset that one item's anchor to today
 *  - tap the right-edge refresh button -> re-render all placements
 */
class CountUpWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        pushWidgetUpdate(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_REFRESH) {
            pushWidgetUpdate(context)
            CountUpStore(context).markWidgetRefreshed(LocalDate.now().toEpochDay())
            return
        }
        super.onReceive(context, intent)
    }

    companion object {
        /** Broadcast action sent by the widget's refresh button. */
        const val ACTION_REFRESH = "com.ivanyang.countup.action.WIDGET_REFRESH"
    }
}

/** Pushes a fresh base RemoteViews to every placed widget and re-queries the grid. */
fun pushWidgetUpdate(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val ids = manager.getAppWidgetIds(ComponentName(context, CountUpWidgetReceiver::class.java))
    if (ids.isEmpty()) return
    manager.updateAppWidget(ids, buildBaseViews(context))
    manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_grid)
}

/** Builds the widget frame: background, launch/reset/refresh intents, empty view. */
private fun buildBaseViews(context: Context): RemoteViews {
    val night = isNightMode(context)
    val views = RemoteViews(context.packageName, R.layout.countup_widget)

    // Zen paper background, matching the app theme.
    views.setInt(R.id.widget_root, "setBackgroundColor", if (night) NIGHT_PAPER else PAPER)

    // Tap anywhere outside a cell (and on the empty state) -> open the app.
    val launch = PendingIntent.getActivity(
        context,
        REQUEST_LAUNCH,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    views.setOnClickPendingIntent(R.id.widget_root, launch)
    views.setOnClickPendingIntent(R.id.widget_empty, launch)
    views.setTextColor(R.id.widget_empty, if (night) NIGHT_MUTED else MUTED)

    // Right-edge refresh button: broadcast straight to the provider.
    val refresh = PendingIntent.getBroadcast(
        context,
        REQUEST_REFRESH,
        Intent(context, CountUpWidgetReceiver::class.java).setAction(CountUpWidgetReceiver.ACTION_REFRESH),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    views.setOnClickPendingIntent(R.id.widget_refresh, refresh)
    views.setImageViewResource(
        R.id.widget_refresh,
        if (night) R.drawable.ic_refresh_dark else R.drawable.ic_refresh,
    )

    // Cell taps: template broadcast to the reset receiver; each cell fills in
    // the tapped item's id (see the factory's setOnClickFillInIntent).
    // FLAG_MUTABLE is REQUIRED: the launcher merges the fill-in intent (item id)
    // into this template at tap time; an immutable PendingIntent silently drops
    // the extras and every tap resets nothing.
    val reset = PendingIntent.getBroadcast(
        context,
        REQUEST_RESET,
        Intent(context, ResetCountReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
    )
    views.setPendingIntentTemplate(R.id.widget_grid, reset)

    // Bind the collection and designate the empty view the launcher shows when
    // there is nothing to list.
    views.setRemoteAdapter(R.id.widget_grid, Intent(context, CountUpWidgetService::class.java))
    views.setEmptyView(R.id.widget_grid, R.id.widget_empty)

    return views
}

/** Resets one item's anchor date to today directly from the widget. */
class ResetCountReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_ITEM_ID) ?: return
        val store = CountUpStore(context)
        if (store.resetTo(id, LocalDate.now().toEpochDay())) {
            // Push synchronously from the app process: the launcher redraws
            // immediately, no composition pipeline in between.
            pushWidgetUpdate(context)
            // Keep the app's resume-time refresh gate accurate for today.
            store.markWidgetRefreshed(LocalDate.now().toEpochDay())
        }
    }

    companion object {
        const val EXTRA_ITEM_ID = "com.ivanyang.countup.extra.ITEM_ID"
    }
}

/** Serves the grid's cell views from [CountUpStore] inside the launcher's process. */
class CountUpWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = WidgetViewsFactory(applicationContext)
}

/** A single rendered cell of widget data, derived from a store item. */
internal data class WidgetRowData(
    val id: String,
    val name: String,
    val count: Long,
)

/** Pure derivation shared by the widget (and unit-tested on the JVM). */
internal fun widgetRows(items: List<CountUpItem>, today: LocalDate): List<WidgetRowData> =
    items.map { item ->
        WidgetRowData(id = item.id, name = item.name, count = daysSince(LocalDate.ofEpochDay(item.epochDay), today))
    }

private class WidgetViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private var rows: List<WidgetRowData> = emptyList()
    private var night = false

    override fun onCreate() {
        night = isNightMode(context)
    }

    override fun onDataSetChanged() {
        // Re-read the store: triggered by pushWidgetUpdate's
        // notifyAppWidgetViewDataChanged after every app-side write.
        night = isNightMode(context)
        rows = widgetRows(CountUpStore(context).items(), LocalDate.now())
    }

    override fun onDestroy() {
        rows = emptyList()
    }

    override fun getCount(): Int = rows.size

    override fun getViewAt(position: Int): RemoteViews {
        val row = rows[position]
        val views = RemoteViews(context.packageName, R.layout.countup_widget_cell)

        views.setTextViewText(R.id.cell_name, row.name)
        views.setTextColor(R.id.cell_name, if (night) NIGHT_MUTED else MUTED)
        views.setTextViewText(R.id.cell_count, row.count.toString())
        // Dark ink on the light circle by day; light warm on the dark circle at
        // night so the number always stands out from the plate and the paper.
        views.setTextColor(R.id.cell_count, if (night) NIGHT_NUMBER else NUMBER)
        views.setImageViewResource(
            R.id.cell_circle,
            if (night) R.drawable.ic_solid_circle_dark else R.drawable.ic_solid_circle,
        )

        // Tapping this cell resets ONLY this item to today; the item id rides
        // the fill-in intent onto the grid's template PendingIntent.
        views.setOnClickFillInIntent(
            R.id.cell_root,
            Intent().putExtra(ResetCountReceiver.EXTRA_ITEM_ID, row.id),
        )

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = rows[position].id.hashCode().toLong()

    override fun hasStableIds(): Boolean = true
}

/** Whether the device is in dark (night) mode. */
private fun isNightMode(context: Context): Boolean =
    (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

// Zen-paper palette (mirrors MainActivity's named constants).
private val PAPER: Int = 0xFFF7F6F3.toInt()
private val NIGHT_PAPER: Int = 0xFF242422.toInt()
private val MUTED: Int = 0xFF787774.toInt()
private val NIGHT_MUTED: Int = 0xFFA8A8A3.toInt()
private val NUMBER: Int = 0xFF2F3437.toInt()
private val NIGHT_NUMBER: Int = 0xFFF2EEE4.toInt()

private const val REQUEST_LAUNCH = 1
private const val REQUEST_REFRESH = 2
private const val REQUEST_RESET = 3

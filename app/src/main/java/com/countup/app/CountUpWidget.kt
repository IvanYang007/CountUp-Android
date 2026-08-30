package com.countup.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.os.SystemClock
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import android.widget.Toast
import java.time.LocalDate

/**
 * Classic RemoteViews home-screen widget (the "Gmail method"): the app pushes
 * new RemoteViews imperatively via [pushWidgetUpdate] after every store write,
 * so a data change lands on the launcher immediately instead of waiting for a
 * Glance re-composition pass. Data lives in [CountUpStore]; the grid re-queries
 * it through [CountUpWidgetService] whenever the launcher is told the collection
 * changed ([AppWidgetManager.notifyAppWidgetViewDataChanged]).
 *
 * Interaction:
 *  - tap the widget background -> open the app
 *  - tap a cell -> double-tap in-place confirmation to reset that item to today
 */
class CountUpWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        pushWidgetUpdate(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle?,
    ) {
        pushWidgetUpdate(context)
    }
}

/** Pushes a fresh base RemoteViews to every placed widget and re-queries the grid. */
@Suppress("DEPRECATION")
fun pushWidgetUpdate(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val ids = manager.getAppWidgetIds(ComponentName(context, CountUpWidgetReceiver::class.java))
    if (ids.isEmpty()) return
    for (id in ids) {
        val options = manager.getAppWidgetOptions(id)
        manager.updateAppWidget(id, buildBaseViews(context, options))
    }
    manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_grid)
}

/** Builds the widget frame: dynamic ink background, title, launch/reset intents, empty view. */
@Suppress("DEPRECATION")
internal fun buildBaseViews(context: Context, appWidgetOptions: android.os.Bundle? = null): RemoteViews {
    val night = isNightMode(context)
    val views = RemoteViews(context.packageName, R.layout.countup_widget)

    // Base paper backdrop on root ensures seamless blending on any aspect ratio
    val paperColor = if (night) NIGHT_PAPER else PAPER
    views.setInt(R.id.widget_root, "setBackgroundColor", paperColor)

    // Calculate row count and dynamic height so background graphics scale with widget rows
    val store = CountUpStore(context)
    val theme = store.getBackgroundTheme()
    val widgetItems = store.items().filter { it.showInWidget }
    val itemCount = widgetItems.size
    val rowCount = ((itemCount + 2) / 3).coerceIn(1, 5)

    // Compute target canvas height from launcher options or row count
    val optionsHeightDp = appWidgetOptions?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0) ?: 0
    val density = context.resources.displayMetrics.density
    val targetHeight = if (optionsHeightDp > 100) {
        (optionsHeightDp * density).toInt().coerceIn(180, 800)
    } else {
        when (rowCount) {
            1 -> 200
            2 -> 280
            3 -> 400
            4 -> 520
            else -> 280 + (rowCount - 2) * 120
        }
    }

    val bgBitmap = WidgetBackgroundRenderer.render(
        theme = theme,
        width = 480,
        height = targetHeight,
        isNight = night,
    )
    if (bgBitmap != null) {
        views.setImageViewBitmap(R.id.widget_bg_image, bgBitmap)
    }

    // Minimal title in muted ink typography
    views.setTextColor(R.id.widget_title, if (night) NIGHT_MUTED else MUTED)
    views.setInt(R.id.widget_divider, "setBackgroundColor", if (night) NIGHT_DIVIDER else DIVIDER)

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

    // Cell taps: template broadcast to the reset receiver; each cell fills in
    // the tapped item's id (see the factory's setOnClickFillInIntent).
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

/** Resets one item's anchor date to today via a double-tap in-place confirmation. */
class ResetCountReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_ITEM_ID) ?: return
        val store = CountUpStore(context)
        val item = store.items().firstOrNull { it.id == id } ?: return

        if (isArmed(id)) {
            // Second tap confirmed: disarm and reset to today
            disarm()
            val today = LocalDate.now().toEpochDay()
            if (store.resetTo(id, today)) {
                // Push synchronously from the app process: the launcher redraws
                // immediately, no composition pipeline in between.
                pushWidgetUpdate(context)
                // In-place instant feedback: toast notification if supported by launcher
                Toast.makeText(
                    context.applicationContext,
                    context.getString(R.string.widget_reset_toast, item.name),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        } else {
            // First tap: arm this cell and re-render widget to show "Tap again" / "0?"
            arm(id)
            pushWidgetUpdate(context)
        }
    }

    companion object {
        const val EXTRA_ITEM_ID = "com.countup.app.extra.ITEM_ID"
        private const val ARMED_TIMEOUT_MS = 4000L

        @Volatile
        private var armedItemId: String? = null
        @Volatile
        private var armedTimestamp: Long = 0L

        internal var clock: () -> Long = { SystemClock.elapsedRealtime() }

        fun isArmed(id: String): Boolean {
            val now = try { clock() } catch (_: Exception) { System.currentTimeMillis() }
            return armedItemId == id && (now - armedTimestamp) < ARMED_TIMEOUT_MS
        }

        fun arm(id: String) {
            armedItemId = id
            armedTimestamp = try { clock() } catch (_: Exception) { System.currentTimeMillis() }
        }

        fun disarm() {
            armedItemId = null
            armedTimestamp = 0L
        }
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
    val futureFlag: Boolean,
)

/** Pure derivation shared by the widget (and unit-tested on the JVM). */
internal fun widgetRows(items: List<CountUpItem>, today: LocalDate): List<WidgetRowData> =
    items.filter { it.showInWidget }.map { item ->
        WidgetRowData(
            id = item.id,
            name = item.name,
            count = daysSince(LocalDate.ofEpochDay(item.epochDay), today),
            futureFlag = item.futureFlag,
        )
    }

/** True when an item was created/updated with a future date that has now arrived. */
internal fun arrivedFuture(row: WidgetRowData): Boolean = row.futureFlag && row.count >= 0

/**
 * Count label for the widget cell. RemoteViews has no typeface API, so bold is
 * applied as a character style on the text itself.
 */
internal fun widgetCountText(count: Long, arrived: Boolean): CharSequence =
    if (arrived) {
        val s = SpannableString(count.toString())
        s.setSpan(StyleSpan(Typeface.BOLD), 0, s.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        s
    } else {
        count.toString()
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
        val store = CountUpStore(context)
        val today = LocalDate.now()
        val sorted = sortItems(store.items(), store.getSortOrder(), today)
        rows = widgetRows(sorted, today)
    }

    override fun onDestroy() {
        rows = emptyList()
    }

    override fun getCount(): Int = rows.size

    override fun getViewAt(position: Int): RemoteViews {
        val row = rows[position]
        val views = RemoteViews(context.packageName, R.layout.countup_widget_cell)

        val armed = ResetCountReceiver.isArmed(row.id)
        if (armed) {
            // Confirmation state: visual double-tap prompt directly on the widget
            views.setTextViewText(R.id.cell_name, context.getString(R.string.widget_reset_prompt))
            views.setTextColor(R.id.cell_name, if (night) NIGHT_MUTED else MUTED)
            views.setTextViewText(R.id.cell_count, "0?")
            views.setTextColor(R.id.cell_count, Color.WHITE)
            views.setImageViewResource(R.id.cell_circle, R.drawable.ic_circle_orange)
        } else {
            views.setTextViewText(R.id.cell_name, row.name)
            views.setTextColor(R.id.cell_name, if (night) NIGHT_MUTED else MUTED)
            val arrived = arrivedFuture(row)
            views.setTextViewText(R.id.cell_count, widgetCountText(row.count, arrived))
            if (arrived) {
                // A future-dated item whose date has now arrived: solid green plate,
                // dark-red bold count, on both night and day plates.
                views.setTextColor(R.id.cell_count, ARRIVED_NUMBER)
                views.setImageViewResource(R.id.cell_circle, R.drawable.ic_circle_green)
            } else if (night) {
                // Dark ink on the warm dark plate at night, so the number stands out.
                views.setTextColor(R.id.cell_count, NIGHT_NUMBER)
                views.setImageViewResource(R.id.cell_circle, R.drawable.ic_solid_circle_dark)
            } else {
                // Day plates rotate through matte MCM accents (olive, orange, mustard);
                // the number ink flips to deep brown on the light mustard plate.
                val slot = position % DAY_CIRCLES.size
                views.setTextColor(R.id.cell_count, DAY_NUMBER_INKS[slot])
                views.setImageViewResource(R.id.cell_circle, DAY_CIRCLES[slot])
            }
        }

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
/** Light palette only (design decision): dark text mode is removed, so this is false. */
private fun isNightMode(context: Context): Boolean = false

// Mid-century-modern palette (mirrors MainActivity's named constants).
private val PAPER: Int = 0xFFF5E6D3.toInt()
private val NIGHT_PAPER: Int = 0xFF241D12.toInt()
private val MUTED: Int = 0xFF6B5D4F.toInt()
private val NIGHT_MUTED: Int = 0xFFC4B291.toInt()
private val NUMBER: Int = 0xFF2C2416.toInt()
private val NIGHT_NUMBER: Int = 0xFFF5E6D3.toInt()
private val WHITE: Int = 0xFFFFFFFF.toInt()
private val DIVIDER: Int = 0x66E3D3B8
private val NIGHT_DIVIDER: Int = 0x40D9C6A6

// Day-mode count plates rotate through matte MCM accents; number ink follows contrast.
private val DAY_CIRCLES = intArrayOf(
    R.drawable.ic_circle_olive,
    R.drawable.ic_circle_orange,
    R.drawable.ic_circle_mustard,
)
private val DAY_NUMBER_INKS = intArrayOf(WHITE, WHITE, NUMBER)

// Arrived-future styling: solid green plate with a dark red bold count.
private val ARRIVED_NUMBER: Int = 0xFFB71C1C.toInt()

private const val REQUEST_LAUNCH = 1
private const val REQUEST_RESET = 2

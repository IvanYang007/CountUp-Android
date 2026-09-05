package com.countup.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

internal val widgetReceiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

/**
 * Safely executes asynchronous work in a [BroadcastReceiver] on [widgetReceiverScope],
 * ensuring that [goAsync] / [BroadcastReceiver.PendingResult.finish] are properly paired.
 */
internal inline fun BroadcastReceiver.launchAsync(
    scope: CoroutineScope = widgetReceiverScope,
    crossinline block: suspend () -> Unit,
) {
    val pendingResult = try { goAsync() } catch (_: Exception) { null }
    scope.launch {
        try {
            block()
        } finally {
            pendingResult?.finish()
        }
    }
}

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
        val appContext = context.applicationContext
        launchAsync {
            pushWidgetUpdate(appContext)
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
            pushWidgetUpdate(appContext)
        }
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

/**
 * Calculates bounded canvas dimensions (width x height in px) for the widget background bitmap.
 * Keeps the uncompressed ARGB_8888 bitmap parcel <= 375 KB, well below Android's 1MB
 * Binder IPC transaction buffer limit (preventing TransactionTooLargeException).
 */
internal fun computeWidgetCanvasDimensions(optionsHeightDp: Int, rowCount: Int): Pair<Int, Int> {
    val targetWidth = 360
    val targetHeight = when {
        optionsHeightDp > 100 -> (optionsHeightDp * 0.6f).toInt().coerceIn(140, 260)
        rowCount == 1 -> 160
        rowCount == 2 -> 200
        else -> 240
    }
    return Pair(targetWidth, targetHeight)
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

    // Compute bounded canvas dimensions to keep bitmap parcel well within Android's 1MB Binder IPC limit
    val optionsHeightDp = appWidgetOptions?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0) ?: 0
    val (targetWidth, targetHeight) = computeWidgetCanvasDimensions(optionsHeightDp, rowCount)

    val bgBitmap = WidgetBackgroundRenderer.render(
        theme = theme,
        width = targetWidth,
        height = targetHeight,
        isNight = night,
    )
    if (bgBitmap != null) {
        views.setImageViewBitmap(R.id.widget_bg_image, bgBitmap)
    }

    // Minimal title & '+' quick add button in muted ink typography
    views.setTextColor(R.id.widget_title, if (night) NIGHT_MUTED else MUTED)
    views.setTextColor(R.id.widget_add_button, if (night) NIGHT_MUTED else MUTED)
    views.setInt(R.id.widget_divider, "setBackgroundColor", if (night) NIGHT_DIVIDER else DIVIDER)

    // Tap title or background -> open the app.
    val launch = PendingIntent.getActivity(
        context,
        REQUEST_LAUNCH,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    views.setOnClickPendingIntent(R.id.widget_root, launch)
    views.setOnClickPendingIntent(R.id.widget_title, launch)

    // Tap '+' quick add action or empty state -> open the app straight into Add Item dialog.
    val addIntent = Intent(context, MainActivity::class.java).apply {
        action = ACTION_ADD_ITEM
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val addPendingIntent = PendingIntent.getActivity(
        context,
        REQUEST_ADD,
        addIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    views.setOnClickPendingIntent(R.id.widget_add_button, addPendingIntent)
    views.setOnClickPendingIntent(R.id.widget_empty, addPendingIntent)
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
        val appContext = context.applicationContext

        launchAsync {
            val store = CountUpStore(appContext)
            val item = store.items().firstOrNull { it.id == id } ?: return@launchAsync

            val todayLocalDate = LocalDate.now()
            val today = todayLocalDate.toEpochDay()
            val releasedDays = daysSince(LocalDate.ofEpochDay(item.epochDay), todayLocalDate)
            if (!item.isResettableOn(todayLocalDate)) {
                // Stop trigger reset when the accumulate date is already 0
                return@launchAsync
            }

            if (isArmed(id)) {
                // Second tap confirmed: disarm and reset to today
                disarm()
                val record = WidgetResetRecord(
                    itemId = item.id,
                    itemName = item.name,
                    snapshot = item.toResetSnapshot(),
                    releasedDays = kotlin.math.abs(releasedDays),
                    timestampMillis = System.currentTimeMillis(),
                )
                if (store.resetTo(id, today)) {
                    store.recordWidgetReset(record)
                    pushWidgetUpdate(appContext)
                    pushAllHeroWidgetsUpdate(appContext)
                }
            } else {
                // First tap: arm this cell and re-render widget to show "Tap again" / "0?"
                arm(appContext, id)
                pushWidgetUpdate(appContext)
                pushAllHeroWidgetsUpdate(appContext)
            }
        }
    }

    companion object {
        const val EXTRA_ITEM_ID = "com.countup.app.extra.ITEM_ID"
        private const val ARMED_TIMEOUT_MS = 1500L

        @Volatile
        private var armedItemId: String? = null
        @Volatile
        private var armedTimestamp: Long = 0L

        private val mainHandler by lazy {
            try {
                android.os.Handler(android.os.Looper.getMainLooper())
            } catch (_: Exception) {
                null
            }
        }
        private var disarmRunnable: Runnable? = null

        internal var clock: () -> Long = { SystemClock.elapsedRealtime() }

        fun isArmed(id: String): Boolean {
            val now = try { clock() } catch (_: Exception) { System.currentTimeMillis() }
            return armedItemId == id && (now - armedTimestamp) < ARMED_TIMEOUT_MS
        }

        fun arm(id: String) {
            arm(context = null, id = id)
        }

        fun arm(context: Context?, id: String) {
            armedItemId = id
            armedTimestamp = try { clock() } catch (_: Exception) { System.currentTimeMillis() }
            disarmRunnable?.let { mainHandler?.removeCallbacks(it) }

            if (context != null) {
                val appContext = context.applicationContext
                val runnable = Runnable {
                    if (armedItemId == id) {
                        disarm()
                        widgetReceiverScope.launch {
                            pushWidgetUpdate(appContext)
                            pushAllHeroWidgetsUpdate(appContext)
                        }
                    }
                }
                disarmRunnable = runnable
                mainHandler?.postDelayed(runnable, ARMED_TIMEOUT_MS)
            }
        }

        fun disarm() {
            disarmRunnable?.let { mainHandler?.removeCallbacks(it) }
            disarmRunnable = null
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
    val icon: String = DEFAULT_ICON,
    val cardColor: String = "",
    val resetCount: Int = 0,
)

/** Pure derivation shared by the widget (and unit-tested on the JVM). */
internal fun widgetRows(items: List<CountUpItem>, today: LocalDate): List<WidgetRowData> =
    items.filter { it.showInWidget }.map { item ->
        WidgetRowData(
            id = item.id,
            name = item.name,
            count = daysSince(LocalDate.ofEpochDay(item.epochDay), today),
            futureFlag = item.futureFlag,
            icon = item.icon,
            cardColor = item.cardColor,
            resetCount = item.resetCount,
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

internal class WidgetViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

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

        val textInk = if (night) NIGHT_MUTED else MUTED
        val armed = ResetCountReceiver.isArmed(row.id)

        // Set Phosphor icon and tint to text ink
        val iconDrawableId = iconRes(row.icon.ifEmpty { DEFAULT_ICON })
        views.setImageViewResource(R.id.cell_icon, iconDrawableId)
        views.setInt(R.id.cell_icon, "setColorFilter", textInk)

        if (armed) {
            // Confirmation state: visual double-tap prompt directly on the widget
            views.setTextViewText(R.id.cell_name, context.getString(R.string.widget_reset_prompt))
            views.setTextColor(R.id.cell_name, textInk)
            views.setViewVisibility(R.id.cell_reset_count, View.GONE)
            views.setTextViewText(R.id.cell_count, "0?")
            views.setTextColor(R.id.cell_count, WHITE)
            views.setImageViewResource(R.id.cell_circle, R.drawable.ic_circle_orange)
            views.setInt(R.id.cell_circle, "setColorFilter", 0)
        } else {
            views.setTextViewText(R.id.cell_name, row.name)
            views.setTextColor(R.id.cell_name, textInk)

            if (row.resetCount > 0) {
                views.setViewVisibility(R.id.cell_reset_count, View.VISIBLE)
                views.setTextViewText(R.id.cell_reset_count, context.getString(R.string.widget_reset_count_badge, row.resetCount))
                views.setTextColor(R.id.cell_reset_count, if (night) WIDGET_RESET_BADGE_NIGHT else WIDGET_RESET_BADGE_DAY)
            } else {
                views.setViewVisibility(R.id.cell_reset_count, View.GONE)
            }

            val arrived = arrivedFuture(row)
            views.setTextViewText(R.id.cell_count, widgetCountText(row.count, arrived))
            if (arrived) {
                // A future-dated item whose date has now arrived: solid green plate,
                // dark-red bold count, on both night and day plates.
                views.setTextColor(R.id.cell_count, ARRIVED_NUMBER)
                views.setImageViewResource(R.id.cell_circle, R.drawable.ic_circle_green)
                views.setInt(R.id.cell_circle, "setColorFilter", 0)
            } else {
                val circleStyle = resolveWidgetCircleStyle(row, position)
                views.setTextColor(R.id.cell_count, circleStyle.textInk)
                views.setImageViewResource(R.id.cell_circle, R.drawable.ic_circle_olive)
                views.setInt(R.id.cell_circle, "setColorFilter", circleStyle.circleColor)
            }
        }

        // Tapping the count circle resets ONLY this item to today; the item id rides
        // the fill-in intent onto the grid's template PendingIntent.
        views.setOnClickFillInIntent(
            R.id.cell_count_container,
            Intent().putExtra(ResetCountReceiver.EXTRA_ITEM_ID, row.id),
        )
        views.setOnClickFillInIntent(
            R.id.cell_count,
            Intent().putExtra(ResetCountReceiver.EXTRA_ITEM_ID, row.id),
        )

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = rows[position].id.hashCode().toLong()

    override fun hasStableIds(): Boolean = true
}

/** Styling parameters for an individual widget cell count circle. */
internal data class WidgetCircleStyle(
    val circleColor: Int,
    val textInk: Int,
)

/**
 * Curated Zen & MCM palette of circle badge colors for widget cells.
 * Even when card surface is defaulted to Paper White, widget circles cycle through
 * these rich, harmonious colors to ensure visual variety across home screen cells.
 */
internal val DEFAULT_WIDGET_PALETTE = listOf(
    WidgetCircleStyle(circleColor = 0xFFDEB285.toInt(), textInk = 0xFF2C2416.toInt()), // Ochre Gold (沉金)
    WidgetCircleStyle(circleColor = 0xFF5E8C6D.toInt(), textInk = 0xFFFFFFFF.toInt()), // Willow Sage
    WidgetCircleStyle(circleColor = 0xFFD87A4F.toInt(), textInk = 0xFFFFFFFF.toInt()), // Warm Terracotta
    WidgetCircleStyle(circleColor = 0xFF344C5C.toInt(), textInk = 0xFFFFFFFF.toInt()), // Lapis Indigo
    WidgetCircleStyle(circleColor = 0xFFC45249.toInt(), textInk = 0xFFFFFFFF.toInt()), // Japanese Vermilion
    WidgetCircleStyle(circleColor = 0xFF33523D.toInt(), textInk = 0xFFFFFFFF.toInt()), // Deep Forest
    WidgetCircleStyle(circleColor = 0xFF4D7A58.toInt(), textInk = 0xFFFFFFFF.toInt()), // Jade Green
)

/**
 * Resolves the circle badge color and high-contrast text ink for a widget cell.
 */
internal fun resolveWidgetCircleStyle(row: WidgetRowData, position: Int): WidgetCircleStyle {
    if (row.cardColor.isNotBlank() && row.cardColor != DEFAULT_CARD_COLOR) {
        val customPreset = resolveCardStyle(row.cardColor)
        val badgeColor = customPreset.badgeBg
        val colorInt = badgeColor.toArgb()
        val textInk = if (badgeColor.luminance() < 0.40f) 0xFFFFFFFF.toInt() else 0xFF2C2416.toInt()
        return WidgetCircleStyle(circleColor = colorInt, textInk = textInk)
    }
    // Defaulted card: provide distinct, deterministic, varied colors from the curated palette
    val hash = if (row.id.isNotEmpty()) kotlin.math.abs(row.id.hashCode()) else position
    val index = (hash + position) % DEFAULT_WIDGET_PALETTE.size
    return DEFAULT_WIDGET_PALETTE[index]
}

/** Whether the device is in dark (night) mode. */
/** Light palette only (design decision): dark text mode is removed, so this is false. */
private fun isNightMode(context: Context): Boolean = false

// Mid-century-modern palette (mirrors MainActivity's named constants).
private val PAPER: Int = 0xFFF5E6D3.toInt()
private val NIGHT_PAPER: Int = 0xFF241D12.toInt()
private val MUTED: Int = 0xFF6B5D4F.toInt()
private val NIGHT_MUTED: Int = 0xFFC4B291.toInt()
private val WHITE: Int = 0xFFFFFFFF.toInt()
private val DIVIDER: Int = 0x66E3D3B8
private val NIGHT_DIVIDER: Int = 0x40D9C6A6

// Reset badge gold palette (mirrors @color/widget_reset_count_text)
internal val WIDGET_RESET_BADGE_DAY: Int = 0xFF785D2A.toInt()
internal val WIDGET_RESET_BADGE_NIGHT: Int = 0xFFD4B87C.toInt()

// Arrived-future styling: solid green plate with a dark red bold count.
private val ARRIVED_NUMBER: Int = 0xFFB71C1C.toInt()

const val ACTION_ADD_ITEM: String = "com.countup.app.ACTION_ADD_ITEM"

private const val REQUEST_LAUNCH = 1
private const val REQUEST_RESET = 2
private const val REQUEST_ADD = 3

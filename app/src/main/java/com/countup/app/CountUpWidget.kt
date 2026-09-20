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

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_CYCLE_OVERVIEW_FILTER) {
            val appWidgetId = intent.getOverviewWidgetId()
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                val appContext = context.applicationContext
                launchAsync {
                    val store = CountUpStore.getInstance(appContext)
                    val currentFilter = store.getOverviewWidgetFilter(appWidgetId)
                    val nextFilter = nextOverviewStyleFilter(currentFilter)
                    store.setOverviewWidgetFilter(appWidgetId, nextFilter)
                    pushWidgetUpdate(appContext, appWidgetId)
                }
            }
            return
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val appContext = context.applicationContext
        MidnightAlarmReceiver.scheduleMidnightAlarm(appContext)
        launchAsync {
            pushWidgetUpdate(appContext)
        }
    }

    override fun onEnabled(context: Context) {
        MidnightAlarmReceiver.scheduleMidnightAlarm(context.applicationContext)
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

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val appContext = context.applicationContext
        launchAsync {
            val store = CountUpStore.getInstance(appContext)
            for (id in appWidgetIds) {
                store.removeOverviewWidgetFilter(id)
            }
        }
    }

    override fun onRestored(context: Context, oldWidgetIds: IntArray, newWidgetIds: IntArray) {
        val appContext = context.applicationContext
        CountUpStore.getInstance(appContext).remapWidgetBindings(oldWidgetIds, newWidgetIds)
        MidnightAlarmReceiver.scheduleMidnightAlarm(appContext)
        launchAsync {
            pushWidgetUpdate(appContext)
        }
    }
}

/** Pushes an update to a specific 4x2 Overview widget instance. */
@Suppress("DEPRECATION")
fun pushWidgetUpdate(context: Context, appWidgetId: Int) {
    val manager = AppWidgetManager.getInstance(context)
    val options = manager.getAppWidgetOptions(appWidgetId)
    manager.updateAppWidget(appWidgetId, buildBaseViews(context, appWidgetId, options))
    manager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_grid)
}

/** Pushes a fresh base RemoteViews to every placed widget and re-queries the grid. */
@Suppress("DEPRECATION")
fun pushWidgetUpdate(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val ids = manager.getAppWidgetIds(ComponentName(context, CountUpWidgetReceiver::class.java))
    if (ids.isEmpty()) return
    for (id in ids) {
        val options = manager.getAppWidgetOptions(id)
        manager.updateAppWidget(id, buildBaseViews(context, id, options))
    }
    manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_grid)
}

/**
 * Imperatively updates all 5 widget families and synchronizes the midnight alarm.
 */
internal suspend fun updateAllWidgets(context: Context) {
    val appContext = context.applicationContext
    runCatching { pushWidgetUpdate(appContext) }
    runCatching { pushAllHeroWidgetsUpdate(appContext) }
    runCatching { pushAllZenHorizonWidgetsUpdate(appContext) }
    runCatching { pushAllSolarRhythmWidgetsUpdate(appContext) }
    runCatching { pushAllZenPebbleWidgetsUpdate(appContext) }
    runCatching { MidnightAlarmReceiver.scheduleMidnightAlarm(appContext) }
}

/**
 * Imperatively launches an update across all 5 widget families and synchronizes the midnight alarm.
 * Safe to call from any background coroutine or thread.
 */
fun pushAllWidgetsUpdate(context: Context) {
    val appContext = context.applicationContext
    widgetReceiverScope.launch {
        updateAllWidgets(appContext)
    }
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
internal fun buildBaseViews(
    context: Context,
    appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID,
    appWidgetOptions: android.os.Bundle? = null,
): RemoteViews {
    val night = isNightMode(context)
    val views = RemoteViews(context.packageName, R.layout.countup_widget)

    // Resolve per-instance style filter
    val store = CountUpStore.getInstance(context)
    val filter = if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
        store.getOverviewWidgetFilter(appWidgetId)
    } else {
        CountUpStore.OVERVIEW_FILTER_ALL
    }

    // Base paper backdrop on root ensures seamless blending on any aspect ratio
    val paperColor = resolveOverviewPaperColor(filter, night)
    views.setInt(R.id.widget_root, "setBackgroundColor", paperColor)

    // Calculate row count and dynamic height so background graphics scale with filtered widget rows
    val theme = store.getBackgroundTheme()
    val widgetItems = store.items().filter { it.showInWidget && it.matchesStyleFilter(filter) }
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
        paperColor = paperColor,
    )
    if (bgBitmap != null) {
        views.setImageViewBitmap(R.id.widget_bg_image, bgBitmap)
    }

    // Header styling & title: typographic compound "CountUp · All", "CountUp · Washi", etc.
    val baseTitle = context.getString(R.string.app_name)
    val compoundTitle = resolveOverviewCompoundTitle(context, filter)
    val span = SpannableString(compoundTitle)
    span.setSpan(StyleSpan(Typeface.BOLD), 0, baseTitle.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    span.setSpan(StyleSpan(Typeface.NORMAL), baseTitle.length, compoundTitle.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    views.setTextViewText(R.id.widget_title, span)

    val mutedInk = resolveOverviewMutedInk(filter, night)
    val dividerColor = resolveOverviewDividerColor(filter, night)
    views.setTextColor(R.id.widget_title, mutedInk)
    views.setInt(R.id.widget_filter_dot, "setColorFilter", resolveOverviewDotColor(filter, night))
    views.setContentDescription(R.id.widget_title_container, "$compoundTitle. Tap to cycle style.")

    views.setInt(R.id.widget_voice_button, "setColorFilter", mutedInk)
    views.setTextColor(R.id.widget_add_button, mutedInk)
    views.setInt(R.id.widget_divider, "setBackgroundColor", dividerColor)

    // Tap background -> open the app.
    val launch = PendingIntent.getActivity(
        context,
        REQUEST_LAUNCH,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    views.setOnClickPendingIntent(R.id.widget_root, launch)

    // Tap title container -> cycle suite filter in place
    if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
        val cycleIntent = Intent(context, CountUpWidgetReceiver::class.java).apply {
            action = ACTION_CYCLE_OVERVIEW_FILTER
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = android.net.Uri.parse("countup://widget/cycle/$appWidgetId")
        }
        val cyclePendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CYCLE_FILTER + appWidgetId,
            cycleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(R.id.widget_title_container, cyclePendingIntent)
        views.setOnClickPendingIntent(R.id.widget_title, cyclePendingIntent)
        views.setOnClickPendingIntent(R.id.widget_filter_dot, cyclePendingIntent)
    } else {
        views.setOnClickPendingIntent(R.id.widget_title_container, launch)
    }

    // Tap voice button -> open VoiceAddActivity
    val voiceIntent = Intent(context, VoiceAddActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val voicePendingIntent = PendingIntent.getActivity(
        context,
        REQUEST_VOICE_ADD,
        voiceIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    views.setOnClickPendingIntent(R.id.widget_voice_button, voicePendingIntent)

    // Tap '+' quick add action or empty state -> open the app straight into Add Item dialog with Intent Continuity.
    val addIntent = Intent(context, MainActivity::class.java).apply {
        action = ACTION_ADD_ITEM
        if (filter != CountUpStore.OVERVIEW_FILTER_ALL) {
            putExtra(EXTRA_INITIAL_CARD_STYLE_CATEGORY, filter)
        }
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val addRequestCode = REQUEST_ADD + (if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) appWidgetId * 10 else 0)
    val addPendingIntent = PendingIntent.getActivity(
        context,
        addRequestCode,
        addIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    views.setOnClickPendingIntent(R.id.widget_add_button, addPendingIntent)
    views.setOnClickPendingIntent(R.id.widget_empty, addPendingIntent)

    val emptyText = when (filter) {
        "washi" -> context.getString(R.string.widget_empty_washi)
        "earth" -> context.getString(R.string.widget_empty_earth)
        "sumi" -> context.getString(R.string.widget_empty_sumi)
        else -> context.getString(R.string.widget_empty)
    }
    views.setTextViewText(R.id.widget_empty, emptyText)
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

    // Bind the collection with unique URI per appWidgetId to prevent RemoteViewsAdapter factory cache collision
    val serviceIntent = Intent(context, CountUpWidgetService::class.java).apply {
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            data = android.net.Uri.parse("countup://widget/overview/$appWidgetId")
        }
    }
    views.setRemoteAdapter(R.id.widget_grid, serviceIntent)
    views.setEmptyView(R.id.widget_grid, R.id.widget_empty)

    return views
}

/** Resets one item's anchor date to today via a double-tap in-place confirmation. */
class ResetCountReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_ITEM_ID) ?: return
        val appContext = context.applicationContext

        launchAsync {
            val store = CountUpStore.getInstance(appContext)
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
                if (store.resetWithUndo(id, today, record)) {
                    pushWidgetUpdate(appContext)
                    pushAllHeroWidgetsUpdate(appContext)
                    pushAllZenHorizonWidgetsUpdate(appContext)
                    pushAllSolarRhythmWidgetsUpdate(appContext)
                    pushAllZenPebbleWidgetsUpdate(appContext)
                }
            } else {
                // First tap: arm this cell and re-render grid widget to show "Tap again" / "0?"
                arm(appContext, id)
                pushWidgetUpdate(appContext)
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
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return WidgetViewsFactory(applicationContext, intent.getOverviewWidgetId())
    }
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
    val isPinned: Boolean = false,
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
            isPinned = item.isPinned,
        )
    }

/** True when an item was created/updated with a future date that has now arrived. */
internal fun arrivedFuture(row: WidgetRowData): Boolean = row.futureFlag && row.count >= 0

/**
 * Count label for the widget cell. RemoteViews has no typeface API, so bold is
 * applied as a character style on the text itself.
 */
private fun widgetCountText(count: Long, arrived: Boolean): CharSequence =
    if (arrived) {
        val s = SpannableString(count.toString())
        s.setSpan(StyleSpan(Typeface.BOLD), 0, s.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        s
    } else {
        count.toString()
    }

internal class WidgetViewsFactory(
    private val context: Context,
    private val appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID,
) : RemoteViewsService.RemoteViewsFactory {

    private var rows: List<WidgetRowData> = emptyList()
    private var night = false
    private var filter: String = CountUpStore.OVERVIEW_FILTER_ALL

    override fun onCreate() {
        night = isNightMode(context)
    }

    override fun onDataSetChanged() {
        // Re-read the store: triggered by pushWidgetUpdate's
        // notifyAppWidgetViewDataChanged after every app-side write.
        night = isNightMode(context)
        val store = CountUpStore.getInstance(context)
        val today = LocalDate.now()
        filter = if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            store.getOverviewWidgetFilter(appWidgetId)
        } else {
            CountUpStore.OVERVIEW_FILTER_ALL
        }
        val filtered = store.items().filter { it.matchesStyleFilter(filter) }
        val sorted = sortItems(filtered, store.getSortOrder(), today)
        rows = widgetRows(sorted, today)
    }

    override fun onDestroy() {
        rows = emptyList()
    }

    override fun getCount(): Int = rows.size

    override fun getViewAt(position: Int): RemoteViews {
        val row = rows[position]
        val views = RemoteViews(context.packageName, R.layout.countup_widget_cell)

        val textInk = resolveOverviewMutedInk(filter, night)
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
            views.setViewVisibility(R.id.cell_pin, View.GONE)
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

            if (row.isPinned) {
                views.setViewVisibility(R.id.cell_pin, View.VISIBLE)
                val pinTint = if (night) WIDGET_RESET_BADGE_NIGHT else WIDGET_RESET_BADGE_DAY
                views.setInt(R.id.cell_pin, "setColorFilter", pinTint)
            } else {
                views.setViewVisibility(R.id.cell_pin, View.GONE)
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
                val circleStyle = resolveWidgetCircleStyle(row, isDark = night)
                views.setTextColor(R.id.cell_count, circleStyle.textInk)
                views.setImageViewResource(R.id.cell_circle, R.drawable.ic_circle_plate)
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
 * Resolves the circle badge color and high-contrast text ink for a widget cell,
 * ensuring 1:1 visual parity with the app card's badge circle in both light and dark modes.
 */
internal fun resolveWidgetCircleStyle(row: WidgetRowData, isDark: Boolean = false): WidgetCircleStyle {
    val preset = resolveCardStyle(row.cardColor, isDark = isDark)
    return WidgetCircleStyle(
        circleColor = preset.badgeBg.toArgb(),
        textInk = preset.badgeTint.toArgb(),
    )
}

/** Whether the widget should render in dark (night) mode based on theme setting and system night mode. */
internal fun isNightMode(context: Context): Boolean {
    val store = CountUpStore.getInstance(context)
    val mode = store.getThemeMode()
    val isSystemNight = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
        android.content.res.Configuration.UI_MODE_NIGHT_YES
    return mode.isDark(isSystemNight)
}

// Mid-century-modern palette (mirrors MainActivity's named constants).
private val PAPER: Int = 0xFFF5E6D3.toInt()
private val NIGHT_PAPER: Int = 0xFF191B17.toInt()
private val MUTED: Int = 0xFF6B5D4F.toInt()
private val NIGHT_MUTED: Int = 0xFFC4BEAE.toInt()
private val WHITE: Int = 0xFFFFFFFF.toInt()
private val DIVIDER: Int = 0x66E3D3B8
private val NIGHT_DIVIDER: Int = 0x403A3D35

// Reset badge gold palette (mirrors @color/widget_reset_count_text)
internal val WIDGET_RESET_BADGE_DAY: Int = 0xFF785D2A.toInt()
internal val WIDGET_RESET_BADGE_NIGHT: Int = 0xFFD4A574.toInt()

// Arrived-future styling: solid green plate with a dark red bold count.
private val ARRIVED_NUMBER: Int = 0xFFB71C1C.toInt()

const val ACTION_ADD_ITEM: String = "com.countup.app.ACTION_ADD_ITEM"
const val EXTRA_INITIAL_CARD_STYLE_CATEGORY: String = "com.countup.app.EXTRA_INITIAL_CARD_STYLE_CATEGORY"
const val ACTION_CYCLE_OVERVIEW_FILTER: String = "com.countup.app.ACTION_CYCLE_OVERVIEW_FILTER"

private const val REQUEST_LAUNCH = 1
private const val REQUEST_RESET = 2
private const val REQUEST_ADD = 3
private const val REQUEST_VOICE_ADD = 4
private const val REQUEST_CYCLE_FILTER = 5

/** Safely resolves the target AppWidget ID from extras or URI data segment. */
internal fun Intent.getOverviewWidgetId(): Int = getIntExtra(
    AppWidgetManager.EXTRA_APPWIDGET_ID,
    data?.lastPathSegment?.toIntOrNull() ?: AppWidgetManager.INVALID_APPWIDGET_ID
)

/**
 * Cycles the overview widget card style filter:
 * All -> washi -> earth -> sumi -> All.
 */
internal fun nextOverviewStyleFilter(currentFilter: String): String = when (currentFilter) {
    CountUpStore.OVERVIEW_FILTER_ALL -> "washi"
    "washi" -> "earth"
    "earth" -> "sumi"
    "sumi" -> CountUpStore.OVERVIEW_FILTER_ALL
    else -> "washi"
}

/**
 * Resolves the signature color of the suite affordance dot for the overview widget header.
 * Washi: Warm Ochre Gold
 * Earth: Willow Bamboo / Sage
 * Sumi: Deep Charcoal / Amber Gold
 * All: Neutral Sand / Pale Linen
 */
internal fun resolveOverviewDotColor(filter: String, night: Boolean): Int = when (filter) {
    "washi" -> if (night) 0xFFE8C5A0.toInt() else 0xFFDEB285.toInt()
    "earth" -> if (night) 0xFF8BC4A2.toInt() else 0xFF6DB88A.toInt()
    "sumi" -> if (night) 0xFFD4A574.toInt() else 0xFF3C3F41.toInt()
    else -> if (night) 0xFFA8A095.toInt() else 0xFF8C8275.toInt()
}

/** Resolves the string resource IDs for the base title and category label. */
internal fun resolveOverviewTitleRes(filter: String): Pair<Int, Int> {
    val activeCategory = CARD_COLOR_CATEGORIES.firstOrNull { it.id == filter }
    val categoryRes = activeCategory?.labelRes ?: R.string.category_all
    return Pair(R.string.app_name, categoryRes)
}

/** Resolves the full compound title (e.g. "CountUp · All", "CountUp · Washi"). */
internal fun resolveOverviewCompoundTitle(context: Context, filter: String): String {
    val (baseRes, categoryRes) = resolveOverviewTitleRes(filter)
    val baseTitle = context.getString(baseRes)
    val categoryName = context.getString(categoryRes)
    return "$baseTitle · $categoryName"
}

/**
 * Resolves the subtle base paper background color for each overview widget card style filter.
 * - All: Classic Xuan paper (#F5E6D3 / #191B17)
 * - Washi: Warm golden mulberry fiber (#F7E3C8 / #201B15)
 * - Earth: Celadon mist stone (#EBECE3 / #161B17)
 * - Sumi: Cool inkstone wash (#ECEBE8 / #141415)
 */
internal fun resolveOverviewPaperColor(filter: String, night: Boolean): Int = when (filter) {
    "washi" -> if (night) 0xFF201B15.toInt() else 0xFFF7E3C8.toInt()
    "earth" -> if (night) 0xFF161B17.toInt() else 0xFFEBECE3.toInt()
    "sumi" -> if (night) 0xFF141415.toInt() else 0xFFECEBE8.toInt()
    else -> if (night) NIGHT_PAPER else PAPER
}

/**
 * Resolves the hairline divider color matching the active paper substrate.
 */
internal fun resolveOverviewDividerColor(filter: String, night: Boolean): Int = when (filter) {
    "washi" -> if (night) 0x4045382D else 0x66DFBE93
    "earth" -> if (night) 0x40333C36 else 0x66CEDBD1
    "sumi" -> if (night) 0x402E3033 else 0x66CBC7C0
    else -> if (night) NIGHT_DIVIDER else DIVIDER
}

/**
 * Resolves the muted text and icon ink color matching the active paper substrate.
 */
internal fun resolveOverviewMutedInk(filter: String, night: Boolean): Int = when (filter) {
    "washi" -> if (night) 0xFFD2C3AA.toInt() else 0xFF68513B.toInt()
    "earth" -> if (night) 0xFFBDC7BE.toInt() else 0xFF546358.toInt()
    "sumi" -> if (night) 0xFFC4C5C8.toInt() else 0xFF505359.toInt()
    else -> if (night) NIGHT_MUTED else MUTED
}




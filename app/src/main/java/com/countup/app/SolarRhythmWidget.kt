package com.countup.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import java.time.LocalDate

/**
 * Procedural vector renderer for the Solar Rhythm seasonal timeline track.
 * Renders an anti-aliased, rounded-cap progress bar with seasonal and hairline palette colors.
 */
object SolarRhythmTrackRenderer {
    const val DEFAULT_WIDTH_PX = 400
    const val DEFAULT_HEIGHT_PX = 16
    const val MAX_BITMAP_BYTES = 32 * 1024

    fun renderTrack(
        progress: Float,
        progressColor: Int,
        trackColor: Int,
        widthPx: Int = DEFAULT_WIDTH_PX,
        heightPx: Int = DEFAULT_HEIGHT_PX,
    ): Bitmap? {
        return try {
            val (safeWidth, safeHeight) = ZenHorizonTrackRenderer.computeSafeDimensions(widthPx, heightPx, MAX_BITMAP_BYTES)
            val bitmap = createBitmap(safeWidth, safeHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val centerY = safeHeight / 2f
            val trackThickness = safeHeight * 0.75f
            val startX = trackThickness / 2f
            val endX = safeWidth - (trackThickness / 2f)
            val trackLength = maxOf(1f, endX - startX)

            ZenHorizonTrackRenderer.drawTrackLine(canvas, startX, endX, centerY, trackThickness, trackColor)

            val clampedProgress = progress.coerceIn(0f, 1f)
            if (clampedProgress > 0f) {
                val currentX = startX + (trackLength * clampedProgress)
                ZenHorizonTrackRenderer.drawTrackLine(canvas, startX, currentX, centerY, trackThickness, progressColor)
            }
            bitmap
        } catch (_: Throwable) {
            null
        }
    }
}

/**
 * The Solar Rhythm (时节同行 · 岁华流转) Widget provider.
 * 4x2 Rich Zen Canvas & 2x2 responsive fallback harmonizing personal count-ups
 * with the on-device 24 Solar Terms calendar and seasonal micro-poetry.
 */
class SolarRhythmWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val appContext = context.applicationContext
        launchAsync {
            for (appWidgetId in appWidgetIds) {
                pushSolarRhythmWidgetUpdate(appContext, appWidgetId)
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
            pushSolarRhythmWidgetUpdate(appContext, appWidgetId)
        }
    }
}

/** Pushes update to all placed Solar Rhythm widgets on the launcher. */
fun pushAllSolarRhythmWidgetsUpdate(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val ids = manager.getAppWidgetIds(ComponentName(context, SolarRhythmWidgetReceiver::class.java))
    if (ids.isEmpty()) return
    for (id in ids) {
        pushSolarRhythmWidgetUpdate(context, id)
    }
}

/** Renders and pushes RemoteViews for a single Solar Rhythm widget. */
fun pushSolarRhythmWidgetUpdate(context: Context, appWidgetId: Int) {
    val manager = AppWidgetManager.getInstance(context)
    val store = CountUpStore(context)
    val items = store.items()
    val boundId = store.getSolarRhythmBinding(appWidgetId)
    val targetItem = resolveWidgetTargetItem(items, boundId)
    val today = LocalDate.now()
    val isDark = isNightMode(context)

    val transition = SolarTermCalendar.getSolarTermTransition(today)
    val display = SolarTermPoetryBridge.resolveWidgetDisplay(transition.currentTerm, isDark)
    val daysCount = if (targetItem != null) {
        daysSince(LocalDate.ofEpochDay(targetItem.epochDay), today)
    } else 0L

    val views4x2 = buildSolarRhythmRemoteViews(
        context = context,
        targetItem = targetItem,
        daysCount = daysCount,
        transition = transition,
        display = display,
        isDark = isDark,
        isCompact = false,
        appWidgetId = appWidgetId,
    )

    val views2x2 = buildSolarRhythmRemoteViews(
        context = context,
        targetItem = targetItem,
        daysCount = daysCount,
        transition = transition,
        display = display,
        isDark = isDark,
        isCompact = true,
        appWidgetId = appWidgetId,
    )

    val finalViews = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        RemoteViews(
            mapOf(
                SizeF(140f, 110f) to views2x2,
                SizeF(260f, 110f) to views4x2,
            )
        )
    } else {
        val options = manager.getAppWidgetOptions(appWidgetId)
        val minWidth = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 260) ?: 260
        if (minWidth < 220) views2x2 else views4x2
    }

    manager.updateAppWidget(appWidgetId, finalViews)
}

/** Builds the RemoteViews hierarchy for a Solar Rhythm widget. */
fun buildSolarRhythmRemoteViews(
    context: Context,
    targetItem: CountUpItem?,
    daysCount: Long,
    transition: SolarTermTransition,
    display: SolarTermWidgetDisplay,
    isDark: Boolean,
    isCompact: Boolean,
    appWidgetId: Int,
): RemoteViews {
    val layoutRes = if (isCompact) R.layout.widget_solar_rhythm_2x2 else R.layout.widget_solar_rhythm_4x2
    val views = RemoteViews(context.packageName, layoutRes)

    val themeTokens = WidgetThemeTokens.resolve(isDark)
    val cardStyle = targetItem?.let { resolveCardStyle(it.cardColor, isDark = isDark) }
    val canvasBg = cardStyle?.cardBg?.toArgb() ?: themeTokens.canvasBg
    val primaryInk = cardStyle?.primaryInk?.toArgb() ?: themeTokens.primaryInk
    val secondaryInk = cardStyle?.mutedInk?.toArgb() ?: themeTokens.secondaryInk
    val seasonalPrimary = display.palette.primaryTint
    val seasonalProgress = display.palette.progressTint
    val hairlineColor = themeTokens.hairline

    // Base background color
    views.setInt(R.id.solar_rhythm_root, "setBackgroundColor", canvasBg)

    // Empty state handling
    if (targetItem == null) {
        views.setViewVisibility(R.id.solar_rhythm_content, View.GONE)
        views.setViewVisibility(R.id.solar_rhythm_empty, View.VISIBLE)

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(R.id.solar_rhythm_root, pendingIntent)
        return views
    }

    views.setViewVisibility(R.id.solar_rhythm_content, View.VISIBLE)
    views.setViewVisibility(R.id.solar_rhythm_empty, View.GONE)

    // Badge
    views.setTextViewText(R.id.solar_rhythm_badge_text, context.getString(display.nameRes))
    views.setTextColor(R.id.solar_rhythm_badge_text, display.palette.badgeTextColor)
    views.setInt(R.id.solar_rhythm_badge_container, "setBackgroundColor", display.palette.badgeBg)

    // Numeral
    views.setTextViewText(R.id.solar_rhythm_days_count, daysCount.toString())
    views.setTextColor(R.id.solar_rhythm_days_count, seasonalPrimary)

    // Timeline Track Bitmap
    val trackWidth = if (isCompact) 200 else 400
    val trackBitmap = SolarRhythmTrackRenderer.renderTrack(
        progress = transition.progressFraction,
        progressColor = seasonalProgress,
        trackColor = hairlineColor,
        widthPx = trackWidth,
        heightPx = 14,
    )
    if (trackBitmap != null) {
        views.setImageViewBitmap(R.id.solar_rhythm_timeline_track, trackBitmap)
    }

    if (isCompact) {
        // 2x2 specific views
        views.setTextViewText(R.id.solar_rhythm_event_title, targetItem.name.uppercase())
        views.setTextColor(R.id.solar_rhythm_event_title, secondaryInk)
    } else {
        // 4x2 specific views
        val quoteText = formatSolarWhisperQuote(
            context.getString(display.line1Res),
            context.getString(display.line2Res),
        )
        views.setTextViewText(R.id.solar_rhythm_quote, quoteText)
        views.setTextColor(R.id.solar_rhythm_quote, secondaryInk)

        views.setInt(R.id.solar_rhythm_divider, "setBackgroundColor", hairlineColor)

        views.setTextViewText(R.id.solar_rhythm_event_title, targetItem.name)
        views.setTextColor(R.id.solar_rhythm_event_title, primaryInk)

        val startDateFormatted = formatAnchorDateSubLabel(
            count = daysCount,
            date = LocalDate.ofEpochDay(targetItem.epochDay),
            sinceTemplate = context.getString(R.string.since_label),
            untilTemplate = context.getString(R.string.until_label),
        )
        views.setTextViewText(R.id.solar_rhythm_event_subtitle, startDateFormatted)
        views.setTextColor(R.id.solar_rhythm_event_subtitle, secondaryInk)

        views.setTextColor(R.id.solar_rhythm_days_unit, seasonalPrimary)

        views.setTextViewText(R.id.solar_rhythm_current_term, context.getString(display.nameRes))
        views.setTextColor(R.id.solar_rhythm_current_term, secondaryInk)

        views.setTextViewText(
            R.id.solar_rhythm_term_day_progress,
            context.getString(R.string.solar_rhythm_day_of, transition.elapsedDays + 1, transition.daysInTerm),
        )
        views.setTextColor(R.id.solar_rhythm_term_day_progress, secondaryInk)

        views.setTextViewText(R.id.solar_rhythm_next_term, context.getString(transition.nextTerm.nameRes))
        views.setTextColor(R.id.solar_rhythm_next_term, secondaryInk)
    }

    // Tap anywhere on widget opens the target item in MainActivity
    val launchIntent = WidgetNavigationContract.createLaunchIntent(context, targetItem.id)
    val pendingIntent = PendingIntent.getActivity(
        context,
        WidgetNavigationContract.resolveRequestCode(targetItem.id, appWidgetId, WidgetNavigationContract.SOLAR_RHYTHM_PENDING_INTENT_OFFSET),
        launchIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    views.setOnClickPendingIntent(R.id.solar_rhythm_root, pendingIntent)

    return views
}

/** Formats the dual-line seasonal whisper with localized punctuation and balanced line wrapping. */
fun formatSolarWhisperQuote(line1: String, line2: String): String {
    if (line1.isBlank() && line2.isBlank()) return ""
    if (line1.isBlank()) return line2
    if (line2.isBlank()) return line1
    val isCjk = line1.any { it.code in 0x4E00..0x9FFF } || line2.any { it.code in 0x4E00..0x9FFF }
    return if (isCjk) "“$line1，$line2”" else "\"$line1;\n$line2\""
}


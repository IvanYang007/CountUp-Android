package com.countup.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import java.time.LocalDate
import kotlin.math.abs

/**
 * Focused Hero Milestone Widget provider (2x1 Poetic Card).
 *
 * Dedicated to honoring a single chosen counter on the home screen with
 * ambient milestone gold accents, custom palette styling, and zero battery drain.
 */
class HeroWidgetReceiver : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == WidgetNavigationContract.ACTION_CYCLE_HERO_DISPLAY_MODE) {
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
                    val store = CountUpStore.getInstance(appContext)
                    val boundItemId = store.getHeroWidgetBinding(appWidgetId)
                    val items = store.items()
                    val targetItem = ZenWidgetReducer.resolveTargetItem(items, boundItemId)
                    val count = targetItem?.let { daysSince(LocalDate.ofEpochDay(it.epochDay), LocalDate.now()) } ?: 0L
                    val currentMode = store.getHeroWidgetDisplayMode(appWidgetId)
                    val nextMode = currentMode.next(count)
                    store.setHeroWidgetDisplayMode(appWidgetId, nextMode)
                    pushHeroWidgetUpdate(appContext, appWidgetId)
                }
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val appContext = context.applicationContext
        MidnightAlarmReceiver.scheduleMidnightAlarm(appContext)
        launchAsync {
            for (appWidgetId in appWidgetIds) {
                pushHeroWidgetUpdate(appContext, appWidgetId)
            }
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
            pushHeroWidgetUpdate(appContext, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val appContext = context.applicationContext
        launchAsync {
            val store = CountUpStore.getInstance(appContext)
            for (id in appWidgetIds) {
                store.removeHeroWidgetBinding(id)
            }
        }
    }
}

/** Pushes an update to all placed Hero Milestone widgets on the launcher. */
fun pushAllHeroWidgetsUpdate(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val ids = manager.getAppWidgetIds(ComponentName(context, HeroWidgetReceiver::class.java))
    if (ids.isEmpty()) return
    for (id in ids) {
        pushHeroWidgetUpdate(context, id)
    }
}

/** Renders and pushes RemoteViews for a single Hero Milestone widget (2x1 Poetic Card). */
fun pushHeroWidgetUpdate(context: Context, appWidgetId: Int) {
    val manager = AppWidgetManager.getInstance(context)
    val store = CountUpStore.getInstance(context)
    val items = store.items()
    val boundItemId = store.getHeroWidgetBinding(appWidgetId)
    val targetItem = ZenWidgetReducer.resolveTargetItem(items, boundItemId)

    val today = LocalDate.now()
    val views = buildHero2x1RemoteViews(context, targetItem, today, appWidgetId)
    manager.updateAppWidget(appWidgetId, views)
}

/** Builds the RemoteViews hierarchy for the 2x1 Hero Poetic Card layout. */
private fun buildHero2x1RemoteViews(
    context: Context,
    item: CountUpItem?,
    today: LocalDate,
    appWidgetId: Int,
): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.countup_hero_widget_2x1)

    if (item == null) {
        // Empty state: show prompt and tap to open app
        views.setViewVisibility(R.id.hero_empty_view, View.VISIBLE)
        views.setViewVisibility(R.id.hero_content_container, View.GONE)
        views.setViewVisibility(R.id.hero_badge_container, View.GONE)

        WidgetNavigationContract.attachEmptyStateLaunchIntent(views, context, appWidgetId, R.id.hero_widget_root)
        return views
    }

    views.setViewVisibility(R.id.hero_empty_view, View.GONE)
    views.setViewVisibility(R.id.hero_content_container, View.VISIBLE)
    views.setViewVisibility(R.id.hero_badge_container, View.VISIBLE)

    val armed = ResetCountReceiver.isArmed(item.id)
    val count = daysSince(LocalDate.ofEpochDay(item.epochDay), today)
    val isMilestone = isMilestoneDay(count)
    val isNight = isNightMode(context)
    val style = resolveCardStyle(item.cardColor, isDark = isNight)
    val circleStyle = resolveWidgetCircleStyle(
        row = WidgetRowData(
            id = item.id,
            name = item.name,
            count = count,
            futureFlag = item.futureFlag,
            icon = item.icon,
            cardColor = item.cardColor,
        ),
        isDark = isNight,
    )

    val cardBgInt = style.cardBg.toArgb()
    val primaryInkInt = style.primaryInk.toArgb()
    val mutedInkInt = style.mutedInk.toArgb()
    val isDark = style.isDark
    val alertVermilion = 0xFFC45249.toInt()

    views.setInt(R.id.hero_widget_root, "setBackgroundColor", cardBgInt)

    val resetIntent = Intent(context, ResetCountReceiver::class.java).apply {
        putExtra(ResetCountReceiver.EXTRA_ITEM_ID, item.id)
    }
    val resetRequestCode = WidgetNavigationContract.resolveRequestCode(
        targetItemId = item.id,
        appWidgetId = appWidgetId,
        offset = WidgetNavigationContract.HERO_RESET_PENDING_INTENT_OFFSET,
    )

    if (armed) {
        // Armed state: visual confirmation prompt
        views.setTextViewText(R.id.hero_name, context.getString(R.string.hero_widget_reset_prompt).uppercase())
        views.setTextColor(R.id.hero_name, alertVermilion)

        views.setTextViewText(R.id.hero_count, "0?")
        views.setTextColor(R.id.hero_count, alertVermilion)
        views.setTextViewTextSize(R.id.hero_count, TypedValue.COMPLEX_UNIT_SP, 24f)

        views.setTextViewText(R.id.hero_unit, context.getString(R.string.widget_reset_prompt).uppercase())
        views.setTextColor(R.id.hero_unit, alertVermilion)
        views.setTextViewTextSize(R.id.hero_unit, TypedValue.COMPLEX_UNIT_SP, 10f)

        views.setViewVisibility(R.id.hero_milestone_dot, View.GONE)
        views.setViewVisibility(R.id.hero_reset_badge, View.GONE)

        views.setTextViewText(R.id.hero_sublabel, context.getString(R.string.hero_widget_reset_sublabel))
        views.setTextColor(R.id.hero_sublabel, alertVermilion)

        views.setInt(R.id.hero_badge_circle, "setColorFilter", alertVermilion)

        val iconDrawableRes = iconRes(item.icon.ifBlank { DEFAULT_ICON })
        views.setImageViewResource(R.id.hero_badge_icon, iconDrawableRes)
        views.setInt(R.id.hero_badge_icon, "setColorFilter", 0xFFFFFFFF.toInt())

        // When armed, tapping anywhere on the card confirms reset
        WidgetNavigationContract.attachBroadcastPendingIntent(
            views = views,
            context = context,
            requestCode = resetRequestCode,
            intent = resetIntent,
            R.id.hero_widget_root,
            R.id.hero_badge_container,
            R.id.hero_count_container,
        )
    } else {
        // Normal active state with tap-to-decompose ephemeris odometer
        val store = CountUpStore.getInstance(context)
        val canShowWeeks = abs(count) >= 7L
        val rawMode = store.getHeroWidgetDisplayMode(appWidgetId)
        val displayMode = if (!canShowWeeks && rawMode == TimeDisplayMode.TOTAL_WEEKS) TimeDisplayMode.DAYS else rawMode
        val decomposed = decomposeTime(LocalDate.ofEpochDay(item.epochDay), today, displayMode)

        views.setTextViewText(R.id.hero_name, item.name.uppercase())
        views.setTextColor(R.id.hero_name, if (isDark) WidgetThemeTokens.DARK_ACCENT_GOLD else mutedInkInt)

        if (item.resetCount > 0) {
            views.setViewVisibility(R.id.hero_reset_badge, View.VISIBLE)
            views.setTextViewText(R.id.hero_reset_badge, context.getString(R.string.widget_reset_count_badge, item.resetCount))
            views.setTextColor(R.id.hero_reset_badge, if (isDark) WIDGET_RESET_BADGE_NIGHT else WIDGET_RESET_BADGE_DAY)
        } else {
            views.setViewVisibility(R.id.hero_reset_badge, View.GONE)
        }

        views.setTextViewText(R.id.hero_count, decomposed.valueText)
        views.setTextColor(R.id.hero_count, primaryInkInt)

        val (countTextSizeSp, unitTextSizeSp) = when (displayMode) {
            TimeDisplayMode.DAYS -> 24f to 10f
            TimeDisplayMode.ELAPSED_BREAKDOWN -> 15f to 9.5f
            TimeDisplayMode.TOTAL_WEEKS -> 16f to 9.5f
        }
        views.setTextViewTextSize(R.id.hero_count, TypedValue.COMPLEX_UNIT_SP, countTextSizeSp)

        val unitText = if (decomposed.unitLabelRes != 0) context.getString(decomposed.unitLabelRes) else ""
        views.setTextViewText(R.id.hero_unit, unitText)
        views.setTextColor(R.id.hero_unit, mutedInkInt)
        views.setTextViewTextSize(R.id.hero_unit, TypedValue.COMPLEX_UNIT_SP, unitTextSizeSp)
        views.setViewVisibility(R.id.hero_unit, if (unitText.isEmpty()) View.GONE else View.VISIBLE)

        // Badge circle and icon
        views.setInt(R.id.hero_badge_circle, "setColorFilter", circleStyle.circleColor)

        val iconDrawableRes = iconRes(item.icon.ifBlank { DEFAULT_ICON })
        views.setImageViewResource(R.id.hero_badge_icon, iconDrawableRes)
        views.setInt(R.id.hero_badge_icon, "setColorFilter", circleStyle.textInk)

        // Milestone Accent Dot
        if (isMilestone) {
            views.setViewVisibility(R.id.hero_milestone_dot, View.VISIBLE)
            val milestoneColor = if (isDark) WidgetThemeTokens.DARK_MILESTONE_ACCENT else WidgetThemeTokens.LIGHT_MILESTONE_ACCENT
            views.setInt(R.id.hero_milestone_dot, "setColorFilter", milestoneColor)
        } else {
            views.setViewVisibility(R.id.hero_milestone_dot, View.GONE)
        }

        // Sublabel
        val date = LocalDate.ofEpochDay(item.epochDay)
        val sinceTemplate = context.getString(R.string.since_label)
        val untilTemplate = context.getString(R.string.until_label)
        val sublabel = formatAnchorDateSubLabel(count, date, sinceTemplate, untilTemplate)
        views.setTextViewText(R.id.hero_sublabel, sublabel)
        views.setTextColor(R.id.hero_sublabel, mutedInkInt)

        // Cycle display mode pending intent (tapping the count container)
        val cycleIntent = Intent(context, HeroWidgetReceiver::class.java).apply {
            action = WidgetNavigationContract.ACTION_CYCLE_HERO_DISPLAY_MODE
            putExtra(WidgetNavigationContract.EXTRA_APP_WIDGET_ID, appWidgetId)
        }
        WidgetNavigationContract.attachBroadcastPendingIntent(
            views = views,
            context = context,
            requestCode = WidgetNavigationContract.resolveRequestCode(null, appWidgetId, WidgetNavigationContract.HERO_CYCLE_PENDING_INTENT_OFFSET),
            intent = cycleIntent,
            R.id.hero_count_container,
        )

        // Tap badge circle to arm reset
        WidgetNavigationContract.attachBroadcastPendingIntent(
            views = views,
            context = context,
            requestCode = resetRequestCode,
            intent = resetIntent,
            R.id.hero_badge_container,
        )

        // Tap card body / title to open MainActivity
        WidgetNavigationContract.attachItemLaunchIntent(
            views = views,
            context = context,
            appWidgetId = appWidgetId,
            targetItemId = item.id,
            family = WidgetFamily.HERO,
            viewId = R.id.hero_widget_root,
        )
    }

    return views
}


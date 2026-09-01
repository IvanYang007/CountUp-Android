package com.countup.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import java.time.LocalDate

/**
 * Focused Hero Milestone Widget provider (2x1 Poetic Card).
 *
 * Dedicated to honoring a single chosen counter on the home screen with
 * ambient milestone gold accents, custom palette styling, and zero battery drain.
 */
class HeroWidgetReceiver : AppWidgetProvider() {

    companion object {
        const val EXTRA_TARGET_ITEM_ID = "EXTRA_TARGET_ITEM_ID"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            pushHeroWidgetUpdate(context, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle?,
    ) {
        pushHeroWidgetUpdate(context, appWidgetId)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val store = CountUpStore(context)
        for (id in appWidgetIds) {
            store.removeHeroWidgetBinding(id)
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
    val store = CountUpStore(context)
    val items = store.items()
    val boundItemId = store.getHeroWidgetBinding(appWidgetId)

    // Resolve target item: explicit binding -> first visible item -> first item -> null
    val targetItem = items.firstOrNull { it.id == boundItemId }
        ?: items.firstOrNull { it.showInWidget }
        ?: items.firstOrNull()

    val today = LocalDate.now()
    val views = buildHero2x1RemoteViews(context, targetItem, today)
    manager.updateAppWidget(appWidgetId, views)
}

/** Builds the RemoteViews hierarchy for the 2x1 Hero Poetic Card layout. */
private fun buildHero2x1RemoteViews(
    context: Context,
    item: CountUpItem?,
    today: LocalDate,
): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.countup_hero_widget_2x1)

    if (item == null) {
        // Empty state: show prompt and tap to open app
        views.setViewVisibility(R.id.hero_empty_view, View.VISIBLE)
        views.setViewVisibility(R.id.hero_content_container, View.GONE)
        views.setViewVisibility(R.id.hero_badge_container, View.GONE)

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(R.id.hero_widget_root, pendingIntent)
        return views
    }

    views.setViewVisibility(R.id.hero_empty_view, View.GONE)
    views.setViewVisibility(R.id.hero_content_container, View.VISIBLE)
    views.setViewVisibility(R.id.hero_badge_container, View.VISIBLE)

    val armed = ResetCountReceiver.isArmed(item.id)
    val count = daysSince(LocalDate.ofEpochDay(item.epochDay), today)
    val isMilestone = isMilestoneDay(count)
    val style = resolveCardStyle(item.cardColor)
    val circleStyle = resolveWidgetCircleStyle(
        row = WidgetRowData(
            id = item.id,
            name = item.name,
            count = count,
            futureFlag = item.futureFlag,
            icon = item.icon,
            cardColor = item.cardColor,
        ),
        position = 0,
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
    val resetPendingIntent = PendingIntent.getBroadcast(
        context,
        appWidgetIdHashCode(item.id) + 4004,
        resetIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
    )

    val launchIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(HeroWidgetReceiver.EXTRA_TARGET_ITEM_ID, item.id)
    }
    val openAppPendingIntent = PendingIntent.getActivity(
        context,
        appWidgetIdHashCode(item.id),
        launchIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    if (armed) {
        // Armed state: visual confirmation prompt
        views.setTextViewText(R.id.hero_name, context.getString(R.string.hero_widget_reset_prompt).uppercase())
        views.setTextColor(R.id.hero_name, alertVermilion)

        views.setTextViewText(R.id.hero_count, "0?")
        views.setTextColor(R.id.hero_count, alertVermilion)

        views.setTextViewText(R.id.hero_unit, context.getString(R.string.widget_reset_prompt).uppercase())
        views.setTextColor(R.id.hero_unit, alertVermilion)

        views.setViewVisibility(R.id.hero_milestone_dot, View.GONE)

        views.setTextViewText(R.id.hero_sublabel, context.getString(R.string.hero_widget_reset_sublabel))
        views.setTextColor(R.id.hero_sublabel, alertVermilion)

        views.setImageViewResource(R.id.hero_badge_circle, R.drawable.ic_circle_olive)
        views.setInt(R.id.hero_badge_circle, "setColorFilter", alertVermilion)

        val iconDrawableRes = iconRes(item.icon.ifBlank { DEFAULT_ICON })
        views.setImageViewResource(R.id.hero_badge_icon, iconDrawableRes)
        views.setInt(R.id.hero_badge_icon, "setColorFilter", 0xFFFFFFFF.toInt())

        // When armed, tapping anywhere on the card confirms reset
        views.setOnClickPendingIntent(R.id.hero_widget_root, resetPendingIntent)
        views.setOnClickPendingIntent(R.id.hero_badge_container, resetPendingIntent)
        views.setOnClickPendingIntent(R.id.hero_count_container, resetPendingIntent)
    } else {
        // Normal active state
        views.setTextViewText(R.id.hero_name, item.name.uppercase())
        views.setTextColor(R.id.hero_name, if (isDark) 0xFFDEB285.toInt() else mutedInkInt)

        views.setTextViewText(R.id.hero_count, count.toString())
        views.setTextColor(R.id.hero_count, primaryInkInt)

        views.setTextViewText(R.id.hero_unit, context.getString(R.string.unit_days))
        views.setTextColor(R.id.hero_unit, mutedInkInt)

        // Badge circle and icon
        views.setImageViewResource(R.id.hero_badge_circle, R.drawable.ic_circle_olive)
        views.setInt(R.id.hero_badge_circle, "setColorFilter", circleStyle.circleColor)

        val iconDrawableRes = iconRes(item.icon.ifBlank { DEFAULT_ICON })
        views.setImageViewResource(R.id.hero_badge_icon, iconDrawableRes)
        views.setInt(R.id.hero_badge_icon, "setColorFilter", circleStyle.textInk)

        // Milestone Gold Accent Dot
        if (isMilestone) {
            views.setViewVisibility(R.id.hero_milestone_dot, View.VISIBLE)
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

        // Tap badge or count to arm reset; tap background/title to open MainActivity
        views.setOnClickPendingIntent(R.id.hero_badge_container, resetPendingIntent)
        views.setOnClickPendingIntent(R.id.hero_count_container, resetPendingIntent)
        views.setOnClickPendingIntent(R.id.hero_widget_root, openAppPendingIntent)
    }

    return views
}

private fun appWidgetIdHashCode(id: String): Int = (id.hashCode() and 0x7FFFFFFF)


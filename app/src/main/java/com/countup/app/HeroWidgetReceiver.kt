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
 * Focused Hero Milestone Widget provider (1x1 Compact Stamp & 2x1 Poetic Card).
 *
 * Dedicated to honoring a single chosen counter on the home screen with
 * ambient milestone gold accents, custom palette styling, and zero battery drain.
 */
class HeroWidgetReceiver : AppWidgetProvider() {

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

/** Renders and pushes RemoteViews for a single Hero Milestone widget. */
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

    val views = buildHeroRemoteViews(
        context = context,
        layoutResId = R.layout.countup_hero_widget_2x1,
        item = targetItem,
        today = today,
    )

    manager.updateAppWidget(appWidgetId, views)
}

/** Builds the RemoteViews hierarchy for the 2x1 Hero Poetic Card layout. */
private fun buildHeroRemoteViews(
    context: Context,
    layoutResId: Int,
    item: CountUpItem?,
    today: LocalDate,
): RemoteViews {
    val views = RemoteViews(context.packageName, layoutResId)

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

    // Palette & Colors
    val cardBgInt = style.cardBg.toArgb()
    val primaryInkInt = style.primaryInk.toArgb()
    val mutedInkInt = style.mutedInk.toArgb()
    val isDark = style.isDark

    views.setInt(R.id.hero_widget_root, "setBackgroundColor", cardBgInt)
    views.setTextViewText(R.id.hero_name, item.name.uppercase())
    views.setTextColor(R.id.hero_name, if (isDark) 0xFFDEB285.toInt() else mutedInkInt)

    views.setTextViewText(R.id.hero_count, count.toString())
    views.setTextColor(R.id.hero_count, primaryInkInt)
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

    // Tap anywhere on widget -> open MainActivity
    val launchIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra("EXTRA_TARGET_ITEM_ID", item.id)
    }
    val pendingIntent = PendingIntent.getActivity(
        context,
        appWidgetIdHashCode(item.id),
        launchIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    views.setOnClickPendingIntent(R.id.hero_widget_root, pendingIntent)

    return views
}

private fun appWidgetIdHashCode(id: String): Int = (id.hashCode() and 0x7FFFFFFF)

package com.ivanyang.countup

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.updateAll
import androidx.glance.appwidget.lazy.GridCells
import androidx.glance.appwidget.lazy.LazyVerticalGrid
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider as dayNightColorProvider
import androidx.glance.unit.ColorProvider
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.Box
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import java.time.LocalDate

/**
 * Intent extra carried by the widget's launch action (kept for backwards
 * compatibility with previously-placed widgets; [MainActivity] drains it once).
 */
const val EXTRA_RECORD_TODAY = "com.ivanyang.countup.RECORD_TODAY"

/** A single rendered cell of widget data, derived from a store item. */
internal data class WidgetRowData(
    val id: String,
    val name: String,
    val count: Long,
    val epochDay: Long,
    val icon: String = "",
)

/** Pure derivation shared by the widget (and unit-tested on the JVM). */
internal fun widgetRows(items: List<CountUpItem>, today: LocalDate): List<WidgetRowData> =
    items.map { item ->
        val date = LocalDate.ofEpochDay(item.epochDay)
        WidgetRowData(id = item.id, name = item.name, count = daysSince(date, today), epochDay = item.epochDay, icon = item.icon)
    }

/**
 * Passive, stateless home-screen widget. Re-reads all items from [CountUpStore]
 * on every render and lays them out as a 2-column compact grid. Uses a warm
 * paper background and theme-adaptive text.
 */
class HaircutWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val items = CountUpStore(context).items()
        provideContent {
            HaircutWidgetContent(items = items)
        }
    }
}

class HaircutWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HaircutWidget()
}

/** Resets one item's anchor date to today directly from the widget. */
class ResetCountAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[EXTRA_RESET_ITEM] ?: return
        val store = CountUpStore(context)
        if (store.resetTo(id, LocalDate.now().toEpochDay())) {
            // Refresh the EXACT tapped widget first: its GlanceId (passed into onAction)
            // is authoritative and needs no GlanceAppWidgetManager lookup, so it cannot
            // silently no-op when an enumeration returns empty. Then sweep all placed
            // widgets as a safety net. (Expert review: artifacts/expert-review.md, A1)
            HaircutWidget().update(context, glanceId)
            HaircutWidget().updateAll(context)
            // Keep the app's resume-time refresh gate accurate: the widget is now
            // up to date for today.
            store.markWidgetRefreshed(LocalDate.now().toEpochDay())
        }
    }
}

/** Forces every placed widget to re-render (tap the refresh button). */
class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        // Target the tapped widget by its authoritative id, then sweep all placements.
        HaircutWidget().update(context, glanceId)
        HaircutWidget().updateAll(context)
        CountUpStore(context).markWidgetRefreshed(LocalDate.now().toEpochDay())
    }
}

private val EXTRA_RESET_ITEM = ActionParameters.Key<String>("com.ivanyang.countup.RESET_ITEM")

/** Whether the device is in dark (night) mode. */
private fun isNightMode(context: Context): Boolean =
    (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

@Composable
private fun HaircutWidgetContent(items: List<CountUpItem>) {
    val context = LocalContext.current
    val rows = widgetRows(items, LocalDate.now())

    // Launch action opens the app; the pending intent is generated immutable by Glance.
    val launchApp = actionStartActivity(intent = Intent(context, MainActivity::class.java))
    // Refresh action re-renders every widget instance (re-reads the store, so
    // counts for today recompute immediately).
    val refreshAction = actionRunCallback<RefreshWidgetAction>()

    // Theme-aware zen paper: light by default, dark paper in night mode.
    val night = isNightMode(context)
    val paper = if (night) Color(0xFF242422) else Color(0xFFF7F6F3)
    val mutedColor = dayNightColorProvider(day = Color(0xFF787774), night = Color(0xFFA8A8A3))
    // Count number: dark ink on the light circle by day; light warm on the dark
    // circle at night so it always stands out from the plate and the paper.
    val numberColor = dayNightColorProvider(day = Color(0xFF2F3437), night = Color(0xFFF2EEE4))
    val circleRes = if (night) R.drawable.ic_solid_circle_dark else R.drawable.ic_solid_circle
    val refreshRes = if (night) R.drawable.ic_refresh_dark else R.drawable.ic_refresh

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(paper)
            .clickable(launchApp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        LazyVerticalGrid(
            gridCells = GridCells.Fixed(3),
            // Reserve ~34dp on the right so the refresh button never sits under the
            // launcher's rounded corner (which clips the extreme top-right).
            modifier = GlanceModifier.fillMaxSize().padding(start = 6.dp, end = 34.dp, top = 2.dp, bottom = 2.dp),
        ) {
            if (rows.isEmpty()) {
                item {
                    Text(
                        text = context.getString(R.string.widget_empty),
                        style = TextStyle(fontSize = 13.sp, color = mutedColor),
                        modifier = GlanceModifier.padding(6.dp),
                    )
                }
            } else {
                items(rows.size) { index ->
                    WidgetCell(
                        row = rows[index],
                        context = context,
                        nameColor = mutedColor,
                        numberColor = numberColor,
                        circleRes = circleRes,
                    )
                }
            }
        }
        // Refresh button on the right edge, vertically centred — clear of the
        // rounded corners so it is always visible and tappable; updates every widget.
        Image(
            provider = ImageProvider(refreshRes),
            contentDescription = context.getString(R.string.widget_refresh),
            modifier = GlanceModifier.padding(end = 8.dp).size(24.dp).clickable(refreshAction),
        )
    }
}

@Composable
private fun WidgetCell(
    row: WidgetRowData,
    context: Context,
    nameColor: ColorProvider,
    numberColor: ColorProvider,
    circleRes: Int,
) {
    // Tapping this item's card resets ONLY this item to today (per-id action).
    val resetAction = actionRunCallback<ResetCountAction>(
        actionParametersOf(EXTRA_RESET_ITEM to row.id),
    )

    Column(
        modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp).clickable(resetAction),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Item name at the top.
        Text(
            text = row.name,
            style = TextStyle(fontSize = 13.sp, color = nameColor),
            maxLines = 1,
        )
        // Solid circle with the day count (number only) centred on it, below the name.
        Box(
            modifier = GlanceModifier.size(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(circleRes),
                contentDescription = null,
                modifier = GlanceModifier.fillMaxSize(),
            )
            Text(
                text = row.count.toString(),
                style = TextStyle(fontSize = 18.sp, color = numberColor),
                maxLines = 1,
            )
        }
    }
}

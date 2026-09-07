package com.countup.app

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

import androidx.glance.currentState
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey

/**
 * The Solar Rhythm (时节同行 · 岁华流转) Widget.
 * 4x2 Rich Zen Canvas & 2x2 responsive fallback harmonizing personal count-ups
 * with the on-device 24 Solar Terms calendar and seasonal micro-poetry.
 */
class SolarRhythmWidget : GlanceAppWidget() {

    override var stateDefinition: GlanceStateDefinition<*>? = PreferencesGlanceStateDefinition

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(140.dp, 110.dp), // 2x2 breakpoint
            DpSize(260.dp, 110.dp), // 4x2 breakpoint
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            // Subscribe to state changes so updateAppWidgetState forces re-evaluation
            val prefs = currentState<Preferences>()
            @Suppress("UNUSED_VARIABLE")
            val timestamp = prefs[KEY_LAST_UPDATE]

            val store = CountUpStore(context)
            val items = store.items()
            val appWidgetId = (id as? androidx.glance.appwidget.AppWidgetId)?.appWidgetId
            val boundId = appWidgetId?.let { store.getZenHorizonBinding(it) ?: store.getHeroWidgetBinding(it) }
            val targetItem = resolveZenHorizonTargetItem(items, boundId)
            val today = LocalDate.now()
            val isDark = isNightMode(context)

            val transition = SolarTermCalendar.getSolarTermTransition(today)
            val display = SolarTermPoetryBridge.resolveWidgetDisplay(transition.currentTerm, isDark)

            val daysCount = if (targetItem != null) {
                daysSince(LocalDate.ofEpochDay(targetItem.epochDay), today)
            } else 0L

            val size = LocalSize.current
            val isCompact = size.width < 220.dp

            SolarRhythmContent(
                context = context,
                targetItem = targetItem,
                daysCount = daysCount,
                transition = transition,
                display = display,
                isDark = isDark,
                isCompact = isCompact,
            )
        }
    }

    companion object {
        val KEY_LAST_UPDATE = longPreferencesKey("solar_rhythm_last_update")
    }
}

@Composable
fun SolarRhythmContent(
    context: Context,
    targetItem: CountUpItem?,
    daysCount: Long,
    transition: SolarTermTransition,
    display: SolarTermWidgetDisplay,
    isDark: Boolean,
    isCompact: Boolean,
) {
    val themeTokens = WidgetThemeTokens.resolve(isDark)
    val cardStyle = targetItem?.let { resolveCardStyle(it.cardColor, isDark = isDark) }
    val canvasBg = cardStyle?.cardBg ?: Color(themeTokens.canvasBg)
    val primaryInk = cardStyle?.primaryInk ?: Color(themeTokens.primaryInk)
    val secondaryInk = cardStyle?.mutedInk ?: Color(themeTokens.secondaryInk)
    val seasonalPrimary = Color(display.palette.primaryTint)
    val seasonalProgress = Color(display.palette.progressTint)
    val hairlineColor = Color(themeTokens.hairline)

    val launchIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        if (targetItem != null) {
            putExtra(HeroWidgetReceiver.EXTRA_TARGET_ITEM_ID, targetItem.id)
        }
    }

    val quoteText = formatSolarWhisperQuote(
        context.getString(display.line1Res),
        context.getString(display.line2Res),
    )

    val horizPadding = if (isCompact) 14.dp else 20.dp
    val vertPadding = if (isCompact) 14.dp else 16.dp

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(canvasBg)
            .cornerRadius(24.dp)
            .padding(start = horizPadding, top = vertPadding, end = horizPadding, bottom = vertPadding)
            .clickable(actionStartActivity(launchIntent)),
        contentAlignment = Alignment.Center,
    ) {
        if (isCompact) {
            // 2x2 Responsive Layout
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Top: Solar Term Badge
                Row(
                    modifier = GlanceModifier
                        .cornerRadius(6.dp)
                        .background(Color(display.palette.badgeBg))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = context.getString(display.nameRes),
                        style = TextStyle(
                            color = ColorProvider(Color(display.palette.badgeTextColor)),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                // Center: Big Numeral & Title
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = daysCount.toString(),
                        style = TextStyle(
                            color = ColorProvider(seasonalPrimary),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    Text(
                        text = (targetItem?.name ?: "DAYS").uppercase(),
                        style = TextStyle(
                            color = ColorProvider(secondaryInk),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        maxLines = 1,
                    )
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                // Bottom: Compact Timeline
                LinearProgressIndicator(
                    progress = transition.progressFraction,
                    modifier = GlanceModifier.fillMaxWidth().height(4.dp),
                    color = ColorProvider(seasonalProgress),
                    backgroundColor = ColorProvider(hairlineColor),
                )
            }
        } else {
            // 4x2 Rich Canvas Layout
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 1. Top Bar: Solar Term Badge + Contemplative Quote
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = GlanceModifier
                            .cornerRadius(8.dp)
                            .background(Color(display.palette.badgeBg))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = context.getString(display.nameRes),
                            style = TextStyle(
                                color = ColorProvider(Color(display.palette.badgeTextColor)),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(8.dp))

                    Text(
                        text = quoteText,
                        modifier = GlanceModifier
                            .defaultWeight()
                            .padding(end = 2.dp),
                        style = TextStyle(
                            color = ColorProvider(secondaryInk),
                            fontSize = 10.sp,
                            fontStyle = FontStyle.Italic,
                            textAlign = TextAlign.End,
                        ),
                        maxLines = 2,
                    )
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                // Subtle hairline divider matching prototype
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(hairlineColor),
                ) {}

                Spacer(modifier = GlanceModifier.defaultWeight())

                // 2. Middle: Event Name & Subtitle on Left, Large Numeral on Right
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = targetItem?.name ?: context.getString(R.string.zen_horizon_empty),
                            style = TextStyle(
                                color = ColorProvider(primaryInk),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            maxLines = 1,
                        )
                        Spacer(modifier = GlanceModifier.height(3.dp))
                        val startText = targetItem?.let {
                            "Since " + LocalDate.ofEpochDay(it.epochDay).format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))
                        } ?: "Solar rhythm"
                        Text(
                            text = startText,
                            style = TextStyle(
                                color = ColorProvider(secondaryInk),
                                fontSize = 11.sp,
                            ),
                            maxLines = 1,
                        )
                    }

                    Column(
                        modifier = GlanceModifier.padding(end = 12.dp),
                        horizontalAlignment = Alignment.End,
                    ) {
                        Text(
                            text = daysCount.toString(),
                            style = TextStyle(
                                color = ColorProvider(seasonalPrimary),
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        Text(
                            text = "DAYS",
                            style = TextStyle(
                                color = ColorProvider(seasonalPrimary),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                // 3. Bottom: Seasonal Timeline Track
                Column(modifier = GlanceModifier.fillMaxWidth()) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = context.getString(display.nameRes),
                            style = TextStyle(color = ColorProvider(secondaryInk), fontSize = 9.sp),
                        )
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Text(
                            text = context.getString(R.string.solar_rhythm_day_of, transition.elapsedDays + 1, transition.daysInTerm),
                            style = TextStyle(color = ColorProvider(secondaryInk), fontSize = 9.sp),
                        )
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Text(
                            text = context.getString(transition.nextTerm.nameRes),
                            style = TextStyle(color = ColorProvider(secondaryInk), fontSize = 9.sp),
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(4.dp))

                    LinearProgressIndicator(
                        progress = transition.progressFraction,
                        modifier = GlanceModifier.fillMaxWidth().height(4.dp),
                        color = ColorProvider(seasonalProgress),
                        backgroundColor = ColorProvider(hairlineColor),
                    )
                }
            }
        }
    }
}

/** Receiver for Solar Rhythm Glance widget updates. */
class SolarRhythmWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SolarRhythmWidget()
}

/** Pushes update to all placed Solar Rhythm Glance widgets on the launcher. */
suspend fun pushAllSolarRhythmWidgetsUpdate(context: Context) {
    try {
        val glanceManager = GlanceAppWidgetManager(context)
        val glanceIds = glanceManager.getGlanceIds(SolarRhythmWidget::class.java)
        for (glanceId in glanceIds) {
            updateAppWidgetState(context, glanceId) { prefs ->
                prefs[SolarRhythmWidget.KEY_LAST_UPDATE] = System.currentTimeMillis()
            }
            SolarRhythmWidget().update(context, glanceId)
        }
    } catch (_: Throwable) {
        // Best effort
    }

    try {
        val manager = android.appwidget.AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(android.content.ComponentName(context, SolarRhythmWidgetReceiver::class.java))
        if (ids.isNotEmpty()) {
            val intent = Intent(context, SolarRhythmWidgetReceiver::class.java).apply {
                action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    } catch (_: Throwable) {
        // Best effort
    }
}

/** Formats the dual-line seasonal whisper with localized punctuation and balanced line wrapping. */
fun formatSolarWhisperQuote(line1: String, line2: String): String {
    if (line1.isBlank() && line2.isBlank()) return ""
    if (line1.isBlank()) return line2
    if (line2.isBlank()) return line1
    val isCjk = line1.any { it.code in 0x4E00..0x9FFF } || line2.any { it.code in 0x4E00..0x9FFF }
    return if (isCjk) "“$line1，$line2”" else "\"$line1;\n$line2\""
}

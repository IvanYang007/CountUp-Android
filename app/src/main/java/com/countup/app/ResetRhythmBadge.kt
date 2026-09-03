package com.countup.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Curated Chinese Bronze Gold & Ink Pill Palette Tokens (中国青铜金 / 泥金).
 * WCAG AA / AAA contrast compliant, antique, calm, and dignified.
 */
object ZenBronzeTokens {
    // Light Paper Cards (#FFFFFF / #FAFAF7) - 沉香古铜 (WCAG AA 5.27:1 contrast)
    val BronzeGoldDeep = Color(0xFF785D2A)

    // Dark Sumi Cards (#24201A) - 泥金熟绢 (WCAG AAA 7.15:1 contrast)
    val BronzeGoldSilk = Color(0xFFD4B87C)

    // Zen Tooltip Surface (Hover / Touch Popup)
    val TooltipBg = Color(0xFF24201A)
    val TooltipBorder = Color(0x66D4B87C) // 40% alpha Silk Bronze
    val TooltipPaperText = Color(0xFFFAF7F2)
}

/**
 * Production Reset Rhythm Badge (Variant 2: Celestial Dot [ 4 · 28 ]).
 * Displays total resets and average cycle days in Chinese Bronze Gold.
 *
 * Employs Zen negative space: completely silent when [CountUpItem.resetCount] == 0.
 * Long-press or tap summons the Zen Paper Tooltip explaining the metrics.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetRhythmBadge(
    item: CountUpItem,
    modifier: Modifier = Modifier,
    isDarkCard: Boolean = false,
    patina: PatinaData? = null,
) {
    val resets = item.resetCount
    // Zen Emptiness Rule: Silence is truest when no resets have occurred
    if (resets <= 0) {
        return
    }

    val avgDays = item.averageResetDays
    val bg = patina?.badgeBg ?: if (isDarkCard) Color(0x22FFFFFF) else Color(0x14000000)
    val fontColor = if (isDarkCard) ZenBronzeTokens.BronzeGoldSilk else ZenBronzeTokens.BronzeGoldDeep

    val a11yText = if (resets == 1) {
        stringResource(R.string.cd_reset_rhythm_single, avgDays)
    } else {
        stringResource(R.string.cd_reset_rhythm_multiple, resets, avgDays)
    }

    @Suppress("DEPRECATION")
    val digitStyle = remember(fontColor) {
        TextStyle(
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.SansSerif,
            color = fontColor,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            lineHeight = 11.sp,
            fontFeatureSettings = "tnum",
        )
    }

    val tooltipState = rememberTooltipState(isPersistent = false)
    val scope = rememberCoroutineScope()

    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(positioning = androidx.compose.material3.TooltipAnchorPosition.Above),
        tooltip = {
            ZenPaperTooltip(
                resets = resets,
                avgDays = avgDays,
            )
        },
        state = tooltipState,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .height(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(bg)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    scope.launch {
                        tooltipState.show()
                    }
                }
                .padding(horizontal = 5.5.dp)
                .clearAndSetSemantics { contentDescription = a11yText },
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = resets.toString(),
                    style = digitStyle,
                )
                Text(
                    text = stringResource(R.string.reset_rhythm_dot),
                    style = digitStyle.copy(
                        fontWeight = FontWeight.Bold,
                        color = fontColor.copy(alpha = 0.55f),
                    ),
                )
                Text(
                    text = avgDays.toString(),
                    style = digitStyle,
                )
            }
        }
    }
}

/**
 * Zen Paper & Ink Tooltip Box.
 */
@Composable
fun ZenPaperTooltip(
    resets: Int,
    avgDays: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = ZenBronzeTokens.TooltipBg,
        border = androidx.compose.foundation.BorderStroke(
            width = 0.75.dp,
            color = ZenBronzeTokens.TooltipBorder,
        ),
        shadowElevation = 6.dp,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(R.string.reset_rhythm_title),
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = ZenBronzeTokens.BronzeGoldSilk,
            )
            Text(
                text = if (resets == 1) {
                    stringResource(R.string.reset_rhythm_single, avgDays)
                } else {
                    stringResource(R.string.reset_rhythm_multiple, resets, avgDays)
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = ZenBronzeTokens.TooltipPaperText,
            )
        }
    }
}

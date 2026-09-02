package com.countup.app

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.pow

/**
 * Curated Zen pigments representing the natural aging stages of materials (Sage -> Sand -> Ochre -> Gold).
 * Directly grounded in patina_prototype.html.
 */
object PatinaPigments {
    val Sage = Color(0xFF7D9D8B) // Cool morning growth (RGB: 125, 157, 139)
    val Sand = Color(0xFFB5A88F) // Warm paper settling (RGB: 181, 168, 143)
    val Ochre = Color(0xFFC88D58) // Grounded ritual warmth (RGB: 200, 141, 88)
    val Gold = Color(0xFFD6A848) // Enduring Kintsugi gold (RGB: 214, 168, 72)
}

/**
 * Seasoning phases of an anchor counter across calendar time.
 */
@Immutable
enum class PatinaPhase(
    @get:StringRes val labelRes: Int,
) {
    FRESH_GROWTH(R.string.patina_phase_fresh),
    WARMING(R.string.patina_phase_warming),
    GROUNDED(R.string.patina_phase_grounded),
    SEASONED(R.string.patina_phase_seasoned),
    KINTSUGI(R.string.patina_phase_kintsugi),
}

/**
 * Immutable snapshot of calculated Patina styling tokens for a given day count.
 */
@Immutable
data class PatinaData(
    val days: Long,
    val warmth: Float,
    val phase: PatinaPhase,
    val primaryColor: Color,
    val leadingColor: Color,
    val trailingColor: Color,
    val alpha: Float,
    val badgeBg: Color,
    val badgeTextColor: Color,
    val borderBrush: Brush,
)

/**
 * Calculates the perceptual warmth fraction [0.0f, 1.0f] using a concave power curve (p = 0.72)
 * across a 180-day baseline.
 */
fun calculateWarmth(days: Long): Float {
    if (days <= 0L) return 0f
    val fraction = (days.toFloat() / 180f).coerceIn(0f, 1f)
    return fraction.toDouble().pow(0.72).toFloat()
}

/**
 * Linearly interpolates RGB channels between two colors with a normalized factor in [0f, 1f].
 * Delegates to Compose's built-in [androidx.compose.ui.graphics.lerp].
 */
fun lerpPatinaColor(c1: Color, c2: Color, factor: Float): Color {
    return androidx.compose.ui.graphics.lerp(c1, c2, factor.coerceIn(0f, 1f))
}

/**
 * Maps warmth [0.0f, 1.0f] to the piecewise continuous Zen pigment gradient:
 * Sage [0.0] -> Sand [0.25] -> Ochre [0.75] -> Gold [1.0]
 */
fun getPatinaColor(warmth: Float): Color {
    val w = warmth.coerceIn(0f, 1f)
    return when {
        w < 0.25f -> androidx.compose.ui.graphics.lerp(PatinaPigments.Sage, PatinaPigments.Sand, w / 0.25f)
        w < 0.75f -> androidx.compose.ui.graphics.lerp(PatinaPigments.Sand, PatinaPigments.Ochre, (w - 0.25f) / 0.50f)
        else -> androidx.compose.ui.graphics.lerp(PatinaPigments.Ochre, PatinaPigments.Gold, (w - 0.75f) / 0.25f)
    }
}

/**
 * Resolves the milestone seasoning phase for a given day count.
 */
fun resolvePatinaPhase(days: Long): PatinaPhase = when {
    days < 15L -> PatinaPhase.FRESH_GROWTH
    days < 30L -> PatinaPhase.WARMING
    days < 90L -> PatinaPhase.GROUNDED
    days < 180L -> PatinaPhase.SEASONED
    else -> PatinaPhase.KINTSUGI
}

/**
 * Resolves only the primary [Color] for a given day count.
 * Lightweight helper avoiding Compose Brush allocations for widgets and non-card surfaces.
 */
fun getPatinaPrimaryColor(days: Long): Color {
    val warmth = calculateWarmth(days)
    return getPatinaColor(warmth)
}

/**
 * Resolves full [PatinaData] tokens for rendering a card or widget.
 * Implements Variance B (Gradient Border) with strict WCAG AA/AAA accessibility contrast.
 */
fun resolvePatina(days: Long, isDark: Boolean = false): PatinaData {
    val warmth = calculateWarmth(days)
    val phase = resolvePatinaPhase(days)
    val primaryColor = getPatinaColor(warmth)
    val alpha = (0.28f + (warmth * 0.44f)).coerceIn(0.28f, 0.72f)

    val leadingWarmth = (warmth * 1.25f).coerceAtMost(1.0f)
    val trailingWarmth = warmth * 0.70f

    val leadingColor = getPatinaColor(leadingWarmth).copy(alpha = alpha)
    val trailingColor = getPatinaColor(trailingWarmth).copy(alpha = alpha * 0.35f)

    val borderBrush = Brush.linearGradient(
        colors = listOf(leadingColor, trailingColor),
        start = Offset.Zero,
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
    )

    // WCAG AAA (> 7:1) compliant contrast:
    // On dark surfaces, ZenWhite provides luminous readability against subtle tinted pills.
    // On light paper surfaces, ZenInkBlack guarantees > 7:1 contrast ratio against the subtle tinted pill.
    val badgeTextColor = if (isDark) ZenWhite else ZenInkBlack
    val badgeBg = primaryColor.copy(alpha = if (isDark) 0.22f else 0.14f)

    return PatinaData(
        days = days,
        warmth = warmth,
        phase = phase,
        primaryColor = primaryColor,
        leadingColor = leadingColor,
        trailingColor = trailingColor,
        alpha = alpha,
        badgeBg = badgeBg,
        badgeTextColor = badgeTextColor,
        borderBrush = borderBrush,
    )
}

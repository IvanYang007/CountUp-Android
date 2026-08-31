package com.countup.app

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Mid-Century Modern Zen Paper & Chinese Ink Pigment Color Tokens.
 */
@Immutable
data class ZenColorScheme(
    val paperBackground: Color,
    val paperSurface: Color,
    val paperCard: Color,
    val inkBlack: Color,
    val inkMuted: Color,
    val hairlineRule: Color,
    val hairlineRuleVariant: Color,
    val cinnabarVermilion: Color,
    val willowSage: Color,
    val ochreGold: Color,
    val dustyIndigo: Color,
    val errorCrimson: Color,
    val isDark: Boolean = false,
)

// Curated Mid-Century Modern & Ink Wash Palette
val ZenPaperBackground = Color(0xFFF5E6D3)
val ZenPaperSurface = Color(0xFFEBDCC3)
val ZenPaperCard = Color(0xFFFFFFFF)
val ZenInkBlack = Color(0xFF2C2416)
val ZenInkMuted = Color(0xFF6B5D4F)
val ZenHairlineRule = Color(0xFFE3D3B8)
val ZenHairlineRuleVariant = Color(0xFFD9C6A6)
val ZenVermilion = Color(0xFFD97642)
val ZenSage = Color(0xFF4A7C59)
val ZenOchre = Color(0xFFD4A574)
val ZenIndigo = Color(0xFF7D9BA8)
val ZenError = Color(0xFFA64942)
val ZenWhite = Color(0xFFFFFFFF)
val ZenArrivedGreen = Color(0xFF66BB6A)
val ZenArrivedRed = Color(0xFFB71C1C)

// Deterministic item accent chips for card tags
val ZenDayAccents = listOf(
    Color(0xFFD97642),
    Color(0xFFD4A574),
    Color(0xFF4A7C59),
    Color(0xFF7D9BA8),
    Color(0xFFE57A77),
    Color(0xFF8B7355),
)

fun lightZenColors() = ZenColorScheme(
    paperBackground = ZenPaperBackground,
    paperSurface = ZenPaperSurface,
    paperCard = ZenPaperCard,
    inkBlack = ZenInkBlack,
    inkMuted = ZenInkMuted,
    hairlineRule = ZenHairlineRule,
    hairlineRuleVariant = ZenHairlineRuleVariant,
    cinnabarVermilion = ZenVermilion,
    willowSage = ZenSage,
    ochreGold = ZenOchre,
    dustyIndigo = ZenIndigo,
    errorCrimson = ZenError,
    isDark = false,
)

val LocalZenColors = staticCompositionLocalOf { lightZenColors() }

private fun lightMcmMaterialColors() = lightColorScheme(
    primary = ZenVermilion,
    onPrimary = ZenWhite,
    tertiary = ZenSage,
    onTertiary = ZenWhite,
    background = ZenPaperBackground,
    onBackground = ZenInkBlack,
    surface = ZenPaperCard,
    onSurface = ZenInkBlack,
    surfaceVariant = ZenPaperSurface,
    onSurfaceVariant = ZenInkMuted,
    outline = ZenHairlineRule,
    outlineVariant = ZenHairlineRuleVariant,
    error = ZenError,
)

/**
 * Mid-Century Modern Zen Paper Theme.
 */
@Composable
fun ZenTheme(
    colors: ZenColorScheme = lightZenColors(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalZenColors provides colors) {
        MaterialTheme(
            colorScheme = lightMcmMaterialColors(),
            content = content,
        )
    }
}

/**
 * Tactile spring-damped press feedback and micro-haptic sensation for buttons, chips, and interactive cards.
 */
@Composable
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    targetScale: Float = 0.96f,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(pressed) {
        if (pressed) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
    val scale by animateFloatAsState(
        targetValue = if (pressed) targetScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "zenPressScale",
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Reusable MutableInteractionSource helper for tactile press feedback across the UI.
 */
@Composable
fun rememberPressSource(): MutableInteractionSource = remember { MutableInteractionSource() }

/**
 * Honors the system reduce-motion accessibility setting (animator duration scale == 0).
 */
fun isReducedMotion(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

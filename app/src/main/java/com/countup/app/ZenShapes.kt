package com.countup.app

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * 3-tier concentric Zen architectural joinery shape scale:
 * - Macro (20dp): Cards, dialog containers, large sheets
 * - Meso (10dp): Action buttons, text input fields, search bar
 * - Micro (6dp): Chips, tags, micro-badges, dropdown items
 * - ExtraSmall (4dp): Rhythm badges, sub-indicator pills
 */
val ZenShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

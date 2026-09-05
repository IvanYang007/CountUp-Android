package com.countup.app

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validates the 4-tier Zen Tactile Sensory Hierarchy:
 * "Texture, Not Tremor; Deflection, Not Collapse"
 */
class ZenTactileHierarchyTest {

    @Test
    fun `tactile hierarchy values match design specification exactly`() {
        assertEquals(0.92f, ZenTactileHierarchy.Level3Destructive, 0.0001f)
        assertEquals(0.96f, ZenTactileHierarchy.Level2PrimaryAction, 0.0001f)
        assertEquals(0.985f, ZenTactileHierarchy.Level1Card, 0.0001f)
        assertEquals(1.00f, ZenTactileHierarchy.Level0Flat, 0.0001f)
    }

    @Test
    fun `tactile hierarchy strictly preserves monotonic deflection ordering`() {
        assertTrue(
            "Level 3 Destructive must have greatest deflection (lowest scale factor)",
            ZenTactileHierarchy.Level3Destructive < ZenTactileHierarchy.Level2PrimaryAction,
        )
        assertTrue(
            "Level 2 Primary Action must have greater deflection than Level 1 Card",
            ZenTactileHierarchy.Level2PrimaryAction < ZenTactileHierarchy.Level1Card,
        )
        assertTrue(
            "Level 1 Card must have greater deflection than Level 0 Flat",
            ZenTactileHierarchy.Level1Card < ZenTactileHierarchy.Level0Flat,
        )
    }

    @Test
    fun `deflection amounts conform to sensory ergonomics`() {
        // Level 3: 8% deflection for high-consequence / destructive confirmation
        val level3Deflection = 1.0f - ZenTactileHierarchy.Level3Destructive
        assertEquals(0.08f, level3Deflection, 0.001f)

        // Level 2: 4% deflection for primary actions and compact chips
        val level2Deflection = 1.0f - ZenTactileHierarchy.Level2PrimaryAction
        assertEquals(0.04f, level2Deflection, 0.001f)

        // Level 1: 1.5% deflection for large cards (subtle surface yield without collapse)
        val level1Deflection = 1.0f - ZenTactileHierarchy.Level1Card
        assertEquals(0.015f, level1Deflection, 0.001f)

        // Level 0: 0% deflection (pure color/alpha tint)
        val level0Deflection = 1.0f - ZenTactileHierarchy.Level0Flat
        assertEquals(0.0f, level0Deflection, 0.001f)
    }

    @Test
    fun `card press highlight uses luminous white sheen rather than dark ink`() {
        assertEquals(Color.White, ZenTactileHierarchy.CardPressHighlight)
    }
}


package com.countup.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HeroWidgetTest {

    @Test
    fun `milestone days are identified properly for hero widget counts`() {
        assertTrue(isMilestoneDay(7))
        assertTrue(isMilestoneDay(30))
        assertTrue(isMilestoneDay(50))
        assertTrue(isMilestoneDay(100))
        assertTrue(isMilestoneDay(365))
        assertTrue(isMilestoneDay(1000))
        assertTrue(isMilestoneDay(2000))
    }

    @Test
    fun `hero widget circle styling resolves distinct high contrast colors`() {
        val rowSage = WidgetRowData(
            id = "1",
            name = "Meditation",
            count = 30,
            futureFlag = false,
            cardColor = "sage_forest",
        )
        val styleSage = resolveWidgetCircleStyle(rowSage, 0)
        // Forest green badge
        assertEquals(0xFF33523D.toInt(), styleSage.circleColor)
        // Light ink on dark green
        assertEquals(0xFFFFFFFF.toInt(), styleSage.textInk)

        val rowGold = WidgetRowData(
            id = "2",
            name = "Anniversary",
            count = 365,
            futureFlag = false,
            cardColor = "ink_gold",
        )
        val styleGold = resolveWidgetCircleStyle(rowGold, 0)
        // Ochre gold badge
        assertEquals(0xFFDEB285.toInt(), styleGold.circleColor)
        // Dark ink on ochre gold
        assertEquals(0xFF2C2416.toInt(), styleGold.textInk)
    }

    @Test
    fun `daysSince calculation correctly produces day counts for past and future hero items`() {
        val today = LocalDate.of(2026, 8, 31)
        val pastDate = LocalDate.of(2026, 8, 1) // 30 days ago
        val futureDate = LocalDate.of(2026, 9, 7) // 7 days in future

        assertEquals(30L, daysSince(pastDate, today))
        assertEquals(-7L, daysSince(futureDate, today))
    }

    @Test
    fun `splitTitleFor1x1 correctly divides multi-word habit names for zero truncation display`() {
        // Single word -> line1 = word, line2 = ""
        val (s1, s2) = splitTitleFor1x1("Meditation")
        assertEquals("MEDITATION", s1)
        assertEquals("", s2)

        // Two words -> line1 = first word, line2 = second word
        val (w1, w2) = splitTitleFor1x1("Sobriety Streak")
        assertEquals("SOBRIETY", w1)
        assertEquals("STREAK", w2)

        // Three words -> line1 = first 2 words, line2 = 3rd word
        val (t1, t2) = splitTitleFor1x1("Daily Morning Meditation")
        assertEquals("DAILY MORNING", t1)
        assertEquals("MEDITATION", t2)

        // Four words -> balanced 2 and 2
        val (f1, f2) = splitTitleFor1x1("Studying For Bar Exam")
        assertEquals("STUDYING FOR", f1)
        assertEquals("BAR EXAM", f2)

        // Empty string
        val (e1, e2) = splitTitleFor1x1("   ")
        assertEquals("", e1)
        assertEquals("", e2)
    }
}

package com.countup.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.time.LocalDate

class ZenHorizonLayoutTest {

    private lateinit var tempDir: File
    private lateinit var testContext: TestContext
    private val today = LocalDate.of(2026, 9, 6)

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("zen_horizon_test").toFile()
        testContext = TestContext(tempDir)
    }

    @Test
    fun trackRendererHandlesProgressGracefullyWithoutExceptions() {
        // Test varying progress fractions and theme modes
        for (progress in listOf(0.0f, 0.25f, 0.5f, 0.72f, 1.0f, -0.1f, 1.5f)) {
            for (isDark in listOf(false, true)) {
                val result = ZenHorizonTrackRenderer.renderTrack(
                    progress = progress,
                    isDark = isDark,
                    widthPx = 280,
                    heightPx = 28,
                )
                // In an environment with native graphics, verifies dimensions
                if (result != null) {
                    assertEquals(280, result.width)
                    assertEquals(28, result.height)
                }
            }
        }
    }

    @Test
    fun storeManagesZenHorizonBindingsAndUnitPersistently() {
        val store = CountUpStore(testContext)
        val testWidgetId = 8822

        // Initially null binding and default DAYS unit
        assertNull(store.getZenHorizonBinding(testWidgetId))
        assertEquals(ZenWidgetDisplayUnit.DAYS, store.getZenHorizonUnit(testWidgetId))

        // Set binding and unit
        assertTrue(store.setZenHorizonBinding(testWidgetId, "item-zen-100"))
        assertTrue(store.setZenHorizonUnit(testWidgetId, ZenWidgetDisplayUnit.MONTHS))

        // Fresh store instance reads back persisted values
        val freshStore = CountUpStore(testContext)
        assertEquals("item-zen-100", freshStore.getZenHorizonBinding(testWidgetId))
        assertEquals(ZenWidgetDisplayUnit.MONTHS, freshStore.getZenHorizonUnit(testWidgetId))

        // Remove binding clears both
        assertTrue(freshStore.removeZenHorizonBinding(testWidgetId))
        val storeAfterRemoval = CountUpStore(testContext)
        assertNull(storeAfterRemoval.getZenHorizonBinding(testWidgetId))
        assertEquals(ZenWidgetDisplayUnit.DAYS, storeAfterRemoval.getZenHorizonUnit(testWidgetId))
    }

    @Test
    fun targetItemResolverRespectsBindingAndFallbacks() {
        val items = listOf(
            CountUpItem(id = "item-1", name = "First Visible", epochDay = 0L, showInWidget = true),
            CountUpItem(id = "item-2", name = "Pinned Item", epochDay = 0L, pinnedTimestamp = 1000L, showInWidget = true),
            CountUpItem(id = "item-3", name = "Bound Item", epochDay = 0L, showInWidget = false),
        )

        // 1. Explicit bound item takes priority
        assertEquals("item-3", ZenWidgetReducer.resolveTargetItem(items, "item-3")?.id)

        // 2. Fallback when binding does not exist must be null / Deleted (never silently replace)
        assertNull(ZenWidgetReducer.resolveTargetItem(items, "non-existent"))
        assertTrue(ZenWidgetReducer.resolveTarget(items, "non-existent") is WidgetTargetResolution.Deleted)
        assertEquals("item-2", ZenWidgetReducer.resolveTargetItem(items, null)?.id)

        // 3. Fallback when no pinned item exists
        val unpinned = listOf(items[0], items[2])
        assertEquals("item-1", ZenWidgetReducer.resolveTargetItem(unpinned, null)?.id)

        // 4. Empty list returns null safely
        assertNull(ZenWidgetReducer.resolveTargetItem(emptyList(), null))
    }

    @Test
    fun zenWidgetReducerComputesMilestoneProgressAccuratelyForHorizon() {
        val item = CountUpItem(
            id = "item-sobriety",
            name = "Sobriety",
            epochDay = today.minusDays(72).toEpochDay(),
        )

        // Count = 72 days: prev milestone = 50, next = 100, range = 50, elapsed = 22 -> 22/50 = 0.44f
        val state = ZenWidgetReducer.resolveZenWidgetState(
            item = item,
            today = today,
            unit = ZenWidgetDisplayUnit.DAYS,
            isDarkMode = false,
        )

        assertEquals("Sobriety", state.title)
        assertEquals(72L, state.daysCount)
        assertEquals("72", state.primaryValueText)
        assertEquals("DAYS", state.unitLabelText)
        assertEquals(100L, state.milestoneGoal)
        assertEquals(28L, state.milestoneRemainingDays)
        assertEquals(0.44f, state.milestoneProgress, 0.001f)

        // Hairline token assertion
        assertEquals(0xFFE3D3B8.toInt(), state.palette.hairline)
        assertEquals(0xFFDEB285.toInt(), state.palette.accentGold)

        // Dark mode palette assertion
        val stateDark = ZenWidgetReducer.resolveZenWidgetState(
            item = item,
            today = today,
            unit = ZenWidgetDisplayUnit.WEEKS,
            isDarkMode = true,
        )
        assertEquals(0xFF3A3D35.toInt(), stateDark.palette.hairline)
        assertEquals(0xFFDEB285.toInt(), stateDark.palette.accentGold)
        assertEquals("WEEKS", stateDark.unitLabelText)
    }

    @Test
    fun zenHorizonActionConstantAndLayoutsAreConfigured() {
        assertEquals("com.countup.app.ACTION_CYCLE_ZEN_HORIZON_UNIT", WidgetNavigationContract.ACTION_CYCLE_ZEN_HORIZON_UNIT)
        assertTrue(R.layout.widget_zen_horizon_4x1 != 0)
        assertTrue(R.layout.widget_zen_horizon_2x1 != 0)
    }
}

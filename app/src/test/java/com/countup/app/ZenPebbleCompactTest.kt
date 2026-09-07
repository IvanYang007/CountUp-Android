package com.countup.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.time.LocalDate

class ZenPebbleCompactTest {

    private lateinit var tempDir: File
    private lateinit var testContext: TestContext
    private val today = LocalDate.of(2026, 9, 6)

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("zen_pebble_test").toFile()
        testContext = TestContext(tempDir)
    }

    @Test
    fun compactNumeralFormattingEliminatesClippingInSmallCells() {
        // Under 1,000: exact numeral
        assertEquals("0", ZenWidgetReducer.formatCompactNumber(0))
        assertEquals("42", ZenWidgetReducer.formatCompactNumber(42))
        assertEquals("412", ZenWidgetReducer.formatCompactNumber(412))
        assertEquals("999", ZenWidgetReducer.formatCompactNumber(999))

        // 1,000 to 9,999: 1 decimal place with 'k'
        assertEquals("1.0k", ZenWidgetReducer.formatCompactNumber(1000))
        assertEquals("1.2k", ZenWidgetReducer.formatCompactNumber(1200))
        assertEquals("9.5k", ZenWidgetReducer.formatCompactNumber(9500))

        // 10,000+: compact whole thousands with 'k'
        assertEquals("10k", ZenWidgetReducer.formatCompactNumber(10000))
        assertEquals("12k", ZenWidgetReducer.formatCompactNumber(12500))
        assertEquals("100k", ZenWidgetReducer.formatCompactNumber(100000))

        // Negative numbers (future countdowns)
        assertEquals("-5", ZenWidgetReducer.formatCompactNumber(-5))
        assertEquals("-1.2k", ZenWidgetReducer.formatCompactNumber(-1200))
    }

    @Test
    fun oneWordLabelResolvesAppropriatePebbleTag() {
        // Priority 1: Explicit custom tag
        val item1 = CountUpItem(id = "1", name = "Morning Meditation", epochDay = 0L, comment = "habit")
        assertEquals("ZEN", ZenWidgetReducer.resolveOneWordLabel(item1, "zen"))
        assertEquals("FOCUS", ZenWidgetReducer.resolveOneWordLabel(item1, "focus"))

        // Priority 2: Single word comment (<= 8 chars)
        val item2 = CountUpItem(id = "2", name = "Sober Living", epochDay = 0L, comment = "clean")
        assertEquals("CLEAN", ZenWidgetReducer.resolveOneWordLabel(item2, null))

        // Priority 3: First word of name
        val item3 = CountUpItem(id = "3", name = "Study Japanese", epochDay = 0L, comment = "daily kanji review")
        assertEquals("STUDY", ZenWidgetReducer.resolveOneWordLabel(item3, null))

        // Truncation: max 8 characters
        val item4 = CountUpItem(id = "4", name = "Supercalifragilistic", epochDay = 0L)
        val tag = ZenWidgetReducer.resolveOneWordLabel(item4, null)
        assertTrue(tag.length <= 8)
        assertEquals("SUPERCAL", tag)
    }

    @Test
    fun storePersistsZenPebbleBindingAndCustomTag() {
        val store = CountUpStore(testContext)
        val appWidgetId = 3322

        assertNull(store.getZenPebbleBinding(appWidgetId))
        assertNull(store.getZenPebbleTag(appWidgetId))

        store.setZenPebbleBinding(appWidgetId, "item-pebble-1")
        store.setZenPebbleTag(appWidgetId, "PEACE")

        assertEquals("item-pebble-1", store.getZenPebbleBinding(appWidgetId))
        assertEquals("PEACE", store.getZenPebbleTag(appWidgetId))

        store.removeZenPebbleBinding(appWidgetId)
        assertNull(store.getZenPebbleBinding(appWidgetId))
        assertNull(store.getZenPebbleTag(appWidgetId))
    }

    @Test
    fun zenPebbleStylingAndColorsFollowZenTokens() {
        val item = CountUpItem(id = "item-pebble", name = "Hydration", epochDay = today.minusDays(42).toEpochDay())

        // Verify state resolution for Zen Pebble
        val state = ZenWidgetReducer.resolveZenWidgetState(
            item = item,
            today = today,
            unit = ZenWidgetDisplayUnit.DAYS,
            isDarkMode = false,
        )
        assertEquals("42", state.compactValueText)
        assertEquals(WidgetThemeTokens.LIGHT_CANVAS_BG, state.palette.canvasBg)
        assertEquals(WidgetThemeTokens.LIGHT_PRIMARY_INK, state.palette.primaryInk)

        // Verify hairline ink dash colors per spec (#6B5D4F in light, #8E8A7E in dark)
        assertEquals(0xFF6B5D4F.toInt(), WidgetThemeTokens.PEBBLE_DASH_LIGHT)
        assertEquals(0xFF8E8A7E.toInt(), WidgetThemeTokens.PEBBLE_DASH_DARK)

        val stateDark = ZenWidgetReducer.resolveZenWidgetState(
            item = item,
            today = today,
            unit = ZenWidgetDisplayUnit.DAYS,
            isDarkMode = true,
        )
        assertEquals(WidgetThemeTokens.DARK_CANVAS_BG, stateDark.palette.canvasBg)
        assertEquals(WidgetThemeTokens.DARK_PRIMARY_INK, stateDark.palette.primaryInk)
    }

    @Test
    fun zenPebbleResourceAssetsAreConfigured() {
        assertTrue(R.layout.widget_zen_pebble_1x1 != 0)
        assertTrue(R.xml.zen_pebble_widget_info != 0)
        assertTrue(R.string.zen_pebble_widget_label != 0)
        assertTrue(R.string.zen_pebble_widget_description != 0)
        assertTrue(R.string.zen_pebble_configure_title != 0)
        assertTrue(R.string.zen_pebble_configure_subtitle != 0)
    }

    @Test
    fun multipleZenPebbleWidgetsMaintainDistinctBindingsAndTags() {
        val store = CountUpStore(testContext)
        val widget1 = 101
        val widget2 = 102
        val widget3 = 103

        store.setZenPebbleBinding(widget1, "item-alpha")
        store.setZenPebbleTag(widget1, "FOCUS")

        store.setZenPebbleBinding(widget2, "item-beta")
        store.setZenPebbleTag(widget2, "CALM")

        store.setZenPebbleBinding(widget3, "item-gamma")
        store.setZenPebbleTag(widget3, "READ")

        assertEquals("item-alpha", store.getZenPebbleBinding(widget1))
        assertEquals("FOCUS", store.getZenPebbleTag(widget1))

        assertEquals("item-beta", store.getZenPebbleBinding(widget2))
        assertEquals("CALM", store.getZenPebbleTag(widget2))

        assertEquals("item-gamma", store.getZenPebbleBinding(widget3))
        assertEquals("READ", store.getZenPebbleTag(widget3))

        // Deleting widget 2 does not disrupt widget 1 or 3
        store.removeZenPebbleBinding(widget2)
        assertNull(store.getZenPebbleBinding(widget2))
        assertNull(store.getZenPebbleTag(widget2))

        assertEquals("item-alpha", store.getZenPebbleBinding(widget1))
        assertEquals("FOCUS", store.getZenPebbleTag(widget1))
        assertEquals("item-gamma", store.getZenPebbleBinding(widget3))
        assertEquals("READ", store.getZenPebbleTag(widget3))
    }

    @Test
    fun openTargetItemOpensEditorDirectly() = kotlinx.coroutines.test.runTest {
        val store = CountUpStore(testContext)
        val repo = DefaultCountUpRepository(store)
        val item1 = repo.addItem(name = "Meditation", epochDay = today.minusDays(10).toEpochDay())!!
        val item2 = repo.addItem(name = "Running", epochDay = today.minusDays(5).toEpochDay())!!

        val vm = CountUpViewModel(repo, ioDispatcher = kotlinx.coroutines.Dispatchers.Unconfined)

        vm.onEvent(CountUpUiEvent.OpenTargetItem(item2.id))
        assertTrue(vm.state.value.isEditorOpen)
        assertEquals(item2.id, vm.state.value.editorTarget?.id)

        vm.onEvent(CountUpUiEvent.CloseEditor)
        org.junit.Assert.assertFalse(vm.state.value.isEditorOpen)

        vm.onEvent(CountUpUiEvent.OpenTargetItem(item1.id))
        assertTrue(vm.state.value.isEditorOpen)
        assertEquals(item1.id, vm.state.value.editorTarget?.id)
    }
}

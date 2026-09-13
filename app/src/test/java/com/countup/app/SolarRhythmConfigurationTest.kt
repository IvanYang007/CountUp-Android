package com.countup.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class SolarRhythmConfigurationTest {

    private lateinit var tempDir: File
    private lateinit var testContext: TestContext

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("solar_rhythm_config_test").toFile()
        testContext = TestContext(tempDir)
    }

    @Test
    fun solarRhythmConfigurationActivityIsDefined() {
        val clazz = Class.forName("com.countup.app.SolarRhythmConfigureActivity")
        assertNotNull(clazz)
        assertTrue(android.app.Activity::class.java.isAssignableFrom(clazz))
    }

    @Test
    fun solarRhythmConfigurationResourcesAreConfigured() {
        assertTrue(R.xml.solar_rhythm_widget_info != 0)
        assertTrue(R.string.solar_rhythm_widget_label != 0)
        assertTrue(R.string.solar_rhythm_widget_description != 0)
        assertTrue(R.string.solar_rhythm_configure_title != 0)
        assertTrue(R.string.solar_rhythm_configure_subtitle != 0)
    }

    @Test
    fun solarRhythmWidgetXmlDeclaresConfigureActivityAndReconfigurableFeature() {
        val candidates = listOf(
            File("app/src/main/res/xml/solar_rhythm_widget_info.xml"),
            File("src/main/res/xml/solar_rhythm_widget_info.xml"),
        )
        val xmlFile = candidates.firstOrNull { it.exists() }
        assertNotNull("solar_rhythm_widget_info.xml must exist", xmlFile)
        val xmlContent = xmlFile!!.readText(Charsets.UTF_8)

        assertTrue(
            "solar_rhythm_widget_info.xml must declare android:configure",
            xmlContent.contains("android:configure=\"com.countup.app.SolarRhythmConfigureActivity\"")
        )
        assertTrue(
            "solar_rhythm_widget_info.xml must declare android:widgetFeatures=\"reconfigurable\"",
            xmlContent.contains("android:widgetFeatures=\"reconfigurable\"")
        )
    }

    @Test
    fun manifestDeclaresSolarRhythmConfigureActivityAndDeletedAction() {
        val candidates = listOf(
            File("app/src/main/AndroidManifest.xml"),
            File("src/main/AndroidManifest.xml"),
        )
        val manifestFile = candidates.firstOrNull { it.exists() }
        assertNotNull("AndroidManifest.xml must exist", manifestFile)
        val manifestContent = manifestFile!!.readText(Charsets.UTF_8)

        assertTrue(
            "AndroidManifest.xml must register SolarRhythmConfigureActivity",
            manifestContent.contains("SolarRhythmConfigureActivity")
        )
        assertTrue(
            "AndroidManifest.xml must declare APPWIDGET_DELETED for SolarRhythmWidgetReceiver",
            manifestContent.contains("android.appwidget.action.APPWIDGET_DELETED")
        )
    }

    @Test
    fun multipleSolarRhythmWidgetsMaintainDistinctBindings() {
        val store = CountUpStore(testContext)
        val widget1 = 501
        val widget2 = 502
        val widget3 = 503

        store.setSolarRhythmBinding(widget1, "item-meditation")
        store.setSolarRhythmBinding(widget2, "item-reading")
        store.setSolarRhythmBinding(widget3, "item-running")

        assertEquals("item-meditation", store.getSolarRhythmBinding(widget1))
        assertEquals("item-reading", store.getSolarRhythmBinding(widget2))
        assertEquals("item-running", store.getSolarRhythmBinding(widget3))

        // Deleting widget 2 does not disrupt widget 1 or 3
        store.removeSolarRhythmBinding(widget2)
        assertNull(store.getSolarRhythmBinding(widget2))

        assertEquals("item-meditation", store.getSolarRhythmBinding(widget1))
        assertEquals("item-running", store.getSolarRhythmBinding(widget3))
    }

    @Test
    fun resolveTargetItemPrefersBoundItemOverPinnedOrFirstItem() {
        val item1 = CountUpItem(id = "1", name = "First Item", epochDay = 0L)
        val item2 = CountUpItem(id = "2", name = "Pinned Item", epochDay = 0L, pinnedTimestamp = 100L)
        val item3 = CountUpItem(id = "3", name = "Bound Item", epochDay = 0L)

        val items = listOf(item1, item2, item3)

        // With explicit binding
        val resolvedWithBinding = ZenWidgetReducer.resolveTargetItem(items, boundItemId = "3")
        assertNotNull(resolvedWithBinding)
        assertEquals("3", resolvedWithBinding!!.id)
        assertEquals("Bound Item", resolvedWithBinding.name)

        // Without binding (falls back to pinned item)
        val resolvedWithoutBinding = ZenWidgetReducer.resolveTargetItem(items, boundItemId = null)
        assertNotNull(resolvedWithoutBinding)
        assertEquals("2", resolvedWithoutBinding!!.id)
        assertEquals("Pinned Item", resolvedWithoutBinding.name)
    }
}

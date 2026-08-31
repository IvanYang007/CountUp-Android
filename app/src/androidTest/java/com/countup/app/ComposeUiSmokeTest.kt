package com.countup.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Compose UI smoke tests for [CountUpContent] and [ItemCard].
 * Validates rendering, accessibility semantics, and state views.
 */
@RunWith(AndroidJUnit4::class)
class ComposeUiSmokeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val fixedToday = LocalDate.of(2026, 8, 29)

    private fun testItem(
        name: String = "Haircut",
        daysAgo: Long = 5,
        icon: String = "person",
        comment: String = "",
        futureFlag: Boolean = false,
    ) = CountUpItem(
        id = "test-${name.lowercase()}",
        name = name,
        epochDay = fixedToday.minusDays(daysAgo).toEpochDay(),
        icon = icon,
        comment = comment,
        futureFlag = futureFlag,
    )

    @Test
    fun itemCardRendersNameAndCount() {
        val item = testItem(name = "Haircut", daysAgo = 5)
        composeTestRule.setContent {
            ZenTheme {
                ItemCard(item, onClick = {}, onDelete = {}, onReset = {}, onToggleWidget = {}, today = fixedToday)
            }
        }
        composeTestRule.onNodeWithText("HAIRCUT").assertIsDisplayed()
        composeTestRule.onNodeWithText("5", substring = true).assertIsDisplayed()
    }

    @Test
    fun resetButtonHasAccessibilityLabel() {
        composeTestRule.setContent {
            ZenTheme {
                ItemCard(testItem(), onClick = {}, onDelete = {}, onReset = {}, onToggleWidget = {}, today = fixedToday)
            }
        }
        composeTestRule.onNodeWithContentDescription("Reset").assertIsDisplayed()
    }

    @Test
    fun deleteButtonHasAccessibilityLabel() {
        composeTestRule.setContent {
            ZenTheme {
                ItemCard(testItem(), onClick = {}, onDelete = {}, onReset = {}, onToggleWidget = {}, today = fixedToday)
            }
        }
        composeTestRule.onNodeWithContentDescription("Delete").assertIsDisplayed()
    }

    @Test
    fun eyeButtonHasAccessibilityLabel() {
        composeTestRule.setContent {
            ZenTheme {
                ItemCard(testItem(), onClick = {}, onDelete = {}, onReset = {}, onToggleWidget = {}, today = fixedToday)
            }
        }
        composeTestRule.onNodeWithContentDescription("Hide from widget").assertIsDisplayed()
    }

    @Test
    fun itemCardRendersCommentWhenPresent() {
        val item = testItem(name = "Gym", comment = "Felt great today")
        composeTestRule.setContent {
            ZenTheme {
                ItemCard(item, onClick = {}, onDelete = {}, onReset = {}, onToggleWidget = {}, today = fixedToday)
            }
        }
        composeTestRule.onNodeWithText("Felt great today").assertIsDisplayed()
    }

    @Test
    fun countUpContentRendersEmptyStateWhenNoItems() {
        composeTestRule.setContent {
            ZenTheme {
                CountUpContent(
                    state = CountUpUiState(items = emptyList(), today = fixedToday),
                    onEvent = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Nothing here yet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add your first count-up to start tracking days.").assertIsDisplayed()
    }

    @Test
    fun countUpContentRendersItemsAndSubHeader() {
        val items = listOf(testItem("Meditation", 10), testItem("Reading", 2))
        composeTestRule.setContent {
            ZenTheme {
                CountUpContent(
                    state = CountUpUiState(items = items, today = fixedToday),
                    onEvent = {},
                )
            }
        }
        composeTestRule.onNodeWithText("MEDITATION").assertIsDisplayed()
        composeTestRule.onNodeWithText("READING").assertIsDisplayed()
        composeTestRule.onNodeWithText("2 items").assertIsDisplayed()
    }

    @Test
    fun countUpContentRendersEmptySearchWhenNoMatch() {
        val items = listOf(testItem("Meditation", 10))
        composeTestRule.setContent {
            ZenTheme {
                CountUpContent(
                    state = CountUpUiState(items = items, searchQuery = "nonexistent_query", today = fixedToday),
                    onEvent = {},
                )
            }
        }
        composeTestRule.onNodeWithText("No matching habits found").assertIsDisplayed()
        composeTestRule.onNodeWithText("Clear search").assertIsDisplayed()
    }

    @Test
    fun futureItemRendersUntilLabel() {
        val futureItem = testItem("Tokyo Trip", daysAgo = -10, futureFlag = true)
        composeTestRule.setContent {
            ZenTheme {
                ItemCard(futureItem, onClick = {}, onDelete = {}, onReset = {}, onToggleWidget = {}, today = fixedToday)
            }
        }
        composeTestRule.onNodeWithText("TOKYO TRIP").assertIsDisplayed()
        composeTestRule.onNodeWithText("UNTIL", substring = true).assertIsDisplayed()
    }

    @Test
    fun itemCardRendersCustomSolidCardColorAndPhosphorIcon() {
        val customItem = CountUpItem(
            id = "custom-spa",
            name = "Zen Retreat",
            epochDay = fixedToday.minusDays(12).toEpochDay(),
            icon = "spa",
            cardColor = "willow_sage",
            comment = "Deep breath",
        )
        composeTestRule.setContent {
            ZenTheme {
                ItemCard(customItem, onClick = {}, onDelete = {}, onReset = {}, onToggleWidget = {}, today = fixedToday)
            }
        }
        composeTestRule.onNodeWithText("ZEN RETREAT").assertIsDisplayed()
        composeTestRule.onNodeWithText("12", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Deep breath").assertIsDisplayed()
    }

    @Test
    fun itemEditorDialogRendersSwatchesAndIconCategories() {
        composeTestRule.setContent {
            ZenTheme {
                ItemEditorDialog(
                    item = null,
                    today = fixedToday,
                    onDismiss = {},
                    onSave = { _ -> },
                )
            }
        }
        composeTestRule.onNodeWithText("NEW ITEM").assertIsDisplayed()
        composeTestRule.onNodeWithText("Customize style & icon", substring = true).assertIsDisplayed()

        // Expand customization
        composeTestRule.onNodeWithText("Customize style & icon", substring = true).performClick()
        composeTestRule.onNodeWithText("CARD STYLE").assertIsDisplayed()
        composeTestRule.onNodeWithText("ICON").assertIsDisplayed()
        composeTestRule.onNodeWithText("All").assertIsDisplayed()
        composeTestRule.onNodeWithText("Health").assertIsDisplayed()
        composeTestRule.onNodeWithText("Habits").assertIsDisplayed()
    }

    @Test
    fun seedSampleItemsForVisualDemonstration() {
        val targetContext = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val store = CountUpStore(targetContext)
        // Clear old
        store.items().forEach { store.deleteItem(it.id) }
        // Add sample items: mix of default cards ("") and custom presets
        store.addItem("Morning Zazen", fixedToday.minusDays(18).toEpochDay(), comment = "20m meditation", icon = "spa", cardColor = "")
        store.addItem("Reading Habit", fixedToday.minusDays(32).toEpochDay(), comment = "30 pages daily", icon = "book", cardColor = "")
        store.addItem("Hydration Streak", fixedToday.minusDays(8).toEpochDay(), comment = "2.5L daily", icon = "water_drop", cardColor = "")
        store.addItem("Smoke Free", fixedToday.minusDays(195).toEpochDay(), comment = "Cold turkey streak", icon = "smoke_free", cardColor = "paper_terracotta")
        store.addItem("Baby Oliver Born", fixedToday.minusDays(145).toEpochDay(), comment = "Our greatest blessing", icon = "child_care", cardColor = "ink_crimson")
        store.addItem("App Milestone", fixedToday.minusDays(95).toEpochDay(), comment = "v1.4 live", icon = "rocket_launch", cardColor = "ink_gold")
        pushWidgetUpdate(targetContext)
    }

    @Test
    fun pinWidgetToHomeScreen() {
        val targetContext = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val manager = android.appwidget.AppWidgetManager.getInstance(targetContext)
        val provider = android.content.ComponentName(targetContext, CountUpWidgetReceiver::class.java)
        if (manager.isRequestPinAppWidgetSupported) {
            manager.requestPinAppWidget(provider, null, null)
        }
    }

    @Test
    fun captureWidgetCardPreview() {
        val targetContext = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val store = CountUpStore(targetContext)
        store.items().forEach { store.deleteItem(it.id) }
        store.addItem("Morning Zazen", fixedToday.minusDays(18).toEpochDay(), comment = "20m meditation", icon = "spa", cardColor = "")
        store.addItem("Reading Habit", fixedToday.minusDays(32).toEpochDay(), comment = "30 pages daily", icon = "book", cardColor = "")
        store.addItem("Hydration Streak", fixedToday.minusDays(8).toEpochDay(), comment = "2.5L daily", icon = "water_drop", cardColor = "")
        store.addItem("Smoke Free", fixedToday.minusDays(195).toEpochDay(), comment = "Cold turkey streak", icon = "smoke_free", cardColor = "paper_terracotta")
        store.addItem("Baby Oliver Born", fixedToday.minusDays(145).toEpochDay(), comment = "Our greatest blessing", icon = "child_care", cardColor = "ink_crimson")
        store.addItem("App Milestone", fixedToday.minusDays(95).toEpochDay(), comment = "v1.4 live", icon = "rocket_launch", cardColor = "ink_gold")

        val factory = WidgetViewsFactory(targetContext)
        factory.onDataSetChanged()

        val rootLayout = android.widget.LinearLayout(targetContext).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.parseColor("#EAE0D3"))
            setPadding(32, 32, 32, 32)
        }
        val header = android.widget.TextView(targetContext).apply {
            text = "COUNTUP WIDGET (3 COLUMNS)"
            textSize = 12f
            setTextColor(android.graphics.Color.parseColor("#7A746B"))
            setTypeface(android.graphics.Typeface.DEFAULT_BOLD)
            setPadding(0, 0, 0, 24)
        }
        rootLayout.addView(header)

        val gridLayout = android.widget.GridLayout(targetContext).apply {
            columnCount = 3
            useDefaultMargins = true
            alignmentMode = android.widget.GridLayout.ALIGN_BOUNDS
        }
        for (i in 0 until factory.count) {
            val remoteCell = factory.getViewAt(i)
            val cellView = remoteCell.apply(targetContext, gridLayout)
            val lp = android.widget.GridLayout.LayoutParams().apply {
                width = 280
                height = android.widget.GridLayout.LayoutParams.WRAP_CONTENT
                setMargins(8, 12, 8, 12)
            }
            gridLayout.addView(cellView, lp)
        }
        rootLayout.addView(gridLayout)

        val width = 960
        rootLayout.measure(
            android.view.View.MeasureSpec.makeMeasureSpec(width, android.view.View.MeasureSpec.EXACTLY),
            android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
        )
        val measuredHeight = rootLayout.measuredHeight
        rootLayout.layout(0, 0, width, measuredHeight)

        val bitmap = android.graphics.Bitmap.createBitmap(width, measuredHeight, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        rootLayout.draw(canvas)

        val file = java.io.File(targetContext.filesDir, "widget_preview.png")
        java.io.FileOutputStream(file).use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
        }
        val b64 = android.util.Base64.encodeToString(file.readBytes(), android.util.Base64.NO_WRAP)
        val chunkSize = 2000
        for (i in 0 until b64.length step chunkSize) {
            val end = (i + chunkSize).coerceAtMost(b64.length)
            android.util.Log.e("WIDGET_BASE64", "CHUNK:" + b64.substring(i, end))
        }
        android.util.Log.e("WIDGET_BASE64", "CHUNK_DONE")
    }
}

package com.countup.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
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
        composeTestRule.onNodeWithText("No items yet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tap + to add what you count.").assertIsDisplayed()
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
        composeTestRule.onNodeWithText("No matching habits").assertIsDisplayed()
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
}

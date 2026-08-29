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
 * Compose UI smoke tests for [ItemCard]. Validates rendering and accessibility
 * labels without needing a full activity launch.
 */
@RunWith(AndroidJUnit4::class)
class ComposeUiSmokeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun testItem(
        name: String = "Haircut",
        daysAgo: Long = 5,
        icon: String = "person",
    ) = CountUpItem(
        id = "test-${name.lowercase()}",
        name = name,
        epochDay = LocalDate.now().minusDays(daysAgo).toEpochDay(),
        icon = icon,
    )

    @Test
    fun itemCardRendersNameAndCount() {
        val item = testItem(name = "Haircut", daysAgo = 5)
        composeTestRule.setContent {
            ItemCard(item, onClick = {}, onDelete = {}, onReset = {}, onToggleWidget = {})
        }
        composeTestRule.onNodeWithText("HAIRCUT").assertIsDisplayed()
        composeTestRule.onNodeWithText("5", substring = true).assertIsDisplayed()
    }

    @Test
    fun resetButtonHasAccessibilityLabel() {
        composeTestRule.setContent {
            ItemCard(testItem(), onClick = {}, onDelete = {}, onReset = {}, onToggleWidget = {})
        }
        composeTestRule.onNodeWithContentDescription("Reset").assertIsDisplayed()
    }

    @Test
    fun deleteButtonHasAccessibilityLabel() {
        composeTestRule.setContent {
            ItemCard(testItem(), onClick = {}, onDelete = {}, onReset = {}, onToggleWidget = {})
        }
        composeTestRule.onNodeWithContentDescription("Delete").assertIsDisplayed()
    }

    @Test
    fun eyeButtonHasAccessibilityLabel() {
        composeTestRule.setContent {
            ItemCard(testItem(), onClick = {}, onDelete = {}, onReset = {}, onToggleWidget = {})
        }
        composeTestRule.onNodeWithContentDescription("Hide from widget").assertIsDisplayed()
    }

    @Test
    fun itemCardRendersCommentWhenPresent() {
        val item = testItem(name = "Gym").copy(comment = "Felt great today")
        composeTestRule.setContent {
            ItemCard(item, onClick = {}, onDelete = {}, onReset = {}, onToggleWidget = {})
        }
        composeTestRule.onNodeWithText("Felt great today").assertIsDisplayed()
    }
}

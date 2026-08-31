package com.countup.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemIconsTest {

    @Test
    fun `iconRes maps each name in ALL_ICON_NAMES to a valid drawable resource id`() {
        ALL_ICON_NAMES.forEach { name ->
            val resourceId = iconRes(name)
            assertTrue("Invalid resource ID for icon name: $name", resourceId > 0)
        }
    }

    @Test
    fun `unknown or empty name falls back to the person icon`() {
        val personIconId = R.drawable.ic_person
        
        assertEquals(personIconId, iconRes("unknown_icon_name"))
        assertEquals(personIconId, iconRes(""))
        assertEquals(personIconId, iconRes("not_in_list"))
    }

    @Test
    fun `every ALL_ICON_NAMES entry maps to a non-default icon except person`() {
        val defaultIconId = R.drawable.ic_person
        
        ALL_ICON_NAMES.forEach { name ->
            val resourceId = iconRes(name)
            if (name == DEFAULT_ICON) {
                assertEquals("Person icon should map to default", defaultIconId, resourceId)
            } else {
                assertNotEquals("Icon '$name' should not map to default person icon", defaultIconId, resourceId)
            }
        }
    }

    @Test
    fun `all categories contain at least 6 icons and have valid label resources`() {
        assertTrue(ICON_CATEGORIES.isNotEmpty())
        ICON_CATEGORIES.forEach { category ->
            assertTrue("Category label resource should be valid", category.labelRes > 0)
            assertTrue("Category '${category.id}' should have at least 6 icons", category.iconNames.size >= 6)
            category.iconNames.forEach { iconName ->
                val res = iconRes(iconName)
                assertTrue("Category '${category.id}' icon '$iconName' must resolve to a valid drawable", res > 0)
            }
        }
    }

    @Test
    fun `iconDescriptionRes maps all icons to valid string resources and falls back to person`() {
        ALL_ICON_NAMES.forEach { name ->
            val stringResId = iconDescriptionRes(name)
            assertTrue("Icon description resource ID should be valid for $name", stringResId > 0)
        }
        assertEquals(R.string.cd_icon_person, iconDescriptionRes("unknown_nonexistent"))
        assertEquals(R.string.cd_icon_person, iconDescriptionRes(""))
    }
}
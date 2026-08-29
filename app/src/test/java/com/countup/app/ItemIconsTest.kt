package com.countup.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ItemIconsTest {

    @Test
    fun `iconRes maps each name in SOCIAL_ICON_NAMES to a valid drawable resource id`() {
        SOCIAL_ICON_NAMES.forEach { name ->
            val resourceId = iconRes(name)
            // Verify it's a positive resource ID (non-zero)
            assert(resourceId > 0) { "Invalid resource ID for icon name: $name" }
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
    fun `every SOCIAL_ICON_NAMES entry maps to a non-default icon except person`() {
        val defaultIconId = R.drawable.ic_person
        
        SOCIAL_ICON_NAMES.forEach { name ->
            val resourceId = iconRes(name)
            if (name == DEFAULT_ICON) {
                // "person" should map to the default icon
                assertEquals("Person icon should map to default", defaultIconId, resourceId)
            } else {
                // All other names should map to distinct icons (not the default)
                assertNotEquals("Icon '$name' should not map to default person icon", defaultIconId, resourceId)
            }
        }
    }
}
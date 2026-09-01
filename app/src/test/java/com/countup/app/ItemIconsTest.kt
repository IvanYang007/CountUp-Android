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

    @Test
    fun `matchKeywordStyle correctly maps English and Chinese keywords to curated icon and color pairs`() {
        // English keywords
        val haircut = matchKeywordStyle("Haircut appointment")
        assertEquals(KeywordStyleMatch("content_cut", ""), haircut)

        val meditation = matchKeywordStyle("Daily Meditation Streak")
        assertEquals(KeywordStyleMatch("self_improvement", "sage_forest"), meditation)

        val smoke = matchKeywordStyle("Smoke Free Since Jan")
        assertEquals(KeywordStyleMatch("smoke_free", "paper_terracotta"), smoke)

        val oil = matchKeywordStyle("Car Oil Service")
        assertEquals(KeywordStyleMatch("directions_car", "ink_gold"), oil)

        val reading = matchKeywordStyle("Read 30 pages a day")
        assertEquals(KeywordStyleMatch("book", "paper_indigo"), reading)

        val gym = matchKeywordStyle("Morning Gym Routine")
        assertEquals(KeywordStyleMatch("fitness_center", "ink_crimson"), gym)

        val water = matchKeywordStyle("Drink 2.5L Water")
        assertEquals(KeywordStyleMatch("water_drop", "paper_indigo"), water)

        val sleep = matchKeywordStyle("   Sleep by 10pm   ")
        assertEquals(KeywordStyleMatch("bedtime", "sage_forest"), sleep)

        // New concrete habit & life keywords (English)
        val dental = matchKeywordStyle("Visit Dentist")
        assertEquals(KeywordStyleMatch("tooth", "paper_indigo"), dental)

        val pill = matchKeywordStyle("Daily Vitamin D")
        assertEquals(KeywordStyleMatch("pill", "paper_terracotta"), pill)

        val guitar = matchKeywordStyle("Acoustic Guitar Practice")
        assertEquals(KeywordStyleMatch("guitar", "paper_terracotta"), guitar)

        val bike = matchKeywordStyle("Bike commute")
        assertEquals(KeywordStyleMatch("bicycle", "paper_terracotta"), bike)

        val laundry = matchKeywordStyle("Do Laundry")
        assertEquals(KeywordStyleMatch("washing_machine", "paper_indigo"), laundry)

        val mountain = matchKeywordStyle("Mountain hiking trip")
        assertEquals(KeywordStyleMatch("mountains", "sage_forest"), mountain)

        val savings = matchKeywordStyle("Save money for vacation")
        assertEquals(KeywordStyleMatch("piggy_bank", "ink_gold"), savings)

        // Chinese keywords
        val hairZh = matchKeywordStyle("上次剪发")
        assertEquals(KeywordStyleMatch("content_cut", ""), hairZh)

        val zazenZh = matchKeywordStyle("晨间冥想")
        assertEquals(KeywordStyleMatch("self_improvement", "sage_forest"), zazenZh)

        val quitSmokingZh = matchKeywordStyle("戒烟天数")
        assertEquals(KeywordStyleMatch("smoke_free", "paper_terracotta"), quitSmokingZh)

        val toothZh = matchKeywordStyle("早起刷牙")
        assertEquals(KeywordStyleMatch("tooth", "paper_indigo"), toothZh)

        val guitarZh = matchKeywordStyle("练琴吉他")
        assertEquals(KeywordStyleMatch("guitar", "paper_terracotta"), guitarZh)

        val bikeZh = matchKeywordStyle("周末骑车")
        assertEquals(KeywordStyleMatch("bicycle", "paper_terracotta"), bikeZh)

        val laundryZh = matchKeywordStyle("洗衣服")
        assertEquals(KeywordStyleMatch("washing_machine", "paper_indigo"), laundryZh)

        val mountainZh = matchKeywordStyle("周末爬山")
        assertEquals(KeywordStyleMatch("mountains", "sage_forest"), mountainZh)

        val saveZh = matchKeywordStyle("每月存钱")
        assertEquals(KeywordStyleMatch("piggy_bank", "ink_gold"), saveZh)

        // Unmatched / Blank
        org.junit.Assert.assertNull(matchKeywordStyle("Unmatched Random Custom Habit 12345"))
        org.junit.Assert.assertNull(matchKeywordStyle(""))
        org.junit.Assert.assertNull(matchKeywordStyle("   "))
    }
}
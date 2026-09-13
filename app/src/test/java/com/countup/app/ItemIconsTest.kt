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
        assertEquals(KeywordStyleMatch("book", ""), reading)

        val gym = matchKeywordStyle("Morning Gym Routine")
        assertEquals(KeywordStyleMatch("fitness_center", "ink_crimson"), gym)

        val water = matchKeywordStyle("Drink 2.5L Water")
        assertEquals(KeywordStyleMatch("water_drop", "paper_indigo"), water)

        val sleep = matchKeywordStyle("   Sleep by 10pm   ")
        assertEquals(KeywordStyleMatch("bedtime", "sage_forest"), sleep)

        // New concrete habit & life keywords (English)
        val dental = matchKeywordStyle("Visit Dentist")
        assertEquals(KeywordStyleMatch("tooth", "paper_sage"), dental)

        val pill = matchKeywordStyle("Daily Vitamin D")
        assertEquals(KeywordStyleMatch("pill", "paper_terracotta"), pill)

        val guitar = matchKeywordStyle("Acoustic Guitar Practice")
        assertEquals(KeywordStyleMatch("guitar", "paper_terracotta"), guitar)

        val bike = matchKeywordStyle("Bike commute")
        assertEquals(KeywordStyleMatch("bicycle", "paper_terracotta"), bike)

        val laundry = matchKeywordStyle("Do Laundry")
        assertEquals(KeywordStyleMatch("washing_machine", "paper_sage"), laundry)

        val mountain = matchKeywordStyle("Mountain hiking trip")
        assertEquals(KeywordStyleMatch("mountains", "sage_forest"), mountain)

        val savings = matchKeywordStyle("Save money for vacation")
        assertEquals(KeywordStyleMatch("piggy_bank", "ink_gold"), savings)

        val wedding = matchKeywordStyle("Wedding Anniversary")
        assertEquals(KeywordStyleMatch("wedding_ring", "paper_terracotta"), wedding)

        val fasting = matchKeywordStyle("Intermittent Fasting 16:8")
        assertEquals(KeywordStyleMatch("fasting_plate", "paper_sage"), fasting)

        val detox = matchKeywordStyle("Digital detox weekend")
        assertEquals(KeywordStyleMatch("phone_slash", "paper_indigo"), detox)

        val cheers = matchKeywordStyle("Cheers champagne celebration")
        assertEquals(KeywordStyleMatch("champagne_flutes", "ink_gold"), cheers)

        val wellness = matchKeywordStyle("Digital wellness routine")
        assertEquals(KeywordStyleMatch("sprout_device", "sage_forest"), wellness)

        // Substring collision safety (ensuring word boundaries prevent false positive matches like 'tent' in 'intermittent')
        assertEquals(KeywordStyleMatch("restaurant", "paper_terracotta"), matchKeywordStyle("Dinner at a restaurant"))
        assertEquals(KeywordStyleMatch("flight", "paper_indigo"), matchKeywordStyle("Summer vacation in Paris"))
        assertEquals(KeywordStyleMatch("skincare_dropper", "paper_sage"), matchKeywordStyle("Skincare routine"))
        assertEquals(KeywordStyleMatch("heartbeat", "ink_crimson"), matchKeywordStyle("Cardio heart rate"))
        assertEquals(KeywordStyleMatch("game_controller", "ink_gold"), matchKeywordStyle("Xbox gaming session"))
        assertEquals(KeywordStyleMatch("tent", "sage_forest"), matchKeywordStyle("Camp in a tent"))

        // New Pillar assertions
        assertEquals(KeywordStyleMatch("period_moon", "ink_crimson"), matchKeywordStyle("Period tracker"))
        assertEquals(KeywordStyleMatch("cold_tub", "paper_indigo"), matchKeywordStyle("Cold plunge routine"))
        assertEquals(KeywordStyleMatch("sober_glass_inverted", "paper_indigo"), matchKeywordStyle("Sober anniversary"))
        assertEquals(KeywordStyleMatch("journal_ribbon", "paper_terracotta"), matchKeywordStyle("Daily journal reflection"))
        assertEquals(KeywordStyleMatch("alarm_five_am", "ink_gold"), matchKeywordStyle("5 am club"))
        assertEquals(KeywordStyleMatch("translate_bubbles", "paper_indigo"), matchKeywordStyle("Duolingo streak"))
        assertEquals(KeywordStyleMatch("watering_can", "sage_forest"), matchKeywordStyle("Watering houseplants"))
        assertEquals(KeywordStyleMatch("pomodoro_timer", "ink_crimson"), matchKeywordStyle("Pomodoro focus"))
        assertEquals(KeywordStyleMatch("house_keyhole", "ink_gold"), matchKeywordStyle("Moving into new house"))
        assertEquals(KeywordStyleMatch("mortarboard_cap", "ink_gold"), matchKeywordStyle("College graduation day"))
        assertEquals(KeywordStyleMatch("tax_form", "paper_sage"), matchKeywordStyle("Annual tax return"))
        assertEquals(KeywordStyleMatch("briefcase_star", "ink_gold"), matchKeywordStyle("Promotion anniversary"))
        assertEquals(KeywordStyleMatch("twin_candles", "paper_terracotta"), matchKeywordStyle("Romantic date night"))
        assertEquals(KeywordStyleMatch("stage_mic", "ink_crimson"), matchKeywordStyle("Rock concert tonight"))
        assertEquals(KeywordStyleMatch("camper_van", "ink_gold"), matchKeywordStyle("Summer road trip"))
        assertEquals(KeywordStyleMatch("beach_chair", "sage_forest"), matchKeywordStyle("Early FIRE retirement"))

        // Chinese keywords
        val hairZh = matchKeywordStyle("上次剪发")
        assertEquals(KeywordStyleMatch("content_cut", ""), hairZh)

        val weddingZh = matchKeywordStyle("结婚纪念日")
        assertEquals(KeywordStyleMatch("wedding_ring", "paper_terracotta"), weddingZh)

        val fastingZh = matchKeywordStyle("轻断食打卡")
        assertEquals(KeywordStyleMatch("fasting_plate", "paper_sage"), fastingZh)

        val detoxZh = matchKeywordStyle("戒手机记录")
        assertEquals(KeywordStyleMatch("phone_slash", "paper_indigo"), detoxZh)

        val zazenZh = matchKeywordStyle("晨间冥想")
        assertEquals(KeywordStyleMatch("self_improvement", "sage_forest"), zazenZh)

        val quitSmokingZh = matchKeywordStyle("戒烟天数")
        assertEquals(KeywordStyleMatch("smoke_free", "paper_terracotta"), quitSmokingZh)

        val toothZh = matchKeywordStyle("早起刷牙")
        assertEquals(KeywordStyleMatch("tooth", "paper_sage"), toothZh)

        val guitarZh = matchKeywordStyle("练琴吉他")
        assertEquals(KeywordStyleMatch("guitar", "paper_terracotta"), guitarZh)

        val bikeZh = matchKeywordStyle("周末骑车")
        assertEquals(KeywordStyleMatch("bicycle", "paper_terracotta"), bikeZh)

        val laundryZh = matchKeywordStyle("洗衣服")
        assertEquals(KeywordStyleMatch("washing_machine", "paper_sage"), laundryZh)

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
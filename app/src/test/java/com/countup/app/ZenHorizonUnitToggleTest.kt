package com.countup.app

import org.junit.Assert.assertEquals
import org.junit.Test

class ZenHorizonUnitToggleTest {

    @Test
    fun `zen horizon action constant is properly declared`() {
        assertEquals(
            "com.countup.app.ACTION_CYCLE_ZEN_HORIZON_UNIT",
            HeroWidgetReceiver.ACTION_CYCLE_ZEN_HORIZON_UNIT
        )
    }

    @Test
    fun `zen horizon unit cycles through all five perspectives and wraps around`() {
        var unit = ZenWidgetDisplayUnit.DAYS
        unit = unit.next()
        assertEquals(ZenWidgetDisplayUnit.MONTHS, unit)

        unit = unit.next()
        assertEquals(ZenWidgetDisplayUnit.WEEKS, unit)

        unit = unit.next()
        assertEquals(ZenWidgetDisplayUnit.HOURS, unit)

        unit = unit.next()
        assertEquals(ZenWidgetDisplayUnit.YEARS, unit)

        unit = unit.next()
        assertEquals(ZenWidgetDisplayUnit.DAYS, unit)
    }
}


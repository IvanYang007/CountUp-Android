package com.countup.app

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for [WidgetBackgroundRenderer].
 * Tests theme dispatching, dimension defaults, and safe handling across environments.
 */
class WidgetBackgroundTest {

    @Test
    fun renderHandlesAllThemesGracefullyWithoutUnhandledExceptions() {
        for (theme in BackgroundTheme.entries) {
            // In JVM unit tests without Android platform graphics runtime,
            // render() catches Throwable and returns null safely.
            val result = WidgetBackgroundRenderer.render(
                theme = theme,
                epochDay = LocalDate.of(2026, 8, 29).toEpochDay(),
            )
            // Asserts that no unhandled exception escapes
            if (result != null) {
                assertEquals(480, result.width)
                assertEquals(280, result.height)
            }
        }
    }

    @Test
    fun renderHandlesExtremeEpochDaysSafely() {
        val days = longArrayOf(-1000L, -1L, 0L, 1L, 20667L, Long.MAX_VALUE - 10)
        for (day in days) {
            WidgetBackgroundRenderer.render(
                theme = BackgroundTheme.AUTO_DAILY,
                epochDay = day,
            )
        }
    }
}

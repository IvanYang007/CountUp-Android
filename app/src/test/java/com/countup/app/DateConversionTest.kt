package com.countup.app

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DateConversionTest {

    @Test
    fun `datePickerMillisToLocalDate with known UTC millis`() {
        // Test epoch 0 (January 1, 1970)
        val epoch0 = 0L
        val expectedEpoch = LocalDate.of(1970, 1, 1)
        assertEquals(expectedEpoch, datePickerMillisToLocalDate(epoch0))
        
        // Test a mid-year date (July 15, 2024 UTC)
        val july152024 = LocalDate.of(2024, 7, 15)
        val july15Millis = july152024.atStartOfDay().atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        assertEquals(july152024, datePickerMillisToLocalDate(july15Millis))
        
        // Test year boundary (December 31, 2023 to January 1, 2024)
        val dec312023 = LocalDate.of(2023, 12, 31)
        val dec31Millis = dec312023.atStartOfDay().atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        assertEquals(dec312023, datePickerMillisToLocalDate(dec31Millis))
        
        val jan012024 = LocalDate.of(2024, 1, 1)
        val jan01Millis = jan012024.atStartOfDay().atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        assertEquals(jan012024, datePickerMillisToLocalDate(jan01Millis))
    }
}
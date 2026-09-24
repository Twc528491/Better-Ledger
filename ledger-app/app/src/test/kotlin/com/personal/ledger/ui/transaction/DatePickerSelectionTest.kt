package com.personal.ledger.ui.transaction

import java.time.LocalDateTime
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class DatePickerSelectionTest {
    @Test fun `negative offset date picker preserves requested local day and time`() {
        val zone = ZoneId.of("America/Los_Angeles")
        val old = LocalDateTime.of(2026, 9, 24, 18, 37).atZone(zone).toInstant().toEpochMilli()
        val initial = DatePickerSelection.initialUtcMillis(old, zone)
        assertEquals("2026-09-24", java.time.Instant.ofEpochMilli(initial)
            .atZone(java.time.ZoneOffset.UTC).toLocalDate().toString())
        val picked = java.time.LocalDate.of(2026, 10, 3)
            .atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        val result = DatePickerSelection.withPickedDate(old, picked, zone)
        assertEquals(LocalDateTime.of(2026, 10, 3, 18, 37),
            java.time.Instant.ofEpochMilli(result).atZone(zone).toLocalDateTime())
    }

    @Test fun `wheel date keeps transaction local time and clamps leap day at month end`() {
        val zone = ZoneId.of("Asia/Shanghai")
        val old = LocalDateTime.of(2024, 2, 29, 9, 41).atZone(zone).toInstant().toEpochMilli()
        assertEquals(29, DatePickerSelection.monthLength(2024, 2))
        assertEquals(28, DatePickerSelection.clampDay(29, 2025, 2))
        assertEquals(30, DatePickerSelection.clampDay(31, 2026, 4))
        val selected = LocalDate.of(2025, 2, DatePickerSelection.clampDay(29, 2025, 2))
        assertEquals(LocalDateTime.of(2025, 2, 28, 9, 41),
            java.time.Instant.ofEpochMilli(DatePickerSelection.withPickedDate(old, selected, zone))
                .atZone(zone).toLocalDateTime())
    }
}

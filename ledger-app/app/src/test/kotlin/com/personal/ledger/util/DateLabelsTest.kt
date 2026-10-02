package com.personal.ledger.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

class DateLabelsTest {

    private val zh = Locale.SIMPLIFIED_CHINESE
    private val en = Locale.ENGLISH

    @Test
    fun chineseLabels() {
        assertEquals("2026年9月", DateLabels.yearMonth(YearMonth.of(2026, 9), zh))
        assertEquals("9月27日", DateLabels.monthDay(LocalDate.of(2026, 9, 27), zh))
        assertEquals("2026年9月27日", DateLabels.fullDate(LocalDate.of(2026, 9, 27), zh))
        assertEquals("周日", DateLabels.weekday(DayOfWeek.SUNDAY, zh))
        assertEquals("12月", DateLabels.month(12, zh))
    }

    @Test
    fun englishLabels() {
        assertEquals("Sep 2026", DateLabels.yearMonth(YearMonth.of(2026, 9), en))
        assertEquals("Sep 27", DateLabels.monthDay(LocalDate.of(2026, 9, 27), en))
        assertEquals("Sep 27, 2026", DateLabels.fullDate(LocalDate.of(2026, 9, 27), en))
        assertEquals("Sun", DateLabels.weekday(DayOfWeek.SUNDAY, en))
        assertEquals("Dec", DateLabels.month(12, en))
    }
}

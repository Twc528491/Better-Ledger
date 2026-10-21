package io.github.twc528491.betterledger.ui.transaction

import io.github.twc528491.betterledger.util.DateLabels
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.YearMonth
import java.util.Locale

/** Material DatePicker exchanges UTC-midnight dates, not local instants. */
internal object DatePickerSelection {
    fun initialUtcMillis(transactionTime: Long, zone: ZoneId): Long =
        Instant.ofEpochMilli(transactionTime).atZone(zone).toLocalDate()
            .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    fun withPickedDate(transactionTime: Long, pickedUtcMillis: Long, zone: ZoneId): Long {
        val date = Instant.ofEpochMilli(pickedUtcMillis).atZone(ZoneOffset.UTC).toLocalDate()
        return withPickedDate(transactionTime, date, zone)
    }

    fun withPickedDate(transactionTime: Long, date: LocalDate, zone: ZoneId): Long {
        val time = Instant.ofEpochMilli(transactionTime).atZone(zone).toLocalTime()
        return date.atTime(time).atZone(zone).toInstant().toEpochMilli()
    }

    /** 滚轮顶部的完整日期：中文“2026年9月27日 周日”，英文“Sun, Sep 27, 2026”。 */
    fun longLabel(date: LocalDate, locale: Locale = Locale.getDefault()): String =
        if (locale.language == Locale.CHINESE.language) {
            DateLabels.fullDate(date, locale) + " " + DateLabels.weekday(date.dayOfWeek, locale)
        } else {
            DateLabels.weekday(date.dayOfWeek, locale) + ", " + DateLabels.fullDate(date, locale)
        }

    fun monthLength(year: Int, month: Int): Int = YearMonth.of(year, month).lengthOfMonth()

    fun clampDay(day: Int, year: Int, month: Int): Int =
        day.coerceIn(1, monthLength(year, month))
}

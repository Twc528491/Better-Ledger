package com.personal.ledger.ui.transaction

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.YearMonth

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

    private val weekdayLabels = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

    /** 滚轮顶部的完整日期，如“2026年9月27日 周日”。 */
    fun longLabel(date: LocalDate): String =
        "${date.year}年${date.monthValue}月${date.dayOfMonth}日 ${weekdayLabels[date.dayOfWeek.value - 1]}"

    fun monthLength(year: Int, month: Int): Int = YearMonth.of(year, month).lengthOfMonth()

    fun clampDay(day: Int, year: Int, month: Int): Int =
        day.coerceIn(1, monthLength(year, month))
}

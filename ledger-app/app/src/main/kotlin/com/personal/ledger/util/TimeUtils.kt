package com.personal.ledger.util

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

object TimeUtils {

    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun now(): Long = System.currentTimeMillis()

    fun monthKey(yearMonth: YearMonth): String = yearMonth.toString() // yyyy-MM

    fun currentMonthKey(): String = monthKey(YearMonth.now())

    /** 当月起点毫秒（含）。 */
    fun monthStartMs(yearMonth: YearMonth): Long =
        yearMonth.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()

    /** 下月起点毫秒（不含，即当月结束边界）。 */
    fun monthEndMs(yearMonth: YearMonth): Long =
        yearMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()

    fun localDateOf(epochMs: Long): LocalDate =
        Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()

    /** 显示用日期，如 "9月2日"。 */
    fun formatMonthDay(epochMs: Long): String {
        val d = localDateOf(epochMs)
        return "${d.monthValue}月${d.dayOfMonth}日"
    }
}

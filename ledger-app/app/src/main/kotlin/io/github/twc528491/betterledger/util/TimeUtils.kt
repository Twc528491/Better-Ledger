package io.github.twc528491.betterledger.util

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale

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

    /** 显示用日期：中文 "9月2日"，英文 "Sep 2"。Compose 中传 `LocalConfiguration.current.locales[0]`。 */
    fun formatMonthDay(epochMs: Long, locale: Locale = Locale.getDefault()): String =
        DateLabels.monthDay(localDateOf(epochMs), locale)
}

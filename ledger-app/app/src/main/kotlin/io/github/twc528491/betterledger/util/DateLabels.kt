package io.github.twc528491.betterledger.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * 按语言格式化日期文本（纯函数，可 JVM 测试）。
 * 中文："2026年9月" / "9月27日" / "周日"；英文："Sep 2026" / "Sep 27" / "Sun"。
 * Compose 中用 `LocalConfiguration.current.locales[0]` 取当前语言。
 */
object DateLabels {

    private fun isChinese(locale: Locale) = locale.language == Locale.CHINESE.language

    fun yearMonth(ym: YearMonth, locale: Locale): String =
        if (isChinese(locale)) "${ym.year}年${ym.monthValue}月"
        else DateTimeFormatter.ofPattern("MMM yyyy", locale).format(ym)

    /** 月份短名：中文 "9月"，英文 "Sep"。 */
    fun month(monthValue: Int, locale: Locale): String =
        if (isChinese(locale)) "${monthValue}月"
        else java.time.Month.of(monthValue).getDisplayName(TextStyle.SHORT, locale)

    fun monthDay(date: LocalDate, locale: Locale): String =
        if (isChinese(locale)) "${date.monthValue}月${date.dayOfMonth}日"
        else DateTimeFormatter.ofPattern("MMM d", locale).format(date)

    fun fullDate(date: LocalDate, locale: Locale): String =
        if (isChinese(locale)) "${date.year}年${date.monthValue}月${date.dayOfMonth}日"
        else DateTimeFormatter.ofPattern("MMM d, yyyy", locale).format(date)

    /** 星期短名：中文 "周日"，英文 "Sun"。 */
    fun weekday(day: DayOfWeek, locale: Locale): String =
        if (isChinese(locale)) "周" + "一二三四五六日"[day.value - 1]
        else day.getDisplayName(TextStyle.SHORT, locale)
}

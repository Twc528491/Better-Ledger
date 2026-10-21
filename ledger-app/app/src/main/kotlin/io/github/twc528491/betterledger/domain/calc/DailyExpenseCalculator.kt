package io.github.twc528491.betterledger.domain.calc

import io.github.twc528491.betterledger.domain.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** A day's total expense in the selected calendar month. */
data class DailyExpenseTotal(
    val day: Int,
    val expense: Long,
)

/**
 * 本月与上月按“几号”对齐的累计支出对比（统计页「每日支出」）。
 *
 * @property days 本月每天的支出（含 0），长度 = 本月天数
 * @property cumulative 与 [days] 对齐的累计支出
 * @property previousCumulative 上月每天的累计支出，长度 = 上月天数
 * @property elapsedDays 需要绘制的天数：过去月份为整月，本月截至今天，未来日期不绘制
 */
data class DailyExpenseComparison(
    val month: YearMonth,
    val days: List<DailyExpenseTotal>,
    val cumulative: List<Long>,
    val previousCumulative: List<Long>,
    val elapsedDays: Int,
) {
    val total: Long get() = cumulative.lastOrNull() ?: 0L

    val previousTotal: Long get() = previousCumulative.lastOrNull() ?: 0L

    /** 日均支出：按已过天数平均，四舍五入到分。 */
    val dailyAverage: Long
        get() = if (elapsedDays <= 0) 0L else (total + elapsedDays / 2) / elapsedDays

    /** 最高单日；本月无支出时为 null。并列时取较早的一天。 */
    val peak: DailyExpenseTotal?
        get() = days.take(elapsedDays).filter { it.expense > 0L }.maxByOrNull { it.expense }

    /** 截至 [day] 号的本月累计。 */
    fun cumulativeAt(day: Int): Long =
        if (cumulative.isEmpty() || day <= 0) 0L else cumulative[day.coerceAtMost(cumulative.size) - 1]

    /** 上月同期（同为截至 [day] 号；上月较短时取上月最后一天）的累计。 */
    fun previousAt(day: Int): Long =
        if (previousCumulative.isEmpty() || day <= 0) 0L
        else previousCumulative[day.coerceAtMost(previousCumulative.size) - 1]

    /** 截至 [day] 号与上月同期的差额，正数表示花得更多。 */
    fun deltaAt(day: Int): Long = cumulativeAt(day) - previousAt(day)
}

/** Pure daily expense aggregation used by the statistics screen. */
object DailyExpenseCalculator {
    data class TxInput(
        val type: TransactionType,
        val amount: Long,
        val time: Long,
    )

    /** Returns every day in [month], including zero-value days, in calendar order. */
    fun forMonth(
        transactions: List<TxInput>,
        month: YearMonth,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<DailyExpenseTotal> {
        val totals = LongArray(month.lengthOfMonth())
        transactions.forEach { tx ->
            if (tx.type != TransactionType.EXPENSE || tx.amount <= 0L) return@forEach
            val date = Instant.ofEpochMilli(tx.time).atZone(zone).toLocalDate()
            if (YearMonth.from(date) == month) {
                totals[date.dayOfMonth - 1] += tx.amount
            }
        }
        return totals.mapIndexed { index, amount -> DailyExpenseTotal(index + 1, amount) }
    }

    /**
     * 计算 [month] 与上一个月的逐日累计支出。
     * [transactions] 可以包含任意时间段，函数只取这两个月的支出。
     * 本月已记录的“未来日期”支出仍会被绘制，避免与概览总额不一致。
     */
    fun compareWithPrevious(
        transactions: List<TxInput>,
        month: YearMonth,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
    ): DailyExpenseComparison {
        val days = forMonth(transactions, month, zone)
        val previousDays = forMonth(transactions, month.minusMonths(1), zone)
        val lastRecordedDay = days.lastOrNull { it.expense > 0L }?.day ?: 0
        val currentMonth = YearMonth.from(today)
        val elapsed = when {
            month < currentMonth -> month.lengthOfMonth()
            month == currentMonth -> maxOf(today.dayOfMonth, lastRecordedDay)
            else -> lastRecordedDay
        }
        return DailyExpenseComparison(
            month = month,
            days = days,
            cumulative = runningTotal(days),
            previousCumulative = runningTotal(previousDays),
            elapsedDays = elapsed,
        )
    }

    private fun runningTotal(days: List<DailyExpenseTotal>): List<Long> {
        var sum = 0L
        return days.map { sum += it.expense; sum }
    }
}

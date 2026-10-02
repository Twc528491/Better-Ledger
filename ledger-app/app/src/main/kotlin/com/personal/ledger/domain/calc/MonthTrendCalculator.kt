package com.personal.ledger.domain.calc

import com.personal.ledger.domain.model.TransactionType
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

/**
 * 纯函数：月度趋势聚合（MVP.md 第 8 节"月度趋势"）。
 * 转账不计入收入/支出。
 */
object MonthTrendCalculator {

    /** 聚合输入：单笔交易的最小字段集。 */
    data class TxInput(
        val type: TransactionType,
        val amount: Long,
        val time: Long,
    )

    data class MonthTotals(
        val yearMonth: YearMonth,
        val income: Long,
        val expense: Long,
    )

    /**
     * 计算截至 end 月（含）最近 count 个月的收入/支出，按月升序返回。
     */
    fun lastMonths(
        transactions: List<TxInput>,
        end: YearMonth,
        count: Int,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<MonthTotals> {
        val months = (count - 1 downTo 0).map { end.minusMonths(it.toLong()) }
        val totals = HashMap<YearMonth, Pair<Long, Long>>()
        months.forEach { totals[it] = 0L to 0L }

        transactions.forEach { tx ->
            val ym = YearMonth.from(Instant.ofEpochMilli(tx.time).atZone(zone).toLocalDate())
            val current = totals[ym] ?: return@forEach
            when (tx.type) {
                TransactionType.INCOME -> totals[ym] = (current.first + tx.amount) to current.second
                TransactionType.EXPENSE -> totals[ym] = current.first to (current.second + tx.amount)
                TransactionType.TRANSFER -> Unit
            }
        }
        return months.map { ym -> MonthTotals(ym, totals[ym]!!.first, totals[ym]!!.second) }
    }

    /**
     * 月均支出：只统计有收支记录的月份，避免开始记账前的空月份拉低均值。
     * 四舍五入到分；全部为空时返回 0。
     */
    fun averageExpense(months: List<MonthTotals>): Long {
        val active = months.filter { it.income != 0L || it.expense != 0L }
        if (active.isEmpty()) return 0L
        val sum = active.sumOf { it.expense }
        return (sum + active.size / 2) / active.size
    }
}

package io.github.twc528491.betterledger.domain.calc

import io.github.twc528491.betterledger.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime

class MonthTrendCalculatorTest {

    private val zone = ZoneId.of("Asia/Shanghai")

    private fun at(year: Int, month: Int, day: Int): Long =
        ZonedDateTime.of(year, month, day, 12, 0, 0, 0, zone).toInstant().toEpochMilli()

    @Test
    fun `按月聚合最近 N 个月收入支出`() {
        val txs = listOf(
            MonthTrendCalculator.TxInput(TransactionType.EXPENSE, 1000, at(2026, 9, 1)),
            MonthTrendCalculator.TxInput(TransactionType.EXPENSE, 500, at(2026, 9, 2)),
            MonthTrendCalculator.TxInput(TransactionType.INCOME, 3000, at(2026, 9, 3)),
            MonthTrendCalculator.TxInput(TransactionType.EXPENSE, 200, at(2026, 8, 15)),
            MonthTrendCalculator.TxInput(TransactionType.INCOME, 5000, at(2026, 7, 1)),
            MonthTrendCalculator.TxInput(TransactionType.EXPENSE, 999, at(2026, 6, 1)), // 窗口外
        )
        val result = MonthTrendCalculator.lastMonths(txs, YearMonth.of(2026, 9), 3, zone)
        assertEquals(3, result.size)
        assertEquals(YearMonth.of(2026, 7), result[0].yearMonth)
        assertEquals(5000L, result[0].income)
        assertEquals(0L, result[0].expense)
        assertEquals(YearMonth.of(2026, 8), result[1].yearMonth)
        assertEquals(0L, result[1].income)
        assertEquals(200L, result[1].expense)
        assertEquals(YearMonth.of(2026, 9), result[2].yearMonth)
        assertEquals(3000L, result[2].income)
        assertEquals(1500L, result[2].expense)
    }

    @Test
    fun `转账不计入收入支出`() {
        val txs = listOf(
            MonthTrendCalculator.TxInput(TransactionType.TRANSFER, 10000, at(2026, 9, 1)),
            MonthTrendCalculator.TxInput(TransactionType.EXPENSE, 100, at(2026, 9, 2)),
        )
        val result = MonthTrendCalculator.lastMonths(txs, YearMonth.of(2026, 9), 1, zone)
        assertEquals(0L, result[0].income)
        assertEquals(100L, result[0].expense)
    }

    @Test
    fun `无交易月份填零`() {
        val result = MonthTrendCalculator.lastMonths(emptyList(), YearMonth.of(2026, 9), 6, zone)
        assertEquals(6, result.size)
        result.forEach { m ->
            assertEquals(0L, m.income)
            assertEquals(0L, m.expense)
        }
    }

    @Test
    fun `月均支出只统计有记录的月份`() {
        val months = listOf(
            MonthTrendCalculator.MonthTotals(YearMonth.of(2026, 6), 0, 0),
            MonthTrendCalculator.MonthTotals(YearMonth.of(2026, 7), 5000, 0),
            MonthTrendCalculator.MonthTotals(YearMonth.of(2026, 8), 0, 301),
            MonthTrendCalculator.MonthTotals(YearMonth.of(2026, 9), 100, 600),
        )
        assertEquals(300L, MonthTrendCalculator.averageExpense(months))
        assertEquals(0L, MonthTrendCalculator.averageExpense(months.take(1)))
        assertEquals(0L, MonthTrendCalculator.averageExpense(emptyList()))
    }
}

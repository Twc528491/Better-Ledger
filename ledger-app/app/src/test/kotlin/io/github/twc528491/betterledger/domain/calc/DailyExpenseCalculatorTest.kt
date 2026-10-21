package io.github.twc528491.betterledger.domain.calc

import io.github.twc528491.betterledger.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class DailyExpenseCalculatorTest {
    private val utc = ZoneId.of("UTC")

    @Test
    fun `month totals include zero days and ignore income transfers and out of month entries`() {
        val transactions = listOf(
            DailyExpenseCalculator.TxInput(TransactionType.EXPENSE, 3_000_000_000L, at("2026-09-02")),
            DailyExpenseCalculator.TxInput(TransactionType.EXPENSE, 750, at("2026-09-02")),
            DailyExpenseCalculator.TxInput(TransactionType.INCOME, 9000, at("2026-09-02")),
            DailyExpenseCalculator.TxInput(TransactionType.TRANSFER, 5000, at("2026-09-03")),
            DailyExpenseCalculator.TxInput(TransactionType.EXPENSE, 2000, at("2026-08-31")),
            DailyExpenseCalculator.TxInput(TransactionType.EXPENSE, 3000, at("2026-10-01")),
        )
        val result = DailyExpenseCalculator.forMonth(transactions, YearMonth.of(2026, 9), utc)
        assertEquals(30, result.size)
        assertEquals(DailyExpenseTotal(1, 0), result.first())
        assertEquals(DailyExpenseTotal(2, 3_000_000_750L), result[1])
        assertEquals(0L, result[2].expense)
        assertEquals(DailyExpenseTotal(30, 0), result.last())
    }

    @Test
    fun `empty month returns all calendar days with zero totals`() {
        val result = DailyExpenseCalculator.forMonth(emptyList(), YearMonth.of(2024, 2), utc)
        assertEquals(29, result.size)
        assertEquals((1..29).toList(), result.map { it.day })
        assertEquals(0L, result.sumOf { it.expense })
    }

    @Test
    fun `transactions use the supplied zone at month boundaries`() {
        val shanghai = ZoneId.of("Asia/Shanghai")
        val transactions = listOf(
            DailyExpenseCalculator.TxInput(
                TransactionType.EXPENSE,
                200,
                Instant.parse("2026-08-31T16:00:00Z").toEpochMilli(),
            ),
            DailyExpenseCalculator.TxInput(
                TransactionType.EXPENSE,
                300,
                Instant.parse("2026-09-30T15:59:59Z").toEpochMilli(),
            ),
            DailyExpenseCalculator.TxInput(
                TransactionType.EXPENSE,
                400,
                Instant.parse("2026-09-30T16:00:00Z").toEpochMilli(),
            ),
            DailyExpenseCalculator.TxInput(
                TransactionType.EXPENSE,
                500,
                Instant.parse("2026-08-31T15:59:59Z").toEpochMilli(),
            ),
        )

        val result = DailyExpenseCalculator.forMonth(transactions, YearMonth.of(2026, 9), shanghai)

        assertEquals(30, result.size)
        assertEquals(200L, result.first().expense)
        assertEquals(300L, result.last().expense)
        assertEquals(500L, result.sumOf { it.expense })
    }

    @Test
    fun `cumulative comparison aligns days with previous month and stops at today`() {
        val transactions = listOf(
            DailyExpenseCalculator.TxInput(TransactionType.EXPENSE, 1000, at("2026-03-01")),
            DailyExpenseCalculator.TxInput(TransactionType.EXPENSE, 500, at("2026-03-03")),
            DailyExpenseCalculator.TxInput(TransactionType.INCOME, 9000, at("2026-03-03")),
            DailyExpenseCalculator.TxInput(TransactionType.EXPENSE, 200, at("2026-02-01")),
            DailyExpenseCalculator.TxInput(TransactionType.EXPENSE, 300, at("2026-02-28")),
            DailyExpenseCalculator.TxInput(TransactionType.EXPENSE, 999, at("2026-01-31")),
        )
        val result = DailyExpenseCalculator.compareWithPrevious(
            transactions,
            YearMonth.of(2026, 3),
            today = LocalDate.parse("2026-03-05"),
            zone = utc,
        )
        assertEquals(31, result.cumulative.size)
        assertEquals(28, result.previousCumulative.size)
        assertEquals(5, result.elapsedDays)
        assertEquals(listOf(1000L, 1000L, 1500L), result.cumulative.take(3))
        assertEquals(1500L, result.total)
        assertEquals(500L, result.previousTotal)
        assertEquals(300L, result.dailyAverage)
        assertEquals(DailyExpenseTotal(1, 1000), result.peak)
        assertEquals(1300L, result.deltaAt(3))
        // 上月只有 28 天：31 号与上月最后一天比较。
        assertEquals(500L, result.previousAt(31))
        assertEquals(1000L, result.deltaAt(31))
    }

    @Test
    fun `past months draw every day and future months draw nothing without records`() {
        val past = DailyExpenseCalculator.compareWithPrevious(
            emptyList(),
            YearMonth.of(2026, 8),
            today = LocalDate.parse("2026-10-02"),
            zone = utc,
        )
        assertEquals(31, past.elapsedDays)
        assertEquals(0L, past.dailyAverage)
        assertEquals(null, past.peak)

        val future = DailyExpenseCalculator.compareWithPrevious(
            emptyList(),
            YearMonth.of(2026, 11),
            today = LocalDate.parse("2026-10-02"),
            zone = utc,
        )
        assertEquals(0, future.elapsedDays)
        assertEquals(0L, future.cumulativeAt(5))
    }

    @Test
    fun `recorded future dates in current month extend the drawn range`() {
        val transactions = listOf(
            DailyExpenseCalculator.TxInput(TransactionType.EXPENSE, 700, at("2026-10-20")),
        )
        val result = DailyExpenseCalculator.compareWithPrevious(
            transactions,
            YearMonth.of(2026, 10),
            today = LocalDate.parse("2026-10-02"),
            zone = utc,
        )
        assertEquals(20, result.elapsedDays)
        assertEquals(700L, result.total)
        assertEquals(35L, result.dailyAverage)
    }

    private fun at(date: String) = LocalDate.parse(date).atStartOfDay(utc).toInstant().toEpochMilli()
}

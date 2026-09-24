package com.personal.ledger.domain.calc

import com.personal.ledger.domain.model.TransactionType
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

    private fun at(date: String) = LocalDate.parse(date).atStartOfDay(utc).toInstant().toEpochMilli()
}

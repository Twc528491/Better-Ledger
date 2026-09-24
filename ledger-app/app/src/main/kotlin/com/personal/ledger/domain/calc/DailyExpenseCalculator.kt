package com.personal.ledger.domain.calc

import com.personal.ledger.domain.model.TransactionType
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

/** A day's total expense in the selected calendar month. */
data class DailyExpenseTotal(
    val day: Int,
    val expense: Long,
)

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
}

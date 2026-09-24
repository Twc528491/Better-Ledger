package com.personal.ledger.domain.model

import java.time.LocalDate
import java.time.ZoneId

enum class RecurrenceFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
}

/** A local expense template whose occurrences are stored as ordinary transactions. */
data class RecurringCharge(
    val id: String,
    val ledgerId: String,
    val name: String,
    val amount: Long,
    val currency: String,
    val accountId: String,
    val categoryId: String,
    val frequency: RecurrenceFrequency,
    val startDate: LocalDate,
    val minuteOfDay: Int,
    val zoneId: ZoneId,
    val nextOccurrenceIndex: Long,
    val nextDueAt: Long,
    val isPaused: Boolean,
    val lastError: String?,
    val updatedAt: Long,
)

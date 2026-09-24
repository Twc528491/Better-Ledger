package com.personal.ledger.domain.calc

import com.personal.ledger.domain.model.RecurrenceFrequency
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Calendar-based recurrence math. Every date is derived from the original anchor. */
object RecurringSchedule {
    fun dueAt(
        anchorDate: LocalDate,
        minuteOfDay: Int,
        zoneId: ZoneId,
        frequency: RecurrenceFrequency,
        index: Long,
    ): Long {
        require(minuteOfDay in 0..1439)
        require(index >= 0)
        val date = occurrenceDate(anchorDate, frequency, index)
        val time = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
        // atZone handles daylight-saving gaps and overlaps using the platform zone rules.
        return LocalDateTime.of(date, time).atZone(zoneId).toInstant().toEpochMilli()
    }

    /** Finds the first index at or after [thresholdMs], starting no earlier than [minimumIndex]. */
    fun firstIndexAtOrAfter(
        anchorDate: LocalDate,
        minuteOfDay: Int,
        zoneId: ZoneId,
        frequency: RecurrenceFrequency,
        thresholdMs: Long,
        minimumIndex: Long = 0L,
    ): Long {
        require(minimumIndex >= 0)
        val threshold = Instant.ofEpochMilli(thresholdMs).atZone(zoneId)
        val days = ChronoUnit.DAYS.between(anchorDate, threshold.toLocalDate())
        val estimate = when (frequency) {
            RecurrenceFrequency.DAILY -> days
            RecurrenceFrequency.WEEKLY -> Math.floorDiv(days, 7L)
            RecurrenceFrequency.MONTHLY -> ChronoUnit.MONTHS.between(
                anchorDate.withDayOfMonth(1), threshold.toLocalDate().withDayOfMonth(1),
            )
            RecurrenceFrequency.YEARLY -> threshold.year.toLong() - anchorDate.year
        }.coerceAtLeast(minimumIndex)
        var index = estimate
        while (dueAt(anchorDate, minuteOfDay, zoneId, frequency, index) < thresholdMs) {
            index = Math.addExact(index, 1L)
        }
        while (index > minimumIndex &&
            dueAt(anchorDate, minuteOfDay, zoneId, frequency, index - 1) >= thresholdMs
        ) {
            index--
        }
        return index
    }

    fun occurrenceDate(anchorDate: LocalDate, frequency: RecurrenceFrequency, index: Long): LocalDate {
        require(index >= 0)
        return when (frequency) {
            RecurrenceFrequency.DAILY -> anchorDate.plusDays(index)
            RecurrenceFrequency.WEEKLY -> anchorDate.plusWeeks(index)
            // Always derive from the original date, preserving month-end and leap-day anchors.
            RecurrenceFrequency.MONTHLY -> anchorDate.plusMonths(index)
            RecurrenceFrequency.YEARLY -> anchorDate.plusYears(index)
        }
    }
}

package io.github.twc528491.betterledger.domain.calc

import io.github.twc528491.betterledger.domain.model.RecurrenceFrequency
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class RecurringScheduleTest {
    private val utc = ZoneId.of("UTC")

    @Test
    fun monthlyScheduleRemainsAnchoredToOriginalMonthEnd() {
        val anchor = LocalDate.of(2025, 1, 31)

        assertEquals(LocalDate.of(2025, 2, 28), RecurringSchedule.occurrenceDate(anchor, RecurrenceFrequency.MONTHLY, 1))
        assertEquals(LocalDate.of(2025, 3, 31), RecurringSchedule.occurrenceDate(anchor, RecurrenceFrequency.MONTHLY, 2))
    }

    @Test
    fun yearlyScheduleRestoresLeapDayInLaterLeapYear() {
        val anchor = LocalDate.of(2024, 2, 29)

        assertEquals(LocalDate.of(2025, 2, 28), RecurringSchedule.occurrenceDate(anchor, RecurrenceFrequency.YEARLY, 1))
        assertEquals(LocalDate.of(2028, 2, 29), RecurringSchedule.occurrenceDate(anchor, RecurrenceFrequency.YEARLY, 4))
    }

    @Test
    fun dailyAndWeeklySchedulesPreserveWallTime() {
        val anchor = LocalDate.of(2026, 9, 1)
        val daily = RecurringSchedule.dueAt(anchor, 8 * 60 + 15, utc, RecurrenceFrequency.DAILY, 1)
        val weekly = RecurringSchedule.dueAt(anchor, 8 * 60 + 15, utc, RecurrenceFrequency.WEEKLY, 1)

        assertEquals(Instant.parse("2026-09-02T08:15:00Z").toEpochMilli(), daily)
        assertEquals(Instant.parse("2026-09-08T08:15:00Z").toEpochMilli(), weekly)
    }

    @Test
    fun findsFirstMonthlyIndexAtOrAfterLocalDueTime() {
        val anchor = LocalDate.of(2025, 1, 31)
        val threshold = Instant.parse("2025-03-31T10:00:00Z").toEpochMilli()

        assertEquals(
            3L,
            RecurringSchedule.firstIndexAtOrAfter(
                anchor, 9 * 60, utc, RecurrenceFrequency.MONTHLY, threshold,
            ),
        )
    }

    @Test
    fun farFutureDailyIndexIsEstimatedDirectly() {
        val anchor = LocalDate.of(2000, 1, 1)
        val threshold = Instant.parse("2020-01-01T00:00:00Z").toEpochMilli()

        assertEquals(
            7305L,
            RecurringSchedule.firstIndexAtOrAfter(
                anchor, 0, utc, RecurrenceFrequency.DAILY, threshold,
            ),
        )
    }
}

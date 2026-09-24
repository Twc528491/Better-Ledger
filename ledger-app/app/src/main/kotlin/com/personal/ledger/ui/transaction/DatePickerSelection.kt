package com.personal.ledger.ui.transaction

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** Material DatePicker exchanges UTC-midnight dates, not local instants. */
internal object DatePickerSelection {
    fun initialUtcMillis(transactionTime: Long, zone: ZoneId): Long =
        Instant.ofEpochMilli(transactionTime).atZone(zone).toLocalDate()
            .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    fun withPickedDate(transactionTime: Long, pickedUtcMillis: Long, zone: ZoneId): Long {
        val date = Instant.ofEpochMilli(pickedUtcMillis).atZone(ZoneOffset.UTC).toLocalDate()
        val time = Instant.ofEpochMilli(transactionTime).atZone(zone).toLocalTime()
        return date.atTime(time).atZone(zone).toInstant().toEpochMilli()
    }
}

package com.personal.ledger.data.repository

import com.personal.ledger.data.local.entity.RecurringChargeEntity
import com.personal.ledger.domain.model.RecurrenceFrequency
import com.personal.ledger.domain.model.RecurringCharge
import java.time.LocalDate
import java.time.ZoneId

fun RecurringChargeEntity.toDomain() = RecurringCharge(
    id = id,
    ledgerId = ledgerId,
    name = name,
    amount = amount,
    currency = currency,
    accountId = accountId,
    categoryId = categoryId,
    frequency = RecurrenceFrequency.valueOf(frequency),
    startDate = LocalDate.parse(startDate),
    minuteOfDay = minuteOfDay,
    zoneId = ZoneId.of(zoneId),
    nextOccurrenceIndex = nextOccurrenceIndex,
    nextDueAt = nextDueAt,
    isPaused = isPaused,
    lastError = lastError,
    updatedAt = updatedAt,
)

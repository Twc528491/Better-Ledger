package io.github.twc528491.betterledger.data.repository

import io.github.twc528491.betterledger.data.local.entity.RecurringChargeEntity
import io.github.twc528491.betterledger.domain.model.RecurrenceFrequency
import io.github.twc528491.betterledger.domain.model.RecurringCharge
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

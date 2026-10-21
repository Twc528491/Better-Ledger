package io.github.twc528491.betterledger.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recurring_charge",
    foreignKeys = [
        ForeignKey(entity = LedgerEntity::class, parentColumns = ["id"], childColumns = ["ledgerId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["accountId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [
        Index("ledgerId"),
        Index("accountId"),
        Index("categoryId"),
        Index(value = ["isPaused", "nextDueAt"]),
    ],
)
data class RecurringChargeEntity(
    @PrimaryKey val id: String,
    val ledgerId: String,
    val name: String,
    val amount: Long,
    val currency: String,
    val accountId: String,
    val categoryId: String,
    val frequency: String,
    val startDate: String,
    val minuteOfDay: Int,
    val zoneId: String,
    val nextOccurrenceIndex: Long,
    val nextDueAt: Long,
    val isPaused: Boolean,
    val lastError: String?,
    val updatedAt: Long,
)

@Entity(
    tableName = "recurring_occurrence",
    primaryKeys = ["ruleId", "dueAt"],
    foreignKeys = [
        ForeignKey(
            entity = RecurringChargeEntity::class,
            parentColumns = ["id"],
            childColumns = ["ruleId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["ruleId", "dueAt"]), Index(value = ["transactionId"], unique = true)],
)
data class RecurringOccurrenceEntity(
    val ruleId: String,
    val dueAt: Long,
    val transactionId: String,
)

package io.github.twc528491.betterledger.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budget",
    foreignKeys = [
        ForeignKey(
            entity = LedgerEntity::class,
            parentColumns = ["id"],
            childColumns = ["ledgerId"],
            onDelete = ForeignKey.RESTRICT,
        )
    ],
    indices = [Index("ledgerId", "month")],
)
data class BudgetEntity(
    @PrimaryKey val id: String,
    val ledgerId: String,
    /** yyyy-MM，如 "2026-09"。 */
    val month: String,
    val categoryId: String?,
    val amount: Long,
    val currency: String,
    val createdAt: Long,
    val updatedAt: Long,
)

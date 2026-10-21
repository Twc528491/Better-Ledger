package io.github.twc528491.betterledger.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.twc528491.betterledger.domain.model.TransactionType

@Entity(
    tableName = "transaction",
    foreignKeys = [
        ForeignKey(
            entity = LedgerEntity::class,
            parentColumns = ["id"],
            childColumns = ["ledgerId"],
            onDelete = ForeignKey.RESTRICT,
        )
    ],
    indices = [
        Index("ledgerId"),
        Index("ledgerId", "transactionTime"),
        Index("ledgerId", "accountId"),
        Index("ledgerId", "categoryId"),
        Index("ledgerId", "isDeleted", "transactionTime"),
    ],
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val ledgerId: String,
    val type: TransactionType,
    val amount: Long,
    val currency: String,
    val accountId: String,
    val targetAccountId: String?,
    val categoryId: String?,
    val merchantId: String?,
    val note: String?,
    val transactionTime: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean,
)

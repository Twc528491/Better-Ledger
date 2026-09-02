package com.personal.ledger.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.personal.ledger.domain.model.AccountType

@Entity(
    tableName = "account",
    foreignKeys = [
        ForeignKey(
            entity = LedgerEntity::class,
            parentColumns = ["id"],
            childColumns = ["ledgerId"],
            onDelete = ForeignKey.RESTRICT,
        )
    ],
    indices = [Index("ledgerId")],
)
data class AccountEntity(
    @PrimaryKey val id: String,
    val ledgerId: String,
    val name: String,
    val type: AccountType,
    val currency: String,
    val initialBalance: Long,
    val initialBalanceTime: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val isArchived: Boolean,
)

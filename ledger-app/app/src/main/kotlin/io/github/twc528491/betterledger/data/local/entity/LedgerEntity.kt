package io.github.twc528491.betterledger.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "ledger")
data class LedgerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val baseCurrency: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isArchived: Boolean,
)

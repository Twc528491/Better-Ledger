package io.github.twc528491.betterledger.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "merchant",
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
data class MerchantEntity(
    @PrimaryKey val id: String,
    val ledgerId: String,
    val name: String,
    val createdAt: Long,
    val isArchived: Boolean,
)

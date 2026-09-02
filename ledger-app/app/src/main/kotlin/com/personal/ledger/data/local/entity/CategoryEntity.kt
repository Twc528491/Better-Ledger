package com.personal.ledger.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.personal.ledger.domain.model.CategoryType

@Entity(
    tableName = "category",
    foreignKeys = [
        ForeignKey(
            entity = LedgerEntity::class,
            parentColumns = ["id"],
            childColumns = ["ledgerId"],
            onDelete = ForeignKey.RESTRICT,
        )
    ],
    indices = [Index("ledgerId"), Index("ledgerId", "type")],
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    val ledgerId: String,
    val name: String,
    val type: CategoryType,
    val parentId: String?,
    val sortOrder: Int,
    val isArchived: Boolean,
)

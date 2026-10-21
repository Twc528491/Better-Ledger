package io.github.twc528491.betterledger.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.twc528491.betterledger.domain.model.CategoryType

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
    /** 用户在图标库中选择的图标 key；NULL 表示按名称自动匹配（v4 新增）。 */
    val iconKey: String? = null,
)

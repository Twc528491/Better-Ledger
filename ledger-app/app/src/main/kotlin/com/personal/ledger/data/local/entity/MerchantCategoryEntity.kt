package com.personal.ledger.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/** A merchant can belong to several top-level category libraries in the same ledger. */
@Entity(
    tableName = "merchant_category",
    primaryKeys = ["merchantId", "categoryId"],
    foreignKeys = [
        ForeignKey(
            entity = MerchantEntity::class,
            parentColumns = ["id"],
            childColumns = ["merchantId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("categoryId")],
)
data class MerchantCategoryEntity(
    val merchantId: String,
    val categoryId: String,
)

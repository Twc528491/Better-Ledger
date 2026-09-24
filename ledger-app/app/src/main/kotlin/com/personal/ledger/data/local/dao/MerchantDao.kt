package com.personal.ledger.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.personal.ledger.data.local.entity.MerchantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantDao {

    @Query("SELECT * FROM merchant WHERE ledgerId = :ledgerId ORDER BY createdAt ASC")
    fun observeByLedger(ledgerId: String): Flow<List<MerchantEntity>>

    @Query("SELECT * FROM merchant WHERE ledgerId = :ledgerId ORDER BY createdAt ASC")
    suspend fun getByLedger(ledgerId: String): List<MerchantEntity>

    @Query(
        "SELECT DISTINCT m.* FROM merchant m " +
            "JOIN merchant_category mc ON mc.merchantId = m.id " +
            "JOIN category selected ON selected.id = :categoryId AND selected.ledgerId = :ledgerId " +
            "JOIN category root ON root.id = COALESCE(selected.parentId, selected.id) " +
            "WHERE m.ledgerId = :ledgerId AND m.isArchived = 0 " +
            "AND selected.isArchived = 0 AND root.isArchived = 0 " +
            "AND root.ledgerId = :ledgerId AND root.parentId IS NULL " +
            "AND root.type = selected.type AND mc.categoryId = root.id ORDER BY m.createdAt ASC"
    )
    fun observeByCategory(ledgerId: String, categoryId: String): Flow<List<MerchantEntity>>

    @Query(
        "SELECT DISTINCT m.* FROM merchant m " +
            "JOIN merchant_category mc ON mc.merchantId = m.id " +
            "JOIN category selected ON selected.id = :categoryId AND selected.ledgerId = :ledgerId " +
            "JOIN category root ON root.id = COALESCE(selected.parentId, selected.id) " +
            "WHERE m.ledgerId = :ledgerId AND m.isArchived = 0 " +
            "AND selected.isArchived = 0 AND root.isArchived = 0 " +
            "AND root.ledgerId = :ledgerId AND root.parentId IS NULL " +
            "AND root.type = selected.type AND mc.categoryId = root.id ORDER BY m.createdAt ASC"
    )
    suspend fun getByCategory(ledgerId: String, categoryId: String): List<MerchantEntity>

    @Query("SELECT * FROM merchant WHERE id = :id")
    suspend fun getById(id: String): MerchantEntity?

    @Query("SELECT * FROM merchant")
    suspend fun getAll(): List<MerchantEntity>

    @Query("DELETE FROM merchant")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(merchant: MerchantEntity)

    @Update
    suspend fun update(merchant: MerchantEntity)
}

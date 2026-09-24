package com.personal.ledger.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.personal.ledger.data.local.entity.MerchantCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantCategoryDao {
    @Query("SELECT * FROM merchant_category")
    suspend fun getAll(): List<MerchantCategoryEntity>

    @Query("SELECT * FROM merchant_category WHERE merchantId = :merchantId")
    suspend fun getByMerchant(merchantId: String): List<MerchantCategoryEntity>

    @Query("SELECT * FROM merchant_category WHERE merchantId = :merchantId")
    fun observeByMerchant(merchantId: String): Flow<List<MerchantCategoryEntity>>

    @Query("SELECT * FROM merchant_category WHERE merchantId IN (SELECT id FROM merchant WHERE ledgerId = :ledgerId)")
    fun observeByLedger(ledgerId: String): Flow<List<MerchantCategoryEntity>>

    @Query("DELETE FROM merchant_category WHERE merchantId = :merchantId")
    suspend fun deleteByMerchant(merchantId: String)

    @Query("DELETE FROM merchant_category")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(link: MerchantCategoryEntity)
}

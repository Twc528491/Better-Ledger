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

    @Query("SELECT * FROM merchant")
    suspend fun getAll(): List<MerchantEntity>

    @Query("DELETE FROM merchant")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(merchant: MerchantEntity)

    @Update
    suspend fun update(merchant: MerchantEntity)
}

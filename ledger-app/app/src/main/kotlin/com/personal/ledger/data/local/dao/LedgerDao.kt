package com.personal.ledger.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.personal.ledger.data.local.entity.LedgerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerDao {

    @Query("SELECT * FROM ledger ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<LedgerEntity>>

    @Query("SELECT * FROM ledger ORDER BY createdAt ASC")
    suspend fun getAll(): List<LedgerEntity>

    @Query("SELECT * FROM ledger WHERE id = :id")
    suspend fun getById(id: String): LedgerEntity?

    @Query("SELECT * FROM ledger WHERE isArchived = 0 ORDER BY createdAt ASC LIMIT 1")
    suspend fun getFirstActive(): LedgerEntity?

    @Query("DELETE FROM ledger")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(ledger: LedgerEntity)

    @Update
    suspend fun update(ledger: LedgerEntity)
}

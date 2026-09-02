package com.personal.ledger.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.personal.ledger.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Query("SELECT * FROM account WHERE ledgerId = :ledgerId ORDER BY isArchived ASC, createdAt ASC")
    fun observeByLedger(ledgerId: String): Flow<List<AccountEntity>>

    @Query("SELECT * FROM account WHERE ledgerId = :ledgerId ORDER BY isArchived ASC, createdAt ASC")
    suspend fun getByLedger(ledgerId: String): List<AccountEntity>

    @Query("SELECT * FROM account WHERE id = :id")
    suspend fun getById(id: String): AccountEntity?

    @Query("SELECT * FROM account")
    suspend fun getAll(): List<AccountEntity>

    @Query("DELETE FROM account")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(account: AccountEntity)

    @Update
    suspend fun update(account: AccountEntity)
}

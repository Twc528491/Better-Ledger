package com.personal.ledger.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.personal.ledger.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

/** 账户为全局实体（v5 起不再属于某个账本）。 */
@Dao
interface AccountDao {

    @Query("SELECT * FROM account ORDER BY isArchived ASC, createdAt ASC, id ASC")
    fun observeAll(): Flow<List<AccountEntity>>

    /** 某币种的全部账户（含已归档），即该币种账本可使用的账户范围。 */
    @Query("SELECT * FROM account WHERE currency = :currency ORDER BY isArchived ASC, createdAt ASC, id ASC")
    fun observeByCurrency(currency: String): Flow<List<AccountEntity>>

    @Query("SELECT * FROM account WHERE currency = :currency ORDER BY isArchived ASC, createdAt ASC, id ASC")
    suspend fun getByCurrency(currency: String): List<AccountEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM account WHERE currency = :currency AND isArchived = 0)")
    suspend fun hasActiveWithCurrency(currency: String): Boolean

    @Query("SELECT * FROM account WHERE id = :id")
    fun observeById(id: String): Flow<AccountEntity?>

    @Query("SELECT * FROM account WHERE id = :id")
    suspend fun getById(id: String): AccountEntity?

    @Query("SELECT * FROM account ORDER BY createdAt ASC, id ASC")
    suspend fun getAll(): List<AccountEntity>

    @Query("DELETE FROM account")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(account: AccountEntity)

    @Update
    suspend fun update(account: AccountEntity)
}

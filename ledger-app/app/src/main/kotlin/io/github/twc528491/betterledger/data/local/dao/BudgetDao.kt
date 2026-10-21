package io.github.twc528491.betterledger.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.twc528491.betterledger.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budget WHERE ledgerId = :ledgerId ORDER BY month DESC")
    fun observeByLedger(ledgerId: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budget WHERE ledgerId = :ledgerId AND month = :month")
    fun observeByMonth(ledgerId: String, month: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budget WHERE ledgerId = :ledgerId AND month = :month")
    suspend fun getByMonth(ledgerId: String, month: String): List<BudgetEntity>

    @Query("SELECT * FROM budget")
    suspend fun getAll(): List<BudgetEntity>

    @Query("DELETE FROM budget")
    suspend fun deleteAll()

    /**
     * 业务唯一性：同账本同月份最多一个总预算、每分类一个分类预算。
     * 因 categoryId 可为空，由 Repository 在事务内校验。
     */
    @Query(
        "SELECT * FROM budget WHERE ledgerId = :ledgerId AND month = :month " +
            "AND (categoryId IS NULL AND :categoryId IS NULL OR categoryId = :categoryId)"
    )
    suspend fun findByMonthAndCategory(
        ledgerId: String,
        month: String,
        categoryId: String?,
    ): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(budget: BudgetEntity)

    @Update
    suspend fun update(budget: BudgetEntity)

    @Query(
        "DELETE FROM budget WHERE ledgerId = :ledgerId AND month = :month " +
            "AND (categoryId IS NULL AND :categoryId IS NULL OR categoryId = :categoryId)"
    )
    suspend fun delete(ledgerId: String, month: String, categoryId: String?)
}

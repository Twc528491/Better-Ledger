package com.personal.ledger.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.personal.ledger.data.local.entity.TransactionEntity
import com.personal.ledger.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

/** 按类型聚合的金额（最小货币单位）。 */
data class TypeSum(
    val type: TransactionType,
    val total: Long,
)

/** 按分类聚合的金额。 */
data class CategorySum(
    val categoryId: String,
    val total: Long,
)

@Dao
interface TransactionDao {

    @Query(
        "SELECT * FROM `transaction` WHERE ledgerId = :ledgerId AND isDeleted = 0 " +
            "ORDER BY transactionTime DESC LIMIT :limit"
    )
    fun observeRecent(ledgerId: String, limit: Int): Flow<List<TransactionEntity>>

    @Query(
        "SELECT * FROM `transaction` WHERE ledgerId = :ledgerId AND isDeleted = 0 " +
            "ORDER BY transactionTime DESC"
    )
    fun observeByLedger(ledgerId: String): Flow<List<TransactionEntity>>

    @Query(
        "SELECT * FROM `transaction` WHERE ledgerId = :ledgerId AND isDeleted = 0 " +
            "AND transactionTime >= :startMs AND transactionTime < :endMs " +
            "ORDER BY transactionTime DESC"
    )
    fun observeBetween(ledgerId: String, startMs: Long, endMs: Long): Flow<List<TransactionEntity>>

    @Query(
        "SELECT * FROM `transaction` WHERE ledgerId = :ledgerId AND isDeleted = 1 " +
            "ORDER BY transactionTime DESC"
    )
    fun observeRecycleBin(ledgerId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM `transaction` WHERE id = :id")
    suspend fun getById(id: String): TransactionEntity?

    @Query(
        "SELECT * FROM `transaction` WHERE ledgerId = :ledgerId " +
            "AND (accountId = :accountId OR targetAccountId = :accountId)"
    )
    suspend fun getByAccount(ledgerId: String, accountId: String): List<TransactionEntity>

    @Query(
        "SELECT type, SUM(amount) AS total FROM `transaction` " +
            "WHERE ledgerId = :ledgerId AND isDeleted = 0 AND accountId = :accountId " +
            "GROUP BY type"
    )
    suspend fun sumByTypeForAccount(ledgerId: String, accountId: String): List<TypeSum>

    @Query(
        "SELECT type, SUM(amount) AS total FROM `transaction` " +
            "WHERE ledgerId = :ledgerId AND isDeleted = 0 " +
            "AND transactionTime >= :startMs AND transactionTime < :endMs " +
            "GROUP BY type"
    )
    suspend fun sumByTypeBetween(ledgerId: String, startMs: Long, endMs: Long): List<TypeSum>

    @Query(
        "SELECT t.categoryId AS categoryId, SUM(t.amount) AS total FROM `transaction` t " +
            "WHERE t.ledgerId = :ledgerId AND t.isDeleted = 0 AND t.type = :type " +
            "AND t.categoryId IS NOT NULL " +
            "AND t.transactionTime >= :startMs AND t.transactionTime < :endMs " +
            "GROUP BY t.categoryId"
    )
    suspend fun sumByCategoryBetween(
        ledgerId: String,
        type: TransactionType,
        startMs: Long,
        endMs: Long,
    ): List<CategorySum>

    /**
     * 分类树支出（父分类预算包含所有子分类）：直接按分类聚合即可，
     * 父分类金额由 Repository 汇总其子分类得出。
     */
    @Query(
        "SELECT t.categoryId AS categoryId, SUM(t.amount) AS total FROM `transaction` t " +
            "WHERE t.ledgerId = :ledgerId AND t.isDeleted = 0 AND t.type = 'EXPENSE' " +
            "AND t.categoryId IS NOT NULL " +
            "AND t.transactionTime >= :startMs AND t.transactionTime < :endMs " +
            "GROUP BY t.categoryId"
    )
    suspend fun expenseByCategoryBetween(
        ledgerId: String,
        startMs: Long,
        endMs: Long,
    ): List<CategorySum>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transaction: TransactionEntity)

    @Query("SELECT * FROM `transaction`")
    suspend fun getAll(): List<TransactionEntity>

    @Query("DELETE FROM `transaction`")
    suspend fun deleteAll()

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("UPDATE `transaction` SET isDeleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("UPDATE `transaction` SET isDeleted = 0, updatedAt = :now WHERE id = :id")
    suspend fun restore(id: String, now: Long)

    // ---------------------------------------------------------------------
    // 多选批量操作（账单页批量删除 / 回收站批量恢复与彻底删除）。
    // 调用方负责分批（单条 SQL 的绑定参数有上限）并包在同一事务内。
    // ---------------------------------------------------------------------

    @Query("UPDATE `transaction` SET isDeleted = 1, updatedAt = :now WHERE id IN (:ids) AND isDeleted = 0")
    suspend fun softDeleteByIds(ids: List<String>, now: Long): Int

    @Query("UPDATE `transaction` SET isDeleted = 0, updatedAt = :now WHERE id IN (:ids) AND isDeleted = 1")
    suspend fun restoreByIds(ids: List<String>, now: Long): Int

    /** 给定 id 中仍在回收站里的那部分；彻底删除只作用于这些记录。 */
    @Query("SELECT id FROM `transaction` WHERE id IN (:ids) AND isDeleted = 1")
    suspend fun filterRecycledIds(ids: List<String>): List<String>

    @Query("SELECT id FROM `transaction` WHERE ledgerId = :ledgerId AND isDeleted = 1")
    suspend fun getRecycledIds(ledgerId: String): List<String>

    @Query("DELETE FROM transaction_tag WHERE transactionId IN (:ids)")
    suspend fun deleteTagLinksOf(ids: List<String>)

    @Query("DELETE FROM recurring_occurrence WHERE transactionId IN (:ids)")
    suspend fun deleteRecurringOccurrencesOf(ids: List<String>)

    @Query("DELETE FROM `transaction` WHERE id IN (:ids) AND isDeleted = 1")
    suspend fun purgeRecycledByIds(ids: List<String>): Int
}

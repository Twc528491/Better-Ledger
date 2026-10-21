package io.github.twc528491.betterledger.data.local.dao

import androidx.room.Dao
import androidx.room.Query

/**
 * 合并账户专用写操作。调用方负责在同一事务中执行，并持有
 * [io.github.twc528491.betterledger.data.local.database.RecurringDatabaseGate]。
 * 交易与定时扣款均跨全部账本处理（含回收站）。
 */
@Dao
interface AccountMergeDao {

    @Query("SELECT COUNT(*) FROM recurring_charge WHERE accountId = :accountId")
    suspend fun countRecurringForAccount(accountId: String): Int

    @Query("UPDATE `transaction` SET accountId = :toId, updatedAt = :now WHERE accountId = :fromId")
    suspend fun moveSourceAccount(fromId: String, toId: String, now: Long): Int

    @Query("UPDATE `transaction` SET targetAccountId = :toId, updatedAt = :now WHERE targetAccountId = :fromId")
    suspend fun moveTargetAccount(fromId: String, toId: String, now: Long): Int

    @Query("UPDATE recurring_charge SET accountId = :toId, updatedAt = :now WHERE accountId = :fromId")
    suspend fun moveRecurring(fromId: String, toId: String, now: Long): Int

    /** 删除给定交易（不论是否在回收站）；调用方先清理其标签关联与执行记录。 */
    @Query("DELETE FROM `transaction` WHERE id IN (:ids)")
    suspend fun deleteTransactions(ids: List<String>): Int

    @Query("DELETE FROM account WHERE id = :id")
    suspend fun deleteAccount(id: String): Int
}

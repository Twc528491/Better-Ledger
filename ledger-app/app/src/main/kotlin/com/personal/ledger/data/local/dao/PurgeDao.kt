package com.personal.ledger.data.local.dao

import androidx.room.Dao
import androidx.room.Query

/** 账本删除前的数据量统计。 */
data class LedgerPurgeCounts(
    val transactions: Int,
    /** 账户全局共享、不随账本删除；这是余额会因删除该账本账单而变化的账户数（仅计正常账单）。 */
    val affectedAccounts: Int,
    val categories: Int,
    val merchants: Int,
    val tags: Int,
    val budgets: Int,
    val recurringCharges: Int,
)

/**
 * 彻底删除（purge）专用查询。与日常读写 DAO 分开，
 * 统计口径均包含回收站中的已删除交易。调用方负责在同一事务中按外键依赖顺序执行。
 */
@Dao
interface PurgeDao {

    // ---------- 标签 ----------

    @Query("SELECT COUNT(*) FROM transaction_tag WHERE tagId = :tagId")
    suspend fun countTagLinks(tagId: String): Int

    @Query("DELETE FROM transaction_tag WHERE tagId = :tagId")
    suspend fun deleteTagLinks(tagId: String)

    @Query("DELETE FROM tag WHERE id = :id")
    suspend fun deleteTag(id: String): Int

    // ---------- 商家 ----------

    @Query("SELECT COUNT(*) FROM `transaction` WHERE merchantId = :merchantId")
    suspend fun countTransactionsWithMerchant(merchantId: String): Int

    @Query("UPDATE `transaction` SET merchantId = NULL, updatedAt = :now WHERE merchantId = :merchantId")
    suspend fun clearMerchantOnTransactions(merchantId: String, now: Long)

    @Query("DELETE FROM merchant_category WHERE merchantId = :merchantId")
    suspend fun deleteMerchantLinks(merchantId: String)

    @Query("DELETE FROM merchant WHERE id = :id")
    suspend fun deleteMerchant(id: String): Int

    // ---------- 分类 ----------

    @Query("SELECT COUNT(*) FROM `transaction` WHERE categoryId IN (:ids)")
    suspend fun countTransactionsInCategories(ids: List<String>): Int

    @Query("SELECT COUNT(*) FROM recurring_charge WHERE categoryId IN (:ids)")
    suspend fun countRecurringInCategories(ids: List<String>): Int

    @Query("SELECT COUNT(*) FROM budget WHERE categoryId IN (:ids)")
    suspend fun countBudgetsInCategories(ids: List<String>): Int

    @Query("UPDATE `transaction` SET categoryId = :toId, updatedAt = :now WHERE categoryId IN (:ids)")
    suspend fun moveTransactionsToCategory(ids: List<String>, toId: String, now: Long)

    @Query("UPDATE recurring_charge SET categoryId = :toId, updatedAt = :now WHERE categoryId IN (:ids)")
    suspend fun moveRecurringToCategory(ids: List<String>, toId: String, now: Long)

    @Query("DELETE FROM budget WHERE categoryId IN (:ids)")
    suspend fun deleteBudgetsInCategories(ids: List<String>)

    @Query("DELETE FROM merchant_category WHERE categoryId IN (:ids)")
    suspend fun deleteMerchantLinksForCategories(ids: List<String>)

    @Query("DELETE FROM category WHERE id IN (:ids)")
    suspend fun deleteCategories(ids: List<String>): Int

    // ---------- 账户（全局实体：统计与删除均跨全部账本） ----------

    @Query("SELECT COUNT(*) FROM `transaction` WHERE accountId = :accountId OR targetAccountId = :accountId")
    suspend fun countTransactionsForAccount(accountId: String): Int

    @Query(
        "SELECT COUNT(*) FROM `transaction` WHERE type = 'TRANSFER' " +
            "AND (accountId = :accountId OR targetAccountId = :accountId)"
    )
    suspend fun countTransfersForAccount(accountId: String): Int

    @Query("SELECT COUNT(*) FROM recurring_charge WHERE accountId = :accountId")
    suspend fun countRecurringForAccount(accountId: String): Int

    /** 涉及该账户的账单分布在几个账本中（含回收站）。 */
    @Query(
        "SELECT COUNT(DISTINCT ledgerId) FROM `transaction` " +
            "WHERE accountId = :accountId OR targetAccountId = :accountId"
    )
    suspend fun countLedgersForAccount(accountId: String): Int

    @Query(
        "DELETE FROM recurring_occurrence " +
            "WHERE ruleId IN (SELECT id FROM recurring_charge WHERE accountId = :accountId) " +
            "OR transactionId IN (SELECT id FROM `transaction` WHERE accountId = :accountId OR targetAccountId = :accountId)"
    )
    suspend fun deleteOccurrencesForAccount(accountId: String)

    @Query("DELETE FROM recurring_charge WHERE accountId = :accountId")
    suspend fun deleteRecurringForAccount(accountId: String)

    @Query(
        "DELETE FROM transaction_tag WHERE transactionId IN " +
            "(SELECT id FROM `transaction` WHERE accountId = :accountId OR targetAccountId = :accountId)"
    )
    suspend fun deleteTagLinksForAccount(accountId: String)

    @Query("DELETE FROM `transaction` WHERE accountId = :accountId OR targetAccountId = :accountId")
    suspend fun deleteTransactionsForAccount(accountId: String)

    @Query("DELETE FROM account WHERE id = :id")
    suspend fun deleteAccount(id: String): Int

    // ---------- 账本 ----------

    @Query(
        "SELECT " +
            "(SELECT COUNT(*) FROM `transaction` WHERE ledgerId = :ledgerId) AS transactions, " +
            "(SELECT COUNT(*) FROM account WHERE id IN (" +
            "SELECT accountId FROM `transaction` WHERE ledgerId = :ledgerId AND isDeleted = 0 " +
            "UNION SELECT targetAccountId FROM `transaction` " +
            "WHERE ledgerId = :ledgerId AND isDeleted = 0 AND targetAccountId IS NOT NULL)) AS affectedAccounts, " +
            "(SELECT COUNT(*) FROM category WHERE ledgerId = :ledgerId) AS categories, " +
            "(SELECT COUNT(*) FROM merchant WHERE ledgerId = :ledgerId) AS merchants, " +
            "(SELECT COUNT(*) FROM tag WHERE ledgerId = :ledgerId) AS tags, " +
            "(SELECT COUNT(*) FROM budget WHERE ledgerId = :ledgerId) AS budgets, " +
            "(SELECT COUNT(*) FROM recurring_charge WHERE ledgerId = :ledgerId) AS recurringCharges"
    )
    suspend fun ledgerCounts(ledgerId: String): LedgerPurgeCounts

    @Query(
        "DELETE FROM recurring_occurrence " +
            "WHERE ruleId IN (SELECT id FROM recurring_charge WHERE ledgerId = :ledgerId) " +
            "OR transactionId IN (SELECT id FROM `transaction` WHERE ledgerId = :ledgerId)"
    )
    suspend fun deleteOccurrencesForLedger(ledgerId: String)

    @Query("DELETE FROM recurring_charge WHERE ledgerId = :ledgerId")
    suspend fun deleteRecurringForLedger(ledgerId: String)

    @Query(
        "DELETE FROM transaction_tag " +
            "WHERE transactionId IN (SELECT id FROM `transaction` WHERE ledgerId = :ledgerId) " +
            "OR tagId IN (SELECT id FROM tag WHERE ledgerId = :ledgerId)"
    )
    suspend fun deleteTagLinksForLedger(ledgerId: String)

    @Query("DELETE FROM `transaction` WHERE ledgerId = :ledgerId")
    suspend fun deleteTransactionsForLedger(ledgerId: String)

    @Query("DELETE FROM budget WHERE ledgerId = :ledgerId")
    suspend fun deleteBudgetsForLedger(ledgerId: String)

    @Query(
        "DELETE FROM merchant_category " +
            "WHERE merchantId IN (SELECT id FROM merchant WHERE ledgerId = :ledgerId) " +
            "OR categoryId IN (SELECT id FROM category WHERE ledgerId = :ledgerId)"
    )
    suspend fun deleteMerchantLinksForLedger(ledgerId: String)

    @Query("DELETE FROM merchant WHERE ledgerId = :ledgerId")
    suspend fun deleteMerchantsForLedger(ledgerId: String)

    @Query("DELETE FROM tag WHERE ledgerId = :ledgerId")
    suspend fun deleteTagsForLedger(ledgerId: String)

    @Query("DELETE FROM category WHERE ledgerId = :ledgerId")
    suspend fun deleteCategoriesForLedger(ledgerId: String)

    @Query("DELETE FROM ledger WHERE id = :ledgerId")
    suspend fun deleteLedger(ledgerId: String): Int
}

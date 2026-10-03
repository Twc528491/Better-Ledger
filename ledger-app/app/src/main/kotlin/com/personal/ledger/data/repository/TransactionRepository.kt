package com.personal.ledger.data.repository

import androidx.annotation.StringRes
import androidx.room.withTransaction
import com.personal.ledger.R
import com.personal.ledger.data.local.database.AppDatabase
import com.personal.ledger.data.local.entity.TransactionEntity
import com.personal.ledger.data.local.entity.TransactionTagEntity
import com.personal.ledger.domain.calc.TransactionRules
import com.personal.ledger.domain.model.Transaction
import com.personal.ledger.domain.model.TransactionType
import com.personal.ledger.util.TimeUtils
import com.personal.ledger.util.UserFacingException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import java.util.UUID

/** 交易校验失败：错误文案为字符串资源，由 UI 按当前语言解析。 */
private fun validationError(@StringRes res: Int) = UserFacingException(res)

/** 记一笔的输入草稿。 */
data class TransactionDraft(
    val ledgerId: String,
    val type: TransactionType,
    val amount: Long,
    val currency: String,
    val accountId: String,
    val targetAccountId: String? = null,
    val categoryId: String? = null,
    val merchantId: String? = null,
    val note: String? = null,
    val transactionTime: Long,
    val tagIds: List<String> = emptyList(),
)

class TransactionRepository(
    private val database: AppDatabase,
) {

    private val transactionDao = database.transactionDao()
    private val accountDao = database.accountDao()
    private val categoryDao = database.categoryDao()
    private val ledgerDao = database.ledgerDao()
    private val transactionTagDao = database.transactionTagDao()

    fun observeRecent(ledgerId: String, limit: Int = 5): Flow<List<Transaction>> =
        transactionDao.observeRecent(ledgerId, limit).map { list -> list.map { it.toDomain() } }

    fun observeByLedger(ledgerId: String): Flow<List<Transaction>> =
        transactionDao.observeByLedger(ledgerId).map { list -> list.map { it.toDomain() } }

    fun observeMonth(ledgerId: String, month: YearMonth): Flow<List<Transaction>> =
        transactionDao.observeBetween(
            ledgerId,
            TimeUtils.monthStartMs(month),
            TimeUtils.monthEndMs(month),
        ).map { list -> list.map { it.toDomain() } }

    fun observeRecycleBin(ledgerId: String): Flow<List<Transaction>> =
        transactionDao.observeRecycleBin(ledgerId).map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: String): Transaction? = transactionDao.getById(id)?.toDomain()

    /** 交易关联的标签 id。 */
    suspend fun getTagIds(id: String): List<String> = transactionTagDao.getTagIds(id)

    /**
     * 记一笔（支出 / 收入 / 转账）。全部约束见 DATA_MODEL.md 第 7 节：
     * - amount > 0
     * - EXPENSE / INCOME 必须填分类，且分类类型与交易类型一致
     * - TRANSFER 必须填转出与转入账户、两账户不同、不填分类
     * - 交易时间不得早于所属账户 initialBalanceTime（转账为两边账户）
     * - 币种必须等于账本 baseCurrency；账户为全局共享，其币种同样必须等于账本 baseCurrency
     */
    suspend fun add(draft: TransactionDraft): Result<Transaction> {
        return try {
            val saved = database.withTransaction {
                validate(draft)?.let { throw validationError(it) }
                val now = TimeUtils.now()
                val entity = TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    ledgerId = draft.ledgerId,
                    type = draft.type,
                    amount = draft.amount,
                    currency = draft.currency,
                    accountId = draft.accountId,
                    targetAccountId = draft.targetAccountId,
                    categoryId = draft.categoryId,
                    merchantId = draft.merchantId,
                    note = draft.note?.trim()?.ifEmpty { null },
                    transactionTime = draft.transactionTime,
                    createdAt = now,
                    updatedAt = now,
                    isDeleted = false,
                )
                transactionDao.insert(entity)
                if (draft.tagIds.isNotEmpty()) {
                    transactionTagDao.insertAll(
                        draft.tagIds.distinct().map {
                            TransactionTagEntity(transactionId = entity.id, tagId = it)
                        }
                    )
                }
                entity.toDomain()
            }
            Result.success(saved)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 编辑已有交易（同样受全部约束约束）。 */
    suspend fun update(id: String, draft: TransactionDraft): Result<Transaction> {
        return try {
            val saved = database.withTransaction {
                val existing = transactionDao.getById(id) ?: throw UserFacingException(R.string.err_transaction_not_found)
                if (existing.ledgerId != draft.ledgerId) {
                    throw validationError(R.string.err_transaction_move_ledger)
                }
                validate(draft)?.let { throw validationError(it) }
                val updated = existing.copy(
                    type = draft.type,
                    amount = draft.amount,
                    currency = draft.currency,
                    accountId = draft.accountId,
                    targetAccountId = draft.targetAccountId,
                    categoryId = draft.categoryId,
                    merchantId = draft.merchantId,
                    note = draft.note?.trim()?.ifEmpty { null },
                    transactionTime = draft.transactionTime,
                    updatedAt = TimeUtils.now(),
                )
                transactionDao.update(updated)
                transactionTagDao.deleteByTransaction(id)
                if (draft.tagIds.isNotEmpty()) {
                    transactionTagDao.insertAll(
                        draft.tagIds.distinct().map { TransactionTagEntity(transactionId = id, tagId = it) }
                    )
                }
                updated.toDomain()
            }
            Result.success(saved)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 软删除：进入回收站，可恢复。 */
    suspend fun softDelete(id: String): Result<Unit> = try {
        transactionDao.softDelete(id, TimeUtils.now())
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** 从回收站恢复。 */
    suspend fun restore(id: String): Result<Unit> = try {
        transactionDao.restore(id, TimeUtils.now())
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** 某时间段内的收支合计（不含转账；不含软删除）。 */
    suspend fun incomeExpenseBetween(
        ledgerId: String,
        startMs: Long,
        endMs: Long,
    ): Pair<Long, Long> {
        val sums = transactionDao.sumByTypeBetween(ledgerId, startMs, endMs)
        var income = 0L
        var expense = 0L
        sums.forEach { s ->
            when (s.type) {
                TransactionType.INCOME -> income = s.total
                TransactionType.EXPENSE -> expense = s.total
                TransactionType.TRANSFER -> Unit
            }
        }
        return income to expense
    }

    /** 校验通过返回 null，否则返回错误文案的字符串资源 ID。 */
    @StringRes
    private suspend fun validate(draft: TransactionDraft): Int? {
        // 纯规则：金额、分类类型匹配、转账账户。
        TransactionRules.validateAmount(draft.amount)?.let { return it }
        if (draft.type == TransactionType.TRANSFER) {
            if (draft.categoryId != null) return R.string.err_transfer_no_category
            TransactionRules.validateTransferAccounts(draft.accountId, draft.targetAccountId)?.let { return it }
        } else {
            val categoryType = draft.categoryId?.let { categoryDao.getById(it)?.type }
            TransactionRules.validateCategoryForType(draft.type, categoryType)?.let { return it }
        }

        // 依赖数据库的规则：币种、归属账本（分类 / 商家 / 标签）、初始时间。
        val ledger = ledgerDao.getById(draft.ledgerId) ?: return R.string.err_ledger_not_found
        if (draft.currency != ledger.baseCurrency) return R.string.err_currency_mismatch

        // 账户为全局共享：账本可使用币种等于其本位币的任意账户。
        val account = accountDao.getById(draft.accountId) ?: return R.string.err_account_not_found
        if (account.currency != ledger.baseCurrency) return R.string.err_account_currency_mismatch
        if (draft.transactionTime < account.initialBalanceTime) return R.string.err_time_before_account_start

        if (draft.type != TransactionType.TRANSFER) {
            val category = draft.categoryId?.let { categoryDao.getById(it) }
            if (category == null) return R.string.err_select_category
            if (category.ledgerId != draft.ledgerId) return R.string.err_category_not_in_ledger
        } else {
            val targetId = draft.targetAccountId ?: return R.string.err_select_target_account
            val target = accountDao.getById(targetId) ?: return R.string.err_target_account_not_found
            if (target.currency != ledger.baseCurrency) return R.string.err_target_account_currency_mismatch
            if (draft.transactionTime < target.initialBalanceTime) return R.string.err_time_before_target_start
        }

        draft.merchantId?.let { merchantId ->
            val merchant = database.merchantDao().getById(merchantId) ?: return R.string.err_merchant_not_found
            if (merchant.ledgerId != draft.ledgerId) return R.string.err_merchant_not_in_ledger
        }
        draft.tagIds.distinct().forEach { tagId ->
            val tag = database.tagDao().getById(tagId) ?: return R.string.err_tag_not_found
            if (tag.ledgerId != draft.ledgerId) return R.string.err_tag_not_in_ledger
        }
        return null
    }

    // ---------------------------------------------------------------------
    // 多选批量操作：每个方法都在单个数据库事务内完成，要么全部生效、要么全部回滚。
    // ---------------------------------------------------------------------

    /** 批量软删除（进入回收站），返回实际删除的笔数；已在回收站的记录忽略。 */
    suspend fun softDeleteAll(ids: Collection<String>): Result<Int> = batchResult {
        val now = TimeUtils.now()
        database.withTransaction {
            ids.distinct().chunked(BATCH_CHUNK).sumOf { transactionDao.softDeleteByIds(it, now) }
        }
    }

    /** 批量从回收站恢复，返回实际恢复的笔数。 */
    suspend fun restoreAll(ids: Collection<String>): Result<Int> = batchResult {
        val now = TimeUtils.now()
        database.withTransaction {
            ids.distinct().chunked(BATCH_CHUNK).sumOf { transactionDao.restoreByIds(it, now) }
        }
    }

    /**
     * 彻底删除回收站中的账单（不可恢复），只作用于 isDeleted = 1 的记录。
     * 同一事务内先清理依赖行：标签关联与定时扣款执行记录（recurring_occurrence）。
     * 执行记录对应的周期早已被规则的 nextOccurrenceIndex 越过，不会因此重新生成扣款；
     * 保留悬空记录反而会让备份恢复校验（执行记录必须指向存在的交易）失败。
     */
    suspend fun purgeAll(ids: Collection<String>): Result<Int> = batchResult {
        database.withTransaction {
            val targets = ids.distinct().chunked(BATCH_CHUNK)
                .flatMap { transactionDao.filterRecycledIds(it) }
            purgeRecycled(targets)
        }
    }

    /** 清空当前账本的回收站，返回彻底删除的笔数。 */
    suspend fun emptyRecycleBin(ledgerId: String): Result<Int> = batchResult {
        database.withTransaction {
            purgeRecycled(transactionDao.getRecycledIds(ledgerId))
        }
    }

    private suspend fun purgeRecycled(targets: List<String>): Int =
        targets.chunked(BATCH_CHUNK).sumOf { chunk ->
            transactionDao.deleteTagLinksOf(chunk)
            transactionDao.deleteRecurringOccurrencesOf(chunk)
            transactionDao.purgeRecycledByIds(chunk)
        }

    private suspend fun batchResult(block: suspend () -> Int): Result<Int> = try {
        Result.success(block())
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    private companion object {
        /** 低于旧版 SQLite 999 个绑定参数上限，留出余量。 */
        const val BATCH_CHUNK = 500
    }
}

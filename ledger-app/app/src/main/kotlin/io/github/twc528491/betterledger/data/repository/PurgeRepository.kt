package io.github.twc528491.betterledger.data.repository

import androidx.annotation.StringRes
import androidx.room.withTransaction
import io.github.twc528491.betterledger.R
import io.github.twc528491.betterledger.data.local.dao.LedgerPurgeCounts
import io.github.twc528491.betterledger.data.local.database.AppDatabase
import io.github.twc528491.betterledger.data.local.database.RecurringDatabaseGate
import io.github.twc528491.betterledger.domain.calc.PurgeRules
import io.github.twc528491.betterledger.domain.model.Category
import io.github.twc528491.betterledger.util.TimeUtils
import io.github.twc528491.betterledger.util.UserFacingException
import kotlinx.coroutines.sync.withLock

/** 删除分类前的影响评估。 */
data class CategoryPurgePlan(
    val category: Category,
    /** 实际删除的分类（目标 + 已归档的二级分类）。 */
    val removed: List<Category>,
    /** 仍在使用中的二级分类；非空时不允许删除。 */
    val activeChildren: List<Category>,
    /** 引用这些分类的交易数（含回收站）。 */
    val transactionCount: Int,
    val recurringCount: Int,
    val budgetCount: Int,
    /** 可选的替代分类（同账本、同类型、未归档）。 */
    val replacementCandidates: List<Category>,
    /** 用于显示 “父 / 子” 名称的完整分类表。 */
    val allCategories: List<Category>,
) {
    val blocked: Boolean get() = activeChildren.isNotEmpty()
    val needsReplacement: Boolean get() = transactionCount + recurringCount > 0
}

/** 删除账户前的影响评估（均跨全部账本、含回收站中的交易）。 */
data class AccountPurgeImpact(
    val transactionCount: Int,
    val transferCount: Int,
    val recurringCount: Int,
    /** 这些账单分布的账本数。 */
    val ledgerCount: Int,
)

/**
 * 彻底删除已归档的数据（标签 / 商家 / 分类 / 账户 / 账本）。
 * - 只允许删除已归档的项目；活动项目需先归档；
 * - 每次删除在单个数据库事务中完成；涉及定时扣款数据时同时持有 [RecurringDatabaseGate]，
 *   避免与后台生成定时扣款交错；
 * - 不可撤销：界面应提示先备份。
 */
class PurgeRepository(private val database: AppDatabase) {

    private val dao = database.purgeDao()

    // ---------- 标签 ----------

    /** 关联该标签的交易数（含回收站）。 */
    suspend fun tagUsage(tagId: String): Int = dao.countTagLinks(tagId)

    /** 删除标签及其与交易的关联；交易本身保留。 */
    suspend fun purgeTag(tagId: String): Result<Unit> = purgeResult {
        database.withTransaction {
            val tag = database.tagDao().getById(tagId) ?: throw UserFacingException(R.string.err_tag_not_found)
            requireArchived(tag.isArchived, R.string.err_purge_archived_tag)
            dao.deleteTagLinks(tagId)
            dao.deleteTag(tagId)
        }
    }

    // ---------- 商家 ----------

    /** 引用该商家的交易数（含回收站）。 */
    suspend fun merchantUsage(merchantId: String): Int = dao.countTransactionsWithMerchant(merchantId)

    /** 删除商家及其分类关联；引用它的交易（含回收站）改为“无商家”。 */
    suspend fun purgeMerchant(merchantId: String): Result<Unit> = purgeResult {
        database.withTransaction {
            val merchant = database.merchantDao().getById(merchantId) ?: throw UserFacingException(R.string.err_merchant_not_found)
            requireArchived(merchant.isArchived, R.string.err_purge_archived_merchant)
            dao.clearMerchantOnTransactions(merchantId, TimeUtils.now())
            dao.deleteMerchantLinks(merchantId)
            dao.deleteMerchant(merchantId)
        }
    }

    // ---------- 分类 ----------

    suspend fun categoryPlan(categoryId: String): CategoryPurgePlan {
        val entity = database.categoryDao().getById(categoryId) ?: throw UserFacingException(R.string.err_category_not_found)
        val all = database.categoryDao().getByLedger(entity.ledgerId).map { it.toDomain() }
        return buildCategoryPlan(entity.toDomain(), all)
    }

    private suspend fun buildCategoryPlan(target: Category, all: List<Category>): CategoryPurgePlan {
        val scope = PurgeRules.categoryScope(target, all)
        val ids = scope.removedIds
        return CategoryPurgePlan(
            category = target,
            removed = all.filter { it.id in ids },
            activeChildren = all.filter { it.id in scope.activeChildIds },
            transactionCount = dao.countTransactionsInCategories(ids),
            recurringCount = dao.countRecurringInCategories(ids),
            budgetCount = dao.countBudgetsInCategories(ids),
            replacementCandidates = PurgeRules.replacementCandidates(target, all, ids),
            allCategories = all,
        )
    }

    /**
     * 彻底删除已归档分类（删除一级分类时连同其已归档的二级分类）：
     * - 交易（含回收站）与定时扣款若引用这些分类，必须提供 [replacementId]，引用整体迁移到替代分类，
     *   保证收支交易始终有合法分类；
     * - 这些分类的预算直接删除；商家分类关联移除。
     */
    suspend fun purgeCategory(categoryId: String, replacementId: String?): Result<Unit> = purgeResult {
        RecurringDatabaseGate.mutex.withLock {
            database.withTransaction {
                val entity = database.categoryDao().getById(categoryId)
                    ?: throw UserFacingException(R.string.err_category_not_found)
                requireArchived(entity.isArchived, R.string.err_purge_archived_category)
                val all = database.categoryDao().getByLedger(entity.ledgerId).map { it.toDomain() }
                val plan = buildCategoryPlan(entity.toDomain(), all)
                if (plan.blocked) {
                    throw UserFacingException(R.string.err_purge_active_children, plan.activeChildren.size)
                }
                val ids = plan.removed.map { it.id }
                if (plan.needsReplacement) {
                    val replacement = replacementId?.let { id -> all.firstOrNull { it.id == id } }
                    PurgeRules.validateReplacement(plan.category, replacement, all, ids)
                        ?.let { throw UserFacingException(it) }
                    val now = TimeUtils.now()
                    dao.moveTransactionsToCategory(ids, replacement!!.id, now)
                    dao.moveRecurringToCategory(ids, replacement.id, now)
                }
                dao.deleteBudgetsInCategories(ids)
                dao.deleteMerchantLinksForCategories(ids)
                dao.deleteCategories(ids)
            }
        }
    }

    // ---------- 账户 ----------

    suspend fun accountImpact(accountId: String): AccountPurgeImpact = AccountPurgeImpact(
        transactionCount = dao.countTransactionsForAccount(accountId),
        transferCount = dao.countTransfersForAccount(accountId),
        recurringCount = dao.countRecurringForAccount(accountId),
        ledgerCount = dao.countLedgersForAccount(accountId),
    )

    /**
     * 彻底删除已归档账户：同时删除全部账本中涉及它的交易（作为账户或转入账户，含回收站）、
     * 这些交易的标签关联与定时扣款执行记录，以及使用该账户的定时扣款规则。
     * 转账被删除后，转账另一端账户的余额会随之变化。
     */
    suspend fun purgeAccount(accountId: String): Result<Unit> = purgeResult {
        RecurringDatabaseGate.mutex.withLock {
            database.withTransaction {
                val account = database.accountDao().getById(accountId)
                    ?: throw UserFacingException(R.string.err_account_not_found)
                requireArchived(account.isArchived, R.string.err_purge_archived_account)
                dao.deleteOccurrencesForAccount(accountId)
                dao.deleteRecurringForAccount(accountId)
                dao.deleteTagLinksForAccount(accountId)
                dao.deleteTransactionsForAccount(accountId)
                dao.deleteAccount(accountId)
            }
        }
    }

    // ---------- 账本 ----------

    suspend fun ledgerCounts(ledgerId: String): LedgerPurgeCounts = dao.ledgerCounts(ledgerId)

    /**
     * 彻底删除已归档账本及其全部数据，顺序与整库恢复的清空顺序一致（先子表后父表）。
     * 账户为全局共享，不随账本删除；但该账本的账单被删除后，相关账户的余额会随之变化。
     * 至少保留一个未归档账本。调用方随后应回退失效的当前账本设置。
     */
    suspend fun purgeLedger(ledgerId: String): Result<Unit> = purgeResult {
        RecurringDatabaseGate.mutex.withLock {
            database.withTransaction {
                val ledger = database.ledgerDao().getById(ledgerId)
                    ?: throw UserFacingException(R.string.err_ledger_not_found)
                requireArchived(ledger.isArchived, R.string.err_purge_archived_ledger)
                if (database.ledgerDao().getAll().none { it.id != ledgerId && !it.isArchived }) {
                    throw UserFacingException(R.string.err_keep_one_ledger)
                }
                dao.deleteOccurrencesForLedger(ledgerId)
                dao.deleteRecurringForLedger(ledgerId)
                dao.deleteTagLinksForLedger(ledgerId)
                dao.deleteTransactionsForLedger(ledgerId)
                dao.deleteBudgetsForLedger(ledgerId)
                dao.deleteMerchantLinksForLedger(ledgerId)
                dao.deleteMerchantsForLedger(ledgerId)
                dao.deleteTagsForLedger(ledgerId)
                dao.deleteCategoriesForLedger(ledgerId)
                dao.deleteLedger(ledgerId)
            }
        }
    }

    private fun requireArchived(isArchived: Boolean, @StringRes notArchivedError: Int) {
        if (!isArchived) throw UserFacingException(notArchivedError)
    }

    private inline fun <T> purgeResult(block: () -> T): Result<Unit> = try {
        block()
        Result.success(Unit)
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}

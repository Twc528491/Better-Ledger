package io.github.twc528491.betterledger.data.repository

import androidx.room.withTransaction
import io.github.twc528491.betterledger.R
import io.github.twc528491.betterledger.data.local.database.AppDatabase
import io.github.twc528491.betterledger.data.local.entity.BudgetEntity
import io.github.twc528491.betterledger.domain.calc.BudgetCalculator
import io.github.twc528491.betterledger.domain.model.Budget
import io.github.twc528491.betterledger.util.TimeUtils
import io.github.twc528491.betterledger.util.UserFacingException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import java.util.UUID

/** 某预算的执行情况（供 UI 展示）。 */
data class BudgetProgress(
    val budget: Budget,
    val spent: Long,
    /** 千分比进度（0..1000+），预算未设置时为 null。 */
    val permille: Int?,
)

class BudgetRepository(
    private val database: AppDatabase,
    private val categoryRepository: CategoryRepository,
) {

    private val budgetDao = database.budgetDao()
    private val transactionDao = database.transactionDao()

    fun observeByLedger(ledgerId: String): Flow<List<Budget>> =
        budgetDao.observeByLedger(ledgerId).map { list -> list.map { it.toDomain() } }

    fun observeByMonth(ledgerId: String, month: String): Flow<List<Budget>> =
        budgetDao.observeByMonth(ledgerId, month).map { list -> list.map { it.toDomain() } }

    suspend fun getByMonth(ledgerId: String, month: String): List<Budget> =
        budgetDao.getByMonth(ledgerId, month).map { it.toDomain() }

    /**
     * 设置 / 修改预算。约束：
     * - 同账本同月份最多一个总预算、每分类一个分类预算（事务内校验）；
     * - 分类预算只能关联 EXPENSE 分类。
     */
    suspend fun setBudget(
        ledgerId: String,
        month: String,
        categoryId: String?,
        amount: Long,
        currency: String,
    ): Result<Budget> {
        return try {
            val budget = database.withTransaction {
                if (amount <= 0) throw UserFacingException(R.string.err_budget_amount_positive)
                if (!month.matches(Regex("""\d{4}-\d{2}"""))) throw UserFacingException(R.string.err_month_format)
                if (categoryId != null) {
                    val category = categoryRepository.getById(categoryId)
                        ?: throw UserFacingException(R.string.err_category_not_found)
                    if (category.ledgerId != ledgerId) throw UserFacingException(R.string.err_category_not_in_ledger)
                    if (category.type != io.github.twc528491.betterledger.domain.model.CategoryType.EXPENSE) {
                        throw UserFacingException(R.string.err_budget_expense_only)
                    }
                }
                val now = TimeUtils.now()
                val existing = budgetDao.findByMonthAndCategory(ledgerId, month, categoryId)
                val entity = if (existing != null) {
                    existing.copy(amount = amount, currency = currency, updatedAt = now)
                } else {
                    BudgetEntity(
                        id = UUID.randomUUID().toString(),
                        ledgerId = ledgerId,
                        month = month,
                        categoryId = categoryId,
                        amount = amount,
                        currency = currency,
                        createdAt = now,
                        updatedAt = now,
                    )
                }
                if (existing != null) budgetDao.update(entity) else budgetDao.insert(entity)
                entity.toDomain()
            }
            Result.success(budget)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteBudget(ledgerId: String, month: String, categoryId: String?): Result<Unit> = try {
        budgetDao.delete(ledgerId, month, categoryId)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 某月预算执行情况：
     * - 总预算：当月全部支出；
     * - 分类预算：该分类树支出（父分类含所有子分类）。
     */
    suspend fun progressForMonth(ledgerId: String, month: YearMonth): List<BudgetProgress> {
        val startMs = TimeUtils.monthStartMs(month)
        val endMs = TimeUtils.monthEndMs(month)
        val budgets = budgetDao.getByMonth(ledgerId, month.toString())

        val categoryExpense = transactionDao.expenseByCategoryBetween(ledgerId, startMs, endMs)
            .associate { it.categoryId to it.total }
        val parentMap = categoryRepository.parentMap(ledgerId)
        val rolledUp = BudgetCalculator.rollUpToParents(parentMap, categoryExpense)

        return budgets.map { b ->
            val spent = if (b.categoryId == null) {
                transactionDao.sumByTypeBetween(ledgerId, startMs, endMs)
                    .firstOrNull { it.type == io.github.twc528491.betterledger.domain.model.TransactionType.EXPENSE }
                    ?.total ?: 0L
            } else {
                rolledUp[b.categoryId] ?: 0L
            }
            BudgetProgress(
                budget = b.toDomain(),
                spent = spent,
                permille = BudgetCalculator.progressPermille(b.amount, spent),
            )
        }
    }
}

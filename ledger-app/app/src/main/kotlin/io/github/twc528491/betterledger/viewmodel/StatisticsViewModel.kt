package io.github.twc528491.betterledger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.twc528491.betterledger.data.repository.BudgetProgress
import io.github.twc528491.betterledger.data.repository.BudgetRepository
import io.github.twc528491.betterledger.data.repository.CategoryRepository
import io.github.twc528491.betterledger.data.repository.LedgerRepository
import io.github.twc528491.betterledger.data.repository.TransactionRepository
import io.github.twc528491.betterledger.domain.calc.BudgetCalculator
import io.github.twc528491.betterledger.domain.calc.CategoryChangeCalculator
import io.github.twc528491.betterledger.domain.calc.DailyExpenseCalculator
import io.github.twc528491.betterledger.domain.calc.DailyExpenseComparison
import io.github.twc528491.betterledger.domain.calc.MonthTrendCalculator
import io.github.twc528491.betterledger.domain.model.Category
import io.github.twc528491.betterledger.domain.model.TransactionType
import io.github.twc528491.betterledger.util.TimeUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

/** 分类支出条目（含占比）。 */
data class CategoryStat(
    val category: Category,
    val amount: Long,
    val fraction: Float,
)

/** 分类支出环比条目。 */
data class CategoryChangeStat(
    val category: Category,
    val change: CategoryChangeCalculator.Change,
)

data class StatisticsUiState(
    val ledgerName: String = "",
    val baseCurrency: String = "CNY",
    val month: YearMonth = YearMonth.now(),
    val income: Long = 0L,
    val expense: Long = 0L,
    val expenseByCategory: List<CategoryStat> = emptyList(),
    /** 截至所选月份的最近 12 个月；界面按 6 / 12 个月切换取尾部。 */
    val trend: List<MonthTrendCalculator.MonthTotals> = emptyList(),
    val dailyComparison: DailyExpenseComparison? = null,
    val previousExpense: Long = 0L,
    val categoryChanges: List<CategoryChangeStat> = emptyList(),
    val budgets: List<BudgetProgress> = emptyList(),
    val categoryNames: Map<String, String> = emptyMap(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModel(
    private val ledgerRepository: LedgerRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = month

    val uiState: StateFlow<StatisticsUiState> = combine(
        ledgerRepository.currentLedger,
        month,
    ) { ledger, ym -> ledger to ym }
        .flatMapLatest { (ledger, ym) ->
            if (ledger == null) {
                flowOf(StatisticsUiState(month = ym))
            } else {
                combine(
                    transactionRepository.observeMonth(ledger.id, ym),
                    transactionRepository.observeByLedger(ledger.id),
                    categoryRepository.observeByLedger(ledger.id),
                    budgetRepository.observeByMonth(ledger.id, ym.toString()),
                ) { monthTx, allTx, categories, budgets ->
                    var income = 0L
                    var expense = 0L
                    val expenseMap = HashMap<String, Long>()
                    monthTx.forEach { tx ->
                        when (tx.type) {
                            TransactionType.INCOME -> income += tx.amount
                            TransactionType.EXPENSE -> {
                                expense += tx.amount
                                tx.categoryId?.let { cid ->
                                    expenseMap[cid] = (expenseMap[cid] ?: 0L) + tx.amount
                                }
                            }
                            TransactionType.TRANSFER -> Unit
                        }
                    }
                    val stats = expenseMap.entries
                        .mapNotNull { (cid, amount) ->
                            categories.firstOrNull { it.id == cid }?.let {
                                CategoryStat(
                                    category = it,
                                    amount = amount,
                                    fraction = if (expense == 0L) 0f else amount.toFloat() / expense.toFloat(),
                                )
                            }
                        }
                        .sortedByDescending { it.amount }
                    val trend = MonthTrendCalculator.lastMonths(
                        transactions = allTx.map { MonthTrendCalculator.TxInput(it.type, it.amount, it.transactionTime) },
                        end = ym,
                        count = TREND_MONTHS,
                    )
                    // 上月数据直接从全量交易中截取，不新增 DAO 查询。
                    val previousStart = TimeUtils.monthStartMs(ym.minusMonths(1))
                    val currentStart = TimeUtils.monthStartMs(ym)
                    val currentEnd = TimeUtils.monthEndMs(ym)
                    val previousExpenseMap = HashMap<String, Long>()
                    var previousExpense = 0L
                    val dailyInputs = ArrayList<DailyExpenseCalculator.TxInput>()
                    allTx.forEach { tx ->
                        if (tx.transactionTime < previousStart || tx.transactionTime >= currentEnd) return@forEach
                        dailyInputs += DailyExpenseCalculator.TxInput(tx.type, tx.amount, tx.transactionTime)
                        if (tx.transactionTime < currentStart && tx.type == TransactionType.EXPENSE) {
                            previousExpense += tx.amount
                            tx.categoryId?.let { cid ->
                                previousExpenseMap[cid] = (previousExpenseMap[cid] ?: 0L) + tx.amount
                            }
                        }
                    }
                    val dailyComparison = DailyExpenseCalculator.compareWithPrevious(
                        transactions = dailyInputs,
                        month = ym,
                        today = LocalDate.now(),
                    )
                    val categoriesById = categories.associateBy { it.id }
                    val categoryChanges = CategoryChangeCalculator.compare(expenseMap, previousExpenseMap)
                        .mapNotNull { change ->
                            categoriesById[change.categoryId]?.let { CategoryChangeStat(it, change) }
                        }
                    // 与预算页使用同一套父分类汇总规则：父分类预算包含所有子分类支出。
                    val rolledUpExpense = BudgetCalculator.rollUpToParents(
                        categories.associate { it.id to it.parentId },
                        expenseMap,
                    )
                    val budgetProgress = budgets.map { budget ->
                        val spent = budget.categoryId?.let { rolledUpExpense[it] ?: 0L } ?: expense
                        BudgetProgress(
                            budget = budget,
                            spent = spent,
                            permille = BudgetCalculator.progressPermille(budget.amount, spent),
                        )
                    }
                    StatisticsUiState(
                        ledgerName = ledger.name,
                        baseCurrency = ledger.baseCurrency,
                        month = ym,
                        income = income,
                        expense = expense,
                        expenseByCategory = stats,
                        trend = trend,
                        dailyComparison = dailyComparison,
                        previousExpense = previousExpense,
                        categoryChanges = categoryChanges,
                        budgets = budgetProgress,
                        categoryNames = categories.associate { it.id to it.name },
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatisticsUiState())

    fun previousMonth() {
        month.value = month.value.minusMonths(1)
    }

    fun nextMonth() {
        month.value = month.value.plusMonths(1)
    }

    fun setMonth(ym: YearMonth) {
        month.value = ym
    }

    private companion object {
        const val TREND_MONTHS = 12
    }
}

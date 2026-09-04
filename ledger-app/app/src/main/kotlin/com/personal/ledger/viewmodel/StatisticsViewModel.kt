package com.personal.ledger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.ledger.data.repository.BudgetProgress
import com.personal.ledger.data.repository.BudgetRepository
import com.personal.ledger.data.repository.CategoryRepository
import com.personal.ledger.data.repository.LedgerRepository
import com.personal.ledger.data.repository.TransactionRepository
import com.personal.ledger.domain.calc.BudgetCalculator
import com.personal.ledger.domain.calc.MonthTrendCalculator
import com.personal.ledger.domain.model.Category
import com.personal.ledger.domain.model.TransactionType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth

/** 分类支出条目（含占比）。 */
data class CategoryStat(
    val category: Category,
    val amount: Long,
    val fraction: Float,
)

data class StatisticsUiState(
    val ledgerName: String = "",
    val baseCurrency: String = "CNY",
    val monthLabel: String = "",
    val income: Long = 0L,
    val expense: Long = 0L,
    val expenseByCategory: List<CategoryStat> = emptyList(),
    val trend: List<MonthTrendCalculator.MonthTotals> = emptyList(),
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

    val uiState: StateFlow<StatisticsUiState> = combine(
        ledgerRepository.currentLedger,
        month,
    ) { ledger, ym -> ledger to ym }
        .flatMapLatest { (ledger, ym) ->
            if (ledger == null) {
                flowOf(StatisticsUiState(monthLabel = labelOf(ym)))
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
                        count = 6,
                    )
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
                        monthLabel = labelOf(ym),
                        income = income,
                        expense = expense,
                        expenseByCategory = stats,
                        trend = trend,
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

    private fun labelOf(ym: YearMonth) = "${ym.year}年${ym.monthValue}月"
}

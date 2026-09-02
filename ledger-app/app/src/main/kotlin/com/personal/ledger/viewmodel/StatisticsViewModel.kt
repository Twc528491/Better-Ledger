package com.personal.ledger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.ledger.data.repository.BudgetProgress
import com.personal.ledger.data.repository.BudgetRepository
import com.personal.ledger.data.repository.CategoryRepository
import com.personal.ledger.data.repository.LedgerRepository
import com.personal.ledger.data.repository.TransactionRepository
import com.personal.ledger.domain.calc.MonthTrendCalculator
import com.personal.ledger.domain.model.Category
import com.personal.ledger.domain.model.TransactionType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth

/** 分类支出条目（含占比）。 */
data class CategoryStat(
    val category: Category,
    val amount: Long,
    val fraction: Float,
)

data class StatisticsUiState(
    val ledgerName: String = "",
    val monthLabel: String = "",
    val income: Long = 0L,
    val expense: Long = 0L,
    val expenseByCategory: List<CategoryStat> = emptyList(),
    val trend: List<MonthTrendCalculator.MonthTotals> = emptyList(),
    val budgets: List<BudgetProgress> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModel(
    private val ledgerRepository: LedgerRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())
    private val refreshTick = MutableStateFlow(0)

    val uiState: StateFlow<StatisticsUiState> = combine(
        ledgerRepository.currentLedger,
        month,
        refreshTick,
    ) { ledger, ym, _ -> ledger to ym }
        .flatMapLatest { (ledger, ym) ->
            if (ledger == null) {
                flowOf(StatisticsUiState(monthLabel = labelOf(ym)))
            } else {
                combine(
                    transactionRepository.observeMonth(ledger.id, ym),
                    transactionRepository.observeByLedger(ledger.id),
                    categoryRepository.observeByLedger(ledger.id),
                ) { monthTx, allTx, categories ->
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
                    StatisticsUiState(
                        ledgerName = ledger.name,
                        monthLabel = labelOf(ym),
                        income = income,
                        expense = expense,
                        expenseByCategory = stats,
                        trend = trend,
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatisticsUiState())

    /** 预算执行（挂起查询，由 UI 触发刷新）。 */
    fun loadBudgets(onResult: (List<BudgetProgress>) -> Unit) {
        viewModelScope.launch {
            val ledger = ledgerRepository.currentLedger.first() ?: return@launch
            onResult(budgetRepository.progressForMonth(ledger.id, month.value))
        }
    }

    fun previousMonth() {
        month.value = month.value.minusMonths(1)
    }

    fun nextMonth() {
        month.value = month.value.plusMonths(1)
    }

    fun refresh() {
        refreshTick.value++
    }

    private fun labelOf(ym: YearMonth) = "${ym.year}年${ym.monthValue}月"
}

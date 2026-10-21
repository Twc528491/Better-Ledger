package io.github.twc528491.betterledger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.twc528491.betterledger.data.repository.AccountRepository
import io.github.twc528491.betterledger.data.repository.BudgetProgress
import io.github.twc528491.betterledger.data.repository.BudgetRepository
import io.github.twc528491.betterledger.data.repository.CategoryRepository
import io.github.twc528491.betterledger.data.repository.LedgerRepository
import io.github.twc528491.betterledger.data.repository.MerchantRepository
import io.github.twc528491.betterledger.data.repository.TransactionRepository
import io.github.twc528491.betterledger.domain.calc.BudgetCalculator
import io.github.twc528491.betterledger.domain.model.Transaction
import io.github.twc528491.betterledger.domain.model.TransactionType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth

data class HomeUiState(
    val ledgerName: String = "",
    val baseCurrency: String = "CNY",
    val month: YearMonth = YearMonth.now(),
    val income: Long = 0L,
    val expense: Long = 0L,
    val recent: List<Transaction> = emptyList(),
    val totalBudget: BudgetProgress? = null,
    val categoryNames: Map<String, String> = emptyMap(),
    /** 分类 ID → 用户选择的图标 key（未选择则不在表中）。 */
    val categoryIconKeys: Map<String, String> = emptyMap(),
    val merchantNames: Map<String, String> = emptyMap(),
    val accountNames: Map<String, String> = emptyMap(),
    val loading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val ledgerRepository: LedgerRepository,
    private val transactionRepository: TransactionRepository,
    budgetRepository: BudgetRepository,
    categoryRepository: CategoryRepository,
    merchantRepository: MerchantRepository,
    accountRepository: AccountRepository,
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())

    fun switchLedger(id: String) {
        viewModelScope.launch { ledgerRepository.setCurrentLedger(id) }
    }

    fun delete(id: String, onDone: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch { onDone(transactionRepository.softDelete(id)) }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        ledgerRepository.currentLedger,
        month,
    ) { ledger, ym -> ledger to ym }
        .flatMapLatest { (ledger, ym) ->
            if (ledger == null) {
                flowOf(HomeUiState(loading = false))
            } else {
                combine(
                    combine(
                        transactionRepository.observeMonth(ledger.id, ym),
                        transactionRepository.observeRecent(ledger.id, 5),
                        budgetRepository.observeByMonth(ledger.id, ym.toString()),
                    ) { monthTx, recent, budgets -> Triple(monthTx, recent, budgets) },
                    combine(
                        categoryRepository.observeByLedger(ledger.id),
                        merchantRepository.observeByLedger(ledger.id),
                        // 账户全局共享：名称查表用全部账户。
                        accountRepository.observeAll(),
                    ) { categories, merchants, accounts -> Triple(categories, merchants, accounts) },
                ) { t3a, t3b ->
                    val monthTx = t3a.first
                    val recent = t3a.second
                    val budgets = t3a.third
                    val categories = t3b.first
                    val merchants = t3b.second
                    val accounts = t3b.third
                    var income = 0L
                    var expense = 0L
                    monthTx.forEach { tx ->
                        when (tx.type) {
                            TransactionType.INCOME -> income += tx.amount
                            TransactionType.EXPENSE -> expense += tx.amount
                            TransactionType.TRANSFER -> Unit
                        }
                    }
                    // 总预算执行（随预算与交易实时刷新）
                    val totalBudget = budgets.firstOrNull { it.categoryId == null }?.let { b ->
                        BudgetProgress(
                            budget = b,
                            spent = expense,
                            permille = BudgetCalculator.progressPermille(b.amount, expense),
                        )
                    }
                    HomeUiState(
                        ledgerName = ledger.name,
                        baseCurrency = ledger.baseCurrency,
                        month = ym,
                        income = income,
                        expense = expense,
                        recent = recent,
                        totalBudget = totalBudget,
                        categoryNames = categories.associate { it.id to it.name },
                        categoryIconKeys = categories.mapNotNull { c -> c.iconKey?.let { c.id to it } }.toMap(),
                        merchantNames = merchants.associate { it.id to it.name },
                        accountNames = accounts.associate { it.id to it.name },
                        loading = false,
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}

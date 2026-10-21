package io.github.twc528491.betterledger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.twc528491.betterledger.data.repository.AccountRepository
import io.github.twc528491.betterledger.data.repository.CategoryRepository
import io.github.twc528491.betterledger.data.repository.LedgerRepository
import io.github.twc528491.betterledger.data.repository.RecurringChargeDraft
import io.github.twc528491.betterledger.data.repository.RecurringChargeRepository
import io.github.twc528491.betterledger.domain.model.Account
import io.github.twc528491.betterledger.domain.model.Category
import io.github.twc528491.betterledger.domain.model.Ledger
import io.github.twc528491.betterledger.domain.model.RecurringCharge
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RecurringChargesUiState(
    val ledger: Ledger? = null,
    val accounts: List<Account> = emptyList(),
    val expenseCategories: List<Category> = emptyList(),
    val charges: List<RecurringCharge> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class RecurringChargesViewModel(
    ledgerRepository: LedgerRepository,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
    private val recurringRepository: RecurringChargeRepository,
) : ViewModel() {
    private var savingCharge = false

    val uiState: StateFlow<RecurringChargesUiState> = ledgerRepository.currentLedger
        .flatMapLatest { ledger ->
            if (ledger == null) {
                flowOf(RecurringChargesUiState())
            } else {
                kotlinx.coroutines.flow.combine(
                    // 账户全局共享：任一账本中币种一致的账户都可用于扣款（含已归档，供显示旧规则）。
                    accountRepository.observeByCurrency(ledger.baseCurrency),
                    categoryRepository.observeByLedger(ledger.id),
                    recurringRepository.observeByLedger(ledger.id),
                ) { accounts, categories, charges ->
                    RecurringChargesUiState(
                        ledger = ledger,
                        accounts = accounts,
                        expenseCategories = categories.filter {
                            it.type == io.github.twc528491.betterledger.domain.model.CategoryType.EXPENSE
                        },
                        charges = charges,
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecurringChargesUiState())

    fun save(id: String?, draft: RecurringChargeDraft, onDone: (Result<RecurringCharge>) -> Unit) {
        if (savingCharge) return
        savingCharge = true
        viewModelScope.launch {
            try {
                onDone(recurringRepository.save(id, draft))
            } finally {
                savingCharge = false
            }
        }
    }

    fun setPaused(id: String, paused: Boolean, onDone: (Result<Unit>) -> Unit) {
        viewModelScope.launch { onDone(recurringRepository.setPaused(id, paused)) }
    }

    fun retry(id: String, onDone: (Result<Unit>) -> Unit) {
        viewModelScope.launch { onDone(recurringRepository.retry(id)) }
    }

    fun delete(id: String, onDone: (Result<Unit>) -> Unit) {
        viewModelScope.launch { onDone(recurringRepository.delete(id)) }
    }
}

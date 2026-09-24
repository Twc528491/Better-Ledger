package com.personal.ledger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.ledger.data.repository.AccountRepository
import com.personal.ledger.data.repository.CategoryRepository
import com.personal.ledger.data.repository.LedgerRepository
import com.personal.ledger.data.repository.RecurringChargeDraft
import com.personal.ledger.data.repository.RecurringChargeRepository
import com.personal.ledger.domain.model.Account
import com.personal.ledger.domain.model.Category
import com.personal.ledger.domain.model.Ledger
import com.personal.ledger.domain.model.RecurringCharge
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
                    accountRepository.observeByLedger(ledger.id),
                    categoryRepository.observeByLedger(ledger.id),
                    recurringRepository.observeByLedger(ledger.id),
                ) { accounts, categories, charges ->
                    RecurringChargesUiState(
                        ledger = ledger,
                        accounts = accounts,
                        expenseCategories = categories.filter {
                            it.type == com.personal.ledger.domain.model.CategoryType.EXPENSE
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

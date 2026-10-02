package com.personal.ledger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.ledger.data.repository.AccountRepository
import com.personal.ledger.data.repository.LedgerRepository
import com.personal.ledger.domain.model.AccountType
import com.personal.ledger.domain.model.AccountWithBalance
import com.personal.ledger.util.TimeUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AccountsUiState(
    val ledgerName: String = "",
    val netWorth: Long = 0L,
    val assets: List<AccountWithBalance> = emptyList(),
    val liabilities: List<AccountWithBalance> = emptyList(),
    /** 已归档账户（不计入资产 / 负债分组，可进入详情恢复或彻底删除）。 */
    val archived: List<AccountWithBalance> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class AccountsViewModel(
    private val ledgerRepository: LedgerRepository,
    private val accountRepository: AccountRepository,
) : ViewModel() {

    val uiState: StateFlow<AccountsUiState> = ledgerRepository.currentLedger
        .flatMapLatest { ledger ->
            if (ledger == null) {
                flowOf(AccountsUiState())
            } else {
                combine(
                    accountRepository.observeWithBalances(ledger.id),
                    ledgerRepository.currentLedger,
                ) { accounts, current ->
                    // 资产 / 负债按余额正负分组：balance >= 0 为资产，balance < 0 为负债。
                    AccountsUiState(
                        ledgerName = current?.name ?: ledger.name,
                        netWorth = accounts.sumOf { it.balance },
                        assets = accounts.filter { it.balance >= 0 && !it.account.isArchived },
                        liabilities = accounts.filter { it.balance < 0 && !it.account.isArchived },
                        archived = accounts.filter { it.account.isArchived },
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountsUiState())

    fun addAccount(
        name: String,
        type: AccountType,
        initialBalanceMinor: Long,
        onDone: (Result<Unit>) -> Unit,
    ) {
        viewModelScope.launch {
            val ledger = ledgerRepository.currentLedger.first() ?: return@launch
            val result = accountRepository.addAccount(
                ledgerId = ledger.id,
                name = name,
                type = type,
                currency = ledger.baseCurrency,
                initialBalance = initialBalanceMinor,
                initialBalanceTime = TimeUtils.now(),
            )
            onDone(result.map { Unit })
        }
    }

    fun toggleArchive(id: String, archived: Boolean, onDone: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            onDone(accountRepository.setArchived(id, archived))
        }
    }
}

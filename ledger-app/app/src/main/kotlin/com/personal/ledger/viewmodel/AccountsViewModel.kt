package com.personal.ledger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.ledger.data.repository.AccountRepository
import com.personal.ledger.data.repository.LedgerRepository
import com.personal.ledger.domain.model.AccountType
import com.personal.ledger.domain.model.AccountWithBalance
import com.personal.ledger.util.TimeUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AccountsUiState(
    /** 净资产的计价币种：当前账本的本位币。 */
    val currency: String = "CNY",
    val netWorth: Long = 0L,
    val assets: List<AccountWithBalance> = emptyList(),
    val liabilities: List<AccountWithBalance> = emptyList(),
    /** 未归档的其他币种账户：V1 不做汇率，单独列出，不计入净资产。 */
    val otherCurrencies: List<AccountWithBalance> = emptyList(),
    /** 已归档账户（不计入资产 / 负债分组，可进入详情恢复或彻底删除）。 */
    val archived: List<AccountWithBalance> = emptyList(),
)

/** 资产页：账户全局共享，展示全部账户（全局净资产），余额汇总全部账本的账单。 */
class AccountsViewModel(
    private val ledgerRepository: LedgerRepository,
    private val accountRepository: AccountRepository,
) : ViewModel() {

    val uiState: StateFlow<AccountsUiState> = combine(
        accountRepository.observeWithBalances(),
        ledgerRepository.currentLedger,
    ) { accounts, ledger ->
        val currency = ledger?.baseCurrency ?: "CNY"
        val active = accounts.filter { !it.account.isArchived }
        val primary = active.filter { it.account.currency == currency }
        // 资产 / 负债按余额正负分组：balance >= 0 为资产，balance < 0 为负债。
        AccountsUiState(
            currency = currency,
            netWorth = primary.sumOf { it.balance },
            assets = primary.filter { it.balance >= 0 },
            liabilities = primary.filter { it.balance < 0 },
            otherCurrencies = active.filter { it.account.currency != currency },
            archived = accounts.filter { it.account.isArchived },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountsUiState())

    fun addAccount(
        name: String,
        type: AccountType,
        initialBalanceMinor: Long,
        onDone: (Result<Unit>) -> Unit,
    ) {
        viewModelScope.launch {
            val ledger = ledgerRepository.currentLedger.first() ?: return@launch
            // 新账户币种取当前账本本位币；创建后所有同币种账本均可使用。
            val result = accountRepository.addAccount(
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

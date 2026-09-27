package com.personal.ledger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.ledger.data.repository.BudgetRepository
import com.personal.ledger.data.repository.CategoryRepository
import com.personal.ledger.data.repository.LedgerRepository
import com.personal.ledger.data.settings.SettingsRepository
import com.personal.ledger.data.settings.ThemeMode
import com.personal.ledger.domain.model.Budget
import com.personal.ledger.domain.model.Category
import com.personal.ledger.domain.model.CategoryType
import com.personal.ledger.domain.model.Ledger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val securityLockEnabled: Boolean = false,
    val ledgers: List<Ledger> = emptyList(),
    val currentLedgerName: String = "",
)

class SettingsViewModel(
    private val ledgerRepository: LedgerRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settings.themeMode,
        settings.securityLockEnabled,
        ledgerRepository.ledgers,
        ledgerRepository.currentLedger,
    ) { theme, lock, ledgers, current ->
        SettingsUiState(
            themeMode = theme,
            securityLockEnabled = lock,
            ledgers = ledgers,
            currentLedgerName = current?.name ?: "",
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    fun setSecurityLockEnabled(enabled: Boolean) {
        viewModelScope.launch { settings.setSecurityLockEnabled(enabled) }
    }

    fun switchLedger(id: String) {
        viewModelScope.launch { ledgerRepository.setCurrentLedger(id) }
    }

    fun createLedger(name: String, onDone: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = runCatching {
                val ledger = ledgerRepository.createLedger(name.trim())
                ledgerRepository.setCurrentLedger(ledger.id)
            }
            onDone(result)
        }
    }

    fun setLedgerArchived(id: String, archived: Boolean, onDone: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            onDone(runCatching { ledgerRepository.setLedgerArchived(id, archived) })
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CategoriesViewModel(
    private val ledgerRepository: LedgerRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    private val type = MutableStateFlow(CategoryType.EXPENSE)

    val uiState: StateFlow<CategoriesUiState> = combine(
        ledgerRepository.currentLedger,
        type,
    ) { ledger, t -> ledger to t }
        .flatMapLatest { (ledger, t) ->
            if (ledger == null) {
                kotlinx.coroutines.flow.flowOf(CategoriesUiState())
            } else {
                categoryRepository.observeByLedger(ledger.id)
                    .map { all ->
                        val list = all.filter { it.type == t }
                        CategoriesUiState(
                            ledgerId = ledger.id,
                            type = t,
                            categories = list,
                        )
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoriesUiState())

    fun setType(t: CategoryType) {
        type.value = t
    }

    fun addCategory(
        name: String,
        parentId: String?,
        iconKey: String? = null,
        onDone: (Result<Unit>) -> Unit,
    ) {
        val s = uiState.value
        viewModelScope.launch {
            val ledger = ledgerRepository.currentLedger.first() ?: return@launch
            onDone(categoryRepository.addCategory(ledger.id, name, s.type, parentId, iconKey).map { Unit })
        }
    }

    /** 更换分类图标；null 表示恢复按名称自动匹配。 */
    fun setIconKey(id: String, iconKey: String?, onDone: (Result<Unit>) -> Unit) {
        viewModelScope.launch { onDone(categoryRepository.setIconKey(id, iconKey)) }
    }

    fun supplementCommonCategories(onDone: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            val ledger = ledgerRepository.currentLedger.first()
            if (ledger == null) {
                onDone(Result.failure(IllegalStateException("当前账本不可用")))
            } else {
                onDone(categoryRepository.supplementCommonCategories(ledger.id))
            }
        }
    }

    fun setArchived(id: String, archived: Boolean, onDone: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            onDone(categoryRepository.setArchived(id, archived))
        }
    }

    /** 分类删除保持历史引用，实际执行安全软归档。 */
    fun deleteCategory(id: String, onDone: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            onDone(categoryRepository.delete(id))
        }
    }

    fun rename(id: String, name: String, onDone: (Result<Unit>) -> Unit) {
        viewModelScope.launch { onDone(categoryRepository.rename(id, name)) }
    }

    fun move(id: String, direction: Int, onDone: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch { onDone(categoryRepository.move(id, direction)) }
    }
}

data class CategoriesUiState(
    val ledgerId: String? = null,
    val type: CategoryType = CategoryType.EXPENSE,
    val categories: List<Category> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetsViewModel(
    private val ledgerRepository: LedgerRepository,
    private val budgetRepository: BudgetRepository,
) : ViewModel() {

    private val month = MutableStateFlow(com.personal.ledger.util.TimeUtils.currentMonthKey())

    val uiState: StateFlow<BudgetsUiState> = combine(
        ledgerRepository.currentLedger,
        month,
    ) { ledger, m -> ledger to m }
        .flatMapLatest { (ledger, m) ->
            if (ledger == null) {
                kotlinx.coroutines.flow.flowOf(BudgetsUiState(month = m))
            } else {
                budgetRepository.observeByMonth(ledger.id, m)
                    .map { budgets ->
                        BudgetsUiState(
                            ledgerId = ledger.id,
                            month = m,
                            budgets = budgets,
                        )
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetsUiState())

    fun setBudget(categoryId: String?, amountMinor: Long, onDone: (Result<Unit>) -> Unit) {
        val s = uiState.value
        viewModelScope.launch {
            val ledger = ledgerRepository.currentLedger.first() ?: return@launch
            onDone(
                budgetRepository.setBudget(ledger.id, s.month, categoryId, amountMinor, ledger.baseCurrency)
                    .map { Unit }
            )
        }
    }

    fun deleteBudget(categoryId: String?, onDone: (Result<Unit>) -> Unit) {
        val s = uiState.value
        viewModelScope.launch {
            val ledger = ledgerRepository.currentLedger.first() ?: return@launch
            onDone(budgetRepository.deleteBudget(ledger.id, s.month, categoryId))
        }
    }

    fun previousMonth() {
        month.value = shiftMonth(month.value, -1)
    }

    fun nextMonth() {
        month.value = shiftMonth(month.value, 1)
    }

    private fun shiftMonth(m: String, delta: Long): String {
        val ym = java.time.YearMonth.parse(m)
        return ym.plusMonths(delta).toString()
    }
}

data class BudgetsUiState(
    val ledgerId: String? = null,
    val month: String = "",
    val budgets: List<com.personal.ledger.domain.model.Budget> = emptyList(),
)

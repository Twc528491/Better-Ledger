package com.personal.ledger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.ledger.data.repository.AccountRepository
import com.personal.ledger.data.repository.CategoryRepository
import com.personal.ledger.data.repository.LedgerRepository
import com.personal.ledger.data.repository.MerchantRepository
import com.personal.ledger.data.repository.TagRepository
import com.personal.ledger.data.repository.TransactionDraft
import com.personal.ledger.data.repository.TransactionRepository
import com.personal.ledger.data.settings.SettingsRepository
import com.personal.ledger.domain.model.Account
import com.personal.ledger.domain.model.Category
import com.personal.ledger.domain.model.Merchant
import com.personal.ledger.domain.model.Tag
import com.personal.ledger.domain.model.Transaction
import com.personal.ledger.domain.model.TransactionType
import com.personal.ledger.util.TimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class AddTransactionUiState(
    val ledgerId: String? = null,
    val baseCurrency: String = "CNY",
    val type: TransactionType = TransactionType.EXPENSE,
    val amountText: String = "",
    val accountId: String? = null,
    val targetAccountId: String? = null,
    val categoryId: String? = null,
    val merchantId: String? = null,
    val selectedTagIds: Set<String> = emptySet(),
    val note: String = "",
    val time: Long = 0L,
    val accounts: List<Account> = emptyList(),
    val expenseCategories: List<Category> = emptyList(),
    val incomeCategories: List<Category> = emptyList(),
    val merchants: List<Merchant> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val error: String? = null,
    val saved: Boolean = false,
    val editing: Transaction? = null,
)

class AddTransactionViewModel(
    private val ledgerRepository: LedgerRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val merchantRepository: MerchantRepository,
    private val tagRepository: TagRepository,
    private val transactionRepository: TransactionRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState: StateFlow<AddTransactionUiState> = _uiState

    fun load(txId: String?) {
        viewModelScope.launch {
            val ledger = ledgerRepository.currentLedger.first() ?: return@launch
            val accounts = accountRepository.observeByLedger(ledger.id).first()
                .filter { !it.isArchived }
            val categories = categoryRepository.getByLedger(ledger.id)
                .filter { !it.isArchived }
            val expenseCats = categories.filter { it.type == com.personal.ledger.domain.model.CategoryType.EXPENSE }
            val incomeCats = categories.filter { it.type == com.personal.ledger.domain.model.CategoryType.INCOME }
            val merchants = merchantRepository.getByLedger(ledger.id).filter { !it.isArchived }
            val tags = tagRepository.getByLedger(ledger.id).filter { !it.isArchived }

            val editing = txId?.let { transactionRepository.getById(it) }
            val editingTags = editing?.id?.let { editingTagIds(it) } ?: emptySet()
            val lastUsed = settings.lastUsedAccountId.first()
            val accountId = editing?.accountId
                ?: (accounts.firstOrNull { it.id == lastUsed }?.id ?: accounts.firstOrNull()?.id)

            _uiState.value = if (editing != null) {
                AddTransactionUiState(
                    ledgerId = ledger.id,
                    baseCurrency = ledger.baseCurrency,
                    type = editing.type,
                    // 数据库存储最小货币单位，编辑框展示十进制主货币单位。
                    amountText = com.personal.ledger.util.Money.formatMinor(editing.amount),
                    accountId = editing.accountId,
                    targetAccountId = editing.targetAccountId,
                    categoryId = editing.categoryId,
                    merchantId = editing.merchantId,
                    selectedTagIds = editingTags,
                    note = editing.note.orEmpty(),
                    time = editing.transactionTime,
                    accounts = accounts,
                    expenseCategories = expenseCats,
                    incomeCategories = incomeCats,
                    merchants = merchants,
                    tags = tags,
                    editing = editing,
                )
            } else {
                AddTransactionUiState(
                    ledgerId = ledger.id,
                    baseCurrency = ledger.baseCurrency,
                    time = TimeUtils.now(),
                    accountId = accountId,
                    accounts = accounts,
                    expenseCategories = expenseCats,
                    incomeCategories = incomeCats,
                    merchants = merchants,
                    tags = tags,
                )
            }
        }
    }

    private suspend fun editingTagIds(txId: String): Set<String> =
        transactionRepository.getTagIds(txId).toSet()

    fun setType(type: TransactionType) {
        val s = _uiState.value
        if (type == s.type) return
        // 类型切换时清空不再适用的字段（分类 / 转入账户），由用户重新选择。
        _uiState.value = s.copy(
            type = type,
            categoryId = null,
            targetAccountId = null,
            error = null,
        )
    }

    fun setAmountText(text: String) {
        _uiState.value = _uiState.value.copy(amountText = text, error = null)
    }

    fun setAccountId(id: String?) {
        _uiState.value = _uiState.value.copy(accountId = id, error = null)
    }

    fun setTargetAccountId(id: String?) {
        _uiState.value = _uiState.value.copy(targetAccountId = id, error = null)
    }

    fun setCategoryId(id: String?) {
        _uiState.value = _uiState.value.copy(categoryId = id, error = null)
    }

    fun setMerchantId(id: String?) {
        _uiState.value = _uiState.value.copy(merchantId = id, error = null)
    }

    fun toggleTag(id: String) {
        val current = _uiState.value.selectedTagIds
        _uiState.value = _uiState.value.copy(
            selectedTagIds = if (id in current) current - id else current + id,
        )
    }

    fun addMerchant(name: String, onDone: (Result<Unit>) -> Unit) {
        val s = _uiState.value
        viewModelScope.launch {
            val ledger = ledgerRepository.currentLedger.first() ?: return@launch
            merchantRepository.addMerchant(ledger.id, name)
                .onSuccess { m ->
                    _uiState.value = s.copy(
                        merchants = s.merchants + m,
                        merchantId = m.id,
                    )
                    onDone(Result.success(Unit))
                }
                .onFailure { onDone(Result.failure(it)) }
        }
    }

    fun addTag(name: String, onDone: (Result<Unit>) -> Unit) {
        val s = _uiState.value
        viewModelScope.launch {
            val ledger = ledgerRepository.currentLedger.first() ?: return@launch
            tagRepository.addTag(ledger.id, name)
                .onSuccess { t ->
                    _uiState.value = s.copy(
                        tags = s.tags + t,
                        selectedTagIds = s.selectedTagIds + t.id,
                    )
                    onDone(Result.success(Unit))
                }
                .onFailure { onDone(Result.failure(it)) }
        }
    }

    fun setNote(text: String) {
        _uiState.value = _uiState.value.copy(note = text)
    }

    fun setTime(ms: Long) {
        _uiState.value = _uiState.value.copy(time = ms, error = null)
    }

    fun save(onSaved: () -> Unit = {}) {
        val s = _uiState.value
        val amount = com.personal.ledger.util.Money.parseToMinor(s.amountText)
        if (amount == null) {
            _uiState.value = s.copy(error = "请输入有效金额（最多两位小数）")
            return
        }
        if (s.ledgerId == null) {
            _uiState.value = s.copy(error = "账本未就绪")
            return
        }
        val draft = TransactionDraft(
            ledgerId = s.ledgerId!!,
            type = s.type,
            amount = amount,
            currency = s.baseCurrency,
            accountId = s.accountId ?: "",
            targetAccountId = s.targetAccountId,
            categoryId = s.categoryId,
            merchantId = s.merchantId,
            note = s.note,
            transactionTime = s.time,
            tagIds = s.selectedTagIds.toList(),
        )
        viewModelScope.launch {
            val result = if (s.editing != null) {
                transactionRepository.update(s.editing.id, draft)
            } else {
                transactionRepository.add(draft)
            }
            result.onSuccess {
                settings.setLastUsedAccountId(draft.accountId.ifEmpty { null })
                _uiState.value = s.copy(saved = true, error = null)
                onSaved()
            }.onFailure { e ->
                _uiState.value = s.copy(error = e.message ?: "保存失败")
            }
        }
    }
}

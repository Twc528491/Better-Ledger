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
import com.personal.ledger.ui.transaction.CategorySelection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CancellationException
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
    val merchantOptions: List<Merchant> = emptyList(),
    val merchantNotice: String? = null,
    val isResolvingMerchant: Boolean = false,
    val tags: List<Tag> = emptyList(),
    val error: String? = null,
    val saved: Boolean = false,
    val isSaving: Boolean = false,
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
    private var loadedTxId: String? = null
    private var hasLoaded = false
    private var isLoading = false
    private var loadingTxId: String? = null
    private var merchantRequestGeneration = 0
    private var loadGeneration = 0

    fun load(txId: String?) {
        if (hasLoaded && loadedTxId == txId) return
        if (isLoading && loadingTxId == txId) return
        isLoading = true
        loadingTxId = txId
        val generation = ++loadGeneration
        viewModelScope.launch {
          try {
            val ledger = ledgerRepository.currentLedger.first() ?: return@launch
            val editing = txId?.let { transactionRepository.getById(it) }
            val editingTags = editing?.id?.let { editingTagIds(it) } ?: emptySet()
            // 编辑旧账单时保留其已归档关联项，否则用户一打开就会看到“未选择”。
            val accounts = accountRepository.observeByLedger(ledger.id).first()
                .filter { !it.isArchived || it.id == editing?.accountId || it.id == editing?.targetAccountId }
            val allCategories = categoryRepository.getByLedger(ledger.id)
            val editingParentId = allCategories.firstOrNull { it.id == editing?.categoryId }?.parentId
            val categories = allCategories.filter {
                !it.isArchived || it.id == editing?.categoryId || it.id == editingParentId
            }
            val expenseCats = categories.filter { it.type == com.personal.ledger.domain.model.CategoryType.EXPENSE }
            val incomeCats = categories.filter { it.type == com.personal.ledger.domain.model.CategoryType.INCOME }
            val merchants = merchantRepository.getByLedger(ledger.id)
                .filter { !it.isArchived || it.id == editing?.merchantId }
            val merchantOptions = editing?.categoryId?.let {
                CategorySelection.merchantRootId(categories, it)?.let { rootId ->
                    merchantRepository.getByCategory(ledger.id, rootId)
                }.orEmpty()
            } ?: if (editing?.type == TransactionType.TRANSFER)
                merchants.filter { !it.isArchived } else emptyList()
            val tags = tagRepository.getByLedger(ledger.id)
                .filter { !it.isArchived || it.id in editingTags }
            val lastUsed = settings.lastUsedAccountId.first()
            val accountId = editing?.accountId
                ?: (accounts.firstOrNull { it.id == lastUsed }?.id ?: accounts.firstOrNull()?.id)

            if (generation != loadGeneration) return@launch
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
                    merchantOptions = merchantOptions,
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
                    merchantOptions = emptyList(),
                    tags = tags,
                )
            }
            loadedTxId = txId
            hasLoaded = true
          } catch (e: CancellationException) {
            throw e
          } catch (e: Exception) {
            if (generation == loadGeneration) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "加载账单失败，请重试")
            }
          } finally {
            if (generation == loadGeneration) isLoading = false
          }
        }
    }

    private suspend fun editingTagIds(txId: String): Set<String> =
        transactionRepository.getTagIds(txId).toSet()

    fun setType(type: TransactionType) {
        val s = _uiState.value
        if (type == s.type) return
        merchantRequestGeneration++
        // 类型切换时清空不再适用的字段（分类 / 转入账户），由用户重新选择。
        _uiState.value = s.copy(
            type = type,
            categoryId = null,
            targetAccountId = null,
            merchantId = null,
            merchantOptions = if (type == TransactionType.TRANSFER)
                s.merchants.filter { !it.isArchived } else emptyList(),
            merchantNotice = if (s.merchantId != null) "类型已更改，请重新选择商家" else null,
            isResolvingMerchant = false,
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
        val before = _uiState.value
        if (before.categoryId == id) return
        val categories = before.expenseCategories + before.incomeCategories
        val merchantCategoryId = CategorySelection.merchantRootId(categories, id)
        val generation = ++merchantRequestGeneration
        _uiState.value = before.copy(
            categoryId = id, merchantOptions = emptyList(), merchantNotice = null,
            isResolvingMerchant = before.ledgerId != null, error = null,
        )
        val ledgerId = before.ledgerId ?: return
        viewModelScope.launch {
          try {
            val options = if (merchantCategoryId == null) emptyList()
                else merchantRepository.getByCategory(ledgerId, merchantCategoryId)
            val current = _uiState.value
            if (current.categoryId != id || current.ledgerId != ledgerId ||
                generation != merchantRequestGeneration) return@launch
            val clearMerchant = current.merchantId != null && options.none { it.id == current.merchantId }
            _uiState.value = current.copy(
                merchantOptions = options,
                merchantId = if (clearMerchant) null else current.merchantId,
                merchantNotice = if (clearMerchant) "分类已更改，原商家不在此分类商家库，已取消选择" else null,
                isResolvingMerchant = false,
            )
          } catch (e: CancellationException) {
            throw e
          } catch (e: Exception) {
            val current = _uiState.value
            if (current.categoryId == id && generation == merchantRequestGeneration) {
                _uiState.value = current.copy(
                    merchantId = null, isResolvingMerchant = false,
                    merchantNotice = "商家库暂时无法读取，原商家已取消选择",
                    error = e.message ?: "商家库读取失败",
                )
            }
          }
        }
    }

    fun setMerchantId(id: String?) {
        _uiState.value = _uiState.value.copy(merchantId = id, merchantNotice = null, error = null)
    }

    fun toggleTag(id: String) {
        val current = _uiState.value.selectedTagIds
        _uiState.value = _uiState.value.copy(
            selectedTagIds = if (id in current) current - id else current + id,
        )
    }

    fun setSelectedTagIds(ids: Set<String>) {
        _uiState.value = _uiState.value.copy(selectedTagIds = ids)
    }

    fun addMerchant(name: String, onDone: (Result<Unit>) -> Unit) {
        val start = _uiState.value
        val ledgerId = start.ledgerId
        if (ledgerId == null) {
            onDone(Result.failure(IllegalStateException("账本未就绪")))
            return
        }
        viewModelScope.launch {
            val categories = start.expenseCategories + start.incomeCategories
            val rootId = CategorySelection.merchantRootId(categories, start.categoryId)
            merchantRepository.addMerchant(ledgerId, name,
                rootId?.let { setOf(it) } ?: emptySet())
                .onSuccess { m ->
                    val current = _uiState.value
                    if (current.ledgerId == ledgerId) {
                        val sameScope = current.type == start.type && current.categoryId == start.categoryId
                        _uiState.value = current.copy(
                            merchants = current.merchants + m,
                            merchantOptions = if (sameScope) current.merchantOptions + m else current.merchantOptions,
                            merchantId = if (sameScope) m.id else current.merchantId,
                        )
                    }
                    onDone(Result.success(Unit))
                }
                .onFailure { onDone(Result.failure(it)) }
        }
    }

    fun addTag(name: String, onDone: (Result<Unit>) -> Unit) {
        val ledgerId = _uiState.value.ledgerId
        if (ledgerId == null) {
            onDone(Result.failure(IllegalStateException("账本未就绪")))
            return
        }
        viewModelScope.launch {
            tagRepository.addTag(ledgerId, name)
                .onSuccess { t ->
                    val current = _uiState.value
                    if (current.ledgerId == ledgerId) {
                        _uiState.value = current.copy(
                            tags = current.tags + t,
                        )
                    }
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
        if (s.isSaving || s.saved) return
        if (s.isResolvingMerchant) {
            _uiState.value = s.copy(error = "正在检查此分类的商家，请稍候保存")
            return
        }
        val amount = com.personal.ledger.util.Money.parseToMinor(
            com.personal.ledger.ui.transaction.AmountEntry.forSaving(s.amountText),
        )
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
        _uiState.value = s.copy(isSaving = true, error = null)
        viewModelScope.launch {
            val result = if (s.editing != null) {
                transactionRepository.update(s.editing.id, draft)
            } else {
                transactionRepository.add(draft)
            }
            result.onSuccess {
                _uiState.value = _uiState.value.copy(saved = true, isSaving = false, error = null)
                runCatching { settings.setLastUsedAccountId(draft.accountId.ifEmpty { null }) }
                onSaved()
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isSaving = false, error = e.message ?: "保存失败")
            }
        }
    }
}

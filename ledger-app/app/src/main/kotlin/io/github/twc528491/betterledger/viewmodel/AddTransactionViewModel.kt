package io.github.twc528491.betterledger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.twc528491.betterledger.data.repository.AccountRepository
import io.github.twc528491.betterledger.data.repository.CategoryRepository
import io.github.twc528491.betterledger.data.repository.LedgerRepository
import io.github.twc528491.betterledger.data.repository.MerchantRepository
import io.github.twc528491.betterledger.data.repository.TagRepository
import io.github.twc528491.betterledger.data.repository.TransactionDraft
import io.github.twc528491.betterledger.data.repository.TransactionRepository
import io.github.twc528491.betterledger.data.settings.SettingsRepository
import io.github.twc528491.betterledger.domain.calc.AccountRules
import io.github.twc528491.betterledger.domain.model.Account
import io.github.twc528491.betterledger.domain.model.Category
import io.github.twc528491.betterledger.domain.model.CategoryType
import io.github.twc528491.betterledger.domain.model.Merchant
import io.github.twc528491.betterledger.domain.model.Tag
import io.github.twc528491.betterledger.domain.model.Transaction
import io.github.twc528491.betterledger.domain.model.TransactionType
import io.github.twc528491.betterledger.R
import io.github.twc528491.betterledger.util.TimeUtils
import io.github.twc528491.betterledger.util.UiText
import io.github.twc528491.betterledger.util.UserFacingException
import io.github.twc528491.betterledger.util.toUiText
import io.github.twc528491.betterledger.ui.transaction.CategorySelection
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
    val merchantNotice: UiText? = null,
    val isResolvingMerchant: Boolean = false,
    val tags: List<Tag> = emptyList(),
    val error: UiText? = null,
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
            val editing = txId?.let { transactionRepository.getById(it) }
            // 编辑时按账单所属账本加载（资产页的账户详情会列出其他账本的账单）；新建用当前账本。
            val ledger = editing?.let { ledgerRepository.getLedger(it.ledgerId) }
                ?: ledgerRepository.currentLedger.first()
                ?: return@launch
            val editingTags = editing?.id?.let { editingTagIds(it) } ?: emptySet()
            // 账户全局共享：提供币种与账本一致的账户；编辑旧账单时保留其已归档关联项，
            // 否则用户一打开就会看到“未选择”。
            val accounts = AccountRules.usableIn(
                accountRepository.observeByCurrency(ledger.baseCurrency).first(),
                ledger.baseCurrency,
                keepIds = setOfNotNull(editing?.accountId, editing?.targetAccountId),
            )
            val allCategories = categoryRepository.getByLedger(ledger.id)
            val editingParentId = allCategories.firstOrNull { it.id == editing?.categoryId }?.parentId
            val categories = allCategories.filter {
                !it.isArchived || it.id == editing?.categoryId || it.id == editingParentId
            }
            val expenseCats = categories.filter { it.type == io.github.twc528491.betterledger.domain.model.CategoryType.EXPENSE }
            val incomeCats = categories.filter { it.type == io.github.twc528491.betterledger.domain.model.CategoryType.INCOME }
            val merchants = merchantRepository.getByLedger(ledger.id)
                .filter { !it.isArchived || it.id == editing?.merchantId }
            val merchantOptions = editing?.categoryId?.let {
                CategorySelection.merchantRootId(categories, it)?.let { rootId ->
                    merchantRepository.getByCategory(ledger.id, rootId)
                }.orEmpty()
            }.orEmpty()
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
                    amountText = io.github.twc528491.betterledger.util.Money.formatMinor(editing.amount),
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
                _uiState.value = _uiState.value.copy(error = e.toUiText(R.string.add_error_load_failed))
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
            merchantOptions = emptyList(),
            // 转账不设置商家；切到收支类型时提示重新选择。
            merchantNotice = if (s.merchantId != null && type != TransactionType.TRANSFER)
                UiText.res(R.string.add_notice_type_changed) else null,
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
                merchantNotice = if (clearMerchant) UiText.res(R.string.add_notice_category_changed) else null,
                isResolvingMerchant = false,
            )
          } catch (e: CancellationException) {
            throw e
          } catch (e: Exception) {
            val current = _uiState.value
            if (current.categoryId == id && generation == merchantRequestGeneration) {
                _uiState.value = current.copy(
                    merchantId = null, isResolvingMerchant = false,
                    merchantNotice = UiText.res(R.string.add_notice_merchant_unavailable),
                    error = e.toUiText(R.string.add_error_merchant_load_failed),
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
            onDone(Result.failure(UserFacingException(R.string.add_error_ledger_not_ready)))
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
                        // 同名商家会被复用，追加前按 ID 去重，避免列表出现重复项。
                        _uiState.value = current.copy(
                            merchants = (current.merchants + m).distinctBy { it.id },
                            merchantOptions = if (sameScope) (current.merchantOptions + m).distinctBy { it.id }
                                else current.merchantOptions,
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
            onDone(Result.failure(UserFacingException(R.string.add_error_ledger_not_ready)))
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

    /**
     * 记一笔中直接新建二级分类：校验（同名、层级、类型）由 [CategoryRepository.addCategory] 负责。
     * 成功后并入当前分类列表，并在收支类型未切换时直接选中新分类。
     */
    fun addSubcategory(
        parentId: String,
        name: String,
        iconKey: String?,
        onDone: (Result<Category>) -> Unit,
    ) {
        val start = _uiState.value
        val ledgerId = start.ledgerId
        if (ledgerId == null) {
            onDone(Result.failure(UserFacingException(R.string.add_error_ledger_not_ready)))
            return
        }
        val parent = (start.expenseCategories + start.incomeCategories).firstOrNull { it.id == parentId }
        if (parent == null) {
            onDone(Result.failure(UserFacingException(R.string.add_error_parent_missing)))
            return
        }
        viewModelScope.launch {
            categoryRepository.addCategory(ledgerId, name, parent.type, parentId, iconKey)
                .onSuccess { created ->
                    val current = _uiState.value
                    if (current.ledgerId == ledgerId) {
                        _uiState.value = if (created.type == CategoryType.EXPENSE) {
                            current.copy(expenseCategories = (current.expenseCategories + created).distinctBy { it.id })
                        } else {
                            current.copy(incomeCategories = (current.incomeCategories + created).distinctBy { it.id })
                        }
                        val sameType = (current.type == TransactionType.EXPENSE && created.type == CategoryType.EXPENSE) ||
                            (current.type == TransactionType.INCOME && created.type == CategoryType.INCOME)
                        if (sameType) setCategoryId(created.id)
                    }
                    onDone(Result.success(created))
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
            _uiState.value = s.copy(error = UiText.res(R.string.add_error_resolving_merchant))
            return
        }
        val amount = io.github.twc528491.betterledger.util.Money.parseToMinor(
            io.github.twc528491.betterledger.ui.transaction.AmountEntry.forSaving(s.amountText),
        )
        if (amount == null) {
            _uiState.value = s.copy(error = UiText.res(R.string.add_error_invalid_amount))
            return
        }
        if (s.ledgerId == null) {
            _uiState.value = s.copy(error = UiText.res(R.string.add_error_ledger_not_ready))
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
            // 转账不设置商家（MVP：转账仅选填备注）；编辑旧转账保存时一并清除。
            merchantId = if (s.type == TransactionType.TRANSFER) null else s.merchantId,
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
                _uiState.value = _uiState.value.copy(isSaving = false, error = e.toUiText(R.string.add_error_save_failed))
            }
        }
    }
}

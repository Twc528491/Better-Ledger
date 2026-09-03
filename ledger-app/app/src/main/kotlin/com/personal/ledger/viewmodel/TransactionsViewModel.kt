package com.personal.ledger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.ledger.data.repository.AccountRepository
import com.personal.ledger.data.repository.CategoryRepository
import com.personal.ledger.data.repository.LedgerRepository
import com.personal.ledger.data.repository.MerchantRepository
import com.personal.ledger.data.repository.TagRepository
import com.personal.ledger.data.repository.TransactionRepository
import com.personal.ledger.domain.model.Account
import com.personal.ledger.domain.model.Category
import com.personal.ledger.domain.model.Merchant
import com.personal.ledger.domain.model.Tag
import com.personal.ledger.domain.model.Transaction
import com.personal.ledger.domain.model.TransactionType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth

data class TransactionsUiState(
    val ledgerName: String = "",
    val monthLabel: String = "",
    val transactions: List<Transaction> = emptyList(),
    val filter: TransactionType? = null,
    val query: String = "",
    val accountId: String? = null,
    val categoryId: String? = null,
    val tagId: String? = null,
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val merchants: List<Merchant> = emptyList(),
    val tagIdsOf: Map<String, List<String>> = emptyMap(),
    val merchantNameOf: Map<String, String> = emptyMap(),
    val expenseTotal: Long = 0L,
    val incomeTotal: Long = 0L,
    val hasActiveFilter: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModel(
    private val ledgerRepository: LedgerRepository,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val tagRepository: TagRepository,
    private val merchantRepository: MerchantRepository,
    private val transactionTagDao: com.personal.ledger.data.local.dao.TransactionTagDao,
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())
    private val filter = MutableStateFlow<TransactionType?>(null)
    private val query = MutableStateFlow("")
    private val accountId = MutableStateFlow<String?>(null)
    private val categoryId = MutableStateFlow<String?>(null)
    private val tagId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<TransactionsUiState> = combine(
        combine(ledgerRepository.currentLedger, month, filter) { l, ym, f -> Triple(l, ym, f) },
        combine(query, accountId, categoryId, tagId) { q, a, c, t -> FilterParams(q, a, c, t) },
    ) { t3, p4 -> FilterBundle(t3.first, t3.second, t3.third, p4.q, p4.acc, p4.cat, p4.tg) }
        .flatMapLatest { bundle ->
            val ledger = bundle.ledger
            if (ledger == null) {
                flowOf(TransactionsUiState(monthLabel = labelOf(bundle.ym)))
            } else {
                combine(
                    combine(
                        transactionRepository.observeMonth(ledger.id, bundle.ym),
                        accountRepository.observeByLedger(ledger.id),
                        categoryRepository.observeByLedger(ledger.id),
                    ) { txList, accounts, categories -> Triple(txList, accounts, categories) },
                    combine(
                        tagRepository.observeByLedger(ledger.id),
                        merchantRepository.observeByLedger(ledger.id),
                        transactionTagDao.observeByLedger(ledger.id),
                    ) { tags, merchants, tagLinks -> Triple(tags, merchants, tagLinks) },
                ) { t3a, t3b ->
                    val txList = t3a.first
                    val accounts = t3a.second
                    val categories = t3a.third
                    val tags = t3b.first
                    val merchants = t3b.second
                    val tagLinks = t3b.third
                    val merchantNameOf = merchants.associate { it.id to it.name }
                    val tagIdsOf = tagLinks.groupBy({ it.transactionId }, { it.tagId })
                    val filtered = txList.filter { tx ->
                        (bundle.f == null || tx.type == bundle.f) &&
                            (bundle.acc == null || tx.accountId == bundle.acc || tx.targetAccountId == bundle.acc) &&
                            (bundle.cat == null || tx.categoryId == bundle.cat) &&
                            (bundle.tg == null || bundle.tg in (tagIdsOf[tx.id].orEmpty())) &&
                            bundle.q.matches(tx, merchantNameOf)
                    }
                    var expense = 0L
                    var income = 0L
                    txList.forEach {
                        when (it.type) {
                            TransactionType.INCOME -> income += it.amount
                            TransactionType.EXPENSE -> expense += it.amount
                            TransactionType.TRANSFER -> Unit
                        }
                    }
                    TransactionsUiState(
                        ledgerName = ledger.name,
                        monthLabel = labelOf(bundle.ym),
                        transactions = filtered,
                        filter = bundle.f,
                        query = bundle.q,
                        accountId = bundle.acc,
                        categoryId = bundle.cat,
                        tagId = bundle.tg,
                        accounts = accounts,
                        categories = categories,
                        tags = tags,
                        merchants = merchants,
                        tagIdsOf = tagIdsOf,
                        merchantNameOf = merchantNameOf,
                        expenseTotal = expense,
                        incomeTotal = income,
                        hasActiveFilter = bundle.f != null || bundle.q.isNotEmpty() || bundle.acc != null ||
                            bundle.cat != null || bundle.tg != null,
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsUiState())

    private fun String.matches(tx: Transaction, merchantNameOf: Map<String, String>): Boolean {
        if (isBlank()) return true
        val needle = trim()
        if (tx.note?.contains(needle, ignoreCase = true) == true) return true
        val merchantName = tx.merchantId?.let { merchantNameOf[it] }
        return merchantName?.contains(needle, ignoreCase = true) == true
    }

    fun previousMonth() {
        month.value = month.value.minusMonths(1)
    }

    fun nextMonth() {
        month.value = month.value.plusMonths(1)
    }

    fun setMonth(ym: YearMonth) {
        month.value = ym
    }

    fun setFilter(type: TransactionType?) {
        filter.value = type
    }

    fun setQuery(q: String) {
        query.value = q
    }

    fun setAccountFilter(id: String?) {
        accountId.value = id
    }

    fun setCategoryFilter(id: String?) {
        categoryId.value = id
    }

    fun setTagFilter(id: String?) {
        tagId.value = id
    }

    fun clearFilters() {
        filter.value = null
        query.value = ""
        accountId.value = null
        categoryId.value = null
        tagId.value = null
    }

    fun delete(id: String, onDone: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch { onDone(transactionRepository.softDelete(id)) }
    }

    private fun labelOf(ym: YearMonth) = "${ym.year}年${ym.monthValue}月"

    private data class FilterParams(
        val q: String,
        val acc: String?,
        val cat: String?,
        val tg: String?,
    )

    private data class FilterBundle(
        val ledger: com.personal.ledger.domain.model.Ledger?,
        val ym: YearMonth,
        val f: TransactionType?,
        val q: String,
        val acc: String?,
        val cat: String?,
        val tg: String?,
    )
}

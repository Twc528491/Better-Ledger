package com.personal.ledger.ui.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.personal.ledger.R
import com.personal.ledger.data.AppContainer
import com.personal.ledger.domain.model.Merchant
import com.personal.ledger.domain.model.Category
import com.personal.ledger.domain.model.CategoryType
import com.personal.ledger.ui.common.EmptyState
import com.personal.ledger.ui.common.LedgerGroupItem
import com.personal.ledger.ui.common.LedgerIconTile
import com.personal.ledger.ui.common.LedgerListRow
import com.personal.ledger.ui.theme.LedgerSpacing
import com.personal.ledger.ui.common.rememberHapticConfirm
import com.personal.ledger.ui.common.rememberHapticDelete
import com.personal.ledger.ui.common.rememberHapticLongPress
import com.personal.ledger.ui.common.rememberHapticNavigation
import com.personal.ledger.ui.common.rememberHapticReject
import com.personal.ledger.util.userMessage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MerchantsScreen(container: AppContainer, navController: NavHostController) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val currentLedger by container.ledgerRepository.currentLedger.collectAsStateWithLifecycle(initialValue = null)
    val merchants by container.merchantRepository.observeByLedger(
        currentLedger?.id ?: "",
    ).collectAsStateWithLifecycle(initialValue = emptyList())
    val categories by container.categoryRepository.observeByLedger(
        currentLedger?.id ?: "",
    ).collectAsStateWithLifecycle(initialValue = emptyList())
    val links by container.merchantRepository.observeLinksByLedger(
        currentLedger?.id ?: "",
    ).collectAsStateWithLifecycle(initialValue = emptyList())
    var showAdd by remember { mutableStateOf(false) }
    var selectedFilter by remember(currentLedger?.id) { mutableStateOf<String?>(null) }
    var editingMerchant by remember { mutableStateOf<Merchant?>(null) }
    var linkingMerchant by remember { mutableStateOf<Merchant?>(null) }
    var actionMerchant by remember { mutableStateOf<Merchant?>(null) }
    var deleteCandidate by remember { mutableStateOf<Merchant?>(null) }
    var purgeTarget by remember { mutableStateOf<Merchant?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val hapticNavigation = rememberHapticNavigation()
    val hapticLongPress = rememberHapticLongPress()
    val hapticConfirm = rememberHapticConfirm()
    val hapticDelete = rememberHapticDelete()
    val hapticReject = rememberHapticReject()
    val activeMerchants = remember(merchants) { merchants.filterNot { it.isArchived } }
    val archivedMerchants = remember(merchants) { merchants.filter { it.isArchived } }
    val activeRoots = remember(categories) { categories.filter { !it.isArchived && it.parentId == null } }
    val linksByMerchant = remember(links) { links.groupBy({ it.merchantId }, { it.categoryId }) }
    val visibleMerchants = remember(activeMerchants, archivedMerchants, linksByMerchant, activeRoots, selectedFilter) {
        when (selectedFilter) {
            null -> activeMerchants
            ARCHIVED_FILTER -> archivedMerchants
            "unlinked" -> activeMerchants.filter { merchant ->
                linksByMerchant[merchant.id].orEmpty().none { id -> activeRoots.any { it.id == id } }
            }
            else -> activeMerchants.filter { selectedFilter in linksByMerchant[it.id].orEmpty() }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.set_merchants_title)) },
                navigationIcon = {
                    IconButton(onClick = {
                        hapticNavigation()
                        navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        hapticNavigation()
                        error = null
                        showAdd = true
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.set_merchant_new))
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .padding(horizontal = LedgerSpacing.md, vertical = LedgerSpacing.xxs),
                horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.xs),
            ) {
                FilterChip(selected = selectedFilter == null, onClick = { selectedFilter = null },
                    label = { Text(stringResource(R.string.set_merchant_filter_all)) })
                FilterChip(selected = selectedFilter == "unlinked",
                    onClick = { selectedFilter = "unlinked" }, label = { Text(stringResource(R.string.set_merchant_filter_unlinked)) })
                activeRoots.forEach { category ->
                    FilterChip(selected = selectedFilter == category.id,
                        onClick = { selectedFilter = category.id },
                        label = { Text(category.name) })
                }
                if (archivedMerchants.isNotEmpty() || selectedFilter == ARCHIVED_FILTER) {
                    FilterChip(selected = selectedFilter == ARCHIVED_FILTER,
                        onClick = { selectedFilter = ARCHIVED_FILTER },
                        label = { Text(stringResource(R.string.set_merchant_filter_deleted, archivedMerchants.size)) })
                }
            }
            if (visibleMerchants.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.Storefront,
                    title = when (selectedFilter) {
                        null -> stringResource(R.string.set_merchant_empty_all)
                        "unlinked" -> stringResource(R.string.set_merchant_empty_unlinked)
                        ARCHIVED_FILTER -> stringResource(R.string.set_merchant_empty_deleted)
                        else -> stringResource(R.string.set_merchant_empty_category)
                    },
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = LedgerSpacing.md,
                    top = LedgerSpacing.xs,
                    end = LedgerSpacing.md,
                    bottom = LedgerSpacing.xl,
                ),
            ) {
                if (visibleMerchants.isNotEmpty()) {
                    item(key = "merchant_hint") {
                        Text(
                            stringResource(
                                if (selectedFilter == ARCHIVED_FILTER) {
                                    R.string.set_merchant_hint_deleted
                                } else {
                                    R.string.set_merchant_hint
                                },
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = LedgerSpacing.xxs, bottom = LedgerSpacing.xs),
                        )
                    }
                }
                val count = visibleMerchants.size
                itemsIndexed(visibleMerchants, key = { _, merchant -> merchant.id }) { index, merchant ->
                    val expenseLabel = stringResource(R.string.type_expense)
                    val incomeLabel = stringResource(R.string.type_income)
                    val disabledSuffix = stringResource(R.string.set_suffix_disabled)
                    val linkLabelFormat = stringResource(R.string.set_merchant_link_label)
                    val labels = linksByMerchant[merchant.id].orEmpty().mapNotNull { id ->
                        categories.firstOrNull { it.id == id }?.let {
                            String.format(
                                linkLabelFormat,
                                if (it.type == CategoryType.EXPENSE) expenseLabel else incomeLabel,
                                it.name,
                            ) + (if (it.isArchived) disabledSuffix else "")
                        }
                    }
                    val listSeparator = stringResource(R.string.set_list_separator)
                    val noLinks = stringResource(R.string.set_merchant_no_links)
                    LedgerGroupItem(index = index, count = count) {
                        if (merchant.isArchived) {
                            LedgerListRow(
                                title = merchant.name,
                                titleColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                subtitle = labels.joinToString(listSeparator).ifEmpty { noLinks },
                                leading = {
                                    LedgerIconTile(
                                        Icons.Outlined.Storefront,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                                trailing = {
                                    Row {
                                        TextButton(onClick = {
                                            scope.launch {
                                                container.merchantRepository.setArchived(merchant.id, false)
                                                    .onSuccess { hapticConfirm() }
                                                    .onFailure { hapticReject() }
                                            }
                                        }) { Text(stringResource(R.string.action_restore)) }
                                        TextButton(onClick = {
                                            hapticConfirm()
                                            purgeTarget = merchant
                                        }) { Text(stringResource(R.string.set_purge_short), color = MaterialTheme.colorScheme.error) }
                                    }
                                },
                            )
                            return@LedgerGroupItem
                        }
                        MerchantRow(
                            merchant = merchant,
                            subtitle = labels.joinToString(listSeparator).ifEmpty { noLinks },
                            onLongClick = {
                                hapticLongPress()
                                actionMerchant = merchant
                            },
                        )
                    }
                }
            }
        }
    }

    if (showAdd) {
        var name by remember { mutableStateOf("") }
        var selectedCategoryIds by remember {
            mutableStateOf(selectedFilter?.takeIf { id -> activeRoots.any { it.id == id } }
                ?.let { setOf(it) } ?: emptySet())
        }
        var saving by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!saving) showAdd = false },
            title = { Text(stringResource(R.string.set_merchant_new)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.set_merchant_name)) },
                        singleLine = true,
                    )
                    Text(stringResource(R.string.set_merchant_category_hint),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 12.dp))
                    MerchantCategoryPicker(activeRoots, selectedCategoryIds) { selectedCategoryIds = it }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(enabled = !saving, onClick = {
                    val ledgerId = currentLedger?.id ?: return@TextButton
                    saving = true
                    scope.launch {
                        container.merchantRepository.addMerchant(ledgerId, name, selectedCategoryIds)
                            .onSuccess {
                                hapticConfirm()
                                showAdd = false
                                error = null
                            }
                            .onFailure {
                                hapticReject()
                                error = it.userMessage(context, R.string.set_create_failed)
                                saving = false
                            }
                    }
                }) { Text(stringResource(R.string.set_create)) }
            },
            dismissButton = {
                TextButton(enabled = !saving, onClick = { showAdd = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    actionMerchant?.let { merchant ->
        AlertDialog(
            onDismissRequest = { actionMerchant = null },
            title = { Text(stringResource(R.string.set_merchant_actions)) },
            text = { Text(merchant.name) },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = {
                        hapticNavigation()
                        actionMerchant = null
                        error = null
                        linkingMerchant = merchant
                    }) { Text(stringResource(R.string.set_merchant_categories)) }
                    TextButton(onClick = {
                        hapticNavigation()
                        actionMerchant = null
                        error = null
                        editingMerchant = merchant
                    }) { Text(stringResource(R.string.action_rename)) }
                    TextButton(onClick = {
                        hapticConfirm()
                        actionMerchant = null
                        error = null
                        deleteCandidate = merchant
                    }) {
                        Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { actionMerchant = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    linkingMerchant?.let { merchant ->
        val assigned = linksByMerchant[merchant.id].orEmpty().toSet()
        var selectedCategoryIds by remember(merchant.id) {
            mutableStateOf(assigned.intersect(activeRoots.map { it.id }.toSet()))
        }
        var saving by remember(merchant.id) { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!saving) linkingMerchant = null },
            title = { Text(stringResource(R.string.set_merchant_categories_title, merchant.name)) },
            text = {
                Column {
                    Text(stringResource(R.string.set_merchant_categories_note),
                        style = MaterialTheme.typography.bodySmall)
                    MerchantCategoryPicker(activeRoots, selectedCategoryIds) { selectedCategoryIds = it }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(enabled = !saving, onClick = {
                    saving = true
                    scope.launch {
                        container.merchantRepository.replaceCategories(merchant.id, selectedCategoryIds)
                            .onSuccess {
                                hapticConfirm()
                                linkingMerchant = null
                                error = null
                            }.onFailure {
                                hapticReject()
                                error = it.userMessage(context, R.string.set_save_failed)
                                saving = false
                            }
                    }
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = { TextButton(enabled = !saving, onClick = { linkingMerchant = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }

    editingMerchant?.let { merchant ->
        var name by remember(merchant.id) { mutableStateOf(merchant.name) }
        AlertDialog(
            onDismissRequest = { editingMerchant = null },
            title = { Text(stringResource(R.string.set_merchant_rename_title)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.set_merchant_name)) },
                        singleLine = true,
                    )
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        container.merchantRepository.rename(merchant.id, name)
                            .onSuccess {
                                hapticConfirm()
                                editingMerchant = null
                                error = null
                            }
                            .onFailure {
                                hapticReject()
                                error = it.userMessage(context, R.string.set_save_failed)
                            }
                    }
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { editingMerchant = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    purgeTarget?.let { merchant ->
        var usage by remember(merchant.id) { mutableStateOf<Int?>(null) }
        LaunchedEffect(merchant.id) { usage = container.purgeRepository.merchantUsage(merchant.id) }
        PurgeDialog(
            container = container,
            title = stringResource(R.string.set_merchant_purge_title),
            impact = usage?.let {
                pluralStringResource(R.plurals.set_merchant_purge_impact, it, merchant.name, it)
            } ?: stringResource(R.string.set_purge_counting),
            confirmEnabled = usage != null,
            onDismiss = { purgeTarget = null },
            onPurged = { purgeTarget = null },
            purge = { container.purgeRepository.purgeMerchant(merchant.id) },
        )
    }

    deleteCandidate?.let { merchant ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text(stringResource(R.string.action_confirm_delete)) },
            text = { Text(stringResource(R.string.set_merchant_delete_message, merchant.name)) },
            confirmButton = {
                TextButton(onClick = {
                    hapticDelete()
                    scope.launch {
                        container.merchantRepository.delete(merchant.id)
                            .onSuccess {
                                deleteCandidate = null
                                error = null
                            }
                            .onFailure {
                                hapticReject()
                                error = it.userMessage(context, R.string.set_delete_failed)
                            }
                    }
                }) {
                    Text(stringResource(R.string.action_confirm_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

/** 商家筛选中的“已删除（已归档）”选项。 */
private const val ARCHIVED_FILTER = "archived"

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MerchantRow(
    merchant: Merchant,
    subtitle: String,
    onLongClick: () -> Unit,
) {
    LedgerListRow(
        title = merchant.name,
        subtitle = subtitle,
        leading = { LedgerIconTile(Icons.Outlined.Storefront) },
        modifier = Modifier.combinedClickable(
            onClick = {},
            onLongClick = onLongClick,
            hapticFeedbackEnabled = false,
        ),
    )
}

@Composable
private fun MerchantCategoryPicker(
    categories: List<Category>,
    selected: Set<String>,
    onChange: (Set<String>) -> Unit,
) {
    LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
        items(categories, key = { it.id }) { category ->
            FilterChip(
                selected = category.id in selected,
                onClick = {
                    onChange(if (category.id in selected) selected - category.id else selected + category.id)
                },
                label = {
                    Text(
                        stringResource(
                            R.string.set_type_category_chip,
                            stringResource(
                                if (category.type == CategoryType.EXPENSE) R.string.type_expense else R.string.type_income,
                            ),
                            category.name,
                        ),
                    )
                },
            )
        }
    }
}

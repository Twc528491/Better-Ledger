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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MerchantsScreen(container: AppContainer, navController: NavHostController) {
    val scope = rememberCoroutineScope()
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
                title = { Text("商家管理") },
                navigationIcon = {
                    IconButton(onClick = {
                        hapticNavigation()
                        navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        hapticNavigation()
                        error = null
                        showAdd = true
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "新建商家")
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
                    label = { Text("全部") })
                FilterChip(selected = selectedFilter == "unlinked",
                    onClick = { selectedFilter = "unlinked" }, label = { Text("未关联") })
                activeRoots.forEach { category ->
                    FilterChip(selected = selectedFilter == category.id,
                        onClick = { selectedFilter = category.id },
                        label = { Text(category.name) })
                }
                if (archivedMerchants.isNotEmpty() || selectedFilter == ARCHIVED_FILTER) {
                    FilterChip(selected = selectedFilter == ARCHIVED_FILTER,
                        onClick = { selectedFilter = ARCHIVED_FILTER },
                        label = { Text("已删除（${archivedMerchants.size}）") })
                }
            }
            if (visibleMerchants.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.Storefront,
                    title = when (selectedFilter) {
                        null -> "暂无商家，可点击右上角新建"
                        "unlinked" -> "暂无未关联商家"
                        ARCHIVED_FILTER -> "没有已删除的商家"
                        else -> "该分类暂无商家，可新建或长按已有商家分配类别"
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
                            if (selectedFilter == ARCHIVED_FILTER) {
                                "已删除的商家仍保留在历史账单中；可恢复，或彻底删除"
                            } else {
                                "长按商家可分配分类、重命名或删除"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = LedgerSpacing.xxs, bottom = LedgerSpacing.xs),
                        )
                    }
                }
                val count = visibleMerchants.size
                itemsIndexed(visibleMerchants, key = { _, merchant -> merchant.id }) { index, merchant ->
                    val labels = linksByMerchant[merchant.id].orEmpty().mapNotNull { id ->
                        categories.firstOrNull { it.id == id }?.let {
                            (if (it.type == CategoryType.EXPENSE) "支出·" else "收入·") +
                                it.name + (if (it.isArchived) "（已停用）" else "")
                        }
                    }
                    LedgerGroupItem(index = index, count = count) {
                        if (merchant.isArchived) {
                            LedgerListRow(
                                title = merchant.name,
                                titleColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                subtitle = labels.joinToString("、").ifEmpty { "未关联分类" },
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
                                        }) { Text("恢复") }
                                        TextButton(onClick = {
                                            hapticConfirm()
                                            purgeTarget = merchant
                                        }) { Text("彻底删除", color = MaterialTheme.colorScheme.error) }
                                    }
                                },
                            )
                            return@LedgerGroupItem
                        }
                        MerchantRow(
                            merchant = merchant,
                            subtitle = labels.joinToString("、").ifEmpty { "未关联分类" },
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
            title = { Text("新建商家") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("商家名称") },
                        singleLine = true,
                    )
                    Text("所属一级分类（可多选，留空则显示在未关联）",
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
                                error = it.message ?: "创建失败"
                                saving = false
                            }
                    }
                }) { Text("创建") }
            },
            dismissButton = {
                TextButton(enabled = !saving, onClick = { showAdd = false }) { Text("取消") }
            },
        )
    }

    actionMerchant?.let { merchant ->
        AlertDialog(
            onDismissRequest = { actionMerchant = null },
            title = { Text("商家操作") },
            text = { Text(merchant.name) },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = {
                        hapticNavigation()
                        actionMerchant = null
                        error = null
                        linkingMerchant = merchant
                    }) { Text("所属分类") }
                    TextButton(onClick = {
                        hapticNavigation()
                        actionMerchant = null
                        error = null
                        editingMerchant = merchant
                    }) { Text("重命名") }
                    TextButton(onClick = {
                        hapticConfirm()
                        actionMerchant = null
                        error = null
                        deleteCandidate = merchant
                    }) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { actionMerchant = null }) { Text("取消") }
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
            title = { Text("所属分类：" + merchant.name) },
            text = {
                Column {
                    Text("可加入多个一级分类的商家库。历史停用分类的关联会保留。",
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
                                error = it.message ?: "保存失败"
                                saving = false
                            }
                    }
                }) { Text("保存") }
            },
            dismissButton = { TextButton(enabled = !saving, onClick = { linkingMerchant = null }) { Text("取消") } },
        )
    }

    editingMerchant?.let { merchant ->
        var name by remember(merchant.id) { mutableStateOf(merchant.name) }
        AlertDialog(
            onDismissRequest = { editingMerchant = null },
            title = { Text("重命名商家") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("商家名称") },
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
                                error = it.message ?: "保存失败"
                            }
                    }
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { editingMerchant = null }) { Text("取消") }
            },
        )
    }

    purgeTarget?.let { merchant ->
        var usage by remember(merchant.id) { mutableStateOf<Int?>(null) }
        LaunchedEffect(merchant.id) { usage = container.purgeRepository.merchantUsage(merchant.id) }
        PurgeDialog(
            container = container,
            title = "彻底删除商家",
            impact = usage?.let {
                "将永久删除商家“${merchant.name}”及其分类关联。$it 笔账单（含回收站）将不再显示商家，账单本身会保留。"
            } ?: "正在统计影响范围…",
            confirmEnabled = usage != null,
            onDismiss = { purgeTarget = null },
            onPurged = { purgeTarget = null },
            purge = { container.purgeRepository.purgeMerchant(merchant.id) },
        )
    }

    deleteCandidate?.let { merchant ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("确认删除") },
            text = { Text("删除“${merchant.name}”后，历史账单仍会保留商家名称；之后可在“已删除”中恢复或彻底删除。") },
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
                                error = it.message ?: "删除失败"
                            }
                    }
                }) {
                    Text("确认删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) { Text("取消") }
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
                label = { Text((if (category.type == CategoryType.EXPENSE) "支出 · " else "收入 · ") + category.name) },
            )
        }
    }
}

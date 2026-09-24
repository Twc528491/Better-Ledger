package com.personal.ledger.ui.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.personal.ledger.data.AppContainer
import com.personal.ledger.domain.model.Merchant
import com.personal.ledger.domain.model.Category
import com.personal.ledger.domain.model.CategoryType
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
    var error by remember { mutableStateOf<String?>(null) }
    val hapticNavigation = rememberHapticNavigation()
    val hapticLongPress = rememberHapticLongPress()
    val hapticConfirm = rememberHapticConfirm()
    val hapticDelete = rememberHapticDelete()
    val hapticReject = rememberHapticReject()
    val activeMerchants = remember(merchants) { merchants.filterNot { it.isArchived } }
    val activeRoots = remember(categories) { categories.filter { !it.isArchived && it.parentId == null } }
    val linksByMerchant = remember(links) { links.groupBy({ it.merchantId }, { it.categoryId }) }
    val visibleMerchants = remember(activeMerchants, linksByMerchant, activeRoots, selectedFilter) {
        when (selectedFilter) {
            null -> activeMerchants
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
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
            }
            if (visibleMerchants.isEmpty()) {
                Text(
                    when (selectedFilter) {
                        null -> "暂无商家，可点击右上角新建"
                        "unlinked" -> "暂无未关联商家"
                        else -> "该分类暂无商家，可新建或长按已有商家分配类别"
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(visibleMerchants, key = { it.id }) { merchant ->
                    val labels = linksByMerchant[merchant.id].orEmpty().mapNotNull { id ->
                        categories.firstOrNull { it.id == id }?.let {
                            (if (it.type == CategoryType.EXPENSE) "支出·" else "收入·") +
                                it.name + (if (it.isArchived) "（已停用）" else "")
                        }
                    }
                    MerchantRow(
                        merchant = merchant,
                        subtitle = labels.joinToString("、").ifEmpty { "未关联分类" },
                        onLongClick = {
                            hapticLongPress()
                            actionMerchant = merchant
                        },
                    )
                    HorizontalDivider()
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

    deleteCandidate?.let { merchant ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("确认删除") },
            text = { Text("删除“${merchant.name}”后，历史账单仍会保留商家名称。") },
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MerchantRow(
    merchant: Merchant,
    subtitle: String,
    onLongClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick,
                hapticFeedbackEnabled = false,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(merchant.name, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        Text("长按操作", style = MaterialTheme.typography.bodySmall)
    }
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

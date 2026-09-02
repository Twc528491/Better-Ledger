package com.personal.ledger.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.personal.ledger.data.AppContainer
import com.personal.ledger.domain.model.Transaction
import com.personal.ledger.domain.model.TransactionType
import com.personal.ledger.navigation.Routes
import com.personal.ledger.ui.common.CategoryBadge
import com.personal.ledger.ui.common.EmptyState
import com.personal.ledger.ui.common.LedgerColors
import com.personal.ledger.ui.common.amountColor
import com.personal.ledger.util.Money
import com.personal.ledger.util.TimeUtils
import com.personal.ledger.viewmodel.TransactionsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(container: AppContainer, navController: NavHostController) {
    val vm: TransactionsViewModel = viewModel {
        TransactionsViewModel(
            container.ledgerRepository,
            container.transactionRepository,
            container.accountRepository,
            container.categoryRepository,
            container.tagRepository,
            container.merchantRepository,
            container.database.transactionTagDao(),
        )
    }
    val state by vm.uiState.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<Transaction?>(null) }
    var accountMenu by remember { mutableStateOf(false) }
    var categoryMenu by remember { mutableStateOf(false) }
    var tagMenu by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.monthLabel) },
                navigationIcon = {
                    IconButton(onClick = { vm.previousMonth() }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "上一月")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.nextMonth() }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "下一月")
                    }
                    IconButton(onClick = { navController.navigate(Routes.RECYCLE) }) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "回收站")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // 搜索：备注 / 商家
            OutlinedTextField(
                value = state.query,
                onValueChange = { vm.setQuery(it) },
                label = { Text("搜索备注 / 商家") },
                singleLine = true,
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { vm.setQuery("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "清除搜索")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )

            // 类型筛选
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.filter == null,
                    onClick = { vm.setFilter(null) },
                    label = { Text("全部") },
                )
                FilterChip(
                    selected = state.filter == TransactionType.EXPENSE,
                    onClick = { vm.setFilter(TransactionType.EXPENSE) },
                    label = { Text("支出") },
                )
                FilterChip(
                    selected = state.filter == TransactionType.INCOME,
                    onClick = { vm.setFilter(TransactionType.INCOME) },
                    label = { Text("收入") },
                )
                FilterChip(
                    selected = state.filter == TransactionType.TRANSFER,
                    onClick = { vm.setFilter(TransactionType.TRANSFER) },
                    label = { Text("转账") },
                )
            }

            // 账户 / 分类 / 标签筛选
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = state.accountId != null,
                    onClick = { accountMenu = true },
                    label = { Text(state.accounts.firstOrNull { it.id == state.accountId }?.name ?: "账户") },
                    trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                )
                FilterChip(
                    selected = state.categoryId != null,
                    onClick = { categoryMenu = true },
                    label = { Text(state.categories.firstOrNull { it.id == state.categoryId }?.name ?: "分类") },
                    trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                )
                FilterChip(
                    selected = state.tagId != null,
                    onClick = { tagMenu = true },
                    label = { Text(state.tags.firstOrNull { it.id == state.tagId }?.name ?: "标签") },
                    trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                )
                if (state.hasActiveFilter) {
                    TextButton(onClick = { vm.clearFilters() }) { Text("清除") }
                }
            }

            DropdownMenu(expanded = accountMenu, onDismissRequest = { accountMenu = false }) {
                DropdownMenuItem(
                    text = { Text("全部账户") },
                    onClick = {
                        accountMenu = false
                        vm.setAccountFilter(null)
                    },
                )
                state.accounts.forEach { acc ->
                    DropdownMenuItem(
                        text = { Text(acc.name) },
                        onClick = {
                            accountMenu = false
                            vm.setAccountFilter(acc.id)
                        },
                    )
                }
            }
            DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
                DropdownMenuItem(
                    text = { Text("全部分类") },
                    onClick = {
                        categoryMenu = false
                        vm.setCategoryFilter(null)
                    },
                )
                state.categories.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat.name) },
                        onClick = {
                            categoryMenu = false
                            vm.setCategoryFilter(cat.id)
                        },
                    )
                }
            }
            DropdownMenu(expanded = tagMenu, onDismissRequest = { tagMenu = false }) {
                DropdownMenuItem(
                    text = { Text("全部标签") },
                    onClick = {
                        tagMenu = false
                        vm.setTagFilter(null)
                    },
                )
                state.tags.forEach { tag ->
                    DropdownMenuItem(
                        text = { Text(tag.name) },
                        onClick = {
                            tagMenu = false
                            vm.setTagFilter(tag.id)
                        },
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("支出 ${Money.formatWithSymbol(state.expenseTotal)}", style = MaterialTheme.typography.bodySmall)
                Text("收入 ${Money.formatWithSymbol(state.incomeTotal)}", style = MaterialTheme.typography.bodySmall)
            }

            if (state.transactions.isEmpty()) {
                EmptyState(
                    emoji = if (state.hasActiveFilter) "🔍" else "🧾",
                    title = if (state.hasActiveFilter) "没有符合条件的账单" else "本月暂无交易",
                    subtitle = if (state.hasActiveFilter) "试试调整或清除筛选条件" else "",
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                val categoryMap = state.categories.associate { it.id to it.name }
                val accountMap = state.accounts.associate { it.id to it.name }
                val tagMap = state.tags.associate { it.id to it.name }
                // 按日期分组显示（新日期在前）
                val grouped = state.transactions
                    .groupBy { TimeUtils.localDateOf(it.transactionTime) }
                    .toSortedMap(compareByDescending { it })
                LazyColumn {
                    grouped.forEach { (date, list) ->
                        val dayExpense = list.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                        item(key = "header-$date") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    TimeUtils.formatMonthDay(date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()),
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                if (dayExpense > 0) {
                                    Text(
                                        "-${Money.formatMinor(dayExpense)}",
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                }
                            }
                        }
                        items(list, key = { it.id }) { tx ->
                            TransactionItem(
                                tx = tx,
                                categoryNames = categoryMap,
                                merchantNameOf = state.merchantNameOf,
                                accountNames = accountMap,
                                tagNames = tagMap,
                                tagIdsOf = state.tagIdsOf,
                                onClick = { navController.navigate(Routes.add(tx.id)) },
                                onDelete = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    pendingDelete = tx
                                },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { tx ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除账单") },
            text = { Text("删除后进入回收站，可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(tx.id) { pendingDelete = null }
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun TransactionItem(
    tx: Transaction,
    categoryNames: Map<String, String>,
    merchantNameOf: Map<String, String>,
    accountNames: Map<String, String>,
    tagNames: Map<String, String>,
    tagIdsOf: Map<String, List<String>>,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            if (tx.type == TransactionType.TRANSFER) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(LedgerColors.TransferContainer, androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🔁")
                }
            } else {
                val categoryName = categoryNames[tx.categoryId] ?: "其他"
                CategoryBadge(
                    name = categoryName,
                    type = if (tx.type == TransactionType.EXPENSE) {
                        com.personal.ledger.domain.model.CategoryType.EXPENSE
                    } else {
                        com.personal.ledger.domain.model.CategoryType.INCOME
                    },
                )
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    titleOf(tx, categoryNames, merchantNameOf),
                    style = MaterialTheme.typography.bodyLarge,
                )
                // 副标题：分类 · 账户；转账显示 "账户A → 账户B"；商家、标签仅在有内容时展示
                val tags = (tagIdsOf[tx.id].orEmpty()).mapNotNull { tagNames[it] }
                val tagText = if (tags.isEmpty()) "" else " · ${tags.joinToString("|")}"
                val subtitle = if (tx.type == TransactionType.TRANSFER) {
                    "${accountNames[tx.accountId] ?: ""} → ${tx.targetAccountId?.let { accountNames[it] } ?: ""}"
                } else {
                    "${categoryNames[tx.categoryId] ?: ""} · ${accountNames[tx.accountId] ?: ""}"
                }
                Text(
                    subtitle + tagText,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                when (tx.type) {
                    TransactionType.EXPENSE -> Money.formatSignedMinor(-tx.amount)
                    TransactionType.INCOME -> Money.formatSignedMinor(tx.amount, explicitPlus = true)
                    TransactionType.TRANSFER -> Money.formatMinor(tx.amount)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor(tx.type, MaterialTheme.colorScheme.onSurface),
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "删除")
            }
        }
    }
}

private fun titleOf(
    tx: Transaction,
    categoryNames: Map<String, String>,
    merchantNameOf: Map<String, String>,
): String = when {
    tx.merchantId != null && merchantNameOf[tx.merchantId] != null -> merchantNameOf[tx.merchantId]!!
    !tx.note.isNullOrEmpty() -> tx.note!!
    else -> categoryNames[tx.categoryId] ?: "转账"
}

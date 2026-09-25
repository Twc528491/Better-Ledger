package com.personal.ledger.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.personal.ledger.ui.common.TransactionDeleteDialogs
import com.personal.ledger.ui.common.amountColor
import com.personal.ledger.ui.common.rememberHapticLight
import com.personal.ledger.ui.common.rememberHapticLongPress
import com.personal.ledger.ui.common.rememberHapticNavigation
import com.personal.ledger.ui.common.transferContainerColor
import com.personal.ledger.util.Money
import com.personal.ledger.util.TimeUtils
import com.personal.ledger.viewmodel.TransactionsViewModel
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
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
    var deleteCandidate by remember { mutableStateOf<Transaction?>(null) }
    var searchOpen by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    val hapticLight = rememberHapticLight()
    val hapticLongPress = rememberHapticLongPress()
    val hapticNavigation = rememberHapticNavigation()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            hapticLight()
                            showMonthPicker = true
                        },
                    ) {
                        Text(state.monthLabel, style = MaterialTheme.typography.titleLarge)
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "选择月份")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        hapticLight()
                        searchOpen = !searchOpen
                    }) {
                        Icon(Icons.Filled.Search, contentDescription = "搜索")
                    }
                    IconButton(onClick = {
                        hapticLight()
                        showFilterSheet = true
                    }) {
                        Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.FilterList, contentDescription = "筛选")
                            if (state.hasActiveFilter) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(8.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                                )
                            }
                        }
                    }
                    IconButton(onClick = {
                        hapticNavigation()
                        navController.navigate(Routes.RECYCLE)
                    }) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "回收站")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(top = 4.dp)) {
            // 搜索行
            if (searchOpen) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = { vm.setQuery(it) },
                    label = { Text("搜索备注 / 商家") },
                    singleLine = true,
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = {
                                hapticLight()
                                vm.setQuery("")
                            }) {
                                Icon(Icons.Filled.Close, contentDescription = "清除搜索")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            // 筛选状态条
            if (state.hasActiveFilter) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        activeFilterSummary(state),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    TextButton(onClick = {
                        hapticLight()
                        vm.clearFilters()
                    }) { Text("清除筛选") }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("支出 ${Money.formatWithCurrency(state.expenseTotal, state.baseCurrency)}", style = MaterialTheme.typography.bodySmall)
                Text("收入 ${Money.formatWithCurrency(state.incomeTotal, state.baseCurrency)}", style = MaterialTheme.typography.bodySmall)
            }

            if (state.transactions.isEmpty()) {
                EmptyState(
                    emoji = if (state.hasActiveFilter) "🔍" else "🧾",
                    title = if (state.hasActiveFilter) "没有符合条件的账单" else "本月暂无交易",
                    subtitle = if (state.hasActiveFilter) "试试调整或清除筛选条件" else "",
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                val categoryMap = remember(state.categories) { state.categories.associate { it.id to it.name } }
                val accountMap = remember(state.accounts) { state.accounts.associate { it.id to it.name } }
                val tagMap = remember(state.tags) { state.tags.associate { it.id to it.name } }
                val grouped = remember(state.transactions) {
                    state.transactions.groupBy { TimeUtils.localDateOf(it.transactionTime) }
                        .toSortedMap(compareByDescending { it })
                }
                val dayExpenses = remember(grouped) {
                    grouped.mapValues { (_, list) ->
                        list.asSequence().filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                    }
                }
                val dayLabels = remember(grouped) {
                    grouped.keys.associateWith { date ->
                        TimeUtils.formatMonthDay(
                            date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        )
                    }
                }
                LazyColumn {
                    grouped.forEach { (date, list) ->
                        val dayExpense = dayExpenses[date] ?: 0L
                        item(key = "header-$date", contentType = "day-header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    dayLabels[date].orEmpty(),
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
                        items(list, key = { it.id }, contentType = { "transaction" }) { tx ->
                            TransactionItem(
                                tx = tx,
                                categoryNames = categoryMap,
                                merchantNameOf = state.merchantNameOf,
                                accountNames = accountMap,
                                tagNames = tagMap,
                                tagIdsOf = state.tagIdsOf,
                                currency = state.baseCurrency,
                                onClick = {
                                    hapticNavigation()
                                    navController.navigate(Routes.add(tx.id))
                                },
                                onLongClick = {
                                    hapticLongPress()
                                    deleteCandidate = tx
                                },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    if (showMonthPicker) {
        MonthPickerDialog(
            current = state.selectedMonth,
            onPick = { ym ->
                hapticLight()
                vm.setMonth(ym)
                showMonthPicker = false
            },
            onDismiss = { showMonthPicker = false },
        )
    }

    if (showFilterSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
            ) {
                Text("筛选", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.padding(4.dp))

                Text("类型", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.filter == null,
                        onClick = {
                            hapticLight()
                            vm.setFilter(null)
                        },
                        label = { Text("全部") },
                    )
                    listOf(
                        TransactionType.EXPENSE to "支出",
                        TransactionType.INCOME to "收入",
                        TransactionType.TRANSFER to "转账",
                    ).forEach { (t, label) ->
                        FilterChip(
                            selected = state.filter == t,
                            onClick = {
                                hapticLight()
                                vm.setFilter(t)
                            },
                            label = { Text(label) },
                        )
                    }
                }

                Text(
                    "账户",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 12.dp),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.accountId == null,
                        onClick = {
                            hapticLight()
                            vm.setAccountFilter(null)
                        },
                        label = { Text("全部") },
                    )
                    state.accounts.forEach { acc ->
                        FilterChip(
                            selected = state.accountId == acc.id,
                            onClick = {
                                hapticLight()
                                vm.setAccountFilter(acc.id)
                            },
                            label = { Text(acc.name) },
                        )
                    }
                }

                Text(
                    "分类",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 12.dp),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.categoryId == null,
                        onClick = {
                            hapticLight()
                            vm.setCategoryFilter(null)
                        },
                        label = { Text("全部") },
                    )
                    state.categories.forEach { cat ->
                        FilterChip(
                            selected = state.categoryId == cat.id,
                            onClick = {
                                hapticLight()
                                vm.setCategoryFilter(cat.id)
                            },
                            label = { Text(cat.name) },
                        )
                    }
                }

                Text(
                    "标签",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 12.dp),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.tagId == null,
                        onClick = {
                            hapticLight()
                            vm.setTagFilter(null)
                        },
                        label = { Text("全部") },
                    )
                    state.tags.forEach { tag ->
                        FilterChip(
                            selected = state.tagId == tag.id,
                            onClick = {
                                hapticLight()
                                vm.setTagFilter(tag.id)
                            },
                            label = { Text(tag.name) },
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = {
                        hapticLight()
                        vm.clearFilters()
                    }) { Text("清除全部") }
                    TextButton(onClick = {
                        hapticLight()
                        showFilterSheet = false
                    }) { Text("完成") }
                }
            }
        }
    }

    TransactionDeleteDialogs(
        transaction = deleteCandidate,
        onDismiss = { deleteCandidate = null },
        onDelete = { tx, onFinished -> vm.delete(tx.id) { onFinished() } },
    )
}

private fun activeFilterSummary(state: com.personal.ledger.viewmodel.TransactionsUiState): String {
    val parts = mutableListOf<String>()
    if (state.query.isNotEmpty()) parts.add("搜索“${state.query}”")
    state.filter?.let { parts.add(typeLabel(it)) }
    if (state.accountId != null) parts.add(state.accounts.firstOrNull { it.id == state.accountId }?.name ?: "账户")
    if (state.categoryId != null) parts.add(state.categories.firstOrNull { it.id == state.categoryId }?.name ?: "分类")
    if (state.tagId != null) parts.add(state.tags.firstOrNull { it.id == state.tagId }?.name ?: "标签")
    return parts.joinToString(" · ")
}

private fun typeLabel(t: TransactionType): String = when (t) {
    TransactionType.EXPENSE -> "支出"
    TransactionType.INCOME -> "收入"
    TransactionType.TRANSFER -> "转账"
}

/** 月份选择对话框：年箭头 + 12 月网格。 */
@Composable
private fun MonthPickerDialog(
    current: YearMonth,
    onPick: (YearMonth) -> Unit,
    onDismiss: () -> Unit,
) {
    var year by remember { mutableStateOf(current.year) }
    val hapticLight = rememberHapticLight()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = {
                    hapticLight()
                    year--
                }) { Text("‹") }
                Text("${year}年", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = {
                    hapticLight()
                    year++
                }) { Text("›") }
            }
        },
        text = {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onPick(YearMonth.now()) }) { Text("本月") }
                }
                (1..12).chunked(3).forEach { rowMonths ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        rowMonths.forEach { m ->
                            val ym = YearMonth.of(year, m)
                            val selected = ym == current
                            FilterChip(
                                selected = selected,
                                onClick = { onPick(ym) },
                                label = { Text("${m}月", fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                                modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TransactionItem(
    tx: Transaction,
    categoryNames: Map<String, String>,
    merchantNameOf: Map<String, String>,
    accountNames: Map<String, String>,
    tagNames: Map<String, String>,
    tagIdsOf: Map<String, List<String>>,
    currency: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val title = remember(tx, categoryNames, merchantNameOf) { titleOf(tx, categoryNames, merchantNameOf) }
    val time = remember(tx.transactionTime) { timeOfDay(tx.transactionTime) }
    val tags = remember(tx.id, tagIdsOf[tx.id], tagNames) {
        (tagIdsOf[tx.id].orEmpty()).mapNotNull { tagNames[it] }
    }
    val tagText = remember(tags) { if (tags.isEmpty()) "" else " · ${tags.joinToString("|")}" }
    val subtitle = remember(tx, categoryNames, accountNames) {
        if (tx.type == TransactionType.TRANSFER) {
            "${accountNames[tx.accountId] ?: ""} → ${tx.targetAccountId?.let { accountNames[it] } ?: ""}"
        } else {
            "${categoryNames[tx.categoryId] ?: ""} · ${accountNames[tx.accountId] ?: ""}"
        }
    }
    val amountText = remember(tx.amount, tx.type, currency) {
        Money.symbolFor(currency) + when (tx.type) {
            TransactionType.EXPENSE -> Money.formatSignedMinor(-tx.amount)
            TransactionType.INCOME -> Money.formatSignedMinor(tx.amount, explicitPlus = true)
            TransactionType.TRANSFER -> Money.formatMinor(tx.amount)
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                hapticFeedbackEnabled = false,
            )
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            if (tx.type == TransactionType.TRANSFER) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(transferContainerColor(), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🔁", style = MaterialTheme.typography.labelLarge)
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
                    sizeDp = 30,
                    visualKey = tx.categoryId ?: categoryName,
                )
            }
            Column(modifier = Modifier.padding(start = 10.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
                Text(
                    "$time · $subtitle$tagText",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }
        }
        Text(
            amountText,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = amountColor(tx.type),
        )
    }
}

private fun timeOfDay(ms: Long): String {
    val time = java.time.Instant.ofEpochMilli(ms)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalTime()
    return "%02d:%02d".format(time.hour, time.minute)
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

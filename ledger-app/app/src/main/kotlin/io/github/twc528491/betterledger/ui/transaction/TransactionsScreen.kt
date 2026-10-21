package io.github.twc528491.betterledger.ui.transaction

import io.github.twc528491.betterledger.ui.common.MonthPickerSheet
import io.github.twc528491.betterledger.ui.common.MonthSwitcher
import io.github.twc528491.betterledger.ui.common.SettingsActionButton
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.SwapHoriz
import io.github.twc528491.betterledger.ui.common.cardColor
import io.github.twc528491.betterledger.ui.common.expenseAmountColor
import io.github.twc528491.betterledger.ui.common.incomeAmountColor
import io.github.twc528491.betterledger.ui.common.transferAmountColor
import io.github.twc528491.betterledger.ui.common.signedMoney
import io.github.twc528491.betterledger.ui.theme.LedgerNumerals
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import io.github.twc528491.betterledger.data.AppContainer
import io.github.twc528491.betterledger.domain.model.Transaction
import io.github.twc528491.betterledger.domain.model.TransactionType
import io.github.twc528491.betterledger.navigation.Routes
import io.github.twc528491.betterledger.ui.common.CategoryBadge
import io.github.twc528491.betterledger.ui.common.EmptyState
import io.github.twc528491.betterledger.ui.common.MultiSelection
import io.github.twc528491.betterledger.ui.common.SelectionCheckCircle
import io.github.twc528491.betterledger.ui.common.SelectionTopBar
import io.github.twc528491.betterledger.ui.common.amountColor
import io.github.twc528491.betterledger.ui.common.rememberHapticDelete
import io.github.twc528491.betterledger.ui.common.rememberHapticLight
import io.github.twc528491.betterledger.ui.common.rememberHapticLongPress
import io.github.twc528491.betterledger.ui.common.rememberHapticNavigation
import io.github.twc528491.betterledger.ui.common.rememberMultiSelection
import io.github.twc528491.betterledger.ui.common.selectionHighlightColor
import io.github.twc528491.betterledger.ui.common.transferContainerColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.twc528491.betterledger.R
import io.github.twc528491.betterledger.util.DateLabels
import io.github.twc528491.betterledger.util.Money
import io.github.twc528491.betterledger.util.TimeUtils
import io.github.twc528491.betterledger.viewmodel.TransactionsViewModel
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.launch
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
    var searchOpen by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var selection by rememberMultiSelection()
    var confirmBatchDelete by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalContext.current.resources
    val locale = LocalConfiguration.current.locales[0]
    val hapticLight = rememberHapticLight()
    val hapticLongPress = rememberHapticLongPress()
    val hapticNavigation = rememberHapticNavigation()
    val hapticDelete = rememberHapticDelete()

    // 多选只针对当前月份、当前筛选下可见的账单；不再可见的 id 自动移出选择。
    val visibleIds = remember(state.transactions) { state.transactions.map { it.id } }
    LaunchedEffect(visibleIds) { selection = selection.retain(visibleIds) }
    BackHandler(enabled = selection.active) { selection = MultiSelection.None }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
          if (selection.active) {
            SelectionTopBar(
                count = selection.count,
                allSelected = selection.allSelected(visibleIds),
                onClose = { selection = MultiSelection.None },
                onToggleAll = {
                    hapticLight()
                    selection = selection.toggleAll(visibleIds)
                },
            ) {
                IconButton(
                    onClick = { confirmBatchDelete = true },
                    enabled = selection.count > 0,
                ) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = stringResource(R.string.tx_delete_selected))
                }
            }
          } else {
            TopAppBar(
                title = { Text(stringResource(R.string.tx_title)) },
                actions = {
                    IconButton(onClick = {
                        hapticLight()
                        searchOpen = !searchOpen
                    }) {
                        Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.action_search))
                    }
                    IconButton(onClick = {
                        hapticLight()
                        showFilterSheet = true
                    }) {
                        Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.FilterList, contentDescription = stringResource(R.string.action_filter))
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
                        Icon(Icons.Filled.DeleteOutline, contentDescription = stringResource(R.string.tx_recycle_title))
                    }
                    SettingsActionButton {
                        hapticNavigation()
                        navController.navigate(Routes.SETTINGS)
                    }
                },
            )
          }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                MonthSwitcher(
                    label = DateLabels.yearMonth(state.selectedMonth, locale),
                    onPrevious = {
                        hapticLight()
                        vm.setMonth(state.selectedMonth.minusMonths(1))
                    },
                    onNext = {
                        hapticLight()
                        vm.setMonth(state.selectedMonth.plusMonths(1))
                    },
                    onOpenPicker = {
                        hapticLight()
                        showMonthPicker = true
                    },
                )
            }
            // 搜索行
            if (searchOpen) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = { vm.setQuery(it) },
                    label = { Text(stringResource(R.string.tx_search_hint)) },
                    singleLine = true,
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = {
                                hapticLight()
                                vm.setQuery("")
                            }) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.tx_clear_search))
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
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    TextButton(onClick = {
                        hapticLight()
                        vm.clearFilters()
                    }) { Text(stringResource(R.string.tx_clear_filters), maxLines = 1) }
                }
            }

            MonthSummary(
                expense = state.expenseTotal,
                income = state.incomeTotal,
                currency = state.baseCurrency,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )

            if (state.transactions.isEmpty()) {
                EmptyState(
                    emoji = if (state.hasActiveFilter) "🔍" else "🧾",
                    title = stringResource(if (state.hasActiveFilter) R.string.tx_empty_filtered else R.string.tx_empty_month),
                    subtitle = if (state.hasActiveFilter) stringResource(R.string.tx_empty_filtered_hint) else "",
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                val categoryMap = remember(state.categories) { state.categories.associate { it.id to it.name } }
                val categoryIconMap = remember(state.categories) {
                    state.categories.mapNotNull { c -> c.iconKey?.let { c.id to it } }.toMap()
                }
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
                val todayLabel = stringResource(R.string.label_today)
                val yesterdayLabel = stringResource(R.string.label_yesterday)
                val dayHeaderFormat = stringResource(R.string.tx_day_header)
                val dayLabels = remember(grouped, locale, todayLabel, yesterdayLabel, dayHeaderFormat) {
                    grouped.keys.associateWith { date ->
                        dayHeaderLabel(date, locale, todayLabel, yesterdayLabel, dayHeaderFormat)
                    }
                }
                val dayIds = remember(grouped) { grouped.mapValues { (_, list) -> list.map { it.id } } }
                LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)) {
                    grouped.forEach { (date, list) ->
                        val dayExpense = dayExpenses[date] ?: 0L
                        item(key = "header-$date", contentType = "day-header") {
                            val ids = dayIds[date].orEmpty()
                            val dayAllSelected = selection.active && selection.allSelected(ids)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    // 选择模式下点日期头整天选中 / 取消。
                                    .then(
                                        if (selection.active) {
                                            Modifier
                                                .clickable(onClickLabel = stringResource(if (dayAllSelected) R.string.tx_deselect_day else R.string.tx_select_day)) {
                                                    hapticLight()
                                                    selection = selection.toggleAll(ids)
                                                }
                                                .semantics { selected = dayAllSelected }
                                        } else {
                                            Modifier
                                        },
                                    )
                                    .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (selection.active) {
                                        SelectionCheckCircle(
                                            selected = dayAllSelected,
                                            sizeDp = 18,
                                            modifier = Modifier.padding(end = 8.dp),
                                        )
                                    }
                                    Text(
                                        dayLabels[date].orEmpty(),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (dayExpense > 0) {
                                    Text(
                                        stringResource(R.string.tx_day_expense, Money.formatMinor(dayExpense)),
                                        style = LedgerNumerals.of(MaterialTheme.typography.labelMedium),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        items(list, key = { it.id }, contentType = { "transaction" }) { tx ->
                            TransactionItem(
                                tx = tx,
                                categoryNames = categoryMap,
                                categoryIconKeys = categoryIconMap,
                                merchantNameOf = state.merchantNameOf,
                                accountNames = accountMap,
                                tagNames = tagMap,
                                tagIdsOf = state.tagIdsOf,
                                currency = state.baseCurrency,
                                selectionActive = selection.active,
                                selected = tx.id in selection,
                                onClick = {
                                    if (selection.active) {
                                        hapticLight()
                                        selection = selection.toggle(tx.id)
                                    } else {
                                        hapticNavigation()
                                        navController.navigate(Routes.add(tx.id))
                                    }
                                },
                                onLongClick = {
                                    // 长按进入多选并选中该笔；已在多选中则切换选择。
                                    if (!selection.active) hapticLongPress() else hapticLight()
                                    selection = selection.longPress(tx.id)
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showMonthPicker) {
        MonthPickerSheet(
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
                Text(stringResource(R.string.action_filter), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.padding(4.dp))

                Text(stringResource(R.string.tx_filter_type), style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.filter == null,
                        onClick = {
                            hapticLight()
                            vm.setFilter(null)
                        },
                        label = { Text(stringResource(R.string.tx_filter_all)) },
                    )
                    listOf(
                        TransactionType.EXPENSE to R.string.type_expense,
                        TransactionType.INCOME to R.string.type_income,
                        TransactionType.TRANSFER to R.string.type_transfer,
                    ).forEach { (t, label) ->
                        FilterChip(
                            selected = state.filter == t,
                            onClick = {
                                hapticLight()
                                vm.setFilter(t)
                            },
                            label = { Text(stringResource(label)) },
                        )
                    }
                }

                Text(
                    stringResource(R.string.tx_filter_account),
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
                        label = { Text(stringResource(R.string.tx_filter_all)) },
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
                    stringResource(R.string.tx_filter_category),
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
                        label = { Text(stringResource(R.string.tx_filter_all)) },
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
                    stringResource(R.string.tx_filter_tag),
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
                        label = { Text(stringResource(R.string.tx_filter_all)) },
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
                    }) { Text(stringResource(R.string.tx_filter_clear_all)) }
                    TextButton(onClick = {
                        hapticLight()
                        showFilterSheet = false
                    }) { Text(stringResource(R.string.action_done)) }
                }
            }
        }
    }

    if (confirmBatchDelete) {
        val ids = selection.selectedIn(visibleIds)
        val n = ids.size
        AlertDialog(
            onDismissRequest = { confirmBatchDelete = false },
            title = { Text(pluralStringResource(R.plurals.tx_delete_title, n, n)) },
            text = { Text(stringResource(R.string.tx_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        hapticDelete()
                        confirmBatchDelete = false
                        vm.deleteAll(ids) { result ->
                            scope.launch {
                                result.onSuccess { deleted ->
                                    selection = MultiSelection.None
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    val action = snackbarHostState.showSnackbar(
                                        message = resources.getQuantityString(R.plurals.tx_deleted, deleted, deleted),
                                        actionLabel = resources.getString(R.string.action_undo),
                                        duration = SnackbarDuration.Long,
                                    )
                                    if (action == SnackbarResult.ActionPerformed) vm.restoreAll(ids)
                                }.onFailure {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    snackbarHostState.showSnackbar(resources.getString(R.string.tx_delete_failed))
                                }
                            }
                        }
                    },
                    enabled = n > 0,
                ) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmBatchDelete = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun activeFilterSummary(state: io.github.twc528491.betterledger.viewmodel.TransactionsUiState): String {
    val parts = mutableListOf<String>()
    if (state.query.isNotEmpty()) parts.add(stringResource(R.string.tx_filter_search_summary, state.query))
    state.filter?.let { parts.add(typeLabel(it)) }
    if (state.accountId != null) parts.add(state.accounts.firstOrNull { it.id == state.accountId }?.name
        ?: stringResource(R.string.tx_filter_account))
    if (state.categoryId != null) parts.add(state.categories.firstOrNull { it.id == state.categoryId }?.name
        ?: stringResource(R.string.tx_filter_category))
    if (state.tagId != null) parts.add(state.tags.firstOrNull { it.id == state.tagId }?.name
        ?: stringResource(R.string.tx_filter_tag))
    return parts.joinToString(" · ")
}

@Composable
private fun typeLabel(t: TransactionType): String = stringResource(
    when (t) {
        TransactionType.EXPENSE -> R.string.type_expense
        TransactionType.INCOME -> R.string.type_income
        TransactionType.TRANSFER -> R.string.type_transfer
    },
)

/** 日期分组头：中文“9月27日  今天”，英文“Sep 27 · Today”。 */
private fun dayHeaderLabel(
    date: java.time.LocalDate,
    locale: java.util.Locale,
    todayLabel: String,
    yesterdayLabel: String,
    format: String,
): String {
    val today = java.time.LocalDate.now()
    val suffix = when (date) {
        today -> todayLabel
        today.minusDays(1) -> yesterdayLabel
        else -> DateLabels.weekday(date.dayOfWeek, locale)
    }
    return String.format(locale, format, DateLabels.monthDay(date, locale), suffix)
}

/** 当月汇总：支出 / 收入 / 结余三栏，数字等宽。 */
@Composable
private fun MonthSummary(expense: Long, income: Long, currency: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(cardColor(), MaterialTheme.shapes.large)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        SummaryCell(stringResource(R.string.type_expense), Money.formatWithCurrency(expense, currency), expenseAmountColor(), Modifier.weight(1f))
        SummaryCell(stringResource(R.string.type_income), Money.formatWithCurrency(income, currency), incomeAmountColor(), Modifier.weight(1f))
        val balance = income - expense
        SummaryCell(
            stringResource(R.string.label_balance),
            signedMoney(balance, currency, explicitPlus = false),
            MaterialTheme.colorScheme.onSurface,
            Modifier.weight(1f),
        )
    }
}

@Composable
private fun SummaryCell(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier,
) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        Text(value, style = LedgerNumerals.of(MaterialTheme.typography.titleMedium), color = color,
            maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TransactionItem(
    tx: Transaction,
    categoryNames: Map<String, String>,
    categoryIconKeys: Map<String, String>,
    merchantNameOf: Map<String, String>,
    accountNames: Map<String, String>,
    tagNames: Map<String, String>,
    tagIdsOf: Map<String, List<String>>,
    currency: String,
    selectionActive: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val rowBackground by animateColorAsState(
        if (selected) selectionHighlightColor() else androidx.compose.ui.graphics.Color.Transparent,
        animationSpec = tween(150),
        label = "tx-selected-bg",
    )
    val transferLabel = stringResource(R.string.type_transfer)
    val title = remember(tx, categoryNames, merchantNameOf, transferLabel) {
        titleOf(tx, categoryNames, merchantNameOf, transferLabel)
    }
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
        when (tx.type) {
            TransactionType.EXPENSE -> signedMoney(-tx.amount, currency)
            TransactionType.INCOME -> signedMoney(tx.amount, currency)
            TransactionType.TRANSFER -> signedMoney(tx.amount, currency, explicitPlus = false)
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBackground)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                onClickLabel = if (selectionActive) stringResource(if (selected) R.string.tx_deselect else R.string.tx_select)
                    else stringResource(R.string.tx_edit),
                onLongClickLabel = if (selectionActive) null else stringResource(R.string.tx_multi_select),
                hapticFeedbackEnabled = false,
            )
            .then(if (selectionActive) Modifier.semantics { this.selected = selected } else Modifier)
            .heightIn(min = 60.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          // 选中时圆标换成主色对勾圆；多选中未选中的行在圆标右下角给出空心选择圈。
          Box(modifier = Modifier.size(38.dp)) {
            androidx.compose.animation.Crossfade(
                targetState = selected,
                animationSpec = tween(150),
                label = "tx-badge-check",
            ) { showCheck ->
              if (showCheck) {
                SelectionCheckCircle(selected = true, sizeDp = 38)
              } else if (tx.type == TransactionType.TRANSFER) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(transferContainerColor(), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.SwapHoriz, contentDescription = stringResource(R.string.type_transfer),
                        tint = transferAmountColor(), modifier = Modifier.size(20.dp))
                }
              } else {
                val categoryName = categoryNames[tx.categoryId] ?: stringResource(R.string.label_other)
                CategoryBadge(
                    name = categoryName,
                    type = if (tx.type == TransactionType.EXPENSE) {
                        io.github.twc528491.betterledger.domain.model.CategoryType.EXPENSE
                    } else {
                        io.github.twc528491.betterledger.domain.model.CategoryType.INCOME
                    },
                    sizeDp = 38,
                    visualKey = tx.categoryId ?: categoryName,
                    iconKey = categoryIconKeys[tx.categoryId],
                )
              }
            }
            if (selectionActive && !selected) {
                SelectionCheckCircle(
                    selected = false,
                    sizeDp = 14,
                    modifier = Modifier.align(Alignment.BottomEnd),
                )
            }
          }
            Column(modifier = Modifier.padding(start = 12.dp, end = 8.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Text(
                    "$time · $subtitle$tagText",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            amountText,
            style = LedgerNumerals.of(MaterialTheme.typography.titleMedium),
            fontWeight = FontWeight.SemiBold,
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
    transferLabel: String,
): String = when {
    tx.merchantId != null && merchantNameOf[tx.merchantId] != null -> merchantNameOf[tx.merchantId]!!
    !tx.note.isNullOrEmpty() -> tx.note!!
    else -> categoryNames[tx.categoryId] ?: transferLabel
}

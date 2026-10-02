package com.personal.ledger.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.personal.ledger.R
import com.personal.ledger.data.AppContainer
import com.personal.ledger.domain.model.Budget
import com.personal.ledger.domain.model.CategoryType
import com.personal.ledger.ui.common.CategoryBadge
import com.personal.ledger.ui.common.LedgerGroupItem
import com.personal.ledger.ui.common.LedgerListRow
import com.personal.ledger.ui.common.LedgerSectionHeader
import com.personal.ledger.ui.common.MoneyText
import com.personal.ledger.ui.common.cardColor
import com.personal.ledger.ui.theme.LedgerSpacing
import com.personal.ledger.ui.common.rememberHapticConfirm
import com.personal.ledger.ui.common.rememberHapticDelete
import com.personal.ledger.ui.common.rememberHapticLight
import com.personal.ledger.ui.common.rememberHapticNavigation
import com.personal.ledger.ui.common.rememberHapticReject
import com.personal.ledger.util.DateLabels
import com.personal.ledger.util.Money
import com.personal.ledger.viewmodel.BudgetsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(container: AppContainer, navController: NavHostController) {
    val vm: BudgetsViewModel = viewModel {
        BudgetsViewModel(container.ledgerRepository, container.budgetRepository)
    }
    val state by vm.uiState.collectAsStateWithLifecycle()
    val currentLedger by container.ledgerRepository.currentLedger.collectAsStateWithLifecycle(initialValue = null)
    val categories by container.categoryRepository.observeByLedger(
        currentLedger?.id ?: ""
    ).collectAsStateWithLifecycle(initialValue = emptyList())
    var editingCategoryId by remember { mutableStateOf<String?>(null) }
    var editingTotal by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    val hapticLight = rememberHapticLight()
    val hapticNavigation = rememberHapticNavigation()
    val hapticConfirm = rememberHapticConfirm()
    val hapticDelete = rememberHapticDelete()
    val hapticReject = rememberHapticReject()

    val categoryNames = categories.associate { it.id to it.name }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val locale = LocalConfiguration.current.locales[0]
                    val monthLabel = runCatching { java.time.YearMonth.parse(state.month) }.getOrNull()
                        ?.let { DateLabels.yearMonth(it, locale) }
                    Text(
                        if (monthLabel != null) {
                            stringResource(R.string.set_budgets_title_month, monthLabel)
                        } else {
                            stringResource(R.string.set_budgets_title)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
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
                        hapticLight()
                        vm.previousMonth()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.set_budget_prev_month))
                    }
                    IconButton(onClick = {
                        hapticLight()
                        vm.nextMonth()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.set_budget_next_month))
                    }
                },
            )
        },
    ) { padding ->
        val expenseCategories = categories.filter { it.type == CategoryType.EXPENSE }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(
                start = LedgerSpacing.md,
                top = LedgerSpacing.xs,
                end = LedgerSpacing.md,
                bottom = LedgerSpacing.xl,
            ),
        ) {
            item {
                val total = state.budgets.firstOrNull { it.categoryId == null }
                Surface(
                    onClick = {
                        hapticNavigation()
                        editingTotal = true
                    },
                    shape = MaterialTheme.shapes.large,
                    color = cardColor(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = LedgerSpacing.md, vertical = LedgerSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.set_budget_monthly_total),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (total != null) {
                                MoneyText(text = Money.formatWithSymbol(total.amount), prominent = true)
                            } else {
                                Text(
                                    stringResource(R.string.set_budget_not_set),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }
            item {
                LedgerSectionHeader(
                    title = stringResource(R.string.set_budget_category_section),
                    modifier = Modifier.padding(start = LedgerSpacing.xxs, top = LedgerSpacing.lg, bottom = LedgerSpacing.xxs),
                )
            }
            val count = expenseCategories.size
            itemsIndexed(expenseCategories, key = { _, cat -> cat.id }) { index, cat ->
                val budget = state.budgets.firstOrNull { it.categoryId == cat.id }
                LedgerGroupItem(index = index, count = count) {
                    LedgerListRow(
                        title = cat.name,
                        leading = {
                            CategoryBadge(
                                name = cat.name,
                                type = cat.type,
                                sizeDp = 36,
                                visualKey = cat.id,
                                iconKey = cat.iconKey,
                            )
                        },
                        trailing = {
                            if (budget != null) {
                                MoneyText(text = Money.formatWithSymbol(budget.amount))
                            } else {
                                Text(
                                    stringResource(R.string.set_budget_not_set),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        onClick = {
                            hapticNavigation()
                            editingCategoryId = cat.id
                        },
                    )
                }
            }
            item {
                TextButton(
                    onClick = {
                        hapticNavigation()
                        showCategoryPicker = true
                    },
                    modifier = Modifier.padding(top = LedgerSpacing.xs),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(LedgerSpacing.xs))
                    Text(stringResource(R.string.set_budget_add_for_category))
                }
            }
        }
    }

    if (showCategoryPicker) {
        AlertDialog(
            onDismissRequest = {
                showCategoryPicker = false
            },
            title = { Text(stringResource(R.string.set_budget_pick_category)) },
            text = {
                Column {
                    categories
                        .filter { it.type == com.personal.ledger.domain.model.CategoryType.EXPENSE }
                        .forEach { cat ->
                            Text(
                                cat.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        hapticLight()
                                        showCategoryPicker = false
                                        editingCategoryId = cat.id
                                    }
                                    .padding(vertical = 12.dp),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCategoryPicker = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    if (editingTotal) {
        BudgetAmountDialog(
            title = stringResource(R.string.set_budget_monthly_total),
            initial = state.budgets.firstOrNull { it.categoryId == null }?.amount,
            onConfirm = { minor ->
                vm.setBudget(null, minor) { result ->
                    result.onSuccess {
                        hapticConfirm()
                        editingTotal = false
                    }.onFailure { hapticReject() }
                }
            },
            onDelete = {
                vm.deleteBudget(null) { result ->
                    result.onSuccess {
                        hapticDelete()
                        editingTotal = false
                    }.onFailure { hapticReject() }
                }
            },
            onDismiss = {
                editingTotal = false
            },
        )
    }

    editingCategoryId?.let { catId ->
        if (catId.isNotEmpty()) {
            BudgetAmountDialog(
                title = stringResource(R.string.set_budget_category_title, categoryNames[catId] ?: ""),
                initial = state.budgets.firstOrNull { it.categoryId == catId }?.amount,
                onConfirm = { minor ->
                    vm.setBudget(catId, minor) { result ->
                        result.onSuccess {
                            hapticConfirm()
                            editingCategoryId = null
                        }.onFailure { hapticReject() }
                    }
                },
                onDelete = {
                vm.deleteBudget(catId) { result ->
                    result.onSuccess {
                        hapticDelete()
                        editingCategoryId = null
                        }.onFailure { hapticReject() }
                    }
                },
                onDismiss = {
                    editingCategoryId = null
                },
            )
        }
    }
}

@Composable
private fun BudgetAmountDialog(
    title: String,
    initial: Long?,
    onConfirm: (Long) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initial?.let { Money.formatMinor(it) } ?: "") }
    val hapticReject = rememberHapticReject()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.set_budget_amount)) },
                    singleLine = true,
                )
                if (initial != null) {
                    TextButton(onClick = onDelete) { Text(stringResource(R.string.set_budget_delete)) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val minor = Money.parseToMinor(text)
                if (minor != null && minor > 0) {
                    onConfirm(minor)
                } else {
                    hapticReject()
                }
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

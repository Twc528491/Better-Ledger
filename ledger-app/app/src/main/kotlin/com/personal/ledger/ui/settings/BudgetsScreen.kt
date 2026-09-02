package com.personal.ledger.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.personal.ledger.data.AppContainer
import com.personal.ledger.domain.model.Budget
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

    val categoryNames = categories.associate { it.id to it.name }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("预算设置 · ${state.month}") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.previousMonth() }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "上一月")
                    }
                    IconButton(onClick = { vm.nextMonth() }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "下一月")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { editingTotal = true }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("月总预算", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        state.budgets.firstOrNull { it.categoryId == null }
                            ?.let { Money.formatWithSymbol(it.amount) } ?: "未设置",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                HorizontalDivider()
            }
            items(
                categories.filter { it.type == com.personal.ledger.domain.model.CategoryType.EXPENSE },
                key = { it.id },
            ) { cat ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { editingCategoryId = cat.id }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(cat.name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        state.budgets.firstOrNull { it.categoryId == cat.id }
                            ?.let { Money.formatWithSymbol(it.amount) } ?: "未设置",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                HorizontalDivider()
            }
            item {
                TextButton(onClick = { showCategoryPicker = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("为分类设置预算")
                }
            }
        }
    }

    if (showCategoryPicker) {
        AlertDialog(
            onDismissRequest = { showCategoryPicker = false },
            title = { Text("选择分类") },
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
                TextButton(onClick = { showCategoryPicker = false }) { Text("取消") }
            },
        )
    }

    if (editingTotal) {
        BudgetAmountDialog(
            title = "月总预算",
            initial = state.budgets.firstOrNull { it.categoryId == null }?.amount,
            onConfirm = { minor ->
                vm.setBudget(null, minor) { editingTotal = false }
            },
            onDelete = {
                vm.deleteBudget(null) { editingTotal = false }
            },
            onDismiss = { editingTotal = false },
        )
    }

    editingCategoryId?.let { catId ->
        if (catId.isNotEmpty()) {
            BudgetAmountDialog(
                title = "分类预算（${categoryNames[catId] ?: ""}）",
                initial = state.budgets.firstOrNull { it.categoryId == catId }?.amount,
                onConfirm = { minor ->
                    vm.setBudget(catId, minor) { editingCategoryId = null }
                },
                onDelete = {
                    vm.deleteBudget(catId) { editingCategoryId = null }
                },
                onDismiss = { editingCategoryId = null },
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("预算金额") },
                    singleLine = true,
                )
                if (initial != null) {
                    TextButton(onClick = onDelete) { Text("删除预算") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val minor = Money.parseToMinor(text)
                if (minor != null && minor > 0) onConfirm(minor)
            }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

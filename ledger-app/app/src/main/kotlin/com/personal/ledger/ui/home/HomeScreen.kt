package com.personal.ledger.ui.home

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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.personal.ledger.navigation.Routes
import com.personal.ledger.util.Money
import com.personal.ledger.util.TimeUtils
import com.personal.ledger.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(container: AppContainer, navController: NavHostController) {
    val vm: HomeViewModel = viewModel {
        HomeViewModel(
            container.ledgerRepository,
            container.transactionRepository,
            container.budgetRepository,
            container.categoryRepository,
            container.merchantRepository,
            container.accountRepository,
        )
    }
    val state by vm.uiState.collectAsStateWithLifecycle()
    val ledgers by container.ledgerRepository.ledgers.collectAsStateWithLifecycle(initialValue = emptyList())
    var ledgerMenuOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { ledgerMenuOpen = true },
                    ) {
                        Text(state.ledgerName.ifEmpty { "个人账本" }, style = MaterialTheme.typography.titleLarge)
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "切换账本")
                        DropdownMenu(expanded = ledgerMenuOpen, onDismissRequest = { ledgerMenuOpen = false }) {
                            ledgers.forEach { ledger ->
                                DropdownMenuItem(
                                    text = { Text(ledger.name) },
                                    onClick = {
                                        ledgerMenuOpen = false
                                        vm.switchLedger(ledger.id)
                                    },
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.SETTINGS) }) {
                        Icon(Icons.Filled.Settings, contentDescription = "设置")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    // 本月支出（金额最高视觉优先级）
                    Text("${state.monthLabel}支出", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        Money.formatWithSymbol(state.expense),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("收入", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                Money.formatSignedMinor(state.income, explicitPlus = true),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("结余", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                Money.formatSignedMinor(state.income - state.expense, explicitPlus = true),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                }
            }

            // 预算进度（总预算）
            item {
                BudgetSection(state.totalBudget)
            }

            item {
                Text(
                    "最近交易",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }

            if (state.recent.isEmpty()) {
                item {
                    Text(
                        "还没有交易，点右下角 + 记一笔",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                items(state.recent, key = { it.id }) { tx ->
                    TransactionRow(
                        tx = tx,
                        categoryNames = state.categoryNames,
                        merchantNames = state.merchantNames,
                        accountNames = state.accountNames,
                        onClick = { navController.navigate(Routes.add(tx.id)) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun BudgetSection(progress: com.personal.ledger.data.repository.BudgetProgress?) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        if (progress == null) {
            Text("本月未设置总预算", style = MaterialTheme.typography.bodyMedium)
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("预算", style = MaterialTheme.typography.bodyMedium)
                val pct = progress.permille?.div(10) ?: 0
                Text(
                    "${Money.formatMinor(progress.spent)} / ${Money.formatMinor(progress.budget.amount)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text("$pct%", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { ((progress.permille ?: 0).coerceAtMost(1000)) / 1000f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
            )
        }
    }
}

@Composable
private fun TransactionRow(
    tx: Transaction,
    categoryNames: Map<String, String>,
    merchantNames: Map<String, String>,
    accountNames: Map<String, String>,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            // 标题优先级：商家 > 备注 > 分类
            Text(
                titleOf(tx, categoryNames, merchantNames),
                style = MaterialTheme.typography.bodyLarge,
            )
            // 副标题：分类 · 账户
            Text(
                "${categoryNames[tx.categoryId] ?: "转账"} · ${accountNames[tx.accountId] ?: ""}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            when (tx.type) {
                com.personal.ledger.domain.model.TransactionType.EXPENSE -> Money.formatSignedMinor(-tx.amount)
                com.personal.ledger.domain.model.TransactionType.INCOME -> Money.formatSignedMinor(tx.amount, explicitPlus = true)
                com.personal.ledger.domain.model.TransactionType.TRANSFER -> Money.formatMinor(tx.amount)
            },
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

private fun titleOf(
    tx: Transaction,
    categoryNames: Map<String, String>,
    merchantNames: Map<String, String>,
): String = when {
    tx.merchantId != null && merchantNames[tx.merchantId] != null -> merchantNames[tx.merchantId]!!
    !tx.note.isNullOrEmpty() -> tx.note!!
    else -> categoryNames[tx.categoryId] ?: "转账"
}

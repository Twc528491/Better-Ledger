package com.personal.ledger.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.personal.ledger.ui.common.expenseAmountColor
import com.personal.ledger.ui.common.expenseContainerColor
import com.personal.ledger.ui.common.incomeAmountColor
import com.personal.ledger.ui.common.incomeContainerColor
import com.personal.ledger.ui.common.transferAmountColor
import com.personal.ledger.ui.common.transferContainerColor
import com.personal.ledger.ui.common.amountColor
import com.personal.ledger.ui.common.budgetColor
import com.personal.ledger.ui.common.highlightCardColor
import com.personal.ledger.ui.common.highlightCardContentColor
import com.personal.ledger.util.Money
import com.personal.ledger.viewmodel.HomeViewModel
import androidx.compose.ui.graphics.Color

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
    val haptic = LocalHapticFeedback.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            ledgerMenuOpen = true
                        },
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
                    IconButton(onClick = { navController.navigate(Routes.BOOKKEEPING_SETTINGS) }) {
                        Icon(Icons.Filled.Tune, contentDescription = "记账设置")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        ) {
            // 本月支出主卡片（金额最高视觉优先级）
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = highlightCardColor(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("${state.monthLabel}支出", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            Money.formatWithSymbol(state.expense),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = highlightCardContentColor(),
                        )
                    }
                }
            }

            // 收入 / 结余 双卡片
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = incomeContainerColor(),
                        modifier = Modifier.weight(1f),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("收入", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                Money.formatSignedMinor(state.income, explicitPlus = true),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = incomeAmountColor(),
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (state.income - state.expense >= 0) transferContainerColor() else expenseContainerColor(),
                        modifier = Modifier.weight(1f),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("结余", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                Money.formatSignedMinor(state.income - state.expense, explicitPlus = true),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (state.income - state.expense >= 0) transferAmountColor() else expenseAmountColor(),
                            )
                        }
                    }
                }
            }

            // 预算卡片（进度条随使用率变色：绿 → 橙 → 红）
            item {
                BudgetSection(state.totalBudget)
            }

            // 最近交易卡片
            item {
                Text(
                    "最近交易",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
            }

            if (state.recent.isEmpty()) {
                item {
                    EmptyState(emoji = "🧾", title = "还没有交易", subtitle = "点右下角 + 记一笔")
                }
            } else {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column {
                            state.recent.forEachIndexed { index, tx ->
                                TransactionRow(
                                    tx = tx,
                                    categoryNames = state.categoryNames,
                                    merchantNames = state.merchantNames,
                                    accountNames = state.accountNames,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        navController.navigate(Routes.add(tx.id))
                                    },
                                )
                                if (index < state.recent.lastIndex) HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetSection(progress: com.personal.ledger.data.repository.BudgetProgress?) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (progress == null) {
                Text("本月未设置总预算", style = MaterialTheme.typography.bodyMedium)
            } else {
                val pct = progress.permille?.div(10) ?: 0
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("预算", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${Money.formatMinor(progress.spent)} / ${Money.formatMinor(progress.budget.amount)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "$pct%",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = budgetColor(progress.permille, MaterialTheme.colorScheme.onSurface),
                    )
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { ((progress.permille ?: 0).coerceAtMost(1000)) / 1000f },
                    color = budgetColor(progress.permille, MaterialTheme.colorScheme.primary),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp),
                )
            }
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
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val categoryName = categoryNames[tx.categoryId]
        val type = if (tx.type == TransactionType.TRANSFER) {
            com.personal.ledger.domain.model.CategoryType.EXPENSE // 转账用占位，不显示分类色
        } else {
            com.personal.ledger.domain.model.CategoryType.entries.first {
                it.name == (if (tx.type == TransactionType.EXPENSE) "EXPENSE" else "INCOME")
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (tx.type == TransactionType.TRANSFER) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(transferContainerColor(), androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🔁")
                }
            } else {
                CategoryBadge(name = categoryName ?: "其他", type = type)
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    titleOf(tx, categoryNames, merchantNames),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    "${categoryName ?: "转账"} · ${accountNames[tx.accountId] ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Text(
            when (tx.type) {
                TransactionType.EXPENSE -> Money.formatSignedMinor(-tx.amount)
                TransactionType.INCOME -> Money.formatSignedMinor(tx.amount, explicitPlus = true)
                TransactionType.TRANSFER -> Money.formatMinor(tx.amount)
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = amountColor(tx.type),
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

package com.personal.ledger.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material3.CircularProgressIndicator
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
import com.personal.ledger.ui.common.expenseAmountColor
import com.personal.ledger.ui.common.incomeAmountColor
import com.personal.ledger.ui.common.transferAmountColor
import com.personal.ledger.ui.common.transferContainerColor
import com.personal.ledger.ui.common.amountColor
import com.personal.ledger.ui.common.balanceColor
import com.personal.ledger.ui.common.budgetColor
import com.personal.ledger.ui.common.cardColor
import com.personal.ledger.ui.common.highlightCardColor
import com.personal.ledger.ui.common.highlightCardContentColor
import com.personal.ledger.ui.common.LedgerCard
import com.personal.ledger.ui.common.LedgerSectionHeader
import com.personal.ledger.ui.common.rememberHapticLight
import com.personal.ledger.ui.common.rememberHapticLongPress
import com.personal.ledger.ui.common.rememberHapticNavigation
import com.personal.ledger.util.Money
import com.personal.ledger.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    var deleteCandidate by remember { mutableStateOf<Transaction?>(null) }
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
                            ledgerMenuOpen = true
                        },
                    ) {
                        Text(state.ledgerName.ifEmpty { "个人账本" }, style = MaterialTheme.typography.titleLarge)
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "切换账本")
                        DropdownMenu(expanded = ledgerMenuOpen, onDismissRequest = { ledgerMenuOpen = false }) {
                            ledgers.filter { !it.isArchived }.forEach { ledger ->
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
                    IconButton(onClick = {
                        hapticNavigation()
                        navController.navigate(Routes.SETTINGS)
                    }) {
                        Icon(Icons.Filled.Tune, contentDescription = "设置")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            // 本月支出主卡片（金额最高视觉优先级）
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = highlightCardColor(),
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 0.dp,
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Text("${state.monthLabel}支出", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            Money.formatWithCurrency(state.expense, state.baseCurrency),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = highlightCardContentColor(),
                        )
                    }
                }
            }

            // 收入 / 结余放在同一低层级容器，减少首页卡片堆叠。
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = cardColor(),
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 0.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("收入", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                Money.symbolFor(state.baseCurrency) + Money.formatSignedMinor(state.income, explicitPlus = true),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = incomeAmountColor(),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("结余", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                Money.symbolFor(state.baseCurrency) + Money.formatSignedMinor(state.income - state.expense, explicitPlus = true),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = balanceColor(state.income - state.expense),
                            )
                        }
                    }
                }
            }

            // 预算卡片（进度条随使用率变色：绿 → 橙 → 红）
            item {
                BudgetSection(state.totalBudget, state.baseCurrency) {
                    hapticNavigation()
                    navController.navigate(Routes.BUDGETS)
                }
            }

            // 最近交易卡片
            item {
                LedgerSectionHeader(
                    title = "最近交易",
                    modifier = Modifier.padding(top = 8.dp),
                    actionLabel = "查看全部",
                    onAction = {
                        hapticNavigation()
                        navController.navigate(Routes.TRANSACTIONS)
                    },
                )
            }

            if (state.loading) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (state.recent.isEmpty()) {
                item {
                    EmptyState(emoji = "🧾", title = "还没有交易", subtitle = "点底部中央 + 记一笔")
                }
            } else {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = cardColor(),
                        modifier = Modifier.fillMaxWidth(),
                        tonalElevation = 0.dp,
                    ) {
                        Column {
                            state.recent.forEachIndexed { index, tx ->
                                TransactionRow(
                                    tx = tx,
                                    categoryNames = state.categoryNames,
                                    merchantNames = state.merchantNames,
                                    accountNames = state.accountNames,
                                    onClick = {
                                        hapticNavigation()
                                        navController.navigate(Routes.add(tx.id))
                                    },
                                    onLongClick = {
                                        hapticLongPress()
                                        deleteCandidate = tx
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

    TransactionDeleteDialogs(
        transaction = deleteCandidate,
        onDismiss = { deleteCandidate = null },
        onDelete = { tx, onFinished -> vm.delete(tx.id) { onFinished() } },
    )
}

@Composable
private fun BudgetSection(
    progress: com.personal.ledger.data.repository.BudgetProgress?,
    currency: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = cardColor(),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            if (progress == null) {
                Text("本月未设置总预算", style = MaterialTheme.typography.bodyMedium)
            } else {
                val pct = progress.permille?.div(10) ?: 0
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("预算", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${Money.formatWithCurrency(progress.spent, currency)} / ${Money.formatWithCurrency(progress.budget.amount, currency)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "$pct%",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = budgetColor(progress.permille, MaterialTheme.colorScheme.onSurface),
                    )
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { ((progress.permille ?: 0).coerceAtMost(1000)) / 1000f },
                    color = budgetColor(progress.permille, MaterialTheme.colorScheme.primary),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun TransactionRow(
    tx: Transaction,
    categoryNames: Map<String, String>,
    merchantNames: Map<String, String>,
    accountNames: Map<String, String>,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                hapticFeedbackEnabled = false,
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
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
                        .size(32.dp)
                        .background(transferContainerColor(), androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🔁")
                }
            } else {
                CategoryBadge(
                    name = categoryName ?: "其他",
                    type = type,
                    sizeDp = 32,
                    visualKey = tx.categoryId ?: categoryName.orEmpty(),
                )
            }
            Column(modifier = Modifier.padding(start = 10.dp)) {
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

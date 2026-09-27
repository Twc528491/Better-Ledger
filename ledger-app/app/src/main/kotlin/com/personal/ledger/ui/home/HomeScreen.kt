package com.personal.ledger.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.personal.ledger.data.AppContainer
import com.personal.ledger.data.repository.BudgetProgress
import com.personal.ledger.domain.model.CategoryType
import com.personal.ledger.domain.model.Transaction
import com.personal.ledger.domain.model.TransactionType
import com.personal.ledger.navigation.Routes
import com.personal.ledger.ui.common.CategoryBadge
import com.personal.ledger.ui.common.EmptyState
import com.personal.ledger.ui.common.HeroFigure
import com.personal.ledger.ui.common.LedgerHeroCard
import com.personal.ledger.ui.common.LedgerProgressBar
import com.personal.ledger.ui.common.LedgerSectionHeader
import com.personal.ledger.ui.common.MoneyText
import com.personal.ledger.ui.common.TransactionDeleteDialogs
import com.personal.ledger.ui.common.amountColor
import com.personal.ledger.ui.common.budgetColor
import com.personal.ledger.ui.common.cardColor
import com.personal.ledger.ui.common.rememberHapticLight
import com.personal.ledger.ui.common.rememberHapticLongPress
import com.personal.ledger.ui.common.rememberHapticNavigation
import com.personal.ledger.ui.common.signedMoney
import com.personal.ledger.ui.common.transferAmountColor
import com.personal.ledger.ui.common.transferContainerColor
import com.personal.ledger.ui.theme.LedgerNumerals
import com.personal.ledger.ui.theme.LedgerSpacing
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
                        Icon(
                            Icons.Filled.ArrowDropDown,
                            contentDescription = "切换账本",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
            verticalArrangement = Arrangement.spacedBy(LedgerSpacing.sm),
            contentPadding = PaddingValues(
                start = LedgerSpacing.md,
                top = LedgerSpacing.xs,
                end = LedgerSpacing.md,
                bottom = LedgerSpacing.xl,
            ),
        ) {
            // 本月概览主卡片：支出为页面主数字，收入 / 结余同卡展示（仅用内容色 + 显式正负号）。
            item {
                val balance = state.income - state.expense
                LedgerHeroCard {
                    HeroFigure(
                        label = "${state.monthLabel}支出",
                        value = Money.formatWithCurrency(state.expense, state.baseCurrency),
                        large = true,
                    )
                    Spacer(Modifier.height(LedgerSpacing.md))
                    Row(horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.md)) {
                        HeroFigure(
                            label = "收入",
                            value = signedMoney(state.income, state.baseCurrency),
                            modifier = Modifier.weight(1f),
                        )
                        HeroFigure(
                            label = "结余",
                            value = signedMoney(balance, state.baseCurrency),
                            modifier = Modifier.weight(1f),
                        )
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

            item {
                LedgerSectionHeader(
                    title = "最近交易",
                    modifier = Modifier.padding(top = LedgerSpacing.xxs, start = LedgerSpacing.xxs),
                    actionLabel = "查看全部",
                    onAction = {
                        hapticNavigation()
                        navController.navigate(Routes.TRANSACTIONS)
                    },
                )
            }

            if (state.loading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = LedgerSpacing.xl),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (state.recent.isEmpty()) {
                item {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = cardColor(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        EmptyState(
                            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                            title = "还没有交易",
                            subtitle = "点底部中央 + 记一笔",
                        )
                    }
                }
            } else {
                item {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = cardColor(),
                        modifier = Modifier.fillMaxWidth(),
                        tonalElevation = 0.dp,
                    ) {
                        Column(modifier = Modifier.padding(vertical = LedgerSpacing.xxs)) {
                            state.recent.forEach { tx ->
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
    progress: BudgetProgress?,
    currency: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = cardColor(),
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = LedgerSpacing.md, vertical = 14.dp)) {
            if (progress == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("预算", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.width(LedgerSpacing.sm))
                    Text(
                        "本月未设置总预算",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp),
                    )
                }
            } else {
                val pct = progress.permille?.div(10) ?: 0
                val statusColor = budgetColor(progress.permille, MaterialTheme.colorScheme.primary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("预算", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Text(
                        "$pct%",
                        style = LedgerNumerals.of(MaterialTheme.typography.titleSmall),
                        fontWeight = FontWeight.SemiBold,
                        color = budgetColor(progress.permille, MaterialTheme.colorScheme.onSurface),
                    )
                }
                Spacer(Modifier.height(10.dp))
                LedgerProgressBar(
                    progress = ((progress.permille ?: 0).coerceAtMost(1000)) / 1000f,
                    color = statusColor,
                    height = 8.dp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "${Money.formatWithCurrency(progress.spent, currency)} / ${Money.formatWithCurrency(progress.budget.amount, currency)}",
                    style = LedgerNumerals.of(MaterialTheme.typography.bodySmall),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
    val title = remember(tx, categoryNames, merchantNames) { titleOf(tx, categoryNames, merchantNames) }
    val categoryName = remember(tx.categoryId, categoryNames) { categoryNames[tx.categoryId] }
    val subtitle = remember(tx, categoryNames, accountNames) {
        "${categoryNames[tx.categoryId] ?: "转账"} · ${accountNames[tx.accountId] ?: ""}"
    }
    val amountText = remember(tx.amount, tx.type) {
        when (tx.type) {
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
            .heightIn(min = 60.dp)
            .padding(horizontal = LedgerSpacing.md, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (tx.type == TransactionType.TRANSFER) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(transferContainerColor(), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.SwapHoriz,
                    contentDescription = "转账",
                    tint = transferAmountColor(),
                    modifier = Modifier.size(22.dp),
                )
            }
        } else {
            CategoryBadge(
                name = categoryName ?: "其他",
                type = if (tx.type == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE,
                sizeDp = 40,
                visualKey = tx.categoryId ?: categoryName.orEmpty(),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = LedgerSpacing.sm),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        MoneyText(text = amountText, color = amountColor(tx.type))
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

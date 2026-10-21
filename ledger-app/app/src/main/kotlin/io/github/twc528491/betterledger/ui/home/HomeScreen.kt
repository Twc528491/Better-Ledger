package io.github.twc528491.betterledger.ui.home

import io.github.twc528491.betterledger.ui.common.SettingsActionButton
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
import io.github.twc528491.betterledger.data.AppContainer
import io.github.twc528491.betterledger.data.repository.BudgetProgress
import io.github.twc528491.betterledger.domain.model.CategoryType
import io.github.twc528491.betterledger.domain.model.Transaction
import io.github.twc528491.betterledger.domain.model.TransactionType
import io.github.twc528491.betterledger.navigation.Routes
import io.github.twc528491.betterledger.ui.common.CategoryBadge
import io.github.twc528491.betterledger.ui.common.EmptyState
import io.github.twc528491.betterledger.ui.common.HeroFigure
import io.github.twc528491.betterledger.ui.common.HeroTone
import io.github.twc528491.betterledger.ui.common.LedgerHeroCard
import io.github.twc528491.betterledger.ui.common.LedgerProgressBar
import io.github.twc528491.betterledger.ui.common.LedgerSectionHeader
import io.github.twc528491.betterledger.ui.common.MoneyText
import io.github.twc528491.betterledger.ui.common.TransactionDeleteDialogs
import io.github.twc528491.betterledger.ui.common.amountColor
import io.github.twc528491.betterledger.ui.common.budgetColor
import io.github.twc528491.betterledger.ui.common.cardColor
import io.github.twc528491.betterledger.ui.common.rememberHapticLight
import io.github.twc528491.betterledger.ui.common.rememberHapticLongPress
import io.github.twc528491.betterledger.ui.common.rememberHapticNavigation
import io.github.twc528491.betterledger.ui.common.signedMoney
import io.github.twc528491.betterledger.ui.common.transferAmountColor
import io.github.twc528491.betterledger.ui.common.transferContainerColor
import io.github.twc528491.betterledger.ui.theme.LedgerNumerals
import io.github.twc528491.betterledger.ui.theme.LedgerSpacing
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import io.github.twc528491.betterledger.R
import io.github.twc528491.betterledger.util.DateLabels
import io.github.twc528491.betterledger.util.Money
import io.github.twc528491.betterledger.viewmodel.HomeViewModel

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
                        val defaultLedgerName = stringResource(R.string.home_default_ledger_name)
                        Text(state.ledgerName.ifEmpty { defaultLedgerName }, style = MaterialTheme.typography.titleLarge,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        Icon(
                            Icons.Filled.ArrowDropDown,
                            contentDescription = stringResource(R.string.home_switch_ledger),
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
                    SettingsActionButton {
                        hapticNavigation()
                        navController.navigate(Routes.SETTINGS)
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
                        label = stringResource(
                            R.string.home_month_expense,
                            DateLabels.month(state.month.monthValue, LocalConfiguration.current.locales[0]),
                        ),
                        value = Money.formatWithCurrency(state.expense, state.baseCurrency),
                        large = true,
                    )
                    Spacer(Modifier.height(LedgerSpacing.md))
                    Row(horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.sm)) {
                        HeroFigure(
                            label = stringResource(R.string.type_income),
                            value = signedMoney(state.income, state.baseCurrency),
                            tone = HeroTone.INCOME,
                            modifier = Modifier.weight(1f),
                        )
                        HeroFigure(
                            label = stringResource(R.string.label_balance),
                            value = signedMoney(balance, state.baseCurrency),
                            tone = if (balance < 0) HeroTone.NEGATIVE else HeroTone.BALANCE,
                            divider = true,
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
                    title = stringResource(R.string.home_recent),
                    modifier = Modifier.padding(top = LedgerSpacing.xxs, start = LedgerSpacing.xxs),
                    actionLabel = stringResource(R.string.home_view_all),
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
                            title = stringResource(R.string.home_empty_title),
                            subtitle = stringResource(R.string.home_empty_subtitle),
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
                                    categoryIconKeys = state.categoryIconKeys,
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
                    Text(stringResource(R.string.home_budget), style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.width(LedgerSpacing.sm))
                    Text(
                        stringResource(R.string.home_no_budget),
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
                    Text(stringResource(R.string.home_budget), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
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
    categoryIconKeys: Map<String, String>,
    merchantNames: Map<String, String>,
    accountNames: Map<String, String>,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val transferLabel = stringResource(R.string.type_transfer)
    val title = remember(tx, categoryNames, merchantNames, transferLabel) {
        titleOf(tx, categoryNames, merchantNames, transferLabel)
    }
    val categoryName = remember(tx.categoryId, categoryNames) { categoryNames[tx.categoryId] }
    val subtitle = remember(tx, categoryNames, accountNames, transferLabel) {
        "${categoryNames[tx.categoryId] ?: transferLabel} · ${accountNames[tx.accountId] ?: ""}"
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
                    contentDescription = transferLabel,
                    tint = transferAmountColor(),
                    modifier = Modifier.size(22.dp),
                )
            }
        } else {
            CategoryBadge(
                name = categoryName ?: stringResource(R.string.label_other),
                type = if (tx.type == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE,
                sizeDp = 40,
                visualKey = tx.categoryId ?: categoryName.orEmpty(),
                iconKey = categoryIconKeys[tx.categoryId],
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
    transferLabel: String,
): String = when {
    tx.merchantId != null && merchantNames[tx.merchantId] != null -> merchantNames[tx.merchantId]!!
    !tx.note.isNullOrEmpty() -> tx.note!!
    else -> categoryNames[tx.categoryId] ?: transferLabel
}

package com.personal.ledger.ui.statistics

import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import com.personal.ledger.navigation.Routes
import com.personal.ledger.ui.common.rememberHapticNavigation
import com.personal.ledger.ui.common.MonthPickerSheet
import com.personal.ledger.ui.common.MonthSwitcher
import com.personal.ledger.ui.common.SettingsActionButton
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.personal.ledger.data.AppContainer
import com.personal.ledger.data.repository.BudgetProgress
import com.personal.ledger.ui.common.CategoryBadge
import com.personal.ledger.ui.common.EmptyState
import com.personal.ledger.ui.common.HeroFigure
import com.personal.ledger.ui.common.HeroTone
import com.personal.ledger.ui.common.LedgerGroupItem
import com.personal.ledger.ui.common.LedgerHeroCard
import com.personal.ledger.ui.common.LedgerProgressBar
import com.personal.ledger.ui.common.LedgerSectionHeader
import com.personal.ledger.ui.common.budgetColor
import com.personal.ledger.ui.common.cardColor
import com.personal.ledger.ui.common.categoryChartColor
import com.personal.ledger.ui.common.charts.CategoryChangeChart
import com.personal.ledger.ui.common.charts.CategoryChangeRow
import com.personal.ledger.ui.common.charts.ChartPageHeight
import com.personal.ledger.ui.common.charts.ChartSlice
import com.personal.ledger.ui.common.charts.DailySpendingChart
import com.personal.ledger.ui.common.charts.IncomeExpenseTrendChart
import com.personal.ledger.ui.common.charts.RingChartWithLegend
import com.personal.ledger.ui.common.rememberHapticLight
import com.personal.ledger.ui.common.signedMoney
import com.personal.ledger.ui.theme.LedgerNumerals
import com.personal.ledger.ui.theme.LedgerSpacing
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.personal.ledger.R
import com.personal.ledger.util.DateLabels
import com.personal.ledger.util.Money
import com.personal.ledger.viewmodel.CategoryStat
import com.personal.ledger.viewmodel.StatisticsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(container: AppContainer, navController: NavHostController) {
    val vm: StatisticsViewModel = viewModel {
        StatisticsViewModel(
            container.ledgerRepository,
            container.transactionRepository,
            container.categoryRepository,
            container.budgetRepository,
        )
    }
    val state by vm.uiState.collectAsStateWithLifecycle()
    val hapticLight = rememberHapticLight()
    val hapticNavigation = rememberHapticNavigation()
    val selectedMonth by vm.selectedMonth.collectAsStateWithLifecycle()
    var showMonthPicker by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(pageCount = { STATISTICS_CHART_TABS.size })
    val coroutineScope = rememberCoroutineScope()
    val topCategories = state.expenseByCategory.take(5)
    // 「其他」使用中性色，避免与任一分类色相混淆。
    val remainderColor = MaterialTheme.colorScheme.outline
    val remainderLabel = stringResource(if (topCategories.isEmpty()) R.string.label_uncategorized else R.string.label_other)
    val monthLabel = DateLabels.yearMonth(state.month, LocalConfiguration.current.locales[0])
    val chartSlices = buildList {
        topCategories.forEach { stat ->
            add(ChartSlice(stat.category.name, stat.amount, categoryChartColor(stat.category.id)))
        }
        val represented = topCategories.sumOf { it.amount }
        val remainder = (state.expense - represented).coerceAtLeast(0L)
        if (remainder > 0L) {
            add(
                ChartSlice(
                    label = remainderLabel,
                    value = remainder,
                    color = remainderColor,
                ),
            )
        }
    }
    val changeRows = state.categoryChanges.map { stat ->
        CategoryChangeRow(
            label = stat.category.name,
            color = categoryChartColor(stat.category.id),
            current = stat.change.current,
            previous = stat.change.previous,
            percent = stat.change.percent,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stats_title)) },
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
            contentPadding = PaddingValues(bottom = LedgerSpacing.xl),
        ) {
            item {
                Box(Modifier.fillMaxWidth().padding(bottom = LedgerSpacing.xs), contentAlignment = Alignment.Center) {
                    MonthSwitcher(
                        label = monthLabel,
                        onPrevious = {
                            hapticLight()
                            vm.previousMonth()
                        },
                        onNext = {
                            hapticLight()
                            vm.nextMonth()
                        },
                        onOpenPicker = {
                            hapticLight()
                            showMonthPicker = true
                        },
                    )
                }
            }
            // 概览主卡片：支出为主数字；收入 / 结余仅用内容色 + 显式正负号。
            item {
                val expenseText = Money.formatWithCurrency(state.expense, state.baseCurrency)
                val balance = state.income - state.expense
                val expenseDescription = stringResource(R.string.stats_month_expense_cd, monthLabel, expenseText)
                val incomeDescription = stringResource(
                    R.string.stats_income_cd, Money.formatWithCurrency(state.income, state.baseCurrency),
                )
                val balanceDescription = stringResource(
                    R.string.stats_balance_cd, Money.formatWithCurrency(balance, state.baseCurrency),
                )
                LedgerHeroCard(modifier = Modifier.padding(horizontal = LedgerSpacing.md)) {
                    HeroFigure(
                        label = stringResource(R.string.stats_month_expense, monthLabel),
                        value = expenseText,
                        large = true,
                        modifier = Modifier.clearAndSetSemantics {
                            contentDescription = expenseDescription
                        },
                    )
                    Spacer(Modifier.height(LedgerSpacing.md))
                    Row(horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.sm)) {
                        HeroFigure(
                            label = stringResource(R.string.type_income),
                            value = signedMoney(state.income, state.baseCurrency),
                            tone = HeroTone.INCOME,
                            modifier = Modifier
                                .weight(1f)
                                .clearAndSetSemantics {
                                    contentDescription = incomeDescription
                                },
                        )
                        HeroFigure(
                            label = stringResource(R.string.label_balance),
                            value = signedMoney(balance, state.baseCurrency),
                            tone = if (balance < 0) HeroTone.NEGATIVE else HeroTone.BALANCE,
                            divider = true,
                            modifier = Modifier
                                .weight(1f)
                                .clearAndSetSemantics {
                                    contentDescription = balanceDescription
                                },
                        )
                    }
                }
            }

            item {
                SectionHeader(stringResource(R.string.stats_charts), Modifier.padding(top = LedgerSpacing.lg))
            }
            item {
                ScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    edgePadding = LedgerSpacing.xs,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    divider = {},
                ) {
                    STATISTICS_CHART_TABS.forEachIndexed { index, title ->
                        val selected = pagerState.currentPage == index
                        Tab(
                            selected = selected,
                            onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            text = {
                                Text(
                                    stringResource(title),
                                    maxLines = 1,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            },
                        )
                    }
                }
            }
            item {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = LedgerSpacing.sm),
                    contentPadding = PaddingValues(horizontal = LedgerSpacing.md),
                    pageSpacing = LedgerSpacing.sm,
                    verticalAlignment = Alignment.Top,
                ) { page ->
                    // 所有分页同一高度，左右切换时页面不跳动。
                    val pageModifier = Modifier.fillMaxWidth().height(ChartPageHeight)
                    when (page) {
                        0 -> if (chartSlices.isEmpty()) {
                            EmptyChartPanel(
                                icon = Icons.Outlined.DonutLarge,
                                title = stringResource(R.string.stats_empty_month_expense),
                                subtitle = stringResource(R.string.stats_empty_breakdown_hint),
                            )
                        } else {
                            RingChartWithLegend(
                                slices = chartSlices,
                                total = state.expense,
                                currency = state.baseCurrency,
                                revealKey = state.month,
                                modifier = pageModifier,
                            )
                        }
                        1 -> if (state.trend.all { it.income == 0L && it.expense == 0L }) {
                            EmptyChartPanel(
                                icon = Icons.Outlined.BarChart,
                                title = stringResource(R.string.stats_empty_trend),
                                subtitle = stringResource(R.string.stats_empty_trend_hint),
                            )
                        } else {
                            IncomeExpenseTrendChart(
                                trend = state.trend,
                                viewedMonth = state.month,
                                currency = state.baseCurrency,
                                modifier = pageModifier,
                            )
                        }
                        2 -> {
                            val daily = state.dailyComparison
                            when {
                                daily == null || daily.elapsedDays <= 0 -> EmptyChartPanel(
                                    icon = Icons.AutoMirrored.Outlined.ShowChart,
                                    title = stringResource(R.string.stats_month_not_started),
                                    subtitle = stringResource(R.string.stats_month_not_started_hint),
                                )
                                daily.total == 0L && daily.previousTotal == 0L -> EmptyChartPanel(
                                    icon = Icons.AutoMirrored.Outlined.ShowChart,
                                    title = stringResource(R.string.stats_empty_month_expense),
                                    subtitle = stringResource(R.string.stats_empty_daily_hint),
                                )
                                else -> DailySpendingChart(
                                    comparison = daily,
                                    currency = state.baseCurrency,
                                    modifier = pageModifier,
                                )
                            }
                        }
                        else -> if (state.expense == 0L && state.previousExpense == 0L) {
                            EmptyChartPanel(
                                icon = Icons.Outlined.SwapVert,
                                title = stringResource(R.string.stats_empty_changes),
                                subtitle = stringResource(R.string.stats_empty_changes_hint),
                            )
                        } else {
                            CategoryChangeChart(
                                rows = changeRows,
                                currentTotal = state.expense,
                                previousTotal = state.previousExpense,
                                currency = state.baseCurrency,
                                revealKey = state.month,
                                modifier = pageModifier,
                            )
                        }
                    }
                }
            }
            item {
                Text(
                    "${pagerState.currentPage + 1} / ${STATISTICS_CHART_TABS.size}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = LedgerSpacing.xs),
                    style = LedgerNumerals.of(MaterialTheme.typography.labelSmall),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            item { SectionHeader(stringResource(R.string.stats_category_details), Modifier.padding(top = LedgerSpacing.md)) }
            if (state.expenseByCategory.isEmpty()) {
                item {
                    PlainNoteCard(
                        stringResource(
                            if (state.expense > 0L) R.string.stats_expense_unlinked else R.string.stats_no_category_expense,
                        ),
                    )
                }
            } else {
                val count = state.expenseByCategory.size
                itemsIndexed(state.expenseByCategory, key = { _, stat -> stat.category.id }) { index, stat ->
                    LedgerGroupItem(
                        index = index,
                        count = count,
                        modifier = Modifier.padding(horizontal = LedgerSpacing.md),
                    ) {
                        CategoryStatRow(stat, state.baseCurrency)
                    }
                }
            }

            item { SectionHeader(stringResource(R.string.stats_budgets), Modifier.padding(top = LedgerSpacing.lg)) }
            if (state.budgets.isEmpty()) {
                item { PlainNoteCard(stringResource(R.string.stats_no_budget)) }
            } else {
                val count = state.budgets.size
                itemsIndexed(state.budgets, key = { _, b -> b.budget.id }) { index, b ->
                    LedgerGroupItem(
                        index = index,
                        count = count,
                        modifier = Modifier.padding(horizontal = LedgerSpacing.md),
                    ) {
                        BudgetRow(
                            b = b,
                            categoryName = b.budget.categoryId?.let { state.categoryNames[it] },
                            currency = state.baseCurrency,
                        )
                    }
                }
            }
        }
    }

    if (showMonthPicker) {
        MonthPickerSheet(
            current = selectedMonth,
            onPick = { ym ->
                vm.setMonth(ym)
                showMonthPicker = false
            },
            onDismiss = { showMonthPicker = false },
        )
    }
}

private val STATISTICS_CHART_TABS = listOf(
    R.string.stats_tab_breakdown,
    R.string.stats_tab_trend,
    R.string.stats_tab_daily,
    R.string.stats_tab_changes,
)

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    LedgerSectionHeader(
        title = title,
        modifier = modifier.padding(start = LedgerSpacing.lg, end = LedgerSpacing.md, bottom = LedgerSpacing.xxs),
    )
}

@Composable
private fun PlainNoteCard(text: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = LedgerSpacing.md),
        shape = MaterialTheme.shapes.large,
        color = cardColor(),
    ) {
        Text(
            text,
            modifier = Modifier.padding(LedgerSpacing.md),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyChartPanel(icon: ImageVector, title: String, subtitle: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(ChartPageHeight),
        shape = MaterialTheme.shapes.large,
        color = cardColor(),
        tonalElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            EmptyState(icon = icon, title = title, subtitle = subtitle)
        }
    }
}

@Composable
private fun CategoryStatRow(stat: CategoryStat, currency: String) {
    val percent = (stat.fraction * 100).toInt()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .padding(horizontal = LedgerSpacing.md, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryBadge(
            name = stat.category.name,
            type = stat.category.type,
            sizeDp = 36,
            visualKey = stat.category.id,
            iconKey = stat.category.iconKey,
        )
        Column(modifier = Modifier.weight(1f).padding(start = LedgerSpacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stat.category.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(LedgerSpacing.xs))
                Text(
                    Money.formatWithCurrency(stat.amount, currency),
                    style = LedgerNumerals.of(MaterialTheme.typography.titleSmall),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LedgerProgressBar(
                    progress = stat.fraction,
                    color = categoryChartColor(stat.category.id),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "$percent%",
                    modifier = Modifier.width(44.dp),
                    style = LedgerNumerals.of(MaterialTheme.typography.labelMedium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun BudgetRow(b: BudgetProgress, categoryName: String?, currency: String) {
    val pct = b.permille?.div(10) ?: 0
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = LedgerSpacing.md, vertical = LedgerSpacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                categoryName ?: stringResource(R.string.stats_total_budget),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "$pct%",
                style = LedgerNumerals.of(MaterialTheme.typography.titleSmall),
                fontWeight = FontWeight.SemiBold,
                color = budgetColor(b.permille, MaterialTheme.colorScheme.onSurface),
            )
        }
        Spacer(Modifier.height(8.dp))
        LedgerProgressBar(
            progress = ((b.permille ?: 0).coerceAtMost(1000)) / 1000f,
            color = budgetColor(b.permille, MaterialTheme.colorScheme.primary),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "${Money.formatWithCurrency(b.spent, currency)} / ${Money.formatWithCurrency(b.budget.amount, currency)}",
            style = LedgerNumerals.of(MaterialTheme.typography.bodySmall),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

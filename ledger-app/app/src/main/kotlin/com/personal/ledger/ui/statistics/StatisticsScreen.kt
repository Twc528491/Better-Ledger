package com.personal.ledger.ui.statistics

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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.DonutLarge
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
import com.personal.ledger.ui.common.LedgerGroupItem
import com.personal.ledger.ui.common.LedgerHeroCard
import com.personal.ledger.ui.common.LedgerProgressBar
import com.personal.ledger.ui.common.LedgerSectionHeader
import com.personal.ledger.ui.common.budgetColor
import com.personal.ledger.ui.common.cardColor
import com.personal.ledger.ui.common.categoryChartColor
import com.personal.ledger.ui.common.charts.ChartSlice
import com.personal.ledger.ui.common.charts.DailyExpenseChart
import com.personal.ledger.ui.common.charts.HorizontalRankingChart
import com.personal.ledger.ui.common.charts.RingChartWithLegend
import com.personal.ledger.ui.common.charts.TrendBarChart
import com.personal.ledger.ui.common.rememberHapticLight
import com.personal.ledger.ui.common.signedMoney
import com.personal.ledger.ui.theme.LedgerNumerals
import com.personal.ledger.ui.theme.LedgerSpacing
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
    val pagerState = rememberPagerState(pageCount = { STATISTICS_CHART_TABS.size })
    val coroutineScope = rememberCoroutineScope()
    val topCategories = state.expenseByCategory.take(5)
    // 「其他」使用中性色，避免与任一分类色相混淆。
    val remainderColor = MaterialTheme.colorScheme.outline
    val chartSlices = buildList {
        topCategories.forEach { stat ->
            add(ChartSlice(stat.category.name, stat.amount, categoryChartColor(stat.category.id)))
        }
        val represented = topCategories.sumOf { it.amount }
        val remainder = (state.expense - represented).coerceAtLeast(0L)
        if (remainder > 0L) {
            add(
                ChartSlice(
                    label = if (topCategories.isEmpty()) "未分类" else "其他",
                    value = remainder,
                    color = remainderColor,
                ),
            )
        }
    }
    val rankingSlices = state.expenseByCategory.map { stat ->
        ChartSlice(stat.category.name, stat.amount, categoryChartColor(stat.category.id))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("统计 · ${state.monthLabel}") },
                navigationIcon = {
                    IconButton(onClick = {
                        hapticLight()
                        vm.previousMonth()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "上一月")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        hapticLight()
                        vm.nextMonth()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "下一月")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(top = LedgerSpacing.xs, bottom = LedgerSpacing.xl),
        ) {
            // 概览主卡片：支出为主数字；收入 / 结余仅用内容色 + 显式正负号。
            item {
                val expenseText = Money.formatWithCurrency(state.expense, state.baseCurrency)
                val balance = state.income - state.expense
                LedgerHeroCard(modifier = Modifier.padding(horizontal = LedgerSpacing.md)) {
                    HeroFigure(
                        label = "${state.monthLabel}支出",
                        value = expenseText,
                        large = true,
                        modifier = Modifier.clearAndSetSemantics {
                            contentDescription = "${state.monthLabel}支出 $expenseText"
                        },
                    )
                    Spacer(Modifier.height(LedgerSpacing.md))
                    Row(horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.md)) {
                        HeroFigure(
                            label = "收入",
                            value = signedMoney(state.income, state.baseCurrency),
                            modifier = Modifier
                                .weight(1f)
                                .clearAndSetSemantics {
                                    contentDescription =
                                        "收入 ${Money.formatWithCurrency(state.income, state.baseCurrency)}"
                                },
                        )
                        HeroFigure(
                            label = "结余",
                            value = signedMoney(balance, state.baseCurrency),
                            modifier = Modifier
                                .weight(1f)
                                .clearAndSetSemantics {
                                    contentDescription =
                                        "结余 ${Money.formatWithCurrency(balance, state.baseCurrency)}"
                                },
                        )
                    }
                }
            }

            item {
                SectionHeader("收支图表", Modifier.padding(top = LedgerSpacing.lg))
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
                                    title,
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
                        .heightIn(min = 324.dp)
                        .padding(top = LedgerSpacing.sm),
                    contentPadding = PaddingValues(horizontal = LedgerSpacing.md),
                    pageSpacing = LedgerSpacing.sm,
                    verticalAlignment = Alignment.Top,
                ) { page ->
                    when (page) {
                        0 -> if (chartSlices.isEmpty()) {
                            EmptyChartPanel(
                                icon = Icons.Outlined.DonutLarge,
                                title = "本月暂无支出",
                                subtitle = "记一笔后这里会出现分类占比",
                            )
                        } else {
                            RingChartWithLegend(
                                slices = chartSlices,
                                total = state.expense,
                                currency = state.baseCurrency,
                                modifier = Modifier.heightIn(min = 324.dp),
                            )
                        }
                        1 -> TrendBarChart(
                            months = state.trend.map { it.yearMonth },
                            incomes = state.trend.map { it.income },
                            expenses = state.trend.map { it.expense },
                            currency = state.baseCurrency,
                        )
                        2 -> DailyExpenseChart(
                            days = state.dailyExpense,
                            currency = state.baseCurrency,
                        )
                        else -> HorizontalRankingChart(
                            items = rankingSlices,
                            currency = state.baseCurrency,
                        )
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

            item { SectionHeader("完整分类明细", Modifier.padding(top = LedgerSpacing.md)) }
            if (state.expenseByCategory.isEmpty()) {
                item {
                    PlainNoteCard(
                        if (state.expense > 0L) "本月支出暂未关联到可用分类" else "本月暂无分类支出",
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

            item { SectionHeader("预算执行", Modifier.padding(top = LedgerSpacing.lg)) }
            if (state.budgets.isEmpty()) {
                item { PlainNoteCard("本月未设置预算") }
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
}

private val STATISTICS_CHART_TABS = listOf("分类占比", "月度收支", "每日支出", "分类排行")

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
        modifier = Modifier.fillMaxWidth().heightIn(min = 324.dp),
        shape = MaterialTheme.shapes.large,
        color = cardColor(),
        tonalElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().heightIn(min = 324.dp),
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
                categoryName ?: "总预算",
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

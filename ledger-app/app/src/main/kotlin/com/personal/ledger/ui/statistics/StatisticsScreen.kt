package com.personal.ledger.ui.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import com.personal.ledger.data.AppContainer
import com.personal.ledger.data.repository.BudgetProgress
import com.personal.ledger.ui.common.CategoryBadge
import com.personal.ledger.ui.common.EmptyState
import com.personal.ledger.ui.common.categoryChartColor
import com.personal.ledger.ui.common.categoryContainerColor
import com.personal.ledger.ui.common.cardColor
import com.personal.ledger.ui.common.balanceColor
import com.personal.ledger.ui.common.expenseAmountColor
import com.personal.ledger.ui.common.expenseContainerColor
import com.personal.ledger.ui.common.incomeAmountColor
import com.personal.ledger.ui.common.incomeContainerColor
import com.personal.ledger.ui.common.highlightCardColor
import com.personal.ledger.ui.common.highlightCardContentColor
import com.personal.ledger.ui.common.transferAmountColor
import com.personal.ledger.ui.common.transferContainerColor
import com.personal.ledger.ui.common.budgetColor
import com.personal.ledger.ui.common.rememberHapticLight
import com.personal.ledger.ui.common.charts.ChartSlice
import com.personal.ledger.ui.common.charts.DailyExpenseChart
import com.personal.ledger.ui.common.charts.HorizontalRankingChart
import com.personal.ledger.ui.common.charts.RingChartWithLegend
import com.personal.ledger.ui.common.charts.TrendBarChart
import com.personal.ledger.ui.common.charts.paletteFor
import com.personal.ledger.util.Money
import com.personal.ledger.viewmodel.CategoryStat
import com.personal.ledger.viewmodel.StatisticsViewModel

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
                    color = paletteFor(7),
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
        ) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = highlightCardColor(),
                        modifier = Modifier.fillMaxWidth(),
                        tonalElevation = 0.dp,
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                            Text("${state.monthLabel}支出", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                Money.formatWithCurrency(state.expense, state.baseCurrency),
                                modifier = Modifier.semantics {
                                    contentDescription =
                                        "${state.monthLabel}支出 ${Money.formatWithCurrency(state.expense, state.baseCurrency)}"
                                },
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = highlightCardContentColor(),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
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
                                    modifier = Modifier.semantics {
                                        contentDescription =
                                            "收入 ${Money.formatWithCurrency(state.income, state.baseCurrency)}"
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = incomeAmountColor(),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                val balance = state.income - state.expense
                                Text("结余", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    Money.symbolFor(state.baseCurrency) + Money.formatSignedMinor(balance, explicitPlus = true),
                                    modifier = Modifier.semantics {
                                        contentDescription =
                                            "结余 ${Money.formatWithCurrency(balance, state.baseCurrency)}"
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = balanceColor(balance),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
                HorizontalDivider()
            }

            item {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)) {
                    Text("收支图表", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        "左右滑动浏览 · ${state.monthLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                ScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    edgePadding = 0.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    STATISTICS_CHART_TABS.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                            text = { Text(title, maxLines = 1) },
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
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    pageSpacing = 12.dp,
                    verticalAlignment = Alignment.Top,
                ) { page ->
                    when (page) {
                        0 -> if (chartSlices.isEmpty()) {
                            EmptyChartPanel(
                                emoji = "🍩",
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
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            item {
                Text(
                    "完整分类明细",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
            if (state.expenseByCategory.isEmpty()) {
                item {
                    Text(
                        if (state.expense > 0L) "本月支出暂未关联到可用分类" else "本月暂无分类支出",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(state.expenseByCategory, key = { it.category.id }) { stat ->
                    CategoryStatRow(stat, state.baseCurrency)
                }
            }
            item {
                Text(
                    "预算执行",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }

            if (state.budgets.isEmpty()) {
                item { Text("本月未设置预算", modifier = Modifier.padding(16.dp)) }
            } else {
                items(state.budgets, key = { it.budget.id }) { b ->
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

private val STATISTICS_CHART_TABS = listOf("分类占比", "月度收支", "每日支出", "分类排行")

@Composable
private fun EmptyChartPanel(emoji: String, title: String, subtitle: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 324.dp),
        shape = RoundedCornerShape(16.dp),
        color = cardColor(),
        tonalElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().heightIn(min = 324.dp),
            contentAlignment = Alignment.Center,
        ) {
            EmptyState(emoji = emoji, title = title, subtitle = subtitle)
        }
    }
}

@Composable
private fun CategoryStatRow(stat: CategoryStat, currency: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp),
        shape = RoundedCornerShape(14.dp),
        color = categoryContainerColor(stat.category.id),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryBadge(
                name = stat.category.name,
                type = stat.category.type,
                sizeDp = 30,
                visualKey = stat.category.id,
            )
            Column(modifier = Modifier.padding(start = 10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        stat.category.name,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${Money.formatWithCurrency(stat.amount, currency)}  ${(stat.fraction * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(3.dp))
                LinearProgressIndicator(
                    progress = { stat.fraction.coerceIn(0f, 1f) },
                    color = categoryChartColor(stat.category.id),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp),
                )
            }
        }
    }
}

@Composable
private fun BudgetRow(b: BudgetProgress, categoryName: String?, currency: String) {
    val pct = b.permille?.div(10) ?: 0
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp),
        shape = RoundedCornerShape(14.dp),
        color = b.budget.categoryId?.let { categoryContainerColor(it) } ?: cardColor(),
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(categoryName ?: "总预算", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    "${Money.formatWithCurrency(b.spent, currency)} / ${Money.formatWithCurrency(b.budget.amount, currency)}  $pct%",
                    style = MaterialTheme.typography.labelMedium,
                    color = budgetColor(b.permille, MaterialTheme.colorScheme.onSurface),
                )
            }
            Spacer(Modifier.height(3.dp))
            LinearProgressIndicator(
                progress = { ((b.permille ?: 0).coerceAtMost(1000)) / 1000f },
                color = budgetColor(b.permille, MaterialTheme.colorScheme.primary),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp),
            )
        }
    }
}

package com.personal.ledger.ui.statistics

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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.personal.ledger.data.repository.BudgetProgress
import com.personal.ledger.ui.common.CategoryBadge
import com.personal.ledger.ui.common.EmptyState
import com.personal.ledger.ui.common.LedgerColors
import com.personal.ledger.ui.common.budgetColor
import com.personal.ledger.ui.common.charts.ChartSlice
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
    var budgets by remember { mutableStateOf<List<BudgetProgress>>(emptyList()) }

    LaunchedEffect(state.monthLabel) { vm.loadBudgets { budgets = it } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("统计 · ${state.monthLabel}") },
                navigationIcon = {
                    IconButton(onClick = { vm.previousMonth() }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "上一月")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.nextMonth() }) {
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
                Column(modifier = Modifier.padding(16.dp)) {
                    // 支出主卡片
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = LedgerColors.ExpenseContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("支出", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                Money.formatWithSymbol(state.expense),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = LedgerColors.Expense,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = LedgerColors.IncomeContainer,
                            modifier = Modifier.weight(1f),
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("收入", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    Money.formatSignedMinor(state.income, explicitPlus = true),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = LedgerColors.Income,
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (state.income - state.expense >= 0) LedgerColors.TransferContainer else LedgerColors.ExpenseContainer,
                            modifier = Modifier.weight(1f),
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("结余", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    Money.formatSignedMinor(state.income - state.expense, explicitPlus = true),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (state.income - state.expense >= 0) LedgerColors.Transfer else LedgerColors.Expense,
                                )
                            }
                        }
                    }
                }
                HorizontalDivider()
            }

            item {
                Text(
                    "分类占比",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
            item {
                if (state.expenseByCategory.isEmpty()) {
                    EmptyState(emoji = "🍩", title = "本月暂无支出", subtitle = "记一笔后这里会出现分类占比")
                } else {
                    val slices = state.expenseByCategory.mapIndexed { i, stat ->
                        ChartSlice(stat.category.name, stat.amount, paletteFor(i))
                    }
                    RingChartWithLegend(
                        slices = slices,
                        total = state.expense,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            item {
                Text(
                    "分类排名",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
            if (state.expenseByCategory.isEmpty()) {
                item { Text("本月暂无支出", modifier = Modifier.padding(16.dp)) }
            } else {
                items(state.expenseByCategory, key = { it.category.id }) { stat ->
                    CategoryStatRow(stat)
                }
            }

            item {
                Text(
                    "月度趋势（近 6 个月）",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
            item {
                TrendBarChart(
                    months = state.trend.map { it.yearMonth },
                    incomes = state.trend.map { it.income },
                    expenses = state.trend.map { it.expense },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            item {
                Text(
                    "预算执行",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }

            if (budgets.isEmpty()) {
                item { Text("本月未设置预算", modifier = Modifier.padding(16.dp)) }
            } else {
                items(budgets, key = { it.budget.id }) { b ->
                    BudgetRow(b)
                }
            }
        }
    }
}

@Composable
private fun CategoryStatRow(stat: CategoryStat) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryBadge(name = stat.category.name, type = stat.category.type)
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stat.category.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${Money.formatMinor(stat.amount)}  ${(stat.fraction * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { stat.fraction },
                color = LedgerColors.Expense,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
            )
        }
    }
}

@Composable
private fun BudgetRow(b: BudgetProgress) {
    val pct = b.permille?.div(10) ?: 0
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(b.budget.categoryId?.let { "分类预算" } ?: "总预算", style = MaterialTheme.typography.bodyLarge)
            Text(
                "${Money.formatMinor(b.spent)} / ${Money.formatMinor(b.budget.amount)}  $pct%",
                style = MaterialTheme.typography.bodyMedium,
                color = budgetColor(b.permille, MaterialTheme.colorScheme.onSurface),
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { ((b.permille ?: 0).coerceAtMost(1000)) / 1000f },
            color = budgetColor(b.permille, MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
        )
    }
    HorizontalDivider()
}

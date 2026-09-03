package com.personal.ledger.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.personal.ledger.data.AppContainer
import com.personal.ledger.ui.account.AccountDetailScreen
import com.personal.ledger.ui.account.AccountsScreen
import com.personal.ledger.ui.home.HomeScreen
import com.personal.ledger.ui.settings.BookkeepingSettingsScreen
import com.personal.ledger.ui.settings.CategoriesScreen
import com.personal.ledger.ui.settings.BudgetsScreen
import com.personal.ledger.ui.settings.DataScreen
import com.personal.ledger.ui.settings.MerchantsScreen
import com.personal.ledger.ui.settings.SettingsScreen
import com.personal.ledger.ui.settings.TagsScreen
import com.personal.ledger.ui.statistics.StatisticsScreen
import com.personal.ledger.ui.transaction.AddTransactionScreen
import com.personal.ledger.ui.transaction.RecycleBinScreen
import com.personal.ledger.ui.transaction.TransactionsScreen

object Routes {
    const val HOME = "home"
    const val TRANSACTIONS = "transactions"
    const val STATISTICS = "statistics"
    const val ACCOUNTS = "accounts"
    const val SETTINGS = "settings"
    const val BOOKKEEPING_SETTINGS = "bookkeeping_settings"
    const val ADD = "add?txId={txId}"
    const val RECYCLE = "recycle"
    const val CATEGORIES = "categories"
    const val BUDGETS = "budgets"
    const val DATA = "data"
    const val MERCHANTS = "merchants"
    const val TAGS = "tags"
    const val ACCOUNT_DETAIL = "account/{accountId}"

    fun add(txId: String? = null) = if (txId == null) "add?txId=" else "add?txId=$txId"
    fun accountDetail(accountId: String) = "account/$accountId"
}

private data class TabItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val leftTabs = listOf(
    TabItem(Routes.HOME, "首页", Icons.Filled.Home),
    TabItem(Routes.TRANSACTIONS, "账单", Icons.AutoMirrored.Filled.List),
)

private val rightTabs = listOf(
    TabItem(Routes.STATISTICS, "统计", Icons.Filled.BarChart),
    TabItem(Routes.ACCOUNTS, "资产", Icons.Filled.AccountBalanceWallet),
)

private val tabRoutes = leftTabs.map { it.route } + rightTabs.map { it.route }

@Composable
fun LedgerApp(container: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTabRoute = currentRoute in tabRoutes

    // 首次启动：创建默认账本 + 默认分类；恢复后当前账本失效时回退到第一个有效账本。
    LaunchedEffect(Unit) {
        container.ledgerRepository.ensureDefaultLedger()
        container.ledgerRepository.fallbackCurrentLedgerIfInvalid()
    }

    Scaffold(
        bottomBar = {
            if (isTabRoute) {
                NavigationBar {
                    leftTabs.forEach { tab ->
                        TabItemView(navController, tab, currentRoute)
                    }
                    // 中间重点按钮：记一笔
                    NavigationBarItem(
                        selected = false,
                        onClick = { navController.navigate(Routes.add()) },
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Filled.Add,
                                    contentDescription = "记一笔",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(30.dp),
                                )
                            }
                        },
                        label = { Text("记一笔") },
                    )
                    rightTabs.forEach { tab ->
                        TabItemView(navController, tab, currentRoute)
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.HOME) { HomeScreen(container, navController) }
            composable(Routes.TRANSACTIONS) { TransactionsScreen(container, navController) }
            composable(Routes.STATISTICS) { StatisticsScreen(container, navController) }
            composable(Routes.ACCOUNTS) { AccountsScreen(container, navController) }
            composable(Routes.SETTINGS) { SettingsScreen(container, navController) }
            composable(Routes.BOOKKEEPING_SETTINGS) { BookkeepingSettingsScreen(container, navController) }
            composable(Routes.ADD) { entry ->
                val txId = entry.arguments?.getString("txId")?.ifEmpty { null }
                AddTransactionScreen(container, navController, txId)
            }
            composable(Routes.RECYCLE) { RecycleBinScreen(container, navController) }
            composable(Routes.CATEGORIES) { CategoriesScreen(container, navController) }
            composable(Routes.BUDGETS) { BudgetsScreen(container, navController) }
            composable(Routes.DATA) { DataScreen(container, navController) }
            composable(Routes.MERCHANTS) { MerchantsScreen(container, navController) }
            composable(Routes.TAGS) { TagsScreen(container, navController) }
            composable(Routes.ACCOUNT_DETAIL) { entry ->
                val id = entry.arguments?.getString("accountId").orEmpty()
                AccountDetailScreen(container, navController, id)
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TabItemView(
    navController: androidx.navigation.NavHostController,
    tab: TabItem,
    currentRoute: String?,
) {
    NavigationBarItem(
        selected = currentRoute == tab.route,
        onClick = {
            navController.navigate(tab.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        },
        icon = { Icon(tab.icon, contentDescription = tab.label) },
        label = { Text(tab.label) },
    )
}

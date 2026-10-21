package io.github.twc528491.betterledger.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.twc528491.betterledger.R
import io.github.twc528491.betterledger.data.AppContainer
import io.github.twc528491.betterledger.ui.account.AccountDetailScreen
import io.github.twc528491.betterledger.ui.account.AccountsScreen
import io.github.twc528491.betterledger.ui.common.cardColor
import io.github.twc528491.betterledger.ui.common.rememberHapticNavigation
import io.github.twc528491.betterledger.ui.common.rememberHapticStrong
import io.github.twc528491.betterledger.ui.home.HomeScreen
import io.github.twc528491.betterledger.ui.settings.BackupsScreen
import io.github.twc528491.betterledger.ui.settings.BookkeepingSettingsScreen
import io.github.twc528491.betterledger.ui.settings.CategoriesScreen
import io.github.twc528491.betterledger.ui.settings.BudgetsScreen
import io.github.twc528491.betterledger.ui.settings.DataScreen
import io.github.twc528491.betterledger.ui.settings.MerchantsScreen
import io.github.twc528491.betterledger.ui.settings.SettingsScreen
import io.github.twc528491.betterledger.ui.settings.RecurringChargesScreen
import io.github.twc528491.betterledger.ui.settings.TagsScreen
import io.github.twc528491.betterledger.ui.statistics.StatisticsScreen
import io.github.twc528491.betterledger.ui.transaction.AddTransactionScreen
import io.github.twc528491.betterledger.ui.transaction.RecycleBinScreen
import io.github.twc528491.betterledger.ui.transaction.TransactionsScreen

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
    const val BACKUPS = "backups"
    const val MERCHANTS = "merchants"
    const val TAGS = "tags"
    const val RECURRING_CHARGES = "recurring_charges"
    const val ACCOUNT_DETAIL = "account/{accountId}"

    fun add(txId: String? = null) = if (txId == null) "add?txId=" else "add?txId=$txId"
    fun accountDetail(accountId: String) = "account/$accountId"
}

private data class TabItem(
    val route: String,
    @StringRes val label: Int,
    val icon: ImageVector,
)

private val leftTabs = listOf(
    TabItem(Routes.HOME, R.string.core_tab_home, Icons.Filled.Home),
    TabItem(Routes.TRANSACTIONS, R.string.core_tab_transactions, Icons.AutoMirrored.Filled.List),
)

private val rightTabs = listOf(
    TabItem(Routes.STATISTICS, R.string.core_tab_statistics, Icons.Filled.BarChart),
    TabItem(Routes.ACCOUNTS, R.string.core_tab_accounts, Icons.Filled.AccountBalanceWallet),
)

private val tabRoutes = leftTabs.map { it.route } + rightTabs.map { it.route }

/**
 * 根标签统一导航。首页优先直接回退到现有实例，避免从“最近交易 → 查看全部”
 * 进入账单页后，再点首页时被重复的已保存标签栈吞掉。
 */
fun androidx.navigation.NavHostController.navigateToRootTab(route: String) {
    if (route == Routes.HOME && popBackStack(Routes.HOME, inclusive = false)) return
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun LedgerApp(container: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTabRoute = currentRoute in tabRoutes
    val hapticNavigation = rememberHapticNavigation()
    val hapticStrong = rememberHapticStrong()

    // 首次启动：创建默认账本 + 默认分类；恢复后当前账本失效时回退到第一个有效账本。
    LaunchedEffect(Unit) {
        container.ledgerRepository.ensureDefaultLedger()
        container.ledgerRepository.fallbackCurrentLedgerIfInvalid()
    }

    Scaffold(
        // 各页 TopAppBar 处理状态栏；根导航栏处理 tab 页底部手势区域。
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (isTabRoute) {
                Column {
                    // 白色导航栏与灰底内容之间用一条发丝线分隔，不用阴影。
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    NavigationBar(
                        containerColor = cardColor(),
                        tonalElevation = 0.dp,
                    ) {
                        leftTabs.forEach { tab ->
                            TabItemView(navController, tab, currentRoute, hapticNavigation)
                        }
                        // 中间重点按钮：记一笔（实心品牌色圆角块，无阴影 / 描边；整个导航项为触控区域）
                        NavigationBarItem(
                            selected = false,
                            onClick = {
                                hapticStrong()
                                navController.navigate(Routes.add())
                            },
                            icon = {
                                Box(
                                    modifier = Modifier
                                        .size(width = 56.dp, height = 40.dp)
                                        .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Filled.Add,
                                        contentDescription = stringResource(R.string.core_add_transaction),
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(28.dp),
                                    )
                                }
                            },
                            label = {
                                Text(
                                    stringResource(R.string.core_add_transaction),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent),
                        )
                        rightTabs.forEach { tab ->
                            TabItemView(navController, tab, currentRoute, hapticNavigation)
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
        ) {
            composable(Routes.HOME) { HomeScreen(container, navController) }
            composable(Routes.TRANSACTIONS) { TransactionsScreen(container, navController) }
            composable(Routes.STATISTICS) { StatisticsScreen(container, navController) }
            composable(Routes.ACCOUNTS) { AccountsScreen(container, navController) }
            composable(Routes.SETTINGS) { SettingsScreen(container, navController) }
            composable(Routes.RECURRING_CHARGES) { RecurringChargesScreen(container, navController) }
            composable(Routes.BOOKKEEPING_SETTINGS) { BookkeepingSettingsScreen(container, navController) }
            composable(Routes.ADD) { entry ->
                val txId = entry.arguments?.getString("txId")?.ifEmpty { null }
                AddTransactionScreen(container, navController, txId)
            }
            composable(Routes.RECYCLE) { RecycleBinScreen(container, navController) }
            composable(Routes.CATEGORIES) { CategoriesScreen(container, navController) }
            composable(Routes.BUDGETS) { BudgetsScreen(container, navController) }
            composable(Routes.DATA) { DataScreen(container, navController) }
            composable(Routes.BACKUPS) { BackupsScreen(container, navController) }
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
    hapticNavigation: () -> Unit,
) {
    NavigationBarItem(
        selected = currentRoute == tab.route,
        onClick = {
            hapticNavigation()
            navController.navigateToRootTab(tab.route)
        },
        icon = { Icon(tab.icon, contentDescription = stringResource(tab.label)) },
        label = { TabLabel(stringResource(tab.label)) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

/** 底部导航标签：单行；英文等较长文字在窄屏上自动缩小字号，而不是截断或换行。 */
@Composable
private fun TabLabel(text: String) {
    val style = LocalTextStyle.current
    BasicText(
        text = text,
        style = style.copy(color = LocalContentColor.current),
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(
            minFontSize = 9.sp,
            maxFontSize = style.fontSize.takeIf { it.isSpecified } ?: 12.sp,
            stepSize = 0.5.sp,
        ),
    )
}

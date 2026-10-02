package com.personal.ledger.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.personal.ledger.data.AppContainer
import com.personal.ledger.data.local.dao.LedgerPurgeCounts
import com.personal.ledger.domain.calc.PurgeRules
import com.personal.ledger.domain.model.Ledger
import com.personal.ledger.navigation.Routes
import com.personal.ledger.ui.common.LedgerGroup
import com.personal.ledger.ui.common.LedgerIconTile
import com.personal.ledger.ui.common.LedgerListRow
import com.personal.ledger.ui.theme.LedgerSpacing
import com.personal.ledger.ui.common.rememberHapticConfirm
import com.personal.ledger.ui.common.rememberHapticLight
import com.personal.ledger.ui.common.rememberHapticNavigation
import com.personal.ledger.ui.common.rememberHapticReject
import com.personal.ledger.viewmodel.SettingsViewModel

/**
 * 记账设置（放在首页右上角明显位置）：
 * 分类 / 商家 / 标签 / 预算 / 账本管理——与记账直接相关的配置。
 * 深色模式、安全锁、数据管理等综合设置见 [SettingsScreen]（资产页角落入口）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookkeepingSettingsScreen(container: AppContainer, navController: NavHostController) {
    val vm: SettingsViewModel = viewModel {
        SettingsViewModel(container.ledgerRepository, container.settings)
    }
    val state by vm.uiState.collectAsStateWithLifecycle()
    var showLedgerDialog by remember { mutableStateOf(false) }
    var showCreateLedger by remember { mutableStateOf(false) }
    var purgeLedger by remember { mutableStateOf<Ledger?>(null) }
    val hapticLight = rememberHapticLight()
    val hapticNavigation = rememberHapticNavigation()
    val hapticConfirm = rememberHapticConfirm()
    val hapticReject = rememberHapticReject()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("记账设置") },
                navigationIcon = {
                    IconButton(onClick = {
                        hapticNavigation()
                        navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = LedgerSpacing.md,
                top = LedgerSpacing.xs,
                end = LedgerSpacing.md,
                bottom = LedgerSpacing.xl,
            ),
            verticalArrangement = Arrangement.spacedBy(LedgerSpacing.lg),
        ) {
            item {
                LedgerGroup(title = "记账项") {
                    SettingsRow(Icons.Outlined.Category, "分类管理", "收入 / 支出分类，最多两级") {
                        hapticNavigation()
                        navController.navigate(Routes.CATEGORIES)
                    }
                    SettingsRow(Icons.Outlined.Storefront, "商家管理", "常去商家，记账快速选择") {
                        hapticNavigation()
                        navController.navigate(Routes.MERCHANTS)
                    }
                    SettingsRow(Icons.Outlined.Sell, "标签管理", "自定义标签，一笔可多个") {
                        hapticNavigation()
                        navController.navigate(Routes.TAGS)
                    }
                    SettingsRow(Icons.Outlined.Savings, "预算设置", "月总预算与分类预算") {
                        hapticNavigation()
                        navController.navigate(Routes.BUDGETS)
                    }
                }
            }

            item {
                LedgerGroup(title = "账本") {
                    SettingsRow(
                        icon = Icons.AutoMirrored.Outlined.MenuBook,
                        title = "账本管理",
                        subtitle = "当前：${state.currentLedgerName}",
                    ) {
                        hapticNavigation()
                        showLedgerDialog = true
                    }
                }
            }
        }
    }

    if (showLedgerDialog) {
        AlertDialog(
            onDismissRequest = {
                showLedgerDialog = false
            },
            title = { Text("账本管理") },
            text = {
                Column {
                    state.ledgers.forEach { ledger ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    hapticLight()
                                    vm.switchLedger(ledger.id)
                                    showLedgerDialog = false
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = ledger.name == state.currentLedgerName,
                                onClick = null,
                            )
                            Text(
                                ledger.name + if (ledger.isArchived) "（已归档）" else "",
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp),
                            )
                            if (ledger.name != state.currentLedgerName) {
                                TextButton(onClick = {
                                    vm.setLedgerArchived(ledger.id, !ledger.isArchived) { result ->
                                        result.onSuccess { hapticConfirm() }
                                            .onFailure { hapticReject() }
                                    }
                                }) {
                                    Text(if (ledger.isArchived) "恢复" else "归档")
                                }
                            }
                            if (ledger.isArchived) {
                                TextButton(onClick = {
                                    hapticConfirm()
                                    showLedgerDialog = false
                                    purgeLedger = ledger
                                }) {
                                    Text("删除", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                    TextButton(onClick = {
                        hapticNavigation()
                        showLedgerDialog = false
                        showCreateLedger = true
                    }) { Text("+ 新建账本") }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = {
                    showLedgerDialog = false
                }) { Text("关闭") }
            },
        )
    }

    purgeLedger?.let { ledger ->
        LedgerPurgeDialog(
            container = container,
            ledger = ledger,
            onDismiss = { purgeLedger = null },
        )
    }

    if (showCreateLedger) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = {
                showCreateLedger = false
            },
            title = { Text("新建账本") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("账本名称（币种 V1 固定 CNY）") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        vm.createLedger(name) { result ->
                            result.onSuccess {
                                hapticConfirm()
                                showCreateLedger = false
                            }.onFailure { hapticReject() }
                        }
                    }
                }) { Text("创建") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreateLedger = false
                }) { Text("取消") }
            },
        )
    }
}

/** 设置入口行：浅色图标方块 + 标题 / 说明 + 箭头。 */
@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String = "",
    onClick: () -> Unit,
) {
    LedgerListRow(
        title = title,
        subtitle = subtitle,
        leading = { LedgerIconTile(icon) },
        showChevron = true,
        onClick = onClick,
    )
}

/** 彻底删除已归档账本：列出数据量，并要求输入账本名称确认。 */
@Composable
private fun LedgerPurgeDialog(
    container: AppContainer,
    ledger: Ledger,
    onDismiss: () -> Unit,
) {
    var counts by remember(ledger.id) { mutableStateOf<LedgerPurgeCounts?>(null) }
    var typedName by remember(ledger.id) { mutableStateOf("") }
    LaunchedEffect(ledger.id) { counts = container.purgeRepository.ledgerCounts(ledger.id) }
    val c = counts
    PurgeDialog(
        container = container,
        title = "彻底删除账本",
        impact = if (c == null) {
            "正在统计影响范围…"
        } else {
            "将永久删除账本“${ledger.name}”及其全部数据：${c.transactions} 笔账单（含回收站）、" +
                "${c.accounts} 个账户、${c.categories} 个分类、${c.merchants} 个商家、${c.tags} 个标签、" +
                "${c.budgets} 条预算、${c.recurringCharges} 条定时扣款。"
        },
        confirmEnabled = c != null && PurgeRules.ledgerNameConfirmed(typedName, ledger.name),
        onDismiss = onDismiss,
        onPurged = onDismiss,
        purge = {
            container.purgeRepository.purgeLedger(ledger.id)
                .onSuccess { container.ledgerRepository.fallbackCurrentLedgerIfInvalid() }
        },
    ) {
        OutlinedTextField(
            value = typedName,
            onValueChange = { typedName = it },
            label = { Text("输入账本名称“${ledger.name}”以确认") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

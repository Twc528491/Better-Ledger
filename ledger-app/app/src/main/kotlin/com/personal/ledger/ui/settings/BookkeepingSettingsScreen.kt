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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.personal.ledger.R
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
                title = { Text(stringResource(R.string.set_bookkeeping_title)) },
                navigationIcon = {
                    IconButton(onClick = {
                        hapticNavigation()
                        navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
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
                LedgerGroup(title = stringResource(R.string.set_group_items)) {
                    SettingsRow(
                        Icons.Outlined.Category,
                        stringResource(R.string.set_categories_title),
                        stringResource(R.string.set_categories_subtitle),
                    ) {
                        hapticNavigation()
                        navController.navigate(Routes.CATEGORIES)
                    }
                    SettingsRow(
                        Icons.Outlined.Storefront,
                        stringResource(R.string.set_merchants_title),
                        stringResource(R.string.set_merchants_subtitle),
                    ) {
                        hapticNavigation()
                        navController.navigate(Routes.MERCHANTS)
                    }
                    SettingsRow(
                        Icons.Outlined.Sell,
                        stringResource(R.string.set_tags_title),
                        stringResource(R.string.set_tags_subtitle),
                    ) {
                        hapticNavigation()
                        navController.navigate(Routes.TAGS)
                    }
                    SettingsRow(
                        Icons.Outlined.Savings,
                        stringResource(R.string.set_budgets_title),
                        stringResource(R.string.set_budgets_subtitle),
                    ) {
                        hapticNavigation()
                        navController.navigate(Routes.BUDGETS)
                    }
                }
            }

            item {
                LedgerGroup(title = stringResource(R.string.set_group_ledger)) {
                    SettingsRow(
                        icon = Icons.AutoMirrored.Outlined.MenuBook,
                        title = stringResource(R.string.set_ledgers_title),
                        subtitle = stringResource(R.string.set_ledger_current, state.currentLedgerName),
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
            title = { Text(stringResource(R.string.set_ledgers_title)) },
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
                                ledger.name + if (ledger.isArchived) stringResource(R.string.label_archived_suffix) else "",
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
                                    Text(
                                        stringResource(
                                            if (ledger.isArchived) R.string.action_restore else R.string.action_archive,
                                        ),
                                    )
                                }
                            }
                            if (ledger.isArchived) {
                                TextButton(onClick = {
                                    hapticConfirm()
                                    showLedgerDialog = false
                                    purgeLedger = ledger
                                }) {
                                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                    TextButton(onClick = {
                        hapticNavigation()
                        showLedgerDialog = false
                        showCreateLedger = true
                    }) { Text(stringResource(R.string.set_ledger_new_button)) }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = {
                    showLedgerDialog = false
                }) { Text(stringResource(R.string.action_close)) }
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
            title = { Text(stringResource(R.string.set_ledger_new_title)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.set_ledger_name_label)) },
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
                }) { Text(stringResource(R.string.set_create)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreateLedger = false
                }) { Text(stringResource(R.string.action_cancel)) }
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
        title = stringResource(R.string.set_ledger_purge_title),
        impact = if (c == null) {
            stringResource(R.string.set_purge_counting)
        } else {
            val parts = listOf(
                pluralStringResource(R.plurals.set_count_transactions_incl_bin, c.transactions, c.transactions),
                pluralStringResource(R.plurals.set_count_accounts, c.accounts, c.accounts),
                pluralStringResource(R.plurals.set_count_categories, c.categories, c.categories),
                pluralStringResource(R.plurals.set_count_merchants, c.merchants, c.merchants),
                pluralStringResource(R.plurals.set_count_tags, c.tags, c.tags),
                pluralStringResource(R.plurals.set_count_budgets, c.budgets, c.budgets),
                pluralStringResource(R.plurals.set_count_recurring, c.recurringCharges, c.recurringCharges),
            )
            stringResource(
                R.string.set_ledger_purge_impact,
                ledger.name,
                parts.joinToString(stringResource(R.string.set_list_separator)),
            )
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
            label = { Text(stringResource(R.string.set_ledger_purge_confirm_label, ledger.name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

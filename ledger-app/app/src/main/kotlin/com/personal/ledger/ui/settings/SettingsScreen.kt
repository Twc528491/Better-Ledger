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
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.personal.ledger.data.AppContainer
import com.personal.ledger.data.settings.ThemeMode
import com.personal.ledger.navigation.Routes
import com.personal.ledger.ui.common.LedgerGroup
import com.personal.ledger.ui.common.LedgerIconTile
import com.personal.ledger.ui.common.LedgerListRow
import com.personal.ledger.ui.common.rememberHapticLight
import com.personal.ledger.ui.common.rememberHapticNavigation
import com.personal.ledger.ui.theme.LedgerSpacing
import com.personal.ledger.viewmodel.SettingsViewModel

/**
 * 设置总入口：记账项、外观与安全、数据管理均从这里进入。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(container: AppContainer, navController: NavHostController) {
    val vm: SettingsViewModel = viewModel {
        SettingsViewModel(container.ledgerRepository, container.settings)
    }
    val state by vm.uiState.collectAsStateWithLifecycle()
    var showThemeDialog by remember { mutableStateOf(false) }
    val activity = LocalContext.current as? androidx.fragment.app.FragmentActivity
    val hapticLight = rememberHapticLight()
    val hapticNavigation = rememberHapticNavigation()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
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
                LedgerGroup(title = "记账") {
                    LedgerListRow(
                        title = "记账设置",
                        subtitle = "分类、商家、标签、预算与账本",
                        leading = { LedgerIconTile(Icons.AutoMirrored.Outlined.ReceiptLong) },
                        showChevron = true,
                        onClick = {
                            hapticNavigation()
                            navController.navigate(Routes.BOOKKEEPING_SETTINGS)
                        },
                    )
                    LedgerListRow(
                        title = "定时扣款",
                        subtitle = "按周期在本地账本生成支出记录",
                        leading = { LedgerIconTile(Icons.Outlined.EventRepeat) },
                        showChevron = true,
                        onClick = {
                            navController.navigate(Routes.RECURRING_CHARGES)
                        },
                    )
                }
            }

            item {
                LedgerGroup(title = "数据") {
                    LedgerListRow(
                        title = "数据管理",
                        subtitle = "导出 / 恢复 / 自动备份",
                        leading = { LedgerIconTile(Icons.Outlined.Storage) },
                        showChevron = true,
                        onClick = {
                            hapticNavigation()
                            navController.navigate(Routes.DATA)
                        },
                    )
                }
            }

            item {
                LedgerGroup(title = "应用") {
                    LedgerListRow(
                        title = "深色模式",
                        leading = { LedgerIconTile(Icons.Outlined.DarkMode) },
                        value = themeLabel(state.themeMode),
                        showChevron = true,
                        onClick = {
                            hapticNavigation()
                            showThemeDialog = true
                        },
                    )
                    LedgerListRow(
                        title = "安全锁",
                        subtitle = "生物识别 / 设备凭据；冷启动和长时间离开后验证",
                        leading = { LedgerIconTile(Icons.Outlined.Lock) },
                        trailing = {
                            Switch(
                                checked = state.securityLockEnabled,
                                onCheckedChange = { enabled ->
                                    hapticLight()
                                    if (enabled) {
                                        if (activity != null) {
                                            com.personal.ledger.ui.security.promptForEnrollment(
                                                activity = activity,
                                                onSuccess = { vm.setSecurityLockEnabled(true) },
                                            )
                                        } else {
                                            vm.setSecurityLockEnabled(true)
                                        }
                                    } else {
                                        vm.setSecurityLockEnabled(false)
                                    }
                                },
                            )
                        },
                    )
                    LedgerListRow(
                        title = "关于",
                        leading = { LedgerIconTile(Icons.Outlined.Info) },
                        value = "个人账本 v${com.personal.ledger.BuildConfig.VERSION_NAME}",
                    )
                }
            }
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("深色模式") },
            text = {
                Column {
                    listOf(
                        ThemeMode.SYSTEM to "跟随系统",
                        ThemeMode.LIGHT to "浅色",
                        ThemeMode.DARK to "深色",
                    ).forEach { (mode, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    hapticLight()
                                    vm.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = state.themeMode == mode,
                                onClick = null,
                            )
                            Text(label, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("关闭") }
            },
        )
    }
}

private fun themeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> "跟随系统"
    ThemeMode.LIGHT -> "浅色"
    ThemeMode.DARK -> "深色"
}

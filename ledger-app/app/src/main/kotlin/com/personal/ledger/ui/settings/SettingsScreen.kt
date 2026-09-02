package com.personal.ledger.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.personal.ledger.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(container: AppContainer, navController: NavHostController) {
    val vm: SettingsViewModel = viewModel {
        SettingsViewModel(container.ledgerRepository, container.settings)
    }
    val state by vm.uiState.collectAsStateWithLifecycle()
    var showLedgerDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showCreateLedger by remember { mutableStateOf(false) }
    val activity = LocalContext.current as? androidx.fragment.app.FragmentActivity

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionTitle("账本")
            SettingRow("当前账本", state.currentLedgerName) { showLedgerDialog = true }
            HorizontalDivider()

            SectionTitle("记账")
            SettingRow("分类管理", "") { navController.navigate(Routes.CATEGORIES) }
            SettingRow("商家管理", "") { navController.navigate(Routes.MERCHANTS) }
            SettingRow("标签管理", "") { navController.navigate(Routes.TAGS) }
            SettingRow("预算设置", "") { navController.navigate(Routes.BUDGETS) }
            HorizontalDivider()

            SectionTitle("数据")
            SettingRow("导出 / 恢复 / 自动备份", "") { navController.navigate(Routes.DATA) }
            HorizontalDivider()

            SectionTitle("应用")
            SettingRow("深色模式", themeLabel(state.themeMode)) { showThemeDialog = true }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("安全锁", style = MaterialTheme.typography.bodyLarge)
                    Text("系统生物识别 / 设备凭据；冷启动和长时间离开后验证", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = state.securityLockEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            // 开启前先验证身份，成功后才启用（防止误触锁死）。
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
            }
            SettingRow("关于", "个人账本 v${com.personal.ledger.BuildConfig.VERSION_NAME}") {}
        }
    }

    if (showLedgerDialog) {
        AlertDialog(
            onDismissRequest = { showLedgerDialog = false },
            title = { Text("账本管理") },
            text = {
                Column {
                    state.ledgers.forEach { ledger ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    vm.switchLedger(ledger.id)
                                    showLedgerDialog = false
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = ledger.name == state.currentLedgerName,
                                onClick = {
                                    vm.switchLedger(ledger.id)
                                    showLedgerDialog = false
                                },
                            )
                            Text(
                                ledger.name + if (ledger.isArchived) "（已归档）" else "",
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp),
                            )
                            if (ledger.name != state.currentLedgerName) {
                                TextButton(onClick = { vm.setLedgerArchived(ledger.id, !ledger.isArchived) {} }) {
                                    Text(if (ledger.isArchived) "恢复" else "归档")
                                }
                            }
                        }
                    }
                    TextButton(onClick = {
                        showLedgerDialog = false
                        showCreateLedger = true
                    }) { Text("+ 新建账本") }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLedgerDialog = false }) { Text("关闭") }
            },
        )
    }

    if (showCreateLedger) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateLedger = false },
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
                        vm.createLedger(name) { showCreateLedger = false }
                    }
                }) { Text("创建") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateLedger = false }) { Text("取消") }
            },
        )
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
                                    vm.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = state.themeMode == mode,
                                onClick = {
                                    vm.setThemeMode(mode)
                                    showThemeDialog = false
                                },
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

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun SettingRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        if (subtitle.isNotEmpty()) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

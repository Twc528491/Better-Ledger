package com.personal.ledger.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.personal.ledger.data.AppContainer
import com.personal.ledger.navigation.Routes
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
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text("记账项", style = MaterialTheme.typography.titleSmall)
            }
            item {
                SettingsCard(emoji = "🏷️", title = "分类管理", subtitle = "收入 / 支出分类，最多两级", onClick = {
                    hapticNavigation()
                    navController.navigate(Routes.CATEGORIES)
                })
            }
            item {
                SettingsCard(emoji = "🏪", title = "商家管理", subtitle = "常去商家，记账快速选择", onClick = {
                    hapticNavigation()
                    navController.navigate(Routes.MERCHANTS)
                })
            }
            item {
                SettingsCard(emoji = "#️⃣", title = "标签管理", subtitle = "自定义标签，一笔可多个", onClick = {
                    hapticNavigation()
                    navController.navigate(Routes.TAGS)
                })
            }
            item {
                SettingsCard(emoji = "🎯", title = "预算设置", subtitle = "月总预算与分类预算", onClick = {
                    hapticNavigation()
                    navController.navigate(Routes.BUDGETS)
                })
            }

            item {
                Text(
                    "账本",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            item {
                SettingsCard(
                    emoji = "📚",
                    title = "账本管理",
                    subtitle = "当前：${state.currentLedgerName}",
                    onClick = {
                        hapticNavigation()
                        showLedgerDialog = true
                    },
                )
            }
        }
    }

    if (showLedgerDialog) {
        AlertDialog(
            onDismissRequest = {
                hapticNavigation()
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
                    hapticNavigation()
                    showLedgerDialog = false
                }) { Text("关闭") }
            },
        )
    }

    if (showCreateLedger) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = {
                hapticNavigation()
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
                    hapticNavigation()
                    showCreateLedger = false
                }) { Text("取消") }
            },
        )
    }
}

/** 带 emoji 图标与箭头的设置入口卡片。 */
@Composable
private fun SettingsCard(
    emoji: String,
    title: String,
    subtitle: String = "",
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(emoji, fontSize = 26.sp)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp),
            ) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                if (subtitle.isNotEmpty()) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall)
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

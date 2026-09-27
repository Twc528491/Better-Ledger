package com.personal.ledger.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.personal.ledger.data.AppContainer
import com.personal.ledger.data.backup.BackupConstants
import com.personal.ledger.data.backup.BackupRepository
import com.personal.ledger.data.backup.BackupScheduler
import com.personal.ledger.ui.common.LedgerGroup
import com.personal.ledger.ui.common.LedgerIconTile
import com.personal.ledger.ui.common.LedgerListRow
import com.personal.ledger.ui.theme.LedgerSpacing
import com.personal.ledger.ui.common.rememberHapticConfirm
import com.personal.ledger.ui.common.rememberHapticLight
import com.personal.ledger.ui.common.rememberHapticNavigation
import com.personal.ledger.ui.common.rememberHapticReject
import com.personal.ledger.util.TimeUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataScreen(container: AppContainer, navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hapticLight = rememberHapticLight()
    val hapticNavigation = rememberHapticNavigation()
    val hapticConfirm = rememberHapticConfirm()
    val hapticReject = rememberHapticReject()
    val backupDirUri by container.settings.backupDirUri.collectAsStateWithLifecycle(initialValue = null)
    val autoBackupEnabled by container.settings.autoBackupEnabled.collectAsStateWithLifecycle(initialValue = false)

    var message by remember { mutableStateOf<String?>(null) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }

    fun toast(text: String) {
        message = text
    }

    // 导出：CreateDocument
    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BackupConstants.MIME_CSV)
    ) { uri ->
        if (uri != null) scope.launch {
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(container.backupRepository.buildCsv()) } != null
            }.getOrDefault(false)
            if (ok) hapticConfirm() else hapticReject()
            toast(if (ok) "CSV 已导出" else "CSV 导出失败")
        }
    }
    val jsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BackupConstants.MIME_JSON)
    ) { uri ->
        if (uri != null) scope.launch {
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(container.backupRepository.buildBackupJson()) } != null
            }.getOrDefault(false)
            if (ok) hapticConfirm() else hapticReject()
            toast(if (ok) "JSON 备份已导出" else "JSON 导出失败")
        }
    }
    val sqliteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BackupConstants.MIME_SQLITE)
    ) { uri ->
        if (uri != null) scope.launch {
            val result = runCatching {
                val tmp = java.io.File(context.cacheDir, "ledger-export-${System.currentTimeMillis()}.db")
                container.backupRepository.copyDatabaseFileTo(tmp).getOrThrow()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    tmp.inputStream().use { it.copyTo(out) }
                } ?: throw IllegalStateException("无法写入目标")
                tmp.delete()
            }
            if (result.isSuccess) hapticConfirm() else hapticReject()
            toast(if (result.isSuccess) "SQLite 备份已导出" else "SQLite 导出失败")
        }
    }
    // 恢复：OpenDocument
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            hapticNavigation()
            pendingRestoreUri = uri
        }
    }
    // 自动备份目录：OpenDocumentTree + 持久化权限
    val treeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
            container.settings.setBackupDirUri(uri.toString())
            BackupScheduler.enable(context)
            container.settings.setAutoBackupEnabled(true)
            hapticConfirm()
            toast("备份目录已设置，自动备份已开启")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("数据管理") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = LedgerSpacing.md,
                    top = LedgerSpacing.xs,
                    end = LedgerSpacing.md,
                    bottom = LedgerSpacing.xl,
                ),
            verticalArrangement = Arrangement.spacedBy(LedgerSpacing.lg),
        ) {
            LedgerGroup(title = "导出") {
                ActionRow(Icons.Outlined.TableChart, "导出 CSV", "Excel 可直接打开") {
                    hapticNavigation()
                    csvLauncher.launch("ledger-export-${TimeUtils.currentMonthKey()}.csv")
                }
                ActionRow(Icons.Outlined.DataObject, "导出 JSON", "完整备份") {
                    hapticNavigation()
                    jsonLauncher.launch("ledger-backup-${TimeUtils.currentMonthKey()}.json")
                }
                ActionRow(Icons.Outlined.Storage, "导出 SQLite", "数据库文件") {
                    hapticNavigation()
                    sqliteLauncher.launch("ledger-export-${TimeUtils.currentMonthKey()}.db")
                }
            }

            Column {
                LedgerGroup(title = "恢复") {
                    ActionRow(
                        icon = Icons.Outlined.SettingsBackupRestore,
                        title = "从备份恢复",
                        subtitle = "整库覆盖",
                        tint = MaterialTheme.colorScheme.error,
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ) {
                        hapticNavigation()
                        restoreLauncher.launch(arrayOf(BackupConstants.MIME_JSON, BackupConstants.MIME_SQLITE, "*/*"))
                    }
                }
                GroupFootnote("恢复为整库覆盖，不做合并；恢复前会自动备份当前数据。")
            }

            LedgerGroup(title = "自动备份") {
                ActionRow(
                    icon = Icons.Outlined.FolderOpen,
                    title = if (backupDirUri == null) "选择备份目录" else "更改备份目录",
                    subtitle = "当前目录：${backupDirUri ?: "未设置"}",
                ) {
                    hapticNavigation()
                    treeLauncher.launch(null)
                }
                LedgerListRow(
                    title = "自动备份",
                    subtitle = "每日一次；保留 7 个每日 + 4 个每周 + 12 个每月",
                    leading = { LedgerIconTile(Icons.Outlined.Schedule) },
                    trailing = {
                        Switch(
                            checked = autoBackupEnabled,
                            onCheckedChange = { enabled ->
                                hapticLight()
                                scope.launch {
                                    container.settings.setAutoBackupEnabled(enabled)
                                    if (enabled) {
                                        if (backupDirUri != null) {
                                            BackupScheduler.enable(context)
                                        } else {
                                            hapticReject()
                                            toast("请先选择备份目录")
                                            container.settings.setAutoBackupEnabled(false)
                                        }
                                    } else {
                                        BackupScheduler.disable(context)
                                    }
                                }
                            },
                        )
                    },
                )
            }
        }
    }

    message?.let {
        AlertDialog(
            onDismissRequest = {
                message = null
            },
            title = { Text("提示") },
            text = { Text(it) },
            confirmButton = {
                TextButton(onClick = {
                    message = null
                }) { Text("好的") }
            },
        )
    }

    pendingRestoreUri?.let { uri ->
        AlertDialog(
            onDismissRequest = {
                pendingRestoreUri = null
            },
            title = { Text("确认恢复") },
            text = {
                Text("恢复会覆盖现有全部数据（恢复前将自动备份当前数据）。是否继续？")
            },
            confirmButton = {
                TextButton(onClick = {
                    hapticConfirm()
                    pendingRestoreUri = null
                    scope.launch {
                        val fileName = uri.lastPathSegment.orEmpty()
                        val isJson = fileName.endsWith(".json") ||
                            runCatching {
                                context.contentResolver.openInputStream(uri)?.use { input ->
                                    val buf = ByteArray(1)
                                    input.read(buf) >= 0 && buf[0] == '{'.code.toByte()
                                } == true
                            }.getOrDefault(false)
                        if (isJson) {
                            val result = runCatching {
                                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                                    ?: throw IllegalStateException("无法读取备份文件")
                                container.backupRepository.restoreJson(bytes).getOrThrow()
                            }
                            if (result.isSuccess) {
                                container.ledgerRepository.fallbackCurrentLedgerIfInvalid()
                                toast("恢复成功")
                            } else {
                                hapticReject()
                                toast("恢复失败：${result.exceptionOrNull()?.message}")
                            }
                        } else {
                            val result = runCatching {
                                container.backupRepository.restoreSqlite(uri).getOrThrow()
                            }
                            if (result.isSuccess) {
                                BackupRepository.restartApp(context)
                            } else {
                                hapticReject()
                                toast("恢复失败：${result.exceptionOrNull()?.message}")
                            }
                        }
                    }
                }) { Text("恢复") }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingRestoreUri = null
                }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun GroupFootnote(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = LedgerSpacing.xxs, vertical = LedgerSpacing.xs),
    )
}

/** 数据操作行：图标方块 + 标题 / 说明 + 箭头（替代整宽实心按钮）。 */
@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    onClick: () -> Unit,
) {
    LedgerListRow(
        title = title,
        subtitle = subtitle,
        leading = { LedgerIconTile(icon, tint = tint, containerColor = containerColor) },
        showChevron = true,
        onClick = onClick,
    )
}

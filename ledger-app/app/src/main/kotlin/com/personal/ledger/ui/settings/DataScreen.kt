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
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.personal.ledger.data.backup.BackupCatalog
import com.personal.ledger.data.backup.BackupConstants
import com.personal.ledger.navigation.Routes
import com.personal.ledger.ui.common.LedgerGroup
import com.personal.ledger.ui.common.LedgerIconTile
import com.personal.ledger.ui.common.LedgerListRow
import com.personal.ledger.ui.theme.LedgerSpacing
import com.personal.ledger.ui.common.rememberHapticConfirm
import com.personal.ledger.ui.common.rememberHapticNavigation
import com.personal.ledger.ui.common.rememberHapticReject
import com.personal.ledger.util.TimeUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataScreen(container: AppContainer, navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hapticNavigation = rememberHapticNavigation()
    val hapticConfirm = rememberHapticConfirm()
    val hapticReject = rememberHapticReject()
    val backupDirUri by container.settings.backupDirUri.collectAsStateWithLifecycle(initialValue = null)
    val lastBackupAt by container.settings.lastBackupAt.collectAsStateWithLifecycle(initialValue = null)
    val backupWarning by container.settings.lastBackupWarning.collectAsStateWithLifecycle(initialValue = null)

    var message by remember { mutableStateOf<String?>(null) }
    var pendingRestore by remember { mutableStateOf<RestoreRequest?>(null) }
    var backingUp by remember { mutableStateOf(false) }
    var locationLabel by remember { mutableStateOf("") }

    LaunchedEffect(backupDirUri) {
        val location = container.backupRepository.currentLocation()
        locationLabel = (if (location.isCustom) "自定义文件夹：" else "默认目录：") + location.store.describe()
    }

    fun toast(text: String) {
        message = text
    }

    // 导出到…：CreateDocument（每次选择位置）
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
    // 从文件恢复：OpenDocument
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            hapticNavigation()
            pendingRestore = RestoreRequest(uri)
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
            LedgerGroup(title = "备份") {
                ActionRow(
                    icon = Icons.Outlined.Backup,
                    title = if (backingUp) "正在备份…" else "立即备份",
                    subtitle = lastBackupAt?.let { "上次备份：" + BackupCatalog.formatTime(it) } ?: "尚未备份",
                ) {
                    if (backingUp) return@ActionRow
                    hapticNavigation()
                    backingUp = true
                    scope.launch {
                        container.backupRepository.backupNow()
                            .onSuccess {
                                hapticConfirm()
                                toast(
                                    if (it.fellBackToDefault) {
                                        "自定义文件夹无法访问，备份已保存到默认目录：${it.locationLabel}"
                                    } else {
                                        "已备份到 ${it.locationLabel}"
                                    },
                                )
                            }
                            .onFailure {
                                hapticReject()
                                toast("备份失败：${it.message}")
                            }
                        backingUp = false
                    }
                }
                ActionRow(
                    icon = Icons.Outlined.FolderOpen,
                    title = "备份管理",
                    subtitle = backupWarning ?: locationLabel,
                    tint = if (backupWarning != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    containerColor = if (backupWarning != null) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                ) {
                    hapticNavigation()
                    navController.navigate(Routes.BACKUPS)
                }
            }

            LedgerGroup(title = "导出到…") {
                ActionRow(Icons.Outlined.TableChart, "导出 CSV 到…", "Excel 可直接打开") {
                    hapticNavigation()
                    csvLauncher.launch("ledger-export-${TimeUtils.currentMonthKey()}.csv")
                }
                ActionRow(Icons.Outlined.DataObject, "导出 JSON 到…", "完整备份，自选保存位置") {
                    hapticNavigation()
                    jsonLauncher.launch("ledger-backup-${TimeUtils.currentMonthKey()}.json")
                }
                ActionRow(Icons.Outlined.Storage, "导出 SQLite 到…", "数据库文件") {
                    hapticNavigation()
                    sqliteLauncher.launch("ledger-export-${TimeUtils.currentMonthKey()}.db")
                }
            }

            Column {
                LedgerGroup(title = "恢复") {
                    ActionRow(
                        icon = Icons.Outlined.SettingsBackupRestore,
                        title = "从文件恢复",
                        subtitle = "选择 JSON 或 SQLite 备份，整库覆盖",
                        tint = MaterialTheme.colorScheme.error,
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ) {
                        hapticNavigation()
                        restoreLauncher.launch(arrayOf(BackupConstants.MIME_JSON, BackupConstants.MIME_SQLITE, "*/*"))
                    }
                }
                GroupFootnote("恢复为整库覆盖，不做合并；恢复前会自动备份当前数据。")
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

    RestoreConfirmDialog(
        container = container,
        request = pendingRestore,
        onDismiss = { pendingRestore = null },
        onFinished = {
            pendingRestore = null
            toast(it)
        },
    )
}

@Composable
internal fun GroupFootnote(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        modifier = Modifier.padding(horizontal = LedgerSpacing.xxs, vertical = LedgerSpacing.xs),
    )
}

/** 数据操作行：图标方块 + 标题 / 说明 + 箭头（替代整宽实心按钮）。 */
@Composable
internal fun ActionRow(
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

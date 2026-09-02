package com.personal.ledger.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.personal.ledger.data.AppContainer
import com.personal.ledger.data.backup.BackupConstants
import com.personal.ledger.data.backup.BackupRepository
import com.personal.ledger.data.backup.BackupScheduler
import com.personal.ledger.util.TimeUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataScreen(container: AppContainer, navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
            toast(if (result.isSuccess) "SQLite 备份已导出" else "SQLite 导出失败")
        }
    }
    // 恢复：OpenDocument
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) pendingRestoreUri = uri
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
            toast("备份目录已设置，自动备份已开启")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("数据管理") },
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
            SectionTitle("导出")
            ActionButton("导出 CSV（Excel 可直接打开）") {
                csvLauncher.launch("ledger-export-${TimeUtils.currentMonthKey()}.csv")
            }
            ActionButton("导出 JSON（完整备份）") {
                jsonLauncher.launch("ledger-backup-${TimeUtils.currentMonthKey()}.json")
            }
            ActionButton("导出 SQLite（数据库文件）") {
                sqliteLauncher.launch("ledger-export-${TimeUtils.currentMonthKey()}.db")
            }
            HorizontalDivider()

            SectionTitle("恢复")
            ActionButton("从备份恢复（整库覆盖）") {
                restoreLauncher.launch(arrayOf(BackupConstants.MIME_JSON, BackupConstants.MIME_SQLITE, "*/*"))
            }
            Text(
                "恢复为整库覆盖，不做合并；恢复前会自动备份当前数据。",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            HorizontalDivider()

            SectionTitle("自动备份")
            ActionButton(if (backupDirUri == null) "选择备份目录" else "更改备份目录") {
                treeLauncher.launch(null)
            }
            Text(
                "当前目录：${backupDirUri ?: "未设置"}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("自动备份", style = MaterialTheme.typography.bodyLarge)
                    Text("每日一次；保留 7 个每日 + 4 个每周 + 12 个每月", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = autoBackupEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            container.settings.setAutoBackupEnabled(enabled)
                            if (enabled) {
                                if (backupDirUri != null) {
                                    BackupScheduler.enable(context)
                                } else {
                                    toast("请先选择备份目录")
                                    container.settings.setAutoBackupEnabled(false)
                                }
                            } else {
                                BackupScheduler.disable(context)
                            }
                        }
                    },
                )
            }
        }
    }

    message?.let {
        AlertDialog(
            onDismissRequest = { message = null },
            title = { Text("提示") },
            text = { Text(it) },
            confirmButton = {
                TextButton(onClick = { message = null }) { Text("好的") }
            },
        )
    }

    pendingRestoreUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestoreUri = null },
            title = { Text("确认恢复") },
            text = {
                Text("恢复会覆盖现有全部数据（恢复前将自动备份当前数据）。是否继续？")
            },
            confirmButton = {
                TextButton(onClick = {
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
                                toast("恢复失败：${result.exceptionOrNull()?.message}")
                            }
                        } else {
                            val result = runCatching {
                                container.backupRepository.restoreSqlite(uri).getOrThrow()
                            }
                            if (result.isSuccess) {
                                BackupRepository.restartApp(context)
                            } else {
                                toast("恢复失败：${result.exceptionOrNull()?.message}")
                            }
                        }
                    }
                }) { Text("恢复") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreUri = null }) { Text("取消") }
            },
        )
    }
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
private fun ActionButton(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Text(text)
        }
    }
}

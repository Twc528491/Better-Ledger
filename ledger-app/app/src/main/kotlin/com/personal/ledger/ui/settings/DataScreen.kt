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
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.personal.ledger.R
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
import com.personal.ledger.util.userMessage
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
    // (是否自定义文件夹, 位置说明)；未加载时为 null
    var location by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    LaunchedEffect(backupDirUri) {
        val current = container.backupRepository.currentLocation()
        location = current.isCustom to current.store.describe()
    }
    val locationLabel = location?.let { (isCustom, described) -> backupLocationLabel(isCustom, described) } ?: ""

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
            toast(context.getString(if (ok) R.string.set_export_csv_done else R.string.set_export_csv_failed))
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
            toast(context.getString(if (ok) R.string.set_export_json_done else R.string.set_export_json_failed))
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
            toast(
                context.getString(
                    if (result.isSuccess) R.string.set_export_sqlite_done else R.string.set_export_sqlite_failed,
                ),
            )
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
                title = { Text(stringResource(R.string.set_data_title)) },
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
            LedgerGroup(title = stringResource(R.string.set_group_backup)) {
                ActionRow(
                    icon = Icons.Outlined.Backup,
                    title = stringResource(if (backingUp) R.string.set_backup_working else R.string.set_backup_now),
                    subtitle = lastBackupAt?.let { stringResource(R.string.set_backup_last, backupTimeLabel(it)) }
                        ?: stringResource(R.string.set_backup_never),
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
                                        context.getString(R.string.set_backup_fell_back, it.locationLabel)
                                    } else {
                                        context.getString(R.string.set_backup_done, it.locationLabel)
                                    },
                                )
                            }
                            .onFailure {
                                hapticReject()
                                toast(context.getString(R.string.set_backup_failed, it.userMessage(context)))
                            }
                        backingUp = false
                    }
                }
                ActionRow(
                    icon = Icons.Outlined.FolderOpen,
                    title = stringResource(R.string.set_backups_title),
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

            LedgerGroup(title = stringResource(R.string.set_group_export)) {
                ActionRow(
                    Icons.Outlined.TableChart,
                    stringResource(R.string.set_export_csv),
                    stringResource(R.string.set_export_csv_subtitle),
                ) {
                    hapticNavigation()
                    csvLauncher.launch("ledger-export-${TimeUtils.currentMonthKey()}.csv")
                }
                ActionRow(
                    Icons.Outlined.DataObject,
                    stringResource(R.string.set_export_json),
                    stringResource(R.string.set_export_json_subtitle),
                ) {
                    hapticNavigation()
                    jsonLauncher.launch("ledger-backup-${TimeUtils.currentMonthKey()}.json")
                }
                ActionRow(
                    Icons.Outlined.Storage,
                    stringResource(R.string.set_export_sqlite),
                    stringResource(R.string.set_export_sqlite_subtitle),
                ) {
                    hapticNavigation()
                    sqliteLauncher.launch("ledger-export-${TimeUtils.currentMonthKey()}.db")
                }
            }

            Column {
                LedgerGroup(title = stringResource(R.string.set_group_restore)) {
                    ActionRow(
                        icon = Icons.Outlined.SettingsBackupRestore,
                        title = stringResource(R.string.set_restore_from_file),
                        subtitle = stringResource(R.string.set_restore_from_file_subtitle),
                        tint = MaterialTheme.colorScheme.error,
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ) {
                        hapticNavigation()
                        restoreLauncher.launch(arrayOf(BackupConstants.MIME_JSON, BackupConstants.MIME_SQLITE, "*/*"))
                    }
                }
                GroupFootnote(stringResource(R.string.set_restore_footnote))
            }
        }
    }

    message?.let {
        AlertDialog(
            onDismissRequest = {
                message = null
            },
            title = { Text(stringResource(R.string.dialog_notice)) },
            text = { Text(it) },
            confirmButton = {
                TextButton(onClick = {
                    message = null
                }) { Text(stringResource(R.string.action_ok)) }
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

/** 备份位置说明：“默认目录：…” / “自定义文件夹：…”。 */
@Composable
internal fun backupLocationLabel(isCustom: Boolean, described: String): String =
    stringResource(
        if (isCustom) R.string.set_backup_location_custom else R.string.set_backup_location_default,
        described,
    )

/** 备份时间显示（如 “2026-10-02 14:30”）；未知时间按当前语言显示。 */
@Composable
internal fun backupTimeLabel(epochMs: Long): String =
    if (epochMs <= 0L) stringResource(R.string.set_backup_time_unknown) else BackupCatalog.formatTime(epochMs)

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

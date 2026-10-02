package com.personal.ledger.ui.settings

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.SaveAs
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SettingsBackupRestore
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.personal.ledger.data.AppContainer
import com.personal.ledger.data.backup.BackupCatalog
import com.personal.ledger.data.backup.BackupConstants
import com.personal.ledger.data.backup.BackupEntry
import com.personal.ledger.data.backup.BackupFileKind
import com.personal.ledger.data.backup.BackupListing
import com.personal.ledger.data.backup.BackupScheduler
import com.personal.ledger.data.backup.BackupStore
import com.personal.ledger.data.backup.BackupStoreKind
import com.personal.ledger.ui.common.EmptyState
import com.personal.ledger.ui.common.LedgerGroup
import com.personal.ledger.ui.common.LedgerIconTile
import com.personal.ledger.ui.common.LedgerListRow
import com.personal.ledger.ui.common.rememberHapticConfirm
import com.personal.ledger.ui.common.rememberHapticDelete
import com.personal.ledger.ui.common.rememberHapticLight
import com.personal.ledger.ui.common.rememberHapticNavigation
import com.personal.ledger.ui.common.rememberHapticReject
import com.personal.ledger.ui.theme.LedgerSpacing
import kotlinx.coroutines.launch

/** 备份管理：备份位置、自动备份、立即备份与备份文件列表（恢复 / 另存为 / 删除）。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupsScreen(container: AppContainer, navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hapticLight = rememberHapticLight()
    val hapticNavigation = rememberHapticNavigation()
    val hapticConfirm = rememberHapticConfirm()
    val hapticDelete = rememberHapticDelete()
    val hapticReject = rememberHapticReject()
    val backupDirUri by container.settings.backupDirUri.collectAsStateWithLifecycle(initialValue = null)
    val autoBackupEnabled by container.settings.autoBackupEnabled.collectAsStateWithLifecycle(initialValue = false)
    val lastBackupAt by container.settings.lastBackupAt.collectAsStateWithLifecycle(initialValue = null)
    val backupWarning by container.settings.lastBackupWarning.collectAsStateWithLifecycle(initialValue = null)

    var listing by remember { mutableStateOf<BackupListing?>(null) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var backingUp by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var actionTarget by remember { mutableStateOf<Pair<BackupStore, BackupEntry>?>(null) }
    var deleteTarget by remember { mutableStateOf<Pair<BackupStore, BackupEntry>?>(null) }
    var saveAsTarget by remember { mutableStateOf<Pair<BackupStore, BackupEntry>?>(null) }
    var pendingRestore by remember { mutableStateOf<RestoreRequest?>(null) }
    var confirmResetFolder by remember { mutableStateOf(false) }

    LaunchedEffect(reloadKey, backupDirUri) {
        listing = container.backupRepository.listBackups()
    }

    fun reload() {
        reloadKey++
    }

    val treeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) scope.launch {
            container.backupRepository.useCustomFolder(uri)
                .onSuccess {
                    hapticConfirm()
                    message = "已改用自定义文件夹，之后的备份会保存到这里。"
                }
                .onFailure {
                    hapticReject()
                    message = "无法使用该文件夹：${it.message}"
                }
            reload()
        }
    }
    val saveAsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BackupConstants.MIME_JSON),
    ) { uri: Uri? ->
        val target = saveAsTarget
        saveAsTarget = null
        if (uri != null && target != null) scope.launch {
            container.backupRepository.copyBackupTo(target.first, target.second, uri)
                .onSuccess {
                    hapticConfirm()
                    message = "已另存备份"
                }
                .onFailure {
                    hapticReject()
                    message = "另存失败：${it.message}"
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("备份管理") },
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
        val current = listing
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
            // ---------- 备份位置 ----------
            Column {
                LedgerGroup(title = "备份位置") {
                    val location = current?.location
                    LedgerListRow(
                        title = when {
                            location == null -> "正在读取…"
                            location.isCustom -> "自定义文件夹"
                            else -> "默认目录"
                        },
                        subtitle = location?.store?.describe(),
                        leading = {
                            if (location?.customUnavailable == true) {
                                LedgerIconTile(
                                    Icons.Outlined.ErrorOutline,
                                    tint = MaterialTheme.colorScheme.error,
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                )
                            } else {
                                LedgerIconTile(Icons.Outlined.FolderOpen)
                            }
                        },
                    )
                    ActionRow(
                        icon = Icons.Outlined.Description,
                        title = if (location?.isCustom == true) "更改文件夹" else "改用自定义文件夹",
                        subtitle = "通过系统文件选择器选择",
                    ) {
                        hapticNavigation()
                        treeLauncher.launch(null)
                    }
                    if (location?.isCustom == true) {
                        ActionRow(
                            icon = Icons.Outlined.SettingsBackupRestore,
                            title = "恢复为默认目录",
                            subtitle = "自定义文件夹中已有的备份不会被删除",
                        ) {
                            hapticNavigation()
                            confirmResetFolder = true
                        }
                    }
                }
                when {
                    current?.location?.customUnavailable == true -> GroupFootnote(
                        "无法访问自定义文件夹（可能已被删除或授权失效）。自动备份与立即备份会暂时保存到默认目录，请重新选择文件夹或恢复默认目录。",
                        color = MaterialTheme.colorScheme.error,
                    )
                    backupWarning != null -> GroupFootnote(backupWarning.orEmpty(), color = MaterialTheme.colorScheme.error)
                    current?.location?.store?.kind == BackupStoreKind.DEFAULT_SHARED -> GroupFootnote(
                        "默认目录在文件管理器中可见，卸载应用后仍会保留。重新安装应用后，之前的备份不会出现在下方列表中，可在“数据管理 → 从文件恢复”中手动选择。",
                    )
                    current?.location?.store?.kind == BackupStoreKind.DEFAULT_APP_FILES -> GroupFootnote(
                        "当前系统版本的默认目录为应用专属目录，卸载应用时会一并删除。如需长期保存，请改用自定义文件夹或使用“另存为…”。",
                    )
                }
            }

            // ---------- 自动备份 / 立即备份 ----------
            LedgerGroup(title = "自动备份") {
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
                                    if (enabled) BackupScheduler.enable(context) else BackupScheduler.disable(context)
                                }
                            },
                        )
                    },
                )
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
                                if (it.fellBackToDefault) {
                                    message = "自定义文件夹无法访问，备份已保存到默认目录：${it.locationLabel}"
                                }
                            }
                            .onFailure {
                                hapticReject()
                                message = "备份失败：${it.message}"
                            }
                        backingUp = false
                        reload()
                    }
                }
            }

            // ---------- 备份文件 ----------
            if (current != null) {
                val entries = current.entries
                Column {
                    LedgerGroup(title = if (entries.isEmpty()) "备份文件" else "备份文件（${entries.size}）") {
                        when {
                            current.listError != null -> EmptyState(
                                icon = Icons.Outlined.ErrorOutline,
                                title = "无法读取备份文件",
                                subtitle = current.listError,
                            )
                            entries.isEmpty() -> EmptyState(
                                icon = Icons.Outlined.Inventory2,
                                title = "还没有备份",
                                subtitle = "点击“立即备份”创建第一个备份",
                            )
                            else -> entries.forEach { entry ->
                                BackupEntryRow(entry) {
                                    hapticNavigation()
                                    actionTarget = current.location.store to entry
                                }
                            }
                        }
                    }
                    GroupFootnote("点击备份可恢复、另存为或删除。只有“备份”会参与自动轮换，恢复前备份需手动清理。")
                }

                if (current.internalEntries.isNotEmpty()) {
                    Column {
                        LedgerGroup(title = "应用内部的恢复前备份（${current.internalEntries.size}）") {
                            current.internalEntries.forEach { entry ->
                                BackupEntryRow(entry) {
                                    hapticNavigation()
                                    actionTarget = current.internalStore to entry
                                }
                            }
                        }
                        GroupFootnote("当备份位置不可写时，恢复前备份会保存在应用内部，卸载应用时会一并删除。")
                    }
                }
            }
        }
    }

    actionTarget?.let { (store, entry) ->
        AlertDialog(
            onDismissRequest = { actionTarget = null },
            title = { Text(BackupCatalog.formatTime(BackupCatalog.backupTimeMs(entry))) },
            text = {
                Column {
                    Text(
                        entry.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(bottom = LedgerSpacing.xs),
                    )
                    BackupActionItem(Icons.Outlined.Restore, "恢复") {
                        hapticNavigation()
                        actionTarget = null
                        pendingRestore = RestoreRequest(Uri.parse(entry.location), entry.name)
                    }
                    BackupActionItem(Icons.Outlined.SaveAs, "另存为…") {
                        hapticNavigation()
                        actionTarget = null
                        saveAsTarget = store to entry
                        saveAsLauncher.launch(entry.name)
                    }
                    BackupActionItem(Icons.Outlined.Delete, "删除", color = MaterialTheme.colorScheme.error) {
                        hapticConfirm()
                        actionTarget = null
                        deleteTarget = store to entry
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { actionTarget = null }) { Text("取消") }
            },
        )
    }

    deleteTarget?.let { (store, entry) ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除备份") },
            text = { Text("将删除备份文件“${entry.name}”，此操作无法撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    hapticDelete()
                    deleteTarget = null
                    scope.launch {
                        if (!store.delete(entry)) {
                            hapticReject()
                            message = "删除失败：文件可能已被移动或不再属于本应用"
                        }
                        reload()
                    }
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消") }
            },
        )
    }

    if (confirmResetFolder) {
        AlertDialog(
            onDismissRequest = { confirmResetFolder = false },
            title = { Text("恢复为默认目录") },
            text = {
                Text(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        "之后的备份将保存到“内部存储/Documents/LedgerBackups”。自定义文件夹中已有的备份会保留，但不再显示在列表中。"
                    } else {
                        "之后的备份将保存到应用专属目录。自定义文件夹中已有的备份会保留，但不再显示在列表中。"
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    hapticConfirm()
                    confirmResetFolder = false
                    scope.launch {
                        container.backupRepository.useDefaultFolder()
                        reload()
                    }
                }) { Text("恢复默认") }
            },
            dismissButton = {
                TextButton(onClick = { confirmResetFolder = false }) { Text("取消") }
            },
        )
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

    RestoreConfirmDialog(
        container = container,
        request = pendingRestore,
        onDismiss = { pendingRestore = null },
        onFinished = {
            pendingRestore = null
            message = it
            reload()
        },
    )
}

@Composable
private fun BackupEntryRow(entry: BackupEntry, onClick: () -> Unit) {
    val preRestore = entry.kind == BackupFileKind.PRE_RESTORE
    LedgerListRow(
        title = BackupCatalog.formatTime(BackupCatalog.backupTimeMs(entry)),
        subtitle = BackupCatalog.typeLabel(entry.kind) + " · " + BackupCatalog.formatSize(entry.sizeBytes),
        leading = {
            LedgerIconTile(
                if (preRestore) Icons.Outlined.History else Icons.Outlined.Description,
                tint = if (preRestore) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
            )
        },
        showChevron = true,
        onClick = onClick,
    )
}

@Composable
private fun BackupActionItem(
    icon: ImageVector,
    label: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .heightIn(min = 52.dp)
            .padding(horizontal = LedgerSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = color)
        Text(
            label,
            color = color,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = LedgerSpacing.md),
        )
    }
}

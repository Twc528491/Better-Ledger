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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.personal.ledger.R
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
import com.personal.ledger.util.userMessage
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
                    message = context.getString(R.string.set_backup_custom_folder_set)
                }
                .onFailure {
                    hapticReject()
                    message = context.getString(R.string.set_backup_folder_unusable, it.userMessage(context))
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
                    message = context.getString(R.string.set_backup_saved_as)
                }
                .onFailure {
                    hapticReject()
                    message = context.getString(R.string.set_backup_save_as_failed, it.userMessage(context))
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.set_backups_title)) },
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
                LedgerGroup(title = stringResource(R.string.set_group_backup_location)) {
                    val location = current?.location
                    LedgerListRow(
                        title = when {
                            location == null -> stringResource(R.string.set_loading)
                            location.isCustom -> stringResource(R.string.set_backup_custom_folder)
                            else -> stringResource(R.string.set_backup_default_folder)
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
                        title = stringResource(
                            if (location?.isCustom == true) R.string.set_backup_change_folder else R.string.set_backup_use_custom_folder,
                        ),
                        subtitle = stringResource(R.string.set_backup_pick_folder_subtitle),
                    ) {
                        hapticNavigation()
                        treeLauncher.launch(null)
                    }
                    if (location?.isCustom == true) {
                        ActionRow(
                            icon = Icons.Outlined.SettingsBackupRestore,
                            title = stringResource(R.string.set_backup_reset_folder),
                            subtitle = stringResource(R.string.set_backup_reset_folder_subtitle),
                        ) {
                            hapticNavigation()
                            confirmResetFolder = true
                        }
                    }
                }
                when {
                    current?.location?.customUnavailable == true -> GroupFootnote(
                        stringResource(R.string.set_backup_custom_unavailable_note),
                        color = MaterialTheme.colorScheme.error,
                    )
                    backupWarning != null -> GroupFootnote(backupWarning.orEmpty(), color = MaterialTheme.colorScheme.error)
                    current?.location?.store?.kind == BackupStoreKind.DEFAULT_SHARED -> GroupFootnote(
                        stringResource(R.string.set_backup_default_shared_note),
                    )
                    current?.location?.store?.kind == BackupStoreKind.DEFAULT_APP_FILES -> GroupFootnote(
                        stringResource(R.string.set_backup_default_app_files_note),
                    )
                }
            }

            // ---------- 自动备份 / 立即备份 ----------
            LedgerGroup(title = stringResource(R.string.set_group_auto_backup)) {
                LedgerListRow(
                    title = stringResource(R.string.set_auto_backup),
                    subtitle = stringResource(R.string.set_auto_backup_subtitle),
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
                                if (it.fellBackToDefault) {
                                    message = context.getString(R.string.set_backup_fell_back, it.locationLabel)
                                }
                            }
                            .onFailure {
                                hapticReject()
                                message = context.getString(R.string.set_backup_failed, it.userMessage(context))
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
                    LedgerGroup(
                        title = if (entries.isEmpty()) {
                            stringResource(R.string.set_backup_files)
                        } else {
                            stringResource(R.string.set_backup_files_count, entries.size)
                        },
                    ) {
                        when {
                            current.listError != null -> EmptyState(
                                icon = Icons.Outlined.ErrorOutline,
                                title = stringResource(R.string.set_backup_list_error),
                                subtitle = current.listError,
                            )
                            entries.isEmpty() -> EmptyState(
                                icon = Icons.Outlined.Inventory2,
                                title = stringResource(R.string.set_backup_empty),
                                subtitle = stringResource(R.string.set_backup_empty_hint),
                            )
                            else -> entries.forEach { entry ->
                                BackupEntryRow(entry) {
                                    hapticNavigation()
                                    actionTarget = current.location.store to entry
                                }
                            }
                        }
                    }
                    GroupFootnote(stringResource(R.string.set_backup_list_footnote))
                }

                if (current.internalEntries.isNotEmpty()) {
                    Column {
                        LedgerGroup(title = stringResource(R.string.set_backup_internal_count, current.internalEntries.size)) {
                            current.internalEntries.forEach { entry ->
                                BackupEntryRow(entry) {
                                    hapticNavigation()
                                    actionTarget = current.internalStore to entry
                                }
                            }
                        }
                        GroupFootnote(stringResource(R.string.set_backup_internal_footnote))
                    }
                }
            }
        }
    }

    actionTarget?.let { (store, entry) ->
        AlertDialog(
            onDismissRequest = { actionTarget = null },
            title = { Text(backupTimeLabel(BackupCatalog.backupTimeMs(entry))) },
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
                    BackupActionItem(Icons.Outlined.Restore, stringResource(R.string.action_restore)) {
                        hapticNavigation()
                        actionTarget = null
                        pendingRestore = RestoreRequest(Uri.parse(entry.location), entry.name)
                    }
                    BackupActionItem(Icons.Outlined.SaveAs, stringResource(R.string.set_backup_save_as)) {
                        hapticNavigation()
                        actionTarget = null
                        saveAsTarget = store to entry
                        saveAsLauncher.launch(entry.name)
                    }
                    BackupActionItem(Icons.Outlined.Delete, stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) {
                        hapticConfirm()
                        actionTarget = null
                        deleteTarget = store to entry
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { actionTarget = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    deleteTarget?.let { (store, entry) ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.set_backup_delete_title)) },
            text = { Text(stringResource(R.string.set_backup_delete_message, entry.name)) },
            confirmButton = {
                TextButton(onClick = {
                    hapticDelete()
                    deleteTarget = null
                    scope.launch {
                        if (!store.delete(entry)) {
                            hapticReject()
                            message = context.getString(R.string.set_backup_delete_failed)
                        }
                        reload()
                    }
                }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    if (confirmResetFolder) {
        AlertDialog(
            onDismissRequest = { confirmResetFolder = false },
            title = { Text(stringResource(R.string.set_backup_reset_folder)) },
            text = {
                Text(
                    stringResource(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            R.string.set_backup_reset_message_shared
                        } else {
                            R.string.set_backup_reset_message_app
                        },
                    ),
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
                }) { Text(stringResource(R.string.set_backup_reset_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmResetFolder = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    message?.let {
        AlertDialog(
            onDismissRequest = { message = null },
            title = { Text(stringResource(R.string.dialog_notice)) },
            text = { Text(it) },
            confirmButton = {
                TextButton(onClick = { message = null }) { Text(stringResource(R.string.action_ok)) }
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
        title = backupTimeLabel(BackupCatalog.backupTimeMs(entry)),
        subtitle = stringResource(
            R.string.set_backup_entry_subtitle,
            stringResource(
                when (entry.kind) {
                    BackupFileKind.BACKUP -> R.string.set_backup_kind_backup
                    BackupFileKind.PRE_RESTORE -> R.string.set_backup_kind_pre_restore
                    BackupFileKind.OTHER -> R.string.set_backup_kind_other
                },
            ),
            if (entry.sizeBytes < 0) stringResource(R.string.set_backup_size_unknown) else BackupCatalog.formatSize(entry.sizeBytes),
        ),
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

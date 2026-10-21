package io.github.twc528491.betterledger.ui.settings

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.twc528491.betterledger.R
import io.github.twc528491.betterledger.data.AppContainer
import io.github.twc528491.betterledger.data.backup.BackupRepository
import io.github.twc528491.betterledger.data.backup.RestoreOutcome
import io.github.twc528491.betterledger.ui.common.rememberHapticConfirm
import io.github.twc528491.betterledger.ui.common.rememberHapticDelete
import io.github.twc528491.betterledger.ui.common.rememberHapticReject
import io.github.twc528491.betterledger.ui.theme.LedgerSpacing
import io.github.twc528491.betterledger.util.UserFacingException
import io.github.twc528491.betterledger.util.toUiText
import io.github.twc528491.betterledger.util.userMessage
import kotlinx.coroutines.launch

/** 待恢复的备份文件。 */
data class RestoreRequest(
    val uri: Uri,
    /** 已知文件名（用于判断 JSON / SQLite）；未知时为 null。 */
    val displayName: String? = null,
)

/**
 * 恢复确认与执行（数据管理页与备份管理页共用）。
 * JSON 恢复成功后回退失效的当前账本；SQLite 恢复成功后重启应用。
 * [onFinished] 收到要提示给用户的结果文案。
 */
@Composable
fun RestoreConfirmDialog(
    container: AppContainer,
    request: RestoreRequest?,
    onDismiss: () -> Unit,
    onFinished: (String) -> Unit,
) {
    // 先于提前返回创建，保证恢复过程中对话框关闭也不会取消协程
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val hapticConfirm = rememberHapticConfirm()
    val hapticReject = rememberHapticReject()
    var working by remember(request) { mutableStateOf(false) }
    if (request == null) return

    AlertDialog(
        onDismissRequest = { if (!working) onDismiss() },
        title = { Text(stringResource(R.string.set_restore_confirm_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(LedgerSpacing.xs)) {
                request.displayName?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(stringResource(if (working) R.string.set_restore_working else R.string.set_restore_warning))
            }
        },
        confirmButton = {
            TextButton(enabled = !working, onClick = {
                hapticConfirm()
                working = true
                scope.launch {
                    val result = container.backupRepository.restoreFromUri(request.uri, request.displayName)
                    result.onSuccess { outcome ->
                        when (outcome) {
                            RestoreOutcome.JSON_RESTORED -> {
                                container.ledgerRepository.fallbackCurrentLedgerIfInvalid()
                                onFinished(context.getString(R.string.set_restore_success))
                            }
                            RestoreOutcome.SQLITE_RESTART_REQUIRED -> BackupRepository.restartApp(context)
                        }
                    }.onFailure {
                        hapticReject()
                        onFinished(context.getString(R.string.set_restore_failed, it.userMessage(context)))
                    }
                    working = false
                }
            }) { Text(stringResource(R.string.action_restore)) }
        },
        dismissButton = {
            TextButton(enabled = !working, onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/**
 * 可选地先执行“立即备份”，成功后再执行 [action]（彻底删除、合并账户等不可撤销操作）；
 * 备份失败则不执行，错误文案为 [backupFailedRes]（参数为失败原因）。
 */
suspend fun <T> runAfterOptionalBackup(
    container: AppContainer,
    backupFirst: Boolean,
    @StringRes backupFailedRes: Int,
    action: suspend () -> Result<T>,
): Result<T> {
    if (backupFirst) {
        val backup = container.backupRepository.backupNow()
        backup.exceptionOrNull()?.let {
            return Result.failure(
                UserFacingException(backupFailedRes, it.toUiText(R.string.set_error_unknown_short)),
            )
        }
    }
    return action()
}

/** 不可撤销操作确认框的文案（彻底删除 / 合并账户）。 */
data class IrreversibleActionTexts(
    @StringRes val backupFirst: Int,
    @StringRes val working: Int,
    @StringRes val workingWithBackup: Int,
    @StringRes val confirm: Int,
    @StringRes val failed: Int,
    @StringRes val backupFailed: Int,
) {
    companion object {
        val PURGE = IrreversibleActionTexts(
            backupFirst = R.string.set_purge_backup_first,
            working = R.string.set_purge_working,
            workingWithBackup = R.string.set_purge_working_backup,
            confirm = R.string.set_purge_confirm,
            failed = R.string.set_delete_failed,
            backupFailed = R.string.set_purge_backup_failed,
        )
        val MERGE_ACCOUNT = IrreversibleActionTexts(
            backupFirst = R.string.account_merge_backup_first,
            working = R.string.account_merge_working,
            workingWithBackup = R.string.account_merge_working_backup,
            confirm = R.string.account_merge_confirm,
            failed = R.string.account_merge_failed,
            backupFailed = R.string.account_merge_backup_failed,
        )
    }
}

/**
 * 彻底删除确认框（分类 / 商家 / 标签 / 账户 / 账本共用）：
 * 说明影响范围、强调不可撤销，并默认勾选“删除前先立即备份”。
 */
@Composable
fun PurgeDialog(
    container: AppContainer,
    title: String,
    impact: String,
    onDismiss: () -> Unit,
    onPurged: () -> Unit,
    purge: suspend () -> Result<Unit>,
    confirmEnabled: Boolean = true,
    extra: @Composable ColumnScope.() -> Unit = {},
) = IrreversibleActionDialog(
    container = container,
    title = title,
    impact = impact,
    texts = IrreversibleActionTexts.PURGE,
    onDismiss = onDismiss,
    onDone = onPurged,
    action = purge,
    confirmEnabled = confirmEnabled,
    extra = extra,
)

/**
 * 不可撤销操作的通用确认框：说明影响范围、强调不可撤销，
 * 并默认勾选“先立即备份”（备份失败则不执行）。
 */
@Composable
fun IrreversibleActionDialog(
    container: AppContainer,
    title: String,
    impact: String,
    texts: IrreversibleActionTexts,
    onDismiss: () -> Unit,
    onDone: () -> Unit,
    action: suspend () -> Result<Unit>,
    confirmEnabled: Boolean = true,
    extra: @Composable ColumnScope.() -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val hapticDelete = rememberHapticDelete()
    val hapticReject = rememberHapticReject()
    var working by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var backupFirst by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = { if (!working) onDismiss() },
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(LedgerSpacing.xs),
            ) {
                Text(impact)
                extra()
                Text(
                    stringResource(R.string.set_purge_irreversible),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !working) { backupFirst = !backupFirst },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = backupFirst, onCheckedChange = null, enabled = !working)
                    Text(
                        stringResource(texts.backupFirst),
                        modifier = Modifier.padding(start = LedgerSpacing.xs),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (working) {
                    Text(
                        stringResource(if (backupFirst) texts.workingWithBackup else texts.working),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = confirmEnabled && !working,
                onClick = {
                    hapticDelete()
                    working = true
                    error = null
                    scope.launch {
                        runAfterOptionalBackup(container, backupFirst, texts.backupFailed, action)
                            .onSuccess {
                                working = false
                                onDone()
                            }
                            .onFailure {
                                hapticReject()
                                error = it.userMessage(context, texts.failed)
                                working = false
                            }
                    }
                },
            ) {
                Text(
                    stringResource(texts.confirm),
                    color = if (confirmEnabled && !working) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        dismissButton = {
            TextButton(enabled = !working, onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

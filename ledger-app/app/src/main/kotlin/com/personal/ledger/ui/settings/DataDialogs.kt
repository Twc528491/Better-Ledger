package com.personal.ledger.ui.settings

import android.net.Uri
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
import androidx.compose.ui.unit.dp
import com.personal.ledger.data.AppContainer
import com.personal.ledger.data.backup.BackupRepository
import com.personal.ledger.data.backup.RestoreOutcome
import com.personal.ledger.ui.common.rememberHapticConfirm
import com.personal.ledger.ui.common.rememberHapticDelete
import com.personal.ledger.ui.common.rememberHapticReject
import com.personal.ledger.ui.theme.LedgerSpacing
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
        title = { Text("确认恢复") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(LedgerSpacing.xs)) {
                request.displayName?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(if (working) "正在恢复，请稍候…" else "恢复会覆盖现有全部数据（恢复前将自动备份当前数据）。是否继续？")
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
                                onFinished("恢复成功")
                            }
                            RestoreOutcome.SQLITE_RESTART_REQUIRED -> BackupRepository.restartApp(context)
                        }
                    }.onFailure {
                        hapticReject()
                        onFinished("恢复失败：${it.message}")
                    }
                    working = false
                }
            }) { Text("恢复") }
        },
        dismissButton = {
            TextButton(enabled = !working, onClick = onDismiss) { Text("取消") }
        },
    )
}

/**
 * 可选地先执行“立即备份”，成功后再彻底删除；备份失败则不删除。
 */
suspend fun purgeAfterOptionalBackup(
    container: AppContainer,
    backupFirst: Boolean,
    purge: suspend () -> Result<Unit>,
): Result<Unit> {
    if (backupFirst) {
        val backup = container.backupRepository.backupNow()
        backup.exceptionOrNull()?.let {
            return Result.failure(IllegalStateException("备份失败，未执行删除：${it.message ?: "未知错误"}"))
        }
    }
    return purge()
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
) {
    val scope = rememberCoroutineScope()
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
                    "此操作无法撤销。建议先备份当前数据。",
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
                        "删除前先立即备份",
                        modifier = Modifier.padding(start = LedgerSpacing.xs),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (working) {
                    Text(
                        if (backupFirst) "正在备份并删除…" else "正在删除…",
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
                        purgeAfterOptionalBackup(container, backupFirst, purge)
                            .onSuccess {
                                working = false
                                onPurged()
                            }
                            .onFailure {
                                hapticReject()
                                error = it.message ?: "删除失败"
                                working = false
                            }
                    }
                },
            ) {
                Text(
                    "彻底删除",
                    color = if (confirmEnabled && !working) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        dismissButton = {
            TextButton(enabled = !working, onClick = onDismiss) { Text("取消") }
        },
    )
}

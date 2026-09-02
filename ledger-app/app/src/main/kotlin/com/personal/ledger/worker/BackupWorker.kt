package com.personal.ledger.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.personal.ledger.LedgerApplication
import com.personal.ledger.util.TimeUtils
import kotlinx.coroutines.flow.first

/**
 * 自动备份 Worker（ARCHITECTURE.md 第 8 节）：
 * 由 WorkManager 每日执行一次 best-effort 备份，并按每日 / 每周 / 每月快照策略轮换。
 * 备份失败不得影响正常记账（Result.retry 由 WorkManager 退避重试，不影响主流程）。
 */
class BackupWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as LedgerApplication
        val settings = app.container.settings
        if (!settings.autoBackupEnabled.first()) return Result.success()

        val dirUri = settings.backupDirUri.first() ?: return Result.success()
        val ok = app.container.backupRepository.runAutoBackup(
            android.net.Uri.parse(dirUri)
        )
        return if (ok) Result.success() else Result.retry()
    }
}

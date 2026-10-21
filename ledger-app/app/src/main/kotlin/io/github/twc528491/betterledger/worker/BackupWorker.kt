package io.github.twc528491.betterledger.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.twc528491.betterledger.LedgerApplication
import kotlinx.coroutines.flow.first

/**
 * 自动备份 Worker（ARCHITECTURE.md 第 8 节）：
 * 由 WorkManager 每日执行一次 best-effort 备份，并按每日 / 每周 / 每月快照策略轮换。
 * 备份失败不得影响正常记账（Result.retry 由 WorkManager 退避重试，不影响主流程）。
 *
 * 写入当前备份位置：未选择文件夹时为默认目录；自定义文件夹授权丢失时回退到默认目录并记录警告。
 */
class BackupWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as LedgerApplication
        val settings = app.container.settings
        if (!settings.autoBackupEnabled.first()) return Result.success()

        val ok = app.container.backupRepository.runAutoBackup()
        return if (ok) Result.success() else Result.retry()
    }
}

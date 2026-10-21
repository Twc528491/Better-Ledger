package io.github.twc528491.betterledger.data.backup

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import io.github.twc528491.betterledger.worker.BackupWorker
import java.util.concurrent.TimeUnit

/** 自动备份调度（WorkManager 每日 best-effort，不要求固定精确时间）。 */
object BackupScheduler {

    private const val UNIQUE_WORK_NAME = "ledger-auto-backup"

    fun enable(context: Context) {
        val request = PeriodicWorkRequestBuilder<BackupWorker>(24, TimeUnit.HOURS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun disable(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }
}

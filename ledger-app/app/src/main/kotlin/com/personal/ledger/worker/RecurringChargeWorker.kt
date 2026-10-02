package com.personal.ledger.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.personal.ledger.data.TextResolver
import com.personal.ledger.data.local.database.AppDatabase
import com.personal.ledger.data.local.database.RecurringDatabaseGate
import com.personal.ledger.data.repository.RecurringChargeRepository
import com.personal.ledger.data.repository.TransactionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit

/** Best-effort local generation. Android may defer this worker under power and background limits. */
class RecurringChargeWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = try {
        val summary = RecurringDatabaseGate.mutex.withLock {
            // Resolve the singleton after taking the gate: SQLite restore closes and clears it.
            val database = AppDatabase.getInstance(applicationContext)
            RecurringChargeRepository(
                database,
                TransactionRepository(database),
                TextResolver.of(applicationContext),
            ).processDue()
        }
        if (summary.hasMoreDue) {
            RecurringChargeScheduler.enqueueCatchUp(applicationContext, ExistingWorkPolicy.APPEND_OR_REPLACE)
        }
        Result.success()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        Result.retry()
    }
}

object RecurringChargeScheduler {
    private const val PERIODIC_NAME = "recurring-charge-periodic"
    private const val CATCH_UP_NAME = "recurring-charge-catch-up"

    /** Enqueues the daily fallback once; opening the app separately queues an immediate catch-up. */
    fun ensurePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<RecurringChargeWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            PERIODIC_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun enqueueCatchUp(
        context: Context,
        policy: ExistingWorkPolicy = ExistingWorkPolicy.KEEP,
    ) {
        val request = OneTimeWorkRequestBuilder<RecurringChargeWorker>().build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(CATCH_UP_NAME, policy, request)
    }
}

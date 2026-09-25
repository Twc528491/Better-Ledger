package com.personal.ledger

import android.app.Application
import com.personal.ledger.data.AppContainer
import com.personal.ledger.worker.RecurringChargeScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LedgerApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        applicationScope.launch {
            RecurringChargeScheduler.ensurePeriodic(this@LedgerApplication)
        }
    }

    fun launchBackground(block: suspend () -> Unit) {
        applicationScope.launch { block() }
    }
}

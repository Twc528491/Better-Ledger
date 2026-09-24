package com.personal.ledger

import android.app.Application
import com.personal.ledger.data.AppContainer
import com.personal.ledger.worker.RecurringChargeScheduler

class LedgerApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        RecurringChargeScheduler.ensurePeriodic(this)
    }
}

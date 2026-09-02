package com.personal.ledger

import android.app.Application
import com.personal.ledger.data.AppContainer

class LedgerApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

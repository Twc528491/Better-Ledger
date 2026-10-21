package io.github.twc528491.betterledger

import android.app.Application
import android.content.Context
import io.github.twc528491.betterledger.data.AppContainer
import io.github.twc528491.betterledger.util.AppLanguage
import io.github.twc528491.betterledger.util.AppStrings
import io.github.twc528491.betterledger.worker.RecurringChargeScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LedgerApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var container: AppContainer
        private set

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLanguage.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        AppStrings.init(this)
        container = AppContainer(this)
        applicationScope.launch {
            RecurringChargeScheduler.ensurePeriodic(this@LedgerApplication)
        }
    }

    fun launchBackground(block: suspend () -> Unit) {
        applicationScope.launch { block() }
    }
}

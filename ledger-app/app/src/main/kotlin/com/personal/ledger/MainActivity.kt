package com.personal.ledger

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personal.ledger.data.settings.ThemeMode
import com.personal.ledger.navigation.LedgerApp
import com.personal.ledger.ui.security.SecurityLock
import com.personal.ledger.ui.theme.LedgerTheme
import com.personal.ledger.util.AppLanguage
import com.personal.ledger.worker.RecurringChargeScheduler

class MainActivity : FragmentActivity() {

    private lateinit var securityLock: SecurityLock

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        securityLock = SecurityLock(this)
        enableEdgeToEdge()
        setContent {
            val container = (application as LedgerApplication).container
            val themeMode by container.settings.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            LedgerTheme(mode = themeMode) {
                LedgerApp(container)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        (application as LedgerApplication).launchBackground {
            RecurringChargeScheduler.enqueueCatchUp(applicationContext)
        }
        securityLock.checkAndPromptIfNeeded()
    }

    override fun onStop() {
        super.onStop()
        securityLock.onStopped()
    }
}

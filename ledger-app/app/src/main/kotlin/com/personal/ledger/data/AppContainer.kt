package com.personal.ledger.data

import android.content.Context
import com.personal.ledger.data.backup.BackupRepository
import com.personal.ledger.data.local.database.AppDatabase
import com.personal.ledger.data.repository.AccountRepository
import com.personal.ledger.data.repository.BudgetRepository
import com.personal.ledger.data.repository.CategoryRepository
import com.personal.ledger.data.repository.LedgerRepository
import com.personal.ledger.data.repository.RecurringChargeRepository
import com.personal.ledger.data.repository.MerchantRepository
import com.personal.ledger.data.repository.PurgeRepository
import com.personal.ledger.data.repository.TagRepository
import com.personal.ledger.data.repository.TransactionRepository
import com.personal.ledger.data.settings.SettingsRepository

/**
 * 手动依赖容器（V1 不引入 Hilt / Dagger）。
 * 由 [com.personal.ledger.LedgerApplication] 持有。
 */
class AppContainer(context: Context) {

    val database: AppDatabase = AppDatabase.getInstance(context)
    val settings: SettingsRepository = SettingsRepository(context)

    val categoryRepository = CategoryRepository(database)
    val merchantRepository = MerchantRepository(database)
    val tagRepository = TagRepository(database.tagDao())
    val accountRepository = AccountRepository(database.accountDao(), database.transactionDao())
    val transactionRepository = TransactionRepository(database)
    val recurringChargeRepository = RecurringChargeRepository(database, transactionRepository)
    val budgetRepository = BudgetRepository(
        database = database,
        categoryRepository = categoryRepository,
    )
    val ledgerRepository = LedgerRepository(
        ledgerDao = database.ledgerDao(),
        accountDao = database.accountDao(),
        categoryDao = database.categoryDao(),
        settings = settings,
    )
    val purgeRepository = PurgeRepository(database)
    val backupRepository = BackupRepository(
        context = context.applicationContext,
        database = database,
        settings = settings,
    )
}

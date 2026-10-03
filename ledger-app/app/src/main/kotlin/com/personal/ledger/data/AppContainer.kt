package com.personal.ledger.data

import android.content.Context
import com.personal.ledger.data.backup.BackupRepository
import com.personal.ledger.data.local.database.AppDatabase
import com.personal.ledger.data.repository.AccountMergeRepository
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

    /** 预置数据 / 生成文本按创建时的应用语言解析。 */
    val textResolver: TextResolver = TextResolver.of(context)

    val categoryRepository = CategoryRepository(database, textResolver)
    val merchantRepository = MerchantRepository(database)
    val tagRepository = TagRepository(database.tagDao())
    val accountRepository = AccountRepository(database.accountDao(), database.transactionDao())
    val transactionRepository = TransactionRepository(database)
    val recurringChargeRepository = RecurringChargeRepository(database, transactionRepository, textResolver)
    val budgetRepository = BudgetRepository(
        database = database,
        categoryRepository = categoryRepository,
    )
    val ledgerRepository = LedgerRepository(
        ledgerDao = database.ledgerDao(),
        accountDao = database.accountDao(),
        categoryDao = database.categoryDao(),
        settings = settings,
        text = textResolver,
    )
    val purgeRepository = PurgeRepository(database)
    val accountMergeRepository = AccountMergeRepository(database, settings)
    val backupRepository = BackupRepository(
        context = context.applicationContext,
        database = database,
        settings = settings,
    )
}

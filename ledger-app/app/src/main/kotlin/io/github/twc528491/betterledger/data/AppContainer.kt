package io.github.twc528491.betterledger.data

import android.content.Context
import io.github.twc528491.betterledger.data.backup.BackupRepository
import io.github.twc528491.betterledger.data.local.database.AppDatabase
import io.github.twc528491.betterledger.data.repository.AccountMergeRepository
import io.github.twc528491.betterledger.data.repository.AccountRepository
import io.github.twc528491.betterledger.data.repository.BudgetRepository
import io.github.twc528491.betterledger.data.repository.CategoryRepository
import io.github.twc528491.betterledger.data.repository.LedgerRepository
import io.github.twc528491.betterledger.data.repository.RecurringChargeRepository
import io.github.twc528491.betterledger.data.repository.MerchantRepository
import io.github.twc528491.betterledger.data.repository.PurgeRepository
import io.github.twc528491.betterledger.data.repository.TagRepository
import io.github.twc528491.betterledger.data.repository.TransactionRepository
import io.github.twc528491.betterledger.data.settings.SettingsRepository

/**
 * 手动依赖容器（V1 不引入 Hilt / Dagger）。
 * 由 [io.github.twc528491.betterledger.LedgerApplication] 持有。
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

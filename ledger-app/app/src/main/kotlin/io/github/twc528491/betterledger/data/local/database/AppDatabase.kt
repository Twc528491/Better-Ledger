package io.github.twc528491.betterledger.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.twc528491.betterledger.data.local.Converters
import io.github.twc528491.betterledger.data.local.dao.AccountDao
import io.github.twc528491.betterledger.data.local.dao.AccountMergeDao
import io.github.twc528491.betterledger.data.local.dao.BudgetDao
import io.github.twc528491.betterledger.data.local.dao.CategoryDao
import io.github.twc528491.betterledger.data.local.dao.LedgerDao
import io.github.twc528491.betterledger.data.local.dao.MerchantDao
import io.github.twc528491.betterledger.data.local.dao.MerchantCategoryDao
import io.github.twc528491.betterledger.data.local.dao.PurgeDao
import io.github.twc528491.betterledger.data.local.dao.RecurringChargeDao
import io.github.twc528491.betterledger.data.local.dao.TagDao
import io.github.twc528491.betterledger.data.local.dao.TransactionDao
import io.github.twc528491.betterledger.data.local.dao.TransactionTagDao
import io.github.twc528491.betterledger.data.local.entity.AccountEntity
import io.github.twc528491.betterledger.data.local.entity.BudgetEntity
import io.github.twc528491.betterledger.data.local.entity.CategoryEntity
import io.github.twc528491.betterledger.data.local.entity.LedgerEntity
import io.github.twc528491.betterledger.data.local.entity.MerchantEntity
import io.github.twc528491.betterledger.data.local.entity.MerchantCategoryEntity
import io.github.twc528491.betterledger.data.local.entity.RecurringChargeEntity
import io.github.twc528491.betterledger.data.local.entity.RecurringOccurrenceEntity
import io.github.twc528491.betterledger.data.local.entity.TagEntity
import io.github.twc528491.betterledger.data.local.entity.TransactionEntity
import io.github.twc528491.betterledger.data.local.entity.TransactionTagEntity

const val DATABASE_VERSION = 5

@Database(
    entities = [
        LedgerEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        MerchantEntity::class,
        MerchantCategoryEntity::class,
        TagEntity::class,
        TransactionEntity::class,
        TransactionTagEntity::class,
        BudgetEntity::class,
        RecurringChargeEntity::class,
        RecurringOccurrenceEntity::class,
    ],
    version = DATABASE_VERSION,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun ledgerDao(): LedgerDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun merchantDao(): MerchantDao
    abstract fun merchantCategoryDao(): MerchantCategoryDao
    abstract fun tagDao(): TagDao
    abstract fun transactionTagDao(): TransactionTagDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringChargeDao(): RecurringChargeDao
    abstract fun purgeDao(): PurgeDao
    abstract fun accountMergeDao(): AccountMergeDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ledger.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build().also { instance = it }
            }

        /** 关闭并释放单例（SQLite 恢复替换文件前调用；随后需重启应用）。 */
        fun closeInstance() {
            synchronized(this) {
                instance?.close()
                instance = null
            }
        }
    }
}

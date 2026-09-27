package com.personal.ledger.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.personal.ledger.data.local.Converters
import com.personal.ledger.data.local.dao.AccountDao
import com.personal.ledger.data.local.dao.BudgetDao
import com.personal.ledger.data.local.dao.CategoryDao
import com.personal.ledger.data.local.dao.LedgerDao
import com.personal.ledger.data.local.dao.MerchantDao
import com.personal.ledger.data.local.dao.MerchantCategoryDao
import com.personal.ledger.data.local.dao.RecurringChargeDao
import com.personal.ledger.data.local.dao.TagDao
import com.personal.ledger.data.local.dao.TransactionDao
import com.personal.ledger.data.local.dao.TransactionTagDao
import com.personal.ledger.data.local.entity.AccountEntity
import com.personal.ledger.data.local.entity.BudgetEntity
import com.personal.ledger.data.local.entity.CategoryEntity
import com.personal.ledger.data.local.entity.LedgerEntity
import com.personal.ledger.data.local.entity.MerchantEntity
import com.personal.ledger.data.local.entity.MerchantCategoryEntity
import com.personal.ledger.data.local.entity.RecurringChargeEntity
import com.personal.ledger.data.local.entity.RecurringOccurrenceEntity
import com.personal.ledger.data.local.entity.TagEntity
import com.personal.ledger.data.local.entity.TransactionEntity
import com.personal.ledger.data.local.entity.TransactionTagEntity

const val DATABASE_VERSION = 4

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

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ledger.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also { instance = it }
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

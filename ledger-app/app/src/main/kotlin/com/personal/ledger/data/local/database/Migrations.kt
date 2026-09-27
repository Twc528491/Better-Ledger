package com.personal.ledger.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `recurring_charge` (
                `id` TEXT NOT NULL,
                `ledgerId` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `amount` INTEGER NOT NULL,
                `currency` TEXT NOT NULL,
                `accountId` TEXT NOT NULL,
                `categoryId` TEXT NOT NULL,
                `frequency` TEXT NOT NULL,
                `startDate` TEXT NOT NULL,
                `minuteOfDay` INTEGER NOT NULL,
                `zoneId` TEXT NOT NULL,
                `nextOccurrenceIndex` INTEGER NOT NULL,
                `nextDueAt` INTEGER NOT NULL,
                `isPaused` INTEGER NOT NULL,
                `lastError` TEXT,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`ledgerId`) REFERENCES `ledger`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(`accountId`) REFERENCES `account`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(`categoryId`) REFERENCES `category`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )""".trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_charge_ledgerId` ON `recurring_charge` (`ledgerId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_charge_accountId` ON `recurring_charge` (`accountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_charge_categoryId` ON `recurring_charge` (`categoryId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_charge_isPaused_nextDueAt` ON `recurring_charge` (`isPaused`, `nextDueAt`)")
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `recurring_occurrence` (
                `ruleId` TEXT NOT NULL,
                `dueAt` INTEGER NOT NULL,
                `transactionId` TEXT NOT NULL,
                PRIMARY KEY(`ruleId`, `dueAt`),
                FOREIGN KEY(`ruleId`) REFERENCES `recurring_charge`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )""".trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_occurrence_ruleId_dueAt` ON `recurring_occurrence` (`ruleId`, `dueAt`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_recurring_occurrence_transactionId` ON `recurring_occurrence` (`transactionId`)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `merchant_category` (
                `merchantId` TEXT NOT NULL,
                `categoryId` TEXT NOT NULL,
                PRIMARY KEY(`merchantId`, `categoryId`),
                FOREIGN KEY(`merchantId`) REFERENCES `merchant`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(`categoryId`) REFERENCES `category`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )""".trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_merchant_category_categoryId` ON `merchant_category` (`categoryId`)")
        // Include soft-deleted history, but never infer a relationship across ledgers or types.
        db.execSQL(
            """INSERT OR IGNORE INTO merchant_category (merchantId, categoryId)
                SELECT DISTINCT m.id, root.id
                FROM `transaction` t
                JOIN merchant m ON m.id = t.merchantId AND m.ledgerId = t.ledgerId
                JOIN category c ON c.id = t.categoryId AND c.ledgerId = t.ledgerId
                JOIN category root ON root.id = COALESCE(c.parentId, c.id)
                    AND root.ledgerId = t.ledgerId AND root.parentId IS NULL
                    AND root.type = c.type
                WHERE t.merchantId IS NOT NULL AND t.categoryId IS NOT NULL
                    AND t.type IN ('EXPENSE', 'INCOME') AND c.type = t.type""".trimIndent(),
        )
    }
}

/** v4：分类可选图标库图标。既有分类 iconKey 为 NULL，继续按名称自动匹配。 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `category` ADD COLUMN `iconKey` TEXT")
    }
}

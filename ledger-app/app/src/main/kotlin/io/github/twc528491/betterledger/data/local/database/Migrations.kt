package io.github.twc528491.betterledger.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.twc528491.betterledger.domain.calc.SharedAccountNames

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

/**
 * v5：账户改为全局共享。
 *
 * SQLite 不能直接删除带外键的列，因此按 SQLite 官方步骤重建 `account` 表（新建 → 复制 → 删旧 → 改名），
 * 去掉 `ledgerId` 列、其外键与索引；建表语句与 Room 导出的 5.json 一致。
 * Room 执行迁移时尚未开启外键约束（onOpen 才开启），删除旧表不会触发 `recurring_charge`
 * 对账户的 RESTRICT 外键；改名后该外键按表名重新指向新表（`transaction` 对账户无外键，
 * 由 Repository 校验），结束前用 `foreign_key_check` 确认引用完整。
 *
 * 同名处理：不同账本中的同名账户（忽略首尾空白与大小写）改为 `名称（账本名）`，
 * 规则见 [SharedAccountNames]（与恢复旧版 JSON 备份共用）。
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. 读取旧归属，计算改名（必须在去掉 ledgerId 之前）。
        val ledgers = db.query("SELECT id, name, createdAt FROM ledger").use { c ->
            buildList {
                while (c.moveToNext()) add(SharedAccountNames.LedgerRef(c.getString(0), c.getString(1), c.getLong(2)))
            }
        }
        val accounts = db.query("SELECT id, ledgerId, name, createdAt FROM account").use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(SharedAccountNames.AccountRef(c.getString(0), c.getString(1), c.getString(2), c.getLong(3)))
                }
            }
        }
        val renames = SharedAccountNames.renames(accounts, ledgers)

        // 2. 重建账户表。
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `_new_account` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                "`type` TEXT NOT NULL, `currency` TEXT NOT NULL, `initialBalance` INTEGER NOT NULL, " +
                "`initialBalanceTime` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL, `isArchived` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
        db.execSQL(
            "INSERT INTO `_new_account` (`id`, `name`, `type`, `currency`, `initialBalance`, " +
                "`initialBalanceTime`, `createdAt`, `updatedAt`, `isArchived`) " +
                "SELECT `id`, `name`, `type`, `currency`, `initialBalance`, `initialBalanceTime`, " +
                "`createdAt`, `updatedAt`, `isArchived` FROM `account`",
        )
        db.execSQL("DROP TABLE `account`") // 连同 index_account_ledgerId 一起删除
        db.execSQL("ALTER TABLE `_new_account` RENAME TO `account`")

        // 3. 同名账户改名（保留 updatedAt，结果只取决于数据本身）。
        renames.forEach { (id, name) ->
            db.execSQL("UPDATE `account` SET `name` = ? WHERE `id` = ?", arrayOf<Any>(name, id))
        }

        // 4. 引用完整性检查（同 Room 自动迁移重建表后的检查）：只看指向账户的外键，
        //    其他与本次迁移无关的历史问题不在这里阻断升级。
        db.query("PRAGMA foreign_key_check(`recurring_charge`)").use { c ->
            val parentIndex = c.getColumnIndex("parent")
            while (c.moveToNext()) {
                if (parentIndex < 0 || c.getString(parentIndex) == "account") {
                    throw IllegalStateException("recurring_charge references a missing account after migration 4→5")
                }
            }
        }
    }
}

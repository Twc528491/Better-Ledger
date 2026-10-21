package io.github.twc528491.betterledger

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.twc528491.betterledger.data.local.database.AppDatabase
import io.github.twc528491.betterledger.data.local.database.MIGRATION_1_2
import io.github.twc528491.betterledger.data.local.database.MIGRATION_2_3
import io.github.twc528491.betterledger.data.local.database.MIGRATION_3_4
import io.github.twc528491.betterledger.data.local.database.MIGRATION_4_5
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Room Migration 仪器测试（ARCHITECTURE.md 第 4/12 节）。
 * 检查各版本 schema 可逐级升级到当前版本，并由 Room schema 导出结果校验迁移后的表与索引。
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val testDbName = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    fun createDatabaseAtVersion1() {
        // 打开 v1 数据库：MigrationTestHelper 会用导出的 1.json 校验 schema。
        val db = helper.createDatabase(testDbName, 1)
        db.close()
    }

    @Test
    fun migrateVersion1ToVersion2PreservesExistingRowsAndAddsRecurringTables() {
        val upgradeDbName = "migration-v1-to-v2"
        val oldDb = helper.createDatabase(upgradeDbName, 1)
        oldDb.execSQL(
            "INSERT INTO ledger (id, name, baseCurrency, createdAt, updatedAt, isArchived) " +
                "VALUES ('legacy-ledger', '旧账本', 'CNY', 10, 11, 0)",
        )
        oldDb.close()

        val migrated = helper.runMigrationsAndValidate(upgradeDbName, 2, true, MIGRATION_1_2)
        migrated.query("SELECT name FROM ledger WHERE id = 'legacy-ledger'").use { cursor ->
            org.junit.Assert.assertTrue(cursor.moveToFirst())
            org.junit.Assert.assertEquals("旧账本", cursor.getString(0))
        }
        migrated.query(
            "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' " +
                "AND name IN ('recurring_charge', 'recurring_occurrence')",
        ).use { cursor ->
            org.junit.Assert.assertTrue(cursor.moveToFirst())
            org.junit.Assert.assertEquals(2, cursor.getInt(0))
        }
        migrated.close()
    }

    @Test
    fun migrateVersion2ToVersion3BuildsOnlyValidRootLinksAndKeepsOldMerchants() {
        val name = "migration-v2-to-v3"
        val oldDb = helper.createDatabase(name, 2)
        oldDb.execSQL("INSERT INTO ledger VALUES ('l1', 'one', 'CNY', 1, 1, 0)")
        oldDb.execSQL("INSERT INTO ledger VALUES ('l2', 'two', 'CNY', 1, 1, 0)")
        oldDb.execSQL("INSERT INTO category VALUES ('food', 'l1', 'food', 'EXPENSE', NULL, 0, 0)")
        oldDb.execSQL("INSERT INTO category VALUES ('lunch', 'l1', 'lunch', 'EXPENSE', 'food', 1, 0)")
        oldDb.execSQL("INSERT INTO category VALUES ('foreign', 'l2', 'foreign', 'EXPENSE', NULL, 0, 0)")
        oldDb.execSQL("INSERT INTO merchant VALUES ('m1', 'l1', 'used', 1, 0)")
        oldDb.execSQL("INSERT INTO merchant VALUES ('unused', 'l1', 'unused', 2, 0)")
        oldDb.execSQL(
            "INSERT INTO `transaction` VALUES " +
                "('t1', 'l1', 'EXPENSE', 100, 'CNY', 'account', NULL, 'lunch', 'm1', " +
                "NULL, 1, 1, 1, 1)",
        )
        oldDb.execSQL(
            "INSERT INTO `transaction` VALUES " +
                "('t2', 'l1', 'EXPENSE', 100, 'CNY', 'account', NULL, 'foreign', 'm1', " +
                "NULL, 2, 2, 2, 0)",
        )
        oldDb.close()

        val migrated = helper.runMigrationsAndValidate(name, 3, true, MIGRATION_2_3)
        migrated.query("SELECT merchantId, categoryId FROM merchant_category").use { cursor ->
            org.junit.Assert.assertTrue(cursor.moveToFirst())
            org.junit.Assert.assertEquals("m1", cursor.getString(0))
            org.junit.Assert.assertEquals("food", cursor.getString(1))
            org.junit.Assert.assertFalse(cursor.moveToNext())
        }
        migrated.query("SELECT COUNT(*) FROM merchant").use { cursor ->
            org.junit.Assert.assertTrue(cursor.moveToFirst())
            org.junit.Assert.assertEquals(2, cursor.getInt(0))
        }
        migrated.query("SELECT COUNT(*) FROM `transaction`").use { cursor ->
            org.junit.Assert.assertTrue(cursor.moveToFirst())
            org.junit.Assert.assertEquals(2, cursor.getInt(0))
        }
        migrated.close()
    }

    @Test
    fun migrateVersion1ThroughVersion3KeepsOldLedger() {
        val name = "migration-v1-to-v3"
        val oldDb = helper.createDatabase(name, 1)
        oldDb.execSQL("INSERT INTO ledger VALUES ('legacy', 'old', 'CNY', 1, 1, 0)")
        oldDb.close()
        val migrated = helper.runMigrationsAndValidate(name, 3, true, MIGRATION_1_2, MIGRATION_2_3)
        migrated.query("SELECT name FROM ledger WHERE id = 'legacy'").use { cursor ->
            org.junit.Assert.assertTrue(cursor.moveToFirst())
            org.junit.Assert.assertEquals("old", cursor.getString(0))
        }
        migrated.close()
    }

    @Test
    fun migrateVersion3ToVersion4AddsNullableIconKeyAndKeepsCategories() {
        val name = "migration-v3-to-v4"
        val oldDb = helper.createDatabase(name, 3)
        oldDb.execSQL("INSERT INTO ledger VALUES ('l1', 'one', 'CNY', 1, 1, 0)")
        oldDb.execSQL("INSERT INTO category VALUES ('food', 'l1', '餐饮', 'EXPENSE', NULL, 0, 0)")
        oldDb.execSQL("INSERT INTO category VALUES ('lunch', 'l1', '午餐', 'EXPENSE', 'food', 1, 1)")
        oldDb.close()

        val migrated = helper.runMigrationsAndValidate(name, 4, true, MIGRATION_3_4)
        migrated.query(
            "SELECT id, name, parentId, sortOrder, isArchived, iconKey FROM category ORDER BY sortOrder",
        ).use { cursor ->
            org.junit.Assert.assertTrue(cursor.moveToFirst())
            org.junit.Assert.assertEquals("food", cursor.getString(0))
            org.junit.Assert.assertEquals("餐饮", cursor.getString(1))
            org.junit.Assert.assertTrue(cursor.isNull(2))
            org.junit.Assert.assertEquals(0, cursor.getInt(4))
            org.junit.Assert.assertTrue(cursor.isNull(5))
            org.junit.Assert.assertTrue(cursor.moveToNext())
            org.junit.Assert.assertEquals("lunch", cursor.getString(0))
            org.junit.Assert.assertEquals("food", cursor.getString(2))
            org.junit.Assert.assertEquals(1, cursor.getInt(4))
            org.junit.Assert.assertTrue(cursor.isNull(5))
            org.junit.Assert.assertFalse(cursor.moveToNext())
        }
        // 迁移后可写入自选图标。
        migrated.execSQL("UPDATE category SET iconKey = 'pets' WHERE id = 'food'")
        migrated.query("SELECT iconKey FROM category WHERE id = 'food'").use { cursor ->
            org.junit.Assert.assertTrue(cursor.moveToFirst())
            org.junit.Assert.assertEquals("pets", cursor.getString(0))
        }
        migrated.close()
    }

    @Test
    fun migrateVersion1ThroughVersion4KeepsOldLedgerAndCategories() {
        val name = "migration-v1-to-v4"
        val oldDb = helper.createDatabase(name, 1)
        oldDb.execSQL("INSERT INTO ledger VALUES ('legacy', 'old', 'CNY', 1, 1, 0)")
        oldDb.execSQL("INSERT INTO category VALUES ('food', 'legacy', '餐饮', 'EXPENSE', NULL, 0, 0)")
        oldDb.close()
        val migrated = helper.runMigrationsAndValidate(
            name, 4, true, MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
        )
        migrated.query("SELECT name FROM ledger WHERE id = 'legacy'").use { cursor ->
            org.junit.Assert.assertTrue(cursor.moveToFirst())
            org.junit.Assert.assertEquals("old", cursor.getString(0))
        }
        migrated.query("SELECT name, iconKey FROM category WHERE id = 'food'").use { cursor ->
            org.junit.Assert.assertTrue(cursor.moveToFirst())
            org.junit.Assert.assertEquals("餐饮", cursor.getString(0))
            org.junit.Assert.assertTrue(cursor.isNull(1))
        }
        migrated.close()
    }

    @Test
    fun migrateVersion4ToVersion5MakesAccountsGlobalAndRenamesCrossLedgerClashes() {
        val name = "migration-v4-to-v5"
        val oldDb = helper.createDatabase(name, 4)
        oldDb.execSQL("INSERT INTO ledger VALUES ('l1', '个人账本', 'CNY', 1, 1, 0)")
        oldDb.execSQL("INSERT INTO ledger VALUES ('l2', '旅行', 'CNY', 2, 2, 1)")
        val accountSql = "INSERT INTO account (id, ledgerId, name, type, currency, initialBalance, " +
            "initialBalanceTime, createdAt, updatedAt, isArchived) VALUES "
        oldDb.execSQL(accountSql + "('a1', 'l1', '现金', 'CASH', 'CNY', 100, 1, 1, 1, 0)")
        oldDb.execSQL(accountSql + "('a2', 'l2', '现金', 'CASH', 'CNY', 0, 1, 2, 2, 0)")
        oldDb.execSQL(accountSql + "('a3', 'l2', '银行卡', 'BANK', 'CNY', 500, 1, 3, 3, 1)")
        oldDb.execSQL(accountSql + "('a4', 'l1', 'Card', 'BANK', 'CNY', 0, 1, 4, 4, 0)")
        oldDb.execSQL(accountSql + "('a5', 'l2', ' card ', 'CREDIT_CARD', 'CNY', 0, 1, 5, 5, 0)")
        oldDb.execSQL("INSERT INTO category VALUES ('c1', 'l1', '餐饮', 'EXPENSE', NULL, 0, 0, NULL)")
        oldDb.execSQL("INSERT INTO category VALUES ('c2', 'l2', '餐饮', 'EXPENSE', NULL, 0, 0, NULL)")
        val txSql = "INSERT INTO `transaction` (id, ledgerId, type, amount, currency, accountId, " +
            "targetAccountId, categoryId, merchantId, note, transactionTime, createdAt, updatedAt, isDeleted) VALUES "
        oldDb.execSQL(txSql + "('t1', 'l1', 'EXPENSE', 30, 'CNY', 'a1', NULL, 'c1', NULL, NULL, 10, 10, 10, 0)")
        oldDb.execSQL(txSql + "('t2', 'l2', 'TRANSFER', 200, 'CNY', 'a2', 'a3', NULL, NULL, NULL, 11, 11, 11, 0)")
        oldDb.execSQL(txSql + "('t3', 'l2', 'EXPENSE', 990, 'CNY', 'a3', NULL, 'c2', NULL, '订阅', 12, 12, 12, 1)")
        oldDb.execSQL(
            "INSERT INTO recurring_charge (id, ledgerId, name, amount, currency, accountId, categoryId, " +
                "frequency, startDate, minuteOfDay, zoneId, nextOccurrenceIndex, nextDueAt, isPaused, " +
                "lastError, updatedAt) VALUES ('r1', 'l2', '订阅', 990, 'CNY', 'a3', 'c2', 'MONTHLY', " +
                "'2020-01-01', 540, 'UTC', 1, 1580547600000, 0, NULL, 12)",
        )
        oldDb.execSQL("INSERT INTO recurring_occurrence VALUES ('r1', 1577869200000, 't3')")
        oldDb.close()

        val migrated = helper.runMigrationsAndValidate(name, 5, true, MIGRATION_4_5)

        // account 表不再有 ledgerId 列与索引
        migrated.query("PRAGMA table_info(`account`)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            val columns = buildSet { while (cursor.moveToNext()) add(cursor.getString(nameIndex)) }
            org.junit.Assert.assertFalse("ledgerId" in columns)
        }
        migrated.query("SELECT COUNT(*) FROM sqlite_master WHERE name = 'index_account_ledgerId'").use { cursor ->
            org.junit.Assert.assertTrue(cursor.moveToFirst())
            org.junit.Assert.assertEquals(0, cursor.getInt(0))
        }
        // 跨账本同名改为“名称（账本名）”，其余保留（含已归档账户与原有数据）
        migrated.query(
            "SELECT id, name, type, initialBalance, isArchived, updatedAt FROM account ORDER BY id",
        ).use { cursor ->
            val rows = buildList {
                while (cursor.moveToNext()) {
                    add(listOf(cursor.getString(0), cursor.getString(1), cursor.getString(2),
                        cursor.getLong(3), cursor.getInt(4), cursor.getLong(5)))
                }
            }
            org.junit.Assert.assertEquals(
                listOf(
                    listOf("a1", "现金（个人账本）", "CASH", 100L, 0, 1L),
                    listOf("a2", "现金（旅行）", "CASH", 0L, 0, 2L),
                    listOf("a3", "银行卡", "BANK", 500L, 1, 3L),
                    listOf("a4", "Card（个人账本）", "BANK", 0L, 0, 4L),
                    listOf("a5", "card（旅行）", "CREDIT_CARD", 0L, 0, 5L),
                ),
                rows,
            )
        }
        // 交易（含转账与回收站）、定时扣款及其执行记录保持不变，引用完整
        migrated.query("SELECT id, accountId, targetAccountId, isDeleted FROM `transaction` ORDER BY id").use { cursor ->
            org.junit.Assert.assertTrue(cursor.moveToFirst())
            org.junit.Assert.assertEquals("a1", cursor.getString(1))
            org.junit.Assert.assertTrue(cursor.moveToNext())
            org.junit.Assert.assertEquals("a2", cursor.getString(1))
            org.junit.Assert.assertEquals("a3", cursor.getString(2))
            org.junit.Assert.assertTrue(cursor.moveToNext())
            org.junit.Assert.assertEquals(1, cursor.getInt(3))
            org.junit.Assert.assertFalse(cursor.moveToNext())
        }
        migrated.query("SELECT accountId FROM recurring_charge WHERE id = 'r1'").use { cursor ->
            org.junit.Assert.assertTrue(cursor.moveToFirst())
            org.junit.Assert.assertEquals("a3", cursor.getString(0))
        }
        migrated.query("SELECT COUNT(*) FROM recurring_occurrence").use { cursor ->
            org.junit.Assert.assertTrue(cursor.moveToFirst())
            org.junit.Assert.assertEquals(1, cursor.getInt(0))
        }
        migrated.query("PRAGMA foreign_key_check").use { cursor ->
            org.junit.Assert.assertFalse(cursor.moveToFirst())
        }
        // 外键仍指向重建后的 account 表：删除被定时扣款引用的账户应失败
        migrated.execSQL("PRAGMA foreign_keys = ON")
        val deleteFailed = runCatching { migrated.execSQL("DELETE FROM account WHERE id = 'a3'") }.isFailure
        org.junit.Assert.assertTrue(deleteFailed)
        migrated.close()
    }

    @Test
    fun migrateVersion1ThroughVersion5KeepsLedgerAndUniqueAccount() {
        val name = "migration-v1-to-v5"
        val oldDb = helper.createDatabase(name, 1)
        oldDb.execSQL("INSERT INTO ledger VALUES ('legacy', 'old', 'CNY', 1, 1, 0)")
        oldDb.execSQL(
            "INSERT INTO account (id, ledgerId, name, type, currency, initialBalance, initialBalanceTime, " +
                "createdAt, updatedAt, isArchived) VALUES ('cash', 'legacy', '现金', 'CASH', 'CNY', 0, 1, 1, 1, 0)",
        )
        oldDb.close()
        val migrated = helper.runMigrationsAndValidate(
            name, 5, true, MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
        )
        migrated.query("SELECT name FROM account WHERE id = 'cash'").use { cursor ->
            org.junit.Assert.assertTrue(cursor.moveToFirst())
            org.junit.Assert.assertEquals("现金", cursor.getString(0)) // 无冲突时不改名
        }
        migrated.close()
    }
}

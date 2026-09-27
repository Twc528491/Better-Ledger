package com.personal.ledger

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.personal.ledger.data.local.database.AppDatabase
import com.personal.ledger.data.local.database.MIGRATION_1_2
import com.personal.ledger.data.local.database.MIGRATION_2_3
import com.personal.ledger.data.local.database.MIGRATION_3_4
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
}

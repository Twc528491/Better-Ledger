package com.personal.ledger

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.personal.ledger.data.local.database.AppDatabase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Room Migration 仪器测试（ARCHITECTURE.md 第 4/12 节）。
 * 当前数据库版本为 1（初始版本，暂无迁移路径）；本测试验证导出的
 * Room Schema 与实现一致——未来每次版本升级新增的 Migration 都追加到
 * helper.runMigrationsAndValidate(...) 中，禁止 destructive migration。
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
}

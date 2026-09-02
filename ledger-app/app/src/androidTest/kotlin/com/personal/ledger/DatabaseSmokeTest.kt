package com.personal.ledger

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.personal.ledger.data.local.database.AppDatabase
import com.personal.ledger.data.local.entity.LedgerEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 数据层仪器测试：验证 Room 数据库基础读写。
 * 需要模拟器 / 真机运行；Migration 测试后续补充。
 */
@RunWith(AndroidJUnit4::class)
class DatabaseSmokeTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndQueryLedger() = runBlocking {
        val ledger = LedgerEntity(
            id = "l1",
            name = "测试账本",
            baseCurrency = "CNY",
            createdAt = 1L,
            updatedAt = 1L,
            isArchived = false,
        )
        db.ledgerDao().insert(ledger)
        assertEquals(ledger, db.ledgerDao().getById("l1"))
    }
}

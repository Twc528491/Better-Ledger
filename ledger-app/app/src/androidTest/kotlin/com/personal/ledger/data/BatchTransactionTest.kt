package com.personal.ledger.data

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.personal.ledger.data.local.database.AppDatabase
import com.personal.ledger.data.repository.AccountRepository
import com.personal.ledger.data.repository.CategoryRepository
import com.personal.ledger.data.repository.LedgerRepository
import com.personal.ledger.data.repository.RecurringChargeDraft
import com.personal.ledger.data.repository.RecurringChargeRepository
import com.personal.ledger.data.repository.TagRepository
import com.personal.ledger.data.repository.TransactionDraft
import com.personal.ledger.data.repository.TransactionRepository
import com.personal.ledger.data.settings.SettingsRepository
import com.personal.ledger.domain.model.AccountType
import com.personal.ledger.domain.model.CategoryType
import com.personal.ledger.domain.model.RecurrenceFrequency
import com.personal.ledger.domain.model.TransactionType
import com.personal.ledger.util.TimeUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

/** 多选批量删除 / 恢复 / 彻底删除 / 清空回收站（仪器测试）。 */
@RunWith(AndroidJUnit4::class)
class BatchTransactionTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var ledgerRepo: LedgerRepository
    private lateinit var accountRepo: AccountRepository
    private lateinit var categoryRepo: CategoryRepository
    private lateinit var txRepo: TransactionRepository
    private lateinit var tagRepo: TagRepository
    private lateinit var recurringRepo: RecurringChargeRepository

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val settings = SettingsRepository(context)
        ledgerRepo = LedgerRepository(db.ledgerDao(), db.accountDao(), db.categoryDao(), settings)
        accountRepo = AccountRepository(db.accountDao(), db.transactionDao())
        categoryRepo = CategoryRepository(db)
        txRepo = TransactionRepository(db)
        tagRepo = TagRepository(db.tagDao())
        recurringRepo = RecurringChargeRepository(db, txRepo)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private data class Fixture(val ledgerId: String, val accountId: String, val categoryId: String)

    private suspend fun fixture(): Fixture {
        ledgerRepo.ensureDefaultLedger()
        val ledgerId = db.ledgerDao().getAll().first().id
        val accountId = db.accountDao().getAll().first().id
        val categoryId = db.categoryDao().getByLedgerAndType(ledgerId, CategoryType.EXPENSE).first().id
        return Fixture(ledgerId, accountId, categoryId)
    }

    private suspend fun addExpense(f: Fixture, amount: Long = 100, tagIds: List<String> = emptyList()): String =
        txRepo.add(
            TransactionDraft(
                f.ledgerId, TransactionType.EXPENSE, amount, "CNY", f.accountId,
                categoryId = f.categoryId, transactionTime = TimeUtils.now(), tagIds = tagIds,
            ),
        ).getOrThrow().id

    @Test
    fun batchSoftDeleteMovesAllToRecycleBinAndRestoreUndoesIt() = runTest {
        val f = fixture()
        val ids = (1..3).map { addExpense(f, amount = it * 100L) }
        val keep = addExpense(f)

        assertEquals(3, txRepo.softDeleteAll(ids + ids.first()).getOrThrow())
        assertEquals(listOf(keep), txRepo.observeByLedger(f.ledgerId).first().map { it.id })
        assertEquals(ids.toSet(), txRepo.observeRecycleBin(f.ledgerId).first().map { it.id }.toSet())
        // 已在回收站的不重复计数。
        assertEquals(0, txRepo.softDeleteAll(ids).getOrThrow())

        assertEquals(3, txRepo.restoreAll(ids).getOrThrow())
        assertEquals(4, txRepo.observeByLedger(f.ledgerId).first().size)
        assertTrue(txRepo.observeRecycleBin(f.ledgerId).first().isEmpty())
    }

    @Test
    fun batchOperationsHandleMoreIdsThanOneSqlChunk() = runTest {
        val f = fixture()
        val ids = (1..520).map { addExpense(f) }

        assertEquals(520, txRepo.softDeleteAll(ids).getOrThrow())
        assertEquals(520, txRepo.observeRecycleBin(f.ledgerId).first().size)
        assertEquals(520, txRepo.purgeAll(ids).getOrThrow())
        assertTrue(db.transactionDao().getAll().isEmpty())
    }

    @Test
    fun purgeOnlyRemovesRecycledRowsAndTheirTagLinks() = runTest {
        val f = fixture()
        val tag = tagRepo.addTag(f.ledgerId, "报销").getOrThrow()
        val recycled = addExpense(f, tagIds = listOf(tag.id))
        val active = addExpense(f, tagIds = listOf(tag.id))
        txRepo.softDeleteAll(listOf(recycled)).getOrThrow()

        // 传入未删除的账单 id 也不会被彻底删除。
        assertEquals(1, txRepo.purgeAll(listOf(recycled, active)).getOrThrow())

        assertNull(db.transactionDao().getById(recycled))
        assertFalse(db.transactionDao().getById(active)!!.isDeleted)
        val links = db.transactionTagDao().getAll()
        assertEquals(listOf(active), links.map { it.transactionId })
        // 标签本身保留。
        assertEquals(1, db.tagDao().getByLedger(f.ledgerId).size)
    }

    @Test
    fun emptyRecycleBinIsScopedToTheLedger() = runTest {
        val f = fixture()
        val other = ledgerRepo.createLedger("第二账本")
        // 账户全局共享：第二账本直接使用同一个现金账户
        val otherAccount = f.accountId
        val otherCategory = db.categoryDao().getByLedgerAndType(other.id, CategoryType.EXPENSE).first().id
        val otherTx = txRepo.add(
            TransactionDraft(
                other.id, TransactionType.EXPENSE, 100, "CNY", otherAccount,
                categoryId = otherCategory, transactionTime = TimeUtils.now(),
            ),
        ).getOrThrow().id
        val mine = listOf(addExpense(f), addExpense(f))
        val active = addExpense(f)
        txRepo.softDeleteAll(mine + otherTx).getOrThrow()

        assertEquals(2, txRepo.emptyRecycleBin(f.ledgerId).getOrThrow())

        assertTrue(txRepo.observeRecycleBin(f.ledgerId).first().isEmpty())
        assertEquals(listOf(otherTx), txRepo.observeRecycleBin(other.id).first().map { it.id })
        assertEquals(listOf(active), txRepo.observeByLedger(f.ledgerId).first().map { it.id })
        assertEquals(0, txRepo.emptyRecycleBin(f.ledgerId).getOrThrow())
    }

    @Test
    fun purgingGeneratedRecurringChargeDropsOccurrenceWithoutRegenerating() = runTest {
        val f = fixture()
        val account = accountRepo.addAccount(
            name = "订阅卡",
            type = AccountType.CASH,
            currency = "CNY",
            initialBalance = 0,
            initialBalanceTime = 0,
        ).getOrThrow()
        val zone = ZoneId.of("UTC")
        val charge = recurringRepo.save(
            null,
            RecurringChargeDraft(
                ledgerId = f.ledgerId,
                name = "测试订阅",
                amount = 990,
                accountId = account.id,
                categoryId = f.categoryId,
                frequency = RecurrenceFrequency.MONTHLY,
                startDate = LocalDate.of(2020, 1, 31),
                minuteOfDay = 9 * 60,
                zoneId = zone,
            ),
        ).getOrThrow()
        val firstDue = charge.nextDueAt
        recurringRepo.processDue(firstDue)
        val generated = db.transactionDao().getAll().single()
        assertEquals(1, db.recurringChargeDao().getAllOccurrences().size)

        txRepo.softDeleteAll(listOf(generated.id)).getOrThrow()
        assertEquals(1, txRepo.purgeAll(listOf(generated.id)).getOrThrow())

        // 执行记录随交易一起清理，备份中不会留下指向不存在交易的记录。
        assertTrue(db.recurringChargeDao().getAllOccurrences().isEmpty())
        // 规则已越过该周期：再次处理同一时刻不会重新生成。
        assertEquals(0, recurringRepo.processDue(firstDue).processed)
        assertTrue(db.transactionDao().getAll().isEmpty())

        // 下一周期照常生成，且只生成下一周期这一笔。
        val secondDue = db.recurringChargeDao().getById(charge.id)!!.nextDueAt
        assertTrue(secondDue > firstDue)
        assertEquals(1, recurringRepo.processDue(secondDue).processed)
        val regenerated = db.transactionDao().getAll().single()
        assertEquals(secondDue, regenerated.transactionTime)
        assertEquals(listOf(secondDue), db.recurringChargeDao().getAllOccurrences().map { it.dueAt })
    }
}

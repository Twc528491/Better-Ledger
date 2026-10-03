package com.personal.ledger.data

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.personal.ledger.R
import com.personal.ledger.data.backup.BackupRepository
import com.personal.ledger.data.local.database.AppDatabase
import com.personal.ledger.data.repository.AccountMergeRepository
import com.personal.ledger.data.repository.AccountRepository
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
import com.personal.ledger.util.UserFacingException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

/**
 * v0.6 账户跨账本共享（仪器测试）：跨账本余额、币种范围、合并账户、旧版 JSON 备份恢复。
 */
@RunWith(AndroidJUnit4::class)
class SharedAccountsTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var settings: SettingsRepository
    private lateinit var ledgerRepo: LedgerRepository
    private lateinit var accountRepo: AccountRepository
    private lateinit var txRepo: TransactionRepository
    private lateinit var tagRepo: TagRepository
    private lateinit var recurringRepo: RecurringChargeRepository
    private lateinit var mergeRepo: AccountMergeRepository

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        settings = SettingsRepository(context)
        ledgerRepo = LedgerRepository(db.ledgerDao(), db.accountDao(), db.categoryDao(), settings)
        accountRepo = AccountRepository(db.accountDao(), db.transactionDao())
        txRepo = TransactionRepository(db)
        tagRepo = TagRepository(db.tagDao())
        recurringRepo = RecurringChargeRepository(db, txRepo)
        mergeRepo = AccountMergeRepository(db, settings)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private data class Fixture(val main: String, val travel: String, val cash: String)

    private suspend fun fixture(): Fixture {
        ledgerRepo.ensureDefaultLedger()
        val main = db.ledgerDao().getAll().first().id
        val travel = ledgerRepo.createLedger("旅行").id
        return Fixture(main, travel, db.accountDao().getAll().single().id)
    }

    private suspend fun category(ledgerId: String, type: CategoryType = CategoryType.EXPENSE) =
        db.categoryDao().getByLedgerAndType(ledgerId, type).first().id

    private suspend fun expense(ledgerId: String, accountId: String, amount: Long, time: Long = TimeUtils.now()) =
        txRepo.add(
            TransactionDraft(
                ledgerId, TransactionType.EXPENSE, amount, "CNY", accountId,
                categoryId = category(ledgerId), transactionTime = time,
            ),
        ).getOrThrow().id

    private suspend fun transfer(ledgerId: String, from: String, to: String, amount: Long) =
        txRepo.add(
            TransactionDraft(
                ledgerId, TransactionType.TRANSFER, amount, "CNY", from,
                targetAccountId = to, transactionTime = TimeUtils.now(),
            ),
        ).getOrThrow().id

    private suspend fun balanceOf(accountId: String) =
        accountRepo.observeWithBalances().first().first { it.account.id == accountId }.balance

    @Test
    fun accountBalanceIncludesTransactionsFromAllLedgers() = runTest {
        val f = fixture()
        // 新账本不再自建现金账户
        assertEquals(1, db.accountDao().getAll().size)
        val bank = accountRepo.addAccount("银行卡", AccountType.BANK, "CNY", 10_000, 0).getOrThrow()

        val mainExpense = expense(f.main, f.cash, 1_000)
        expense(f.travel, f.cash, 2_000)
        transfer(f.travel, bank.id, f.cash, 5_000)
        txRepo.add(
            TransactionDraft(
                f.main, TransactionType.INCOME, 700, "CNY", bank.id,
                categoryId = category(f.main, CategoryType.INCOME), transactionTime = TimeUtils.now(),
            ),
        ).getOrThrow()
        txRepo.softDelete(mainExpense).getOrThrow() // 回收站不计入余额

        assertEquals(-2_000L + 5_000L, balanceOf(f.cash))
        assertEquals(10_000L - 5_000L + 700L, balanceOf(bank.id))
        // 账户详情列出全部账本的正常账单
        val cashTxs = accountRepo.observeAccountTransactions(f.cash).first()
        assertEquals(setOf(f.travel), cashTxs.map { it.ledgerId }.toSet())
        assertEquals(2, cashTxs.size)
        // 各账本统计仍只计本账本收支，转账不计入
        val start = TimeUtils.monthStartMs(java.time.YearMonth.now())
        val end = TimeUtils.monthEndMs(java.time.YearMonth.now())
        assertEquals(0L to 2_000L, txRepo.incomeExpenseBetween(f.travel, start, end))
        assertEquals(700L to 0L, txRepo.incomeExpenseBetween(f.main, start, end))
    }

    @Test
    fun ledgersOnlyUseAccountsOfTheirCurrency() = runTest {
        val f = fixture()
        val usdLedger = ledgerRepo.createLedger("美元", "USD")
        // 没有 USD 账户时新账本仍自建一个现金账户
        val usdCash = db.accountDao().getByCurrency("USD").single()
        assertEquals(2, db.accountDao().getAll().size)

        val wrongSource = txRepo.add(
            TransactionDraft(
                f.main, TransactionType.EXPENSE, 100, "CNY", usdCash.id,
                categoryId = category(f.main), transactionTime = TimeUtils.now(),
            ),
        )
        assertEquals(R.string.err_account_currency_mismatch, (wrongSource.exceptionOrNull() as UserFacingException).messageRes)
        val wrongTarget = txRepo.add(
            TransactionDraft(
                f.main, TransactionType.TRANSFER, 100, "CNY", f.cash,
                targetAccountId = usdCash.id, transactionTime = TimeUtils.now(),
            ),
        )
        assertEquals(
            R.string.err_target_account_currency_mismatch,
            (wrongTarget.exceptionOrNull() as UserFacingException).messageRes,
        )
        val usdOk = txRepo.add(
            TransactionDraft(
                usdLedger.id, TransactionType.EXPENSE, 100, "USD", usdCash.id,
                categoryId = category(usdLedger.id), transactionTime = TimeUtils.now(),
            ),
        )
        assertTrue(usdOk.isSuccess)
        assertTrue(db.transactionDao().getAll().none { it.ledgerId == f.main })
    }

    @Test
    fun initialTimeIsCheckedAgainstTransactionsInEveryLedger() = runTest {
        val f = fixture()
        val card = accountRepo.addAccount("卡", AccountType.BANK, "CNY", 0, 1_000).getOrThrow()
        expense(f.travel, card.id, 100, time = 2_000)
        assertTrue(accountRepo.updateAccount(card, card.name, card.type, 0, 2_001).isFailure)
        assertTrue(accountRepo.updateAccount(card, card.name, card.type, 0, 1_500).isSuccess)
    }

    @Test
    fun recurringChargeMayUseSharedAccountAndArchiveIsGlobal() = runTest {
        val f = fixture()
        val card = accountRepo.addAccount("卡", AccountType.BANK, "CNY", 0, 0).getOrThrow()
        val draft = RecurringChargeDraft(
            ledgerId = f.travel,
            name = "会员",
            amount = 990,
            accountId = card.id,
            categoryId = category(f.travel),
            frequency = RecurrenceFrequency.MONTHLY,
            startDate = LocalDate.of(2020, 1, 31),
            minuteOfDay = 9 * 60,
            zoneId = ZoneId.of("UTC"),
        )
        val charge = recurringRepo.save(null, draft).getOrThrow()
        recurringRepo.processDue(charge.nextDueAt)
        assertEquals(listOf(card.id), db.transactionDao().getAll().map { it.accountId })

        accountRepo.setArchived(card.id, true).getOrThrow()
        val other = recurringRepo.save(null, draft.copy(ledgerId = f.main, categoryId = category(f.main)))
        assertEquals(R.string.err_account_archived_unusable, (other.exceptionOrNull() as UserFacingException).messageRes)
    }

    @Test
    fun mergeMovesEverythingDeletesSelfTransfersAndKeepsTotalBalance() = runTest {
        val f = fixture()
        val card = accountRepo.addAccount("卡", AccountType.BANK, "CNY", 3_000, 500).getOrThrow()
        val tag = tagRepo.addTag(f.travel, "互转").getOrThrow()
        expense(f.main, card.id, 100)
        val recycled = expense(f.travel, card.id, 200)
        txRepo.softDelete(recycled).getOrThrow()
        val other = accountRepo.addAccount("其他", AccountType.BANK, "CNY", 0, 0).getOrThrow()
        transfer(f.travel, other.id, card.id, 400)
        val selfA = transfer(f.main, card.id, f.cash, 1_000)
        val selfB = txRepo.add(
            TransactionDraft(
                f.travel, TransactionType.TRANSFER, 300, "CNY", f.cash,
                targetAccountId = card.id, tagIds = listOf(tag.id), transactionTime = TimeUtils.now(),
            ),
        ).getOrThrow().id
        txRepo.softDelete(selfB).getOrThrow()
        val charge = recurringRepo.save(
            null,
            RecurringChargeDraft(
                ledgerId = f.travel, name = "会员", amount = 990, accountId = card.id,
                categoryId = category(f.travel), frequency = RecurrenceFrequency.MONTHLY,
                startDate = LocalDate.now().plusMonths(1), minuteOfDay = 9 * 60, zoneId = ZoneId.systemDefault(),
            ),
        ).getOrThrow()
        settings.setLastUsedAccountId(card.id)
        val before = balanceOf(card.id) + balanceOf(f.cash)
        val cashBefore = db.accountDao().getById(f.cash)!!

        val plan = mergeRepo.plan(card.id, f.cash)
        assertEquals(3, plan.movedTransactions) // 支出 2（含回收站）+ 来自“其他”的转账
        assertEquals(2, plan.selfTransfers)
        assertEquals(1, plan.recurringCharges)
        assertEquals(cashBefore.initialBalance + 3_000, plan.mergedInitialBalance)
        assertEquals(minOf(cashBefore.initialBalanceTime, 500L), plan.mergedInitialTime)

        mergeRepo.merge(card.id, f.cash).getOrThrow()

        assertNull(db.accountDao().getById(card.id))
        val all = db.transactionDao().getAll()
        assertTrue(all.none { it.accountId == card.id || it.targetAccountId == card.id })
        assertTrue(all.none { it.id == selfA || it.id == selfB })
        assertTrue(all.first { it.id == recycled }.isDeleted) // 回收站状态保留
        assertEquals(f.cash, all.first { it.id == recycled }.accountId)
        assertTrue(db.transactionTagDao().getAll().isEmpty())
        assertEquals(f.cash, db.recurringChargeDao().getById(charge.id)!!.accountId)
        val merged = db.accountDao().getById(f.cash)!!
        assertEquals(plan.mergedInitialBalance, merged.initialBalance)
        assertEquals(plan.mergedInitialTime, merged.initialBalanceTime)
        assertEquals(before, balanceOf(f.cash))
        assertEquals(f.cash, settings.lastUsedAccountId.first())
    }

    @Test
    fun mergeRejectsDifferentCurrencyArchivedTargetAndSelf() = runTest {
        val f = fixture()
        val usd = accountRepo.addAccount("美元卡", AccountType.BANK, "USD", 0, 0).getOrThrow()
        val archived = accountRepo.addAccount("旧卡", AccountType.BANK, "CNY", 0, 0).getOrThrow()
        accountRepo.setArchived(archived.id, true).getOrThrow()

        fun code(result: Result<*>) = (result.exceptionOrNull() as UserFacingException).messageRes
        assertEquals(R.string.err_merge_currency, code(mergeRepo.merge(f.cash, usd.id)))
        assertEquals(R.string.err_merge_target_archived, code(mergeRepo.merge(f.cash, archived.id)))
        assertEquals(R.string.err_merge_same_account, code(mergeRepo.merge(f.cash, f.cash)))
        // 已归档账户可以并入未归档账户
        assertTrue(mergeRepo.merge(archived.id, f.cash).isSuccess)
        assertEquals(setOf(f.cash, usd.id), db.accountDao().getAll().map { it.id }.toSet())
    }

    @Test
    fun restoringVersionFourJsonMakesAccountsSharedAndRenamesClashes() = runTest {
        val v4 = """
            {"backupFormatVersion":4,"exportedAt":1,"appVersion":"0.5.0","data":{
              "ledgers":[
                {"id":"l1","name":"个人账本","baseCurrency":"CNY","createdAt":1,"updatedAt":1,"isArchived":false},
                {"id":"l2","name":"旅行","baseCurrency":"CNY","createdAt":2,"updatedAt":2,"isArchived":false}
              ],
              "accounts":[
                {"id":"a1","ledgerId":"l1","name":"现金","type":"CASH","currency":"CNY","initialBalance":100,
                 "initialBalanceTime":1,"createdAt":1,"updatedAt":1,"isArchived":false},
                {"id":"a2","ledgerId":"l2","name":"现金","type":"CASH","currency":"CNY","initialBalance":0,
                 "initialBalanceTime":1,"createdAt":2,"updatedAt":2,"isArchived":false},
                {"id":"a3","ledgerId":"l2","name":"银行卡","type":"BANK","currency":"CNY","initialBalance":0,
                 "initialBalanceTime":1,"createdAt":3,"updatedAt":3,"isArchived":true}
              ],
              "categories":[
                {"id":"c2","ledgerId":"l2","name":"餐饮","type":"EXPENSE","parentId":null,"sortOrder":0,"isArchived":false}
              ],
              "transactions":[
                {"id":"t1","ledgerId":"l2","type":"EXPENSE","amount":50,"currency":"CNY","accountId":"a2",
                 "categoryId":"c2","transactionTime":10,"createdAt":10,"updatedAt":10,"isDeleted":false}
              ]
            }}
        """.trimIndent()
        val backup = BackupRepository(context, db, settings)
        backup.restoreJson(v4.toByteArray()).getOrThrow()

        val names = db.accountDao().getAll().associate { it.id to it.name }
        assertEquals(mapOf("a1" to "现金（个人账本）", "a2" to "现金（旅行）", "a3" to "银行卡"), names)
        assertEquals(-50L, balanceOf("a2"))
        assertEquals(100L, balanceOf("a1"))
        // 恢复后的账户可在任一账本使用
        val tx = txRepo.add(
            TransactionDraft(
                "l2", TransactionType.TRANSFER, 10, "CNY", "a2", targetAccountId = "a1", transactionTime = 20,
            ),
        )
        assertTrue(tx.isSuccess)

        // v5 往返：不再改名
        val rebuilt = backup.buildBackupJson()
        assertTrue(!rebuilt.toString(Charsets.UTF_8).contains("\"ledgerId\": \"l1\"")) // 账户不再写出归属
        backup.restoreJson(rebuilt).getOrThrow()
        assertEquals(names, db.accountDao().getAll().associate { it.id to it.name })
        assertNotNull(db.accountDao().getById("a3"))
    }
}

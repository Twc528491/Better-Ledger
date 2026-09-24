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
import com.personal.ledger.data.repository.TransactionRepository
import com.personal.ledger.data.repository.TransactionDraft
import com.personal.ledger.data.settings.SettingsRepository
import com.personal.ledger.domain.model.AccountType
import com.personal.ledger.domain.model.CategoryType
import com.personal.ledger.domain.model.RecurrenceFrequency
import com.personal.ledger.domain.model.TransactionType
import com.personal.ledger.util.TimeUtils
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class RecurringChargeRepositoryTest {
    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var ledgerRepository: LedgerRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var recurringRepository: RecurringChargeRepository

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val settings = SettingsRepository(context)
        ledgerRepository = LedgerRepository(db.ledgerDao(), db.accountDao(), db.categoryDao(), settings)
        accountRepository = AccountRepository(db.accountDao(), db.transactionDao())
        categoryRepository = CategoryRepository(db)
        transactionRepository = TransactionRepository(db)
        recurringRepository = RecurringChargeRepository(db, transactionRepository)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun dueChargeAndOccurrenceMarkerCommitOnceAndManualDeleteDoesNotRegenerate() = runTest {
        val fixture = createFixture()

        val firstRun = recurringRepository.processDue(fixture.charge.nextDueAt)
        val generated = db.transactionDao().getAll().single()
        assertEquals(1, firstRun.processed)
        assertEquals(TransactionType.EXPENSE, generated.type)
        assertEquals(1, db.recurringChargeDao().getAllOccurrences().size)

        transactionRepository.softDelete(generated.id).getOrThrow()
        // Simulate a retry after rule-position persistence lagged behind an already committed marker.
        val stale = db.recurringChargeDao().getById(fixture.charge.id)!!
        db.recurringChargeDao().update(
            stale.copy(
                nextOccurrenceIndex = 0,
                nextDueAt = fixture.charge.nextDueAt,
                lastError = null,
                updatedAt = TimeUtils.now(),
            ),
        )
        val retry = recurringRepository.processDue(fixture.charge.nextDueAt)

        assertEquals(1, retry.processed)
        assertEquals(1, db.transactionDao().getAll().size)
        assertTrue(db.transactionDao().getAll().single().isDeleted)
        assertEquals(1, db.recurringChargeDao().getAllOccurrences().size)
        assertEquals(1L, db.recurringChargeDao().getById(fixture.charge.id)!!.nextOccurrenceIndex)
    }

    @Test
    fun archivedCategoryPersistsVisibleErrorAndCanRetryAfterRestore() = runTest {
        val fixture = createFixture()
        categoryRepository.setArchived(fixture.categoryId, true).getOrThrow()

        recurringRepository.processDue(fixture.charge.nextDueAt)

        val failedRule = db.recurringChargeDao().getById(fixture.charge.id)!!
        assertNotNull(failedRule.lastError)
        assertTrue(db.transactionDao().getAll().isEmpty())

        categoryRepository.setArchived(fixture.categoryId, false).getOrThrow()
        recurringRepository.retry(fixture.charge.id).getOrThrow()
        val retried = recurringRepository.processDue(fixture.charge.nextDueAt)

        assertEquals(1, retried.processed)
        assertEquals(1, db.transactionDao().getAll().size)
        assertEquals(null, db.recurringChargeDao().getById(fixture.charge.id)!!.lastError)
    }

    @Test
    fun pausedChargeDoesNotCatchUpAndResumeMovesScheduleToCurrentTimeOrLater() = runTest {
        val fixture = createFixture()
        recurringRepository.setPaused(fixture.charge.id, true).getOrThrow()

        val pausedRun = recurringRepository.processDue(fixture.charge.nextDueAt)
        assertEquals(0, pausedRun.processed)
        assertTrue(db.transactionDao().getAll().isEmpty())

        val beforeResume = TimeUtils.now()
        recurringRepository.setPaused(fixture.charge.id, false).getOrThrow()
        val resumed = db.recurringChargeDao().getById(fixture.charge.id)!!
        assertFalse(resumed.isPaused)
        assertTrue(resumed.nextDueAt >= beforeResume)

        recurringRepository.processDue(fixture.charge.nextDueAt)
        assertTrue(db.transactionDao().getAll().isEmpty())
    }

    private suspend fun createFixture(): Fixture {
        ledgerRepository.ensureDefaultLedger()
        val ledger = db.ledgerDao().getAll().single()
        val categoryId = db.categoryDao().getByLedgerAndType(ledger.id, CategoryType.EXPENSE).first().id
        val account = accountRepository.addAccount(
            ledgerId = ledger.id,
            name = "测试账户",
            type = AccountType.CASH,
            currency = ledger.baseCurrency,
            initialBalance = 0,
            initialBalanceTime = 0,
        ).getOrThrow()
        val charge = recurringRepository.save(
            null,
            RecurringChargeDraft(
                ledgerId = ledger.id,
                name = "测试订阅",
                amount = 990,
                accountId = account.id,
                categoryId = categoryId,
                frequency = RecurrenceFrequency.MONTHLY,
                startDate = LocalDate.of(2020, 1, 31),
                minuteOfDay = 9 * 60,
                zoneId = ZoneId.of("UTC"),
            ),
        ).getOrThrow()
        return Fixture(charge, categoryId)
    }

    private data class Fixture(
        val charge: com.personal.ledger.domain.model.RecurringCharge,
        val categoryId: String,
    )
}

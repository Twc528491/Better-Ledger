package com.personal.ledger.data

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.personal.ledger.data.local.database.AppDatabase
import com.personal.ledger.data.repository.AccountRepository
import com.personal.ledger.data.repository.BudgetRepository
import com.personal.ledger.data.repository.CategoryRepository
import com.personal.ledger.data.repository.LedgerRepository
import com.personal.ledger.data.repository.MerchantRepository
import com.personal.ledger.data.repository.TagRepository
import com.personal.ledger.data.repository.TransactionDraft
import com.personal.ledger.data.repository.TransactionRepository
import com.personal.ledger.data.settings.SettingsRepository
import com.personal.ledger.domain.model.AccountType
import com.personal.ledger.domain.model.CategoryType
import com.personal.ledger.domain.model.TransactionType
import com.personal.ledger.util.TimeUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.YearMonth

/**
 * 数据层端到端测试（仪器测试，在真机 / 模拟器上运行 Room + Repository）。
 * 覆盖 MVP.md 完成标准中的核心逻辑：余额、收支、预算、转账、归档、软删除。
 */
@RunWith(AndroidJUnit4::class)
class DataLayerTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var settings: SettingsRepository
    private lateinit var ledgerRepo: LedgerRepository
    private lateinit var accountRepo: AccountRepository
    private lateinit var categoryRepo: CategoryRepository
    private lateinit var txRepo: TransactionRepository
    private lateinit var budgetRepo: BudgetRepository
    private lateinit var merchantRepo: MerchantRepository
    private lateinit var tagRepo: TagRepository

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        settings = SettingsRepository(context)
        ledgerRepo = LedgerRepository(db.ledgerDao(), db.accountDao(), db.categoryDao(), settings)
        accountRepo = AccountRepository(db.accountDao(), db.transactionDao())
        categoryRepo = CategoryRepository(db)
        txRepo = TransactionRepository(db)
        budgetRepo = BudgetRepository(db, categoryRepo)
        merchantRepo = MerchantRepository(db.merchantDao())
        tagRepo = TagRepository(db.tagDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun seededLedgerId(): String {
        ledgerRepo.ensureDefaultLedger()
        return db.ledgerDao().getAll().first().id
    }

    @Test
    fun `首次启动创建默认账本与种子数据`() = runTest {
        ledgerRepo.ensureDefaultLedger()
        val ledgers = db.ledgerDao().getAll()
        assertEquals(1, ledgers.size)
        assertEquals("个人账本", ledgers[0].name)
        assertEquals("CNY", ledgers[0].baseCurrency)
        // 默认分类：7 支出 + 2 收入
        assertEquals(9, db.categoryDao().getAll().size)
        assertEquals(7, db.categoryDao().getByLedgerAndType(ledgers[0].id, CategoryType.EXPENSE).size)
        assertEquals(2, db.categoryDao().getByLedgerAndType(ledgers[0].id, CategoryType.INCOME).size)
        // 种子现金账户
        assertEquals(1, db.accountDao().getAll().size)
        assertEquals(ledgers[0].id, settings.currentLedgerId.first())
        // 幂等
        ledgerRepo.ensureDefaultLedger()
        assertEquals(1, db.ledgerDao().getAll().size)
    }

    @Test
    fun `支出收入与转账的余额计算`() = runTest {
        val ledgerId = seededLedgerId()
        val cashId = db.accountDao().getAll().first().id
        accountRepo.addAccount(ledgerId, "银行卡", AccountType.BANK, "CNY", 0L, TimeUtils.now())
        val bankId = db.accountDao().getAll().first { it.name == "银行卡" }.id
        val foodId = db.categoryDao().getByLedgerAndType(ledgerId, CategoryType.EXPENSE).first().id
        val salaryId = db.categoryDao().getByLedgerAndType(ledgerId, CategoryType.INCOME).first().id

        // 支出 25.00、收入 3000.00
        txRepo.add(TransactionDraft(ledgerId, TransactionType.EXPENSE, 2500, "CNY", cashId, categoryId = foodId, transactionTime = TimeUtils.now()))
        txRepo.add(TransactionDraft(ledgerId, TransactionType.INCOME, 300000, "CNY", cashId, categoryId = salaryId, transactionTime = TimeUtils.now()))

        val balances = accountRepo.observeWithBalances(ledgerId).first()
        assertEquals(297500L, balances.first { it.account.id == cashId }.balance)

        // 转账 100.00 现金 → 银行卡：不影响收支、余额方向正确
        txRepo.add(TransactionDraft(ledgerId, TransactionType.TRANSFER, 10000, "CNY", cashId, targetAccountId = bankId, transactionTime = TimeUtils.now()))
        val balances2 = accountRepo.observeWithBalances(ledgerId).first()
        assertEquals(287500L, balances2.first { it.account.id == cashId }.balance)
        assertEquals(10000L, balances2.first { it.account.id == bankId }.balance)

        // 收支合计不受转账影响
        val start = TimeUtils.monthStartMs(YearMonth.now())
        val end = TimeUtils.monthEndMs(YearMonth.now())
        val (income, expense) = txRepo.incomeExpenseBetween(ledgerId, start, end)
        assertEquals(300000L, income)
        assertEquals(2500L, expense)
    }

    @Test
    fun `交易约束全部生效`() = runTest {
        val ledgerId = seededLedgerId()
        val cashId = db.accountDao().getAll().first().id
        val foodId = db.categoryDao().getByLedgerAndType(ledgerId, CategoryType.EXPENSE).first().id
        val salaryId = db.categoryDao().getByLedgerAndType(ledgerId, CategoryType.INCOME).first().id
        val now = TimeUtils.now()

        fun msgOf(result: Result<*>) = result.exceptionOrNull()?.message

        // 金额必须为正
        assertEquals("金额必须大于 0", msgOf(txRepo.add(TransactionDraft(ledgerId, TransactionType.EXPENSE, 0, "CNY", cashId, categoryId = foodId, transactionTime = now))))
        // 支出不能关联收入分类
        assertEquals("分类类型与交易类型不一致", msgOf(txRepo.add(TransactionDraft(ledgerId, TransactionType.EXPENSE, 100, "CNY", cashId, categoryId = salaryId, transactionTime = now))))
        // 支出必须填分类
        assertEquals("请选择分类", msgOf(txRepo.add(TransactionDraft(ledgerId, TransactionType.EXPENSE, 100, "CNY", cashId, transactionTime = now))))
        // 转账必须有转入账户且不能相同
        assertEquals("请选择转入账户", msgOf(txRepo.add(TransactionDraft(ledgerId, TransactionType.TRANSFER, 100, "CNY", cashId, transactionTime = now))))
        assertEquals("转出与转入账户不能相同", msgOf(txRepo.add(TransactionDraft(ledgerId, TransactionType.TRANSFER, 100, "CNY", cashId, targetAccountId = cashId, transactionTime = now))))
        // 转账不设置分类
        assertEquals("转账不设置分类", msgOf(txRepo.add(TransactionDraft(ledgerId, TransactionType.TRANSFER, 100, "CNY", cashId, targetAccountId = "other", categoryId = foodId, transactionTime = now))))
        // 时间不能早于账户初始时间
        val early = now - 1000
        assertEquals("时间不能早于账户初始时间", msgOf(txRepo.add(TransactionDraft(ledgerId, TransactionType.EXPENSE, 100, "CNY", cashId, categoryId = foodId, transactionTime = early))))
        // 币种必须与账本一致
        assertEquals("币种必须与账本一致", msgOf(txRepo.add(TransactionDraft(ledgerId, TransactionType.EXPENSE, 100, "USD", cashId, categoryId = foodId, transactionTime = now))))
    }

    @Test
    fun `交易关联实体必须属于同一账本且失败不留半条交易`() = runTest {
        val ledgerId = seededLedgerId()
        val otherLedger = ledgerRepo.createLedger("其他账本")
        val cashId = db.accountDao().getByLedger(ledgerId).first().id
        val foodId = db.categoryDao().getByLedgerAndType(ledgerId, CategoryType.EXPENSE).first().id
        val otherTag = tagRepo.addTag(otherLedger.id, "其他账本标签").getOrThrow()
        val otherMerchant = merchantRepo.addMerchant(otherLedger.id, "其他账本商家").getOrThrow()

        val invalidTag = txRepo.add(
            TransactionDraft(
                ledgerId,
                TransactionType.EXPENSE,
                100,
                "CNY",
                cashId,
                categoryId = foodId,
                tagIds = listOf(otherTag.id),
                transactionTime = TimeUtils.now(),
            )
        )
        assertTrue(invalidTag.isFailure)
        assertEquals(0, db.transactionDao().getAll().size)

        val invalidMerchant = txRepo.add(
            TransactionDraft(
                ledgerId,
                TransactionType.EXPENSE,
                100,
                "CNY",
                cashId,
                categoryId = foodId,
                merchantId = otherMerchant.id,
                transactionTime = TimeUtils.now(),
            )
        )
        assertTrue(invalidMerchant.isFailure)
        assertEquals(0, db.transactionDao().getAll().size)
    }

    @Test
    fun `转入账户的初始时间不能越过已有转账`() = runTest {
        val ledgerId = seededLedgerId()
        val cash = accountRepo.observeByLedger(ledgerId).first().first()
        val baseTime = cash.initialBalanceTime
        val bank = accountRepo.addAccount(
            ledgerId,
            "银行卡",
            AccountType.BANK,
            "CNY",
            0L,
            baseTime,
        ).getOrThrow()
        val transferTime = baseTime + 1_000
        txRepo.add(
            TransactionDraft(
                ledgerId,
                TransactionType.TRANSFER,
                100,
                "CNY",
                cash.id,
                targetAccountId = bank.id,
                transactionTime = transferTime,
            )
        ).getOrThrow()

        val result = accountRepo.updateAccount(
            bank,
            bank.name,
            bank.type,
            bank.initialBalance,
            transferTime + 1,
        )
        assertTrue(result.isFailure)
    }

    @Test
    fun `软删除进回收站并可恢复`() = runTest {
        val ledgerId = seededLedgerId()
        val cashId = db.accountDao().getAll().first().id
        val foodId = db.categoryDao().getByLedgerAndType(ledgerId, CategoryType.EXPENSE).first().id
        val tx = txRepo.add(TransactionDraft(ledgerId, TransactionType.EXPENSE, 500, "CNY", cashId, categoryId = foodId, transactionTime = TimeUtils.now())).getOrThrow()

        txRepo.softDelete(tx.id)
        assertTrue(txRepo.observeByLedger(ledgerId).first().isEmpty())
        assertEquals(1, txRepo.observeRecycleBin(ledgerId).first().size)

        txRepo.restore(tx.id)
        assertEquals(1, txRepo.observeByLedger(ledgerId).first().size)
        assertTrue(txRepo.observeRecycleBin(ledgerId).first().isEmpty())
    }

    @Test
    fun `商家与分类删除仅软归档并保留交易引用`() = runTest {
        val ledgerId = seededLedgerId()
        val cashId = db.accountDao().getAll().first().id
        val category = categoryRepo.getByLedger(ledgerId)
            .first { it.type == CategoryType.EXPENSE && !it.isArchived }
        val merchant = merchantRepo.addMerchant(ledgerId, "测试商家").getOrThrow()
        val tx = txRepo.add(
            TransactionDraft(
                ledgerId,
                TransactionType.EXPENSE,
                100,
                "CNY",
                cashId,
                categoryId = category.id,
                merchantId = merchant.id,
                transactionTime = TimeUtils.now(),
            ),
        ).getOrThrow()

        assertTrue(categoryRepo.delete(category.id).isSuccess)
        assertTrue(merchantRepo.delete(merchant.id).isSuccess)

        assertTrue(db.categoryDao().getById(category.id)!!.isArchived)
        assertTrue(db.merchantDao().getById(merchant.id)!!.isArchived)
        val preserved = txRepo.getById(tx.id)!!
        assertEquals(category.id, preserved.categoryId)
        assertEquals(merchant.id, preserved.merchantId)
        assertEquals(1, txRepo.observeByLedger(ledgerId).first().size)
    }

    @Test
    fun `预算唯一性父分类汇总与类型限制`() = runTest {
        val ledgerId = seededLedgerId()
        val cashId = db.accountDao().getAll().first().id
        val foodId = db.categoryDao().getByLedgerAndType(ledgerId, CategoryType.EXPENSE).first().id
        val salaryId = db.categoryDao().getByLedgerAndType(ledgerId, CategoryType.INCOME).first().id
        val month = TimeUtils.currentMonthKey()
        val now = TimeUtils.now()

        // 总预算
        budgetRepo.setBudget(ledgerId, month, null, 100000, "CNY").getOrThrow()
        // 重复设置总预算 → 更新而不是重复记录
        budgetRepo.setBudget(ledgerId, month, null, 120000, "CNY").getOrThrow()
        assertEquals(1, budgetRepo.getByMonth(ledgerId, month).size)
        // 分类预算只能关联支出分类
        assertTrue(budgetRepo.setBudget(ledgerId, month, salaryId, 1000, "CNY").isFailure)
        // 父分类预算
        val child = categoryRepo.addCategory(ledgerId, "早餐", CategoryType.EXPENSE, foodId).getOrThrow()
        budgetRepo.setBudget(ledgerId, month, foodId, 10000, "CNY").getOrThrow()

        txRepo.add(TransactionDraft(ledgerId, TransactionType.EXPENSE, 2000, "CNY", cashId, categoryId = foodId, transactionTime = now))
        txRepo.add(TransactionDraft(ledgerId, TransactionType.EXPENSE, 500, "CNY", cashId, categoryId = child.id, transactionTime = now))

        val progress = budgetRepo.progressForMonth(ledgerId, YearMonth.parse(month))
        val total = progress.first { it.budget.categoryId == null }
        val parent = progress.first { it.budget.categoryId == foodId }
        assertEquals(2500L, total.spent) // 总预算 = 全部支出
        assertEquals(2500L, parent.spent) // 父分类预算包含子分类
        assertEquals(20, total.permille) // 2500 / 120000 = 20‰（即 2%）
        assertEquals(250, parent.permille) // 2500 / 10000 = 250‰（即 25%）
    }

    @Test
    fun `分类唯一性与重命名不误报自身`() = runTest {
        val ledgerId = seededLedgerId()
        assertTrue(categoryRepo.addCategory(ledgerId, "餐饮", CategoryType.EXPENSE).isSuccess)
        assertTrue(categoryRepo.addCategory(ledgerId, "餐饮", CategoryType.EXPENSE).isFailure) // 同名拒绝
        val child = categoryRepo.addCategory(ledgerId, "外卖", CategoryType.EXPENSE, db.categoryDao().getByLedger(ledgerId).first { it.name == "餐饮" }.id).getOrThrow()
        assertTrue(categoryRepo.addCategory(ledgerId, "外卖", CategoryType.EXPENSE, child.parentId).isFailure) // 同级同名拒绝
        // 重命名为原名（排除自身）应成功
        assertTrue(categoryRepo.rename(child.id, "外卖").isSuccess)
    }

    @Test
    fun `标签唯一而商家允许同名`() = runTest {
        val ledgerId = seededLedgerId()
        assertTrue(tagRepo.addTag(ledgerId, "日常").isSuccess)
        assertTrue(tagRepo.addTag(ledgerId, "日常").isFailure)
        assertTrue(merchantRepo.addMerchant(ledgerId, "永辉").isSuccess)
        assertTrue(merchantRepo.addMerchant(ledgerId, "永辉").isSuccess)
        // 交易带标签
        val cashId = db.accountDao().getAll().first().id
        val foodId = db.categoryDao().getByLedgerAndType(ledgerId, CategoryType.EXPENSE).first().id
        val tagId = db.tagDao().getAll().first().id
        val tx = txRepo.add(
            TransactionDraft(ledgerId, TransactionType.EXPENSE, 100, "CNY", cashId, categoryId = foodId, tagIds = listOf(tagId), transactionTime = TimeUtils.now()),
        ).getOrThrow()
        assertEquals(listOf(tagId), txRepo.getTagIds(tx.id))
    }

    @Test
    fun `归档当前账本自动回退到有效账本`() = runTest {
        ledgerRepo.ensureDefaultLedger()
        val first = db.ledgerDao().getAll().first()
        val second = ledgerRepo.createLedger("第二账本")
        ledgerRepo.setCurrentLedger(first.id)

        ledgerRepo.setLedgerArchived(first.id, true)
        val current = ledgerRepo.currentLedger.first()
        assertNotNull(current)
        assertEquals(second.id, current!!.id)
        // 新账本同样有种子分类与账户
        assertEquals(9, db.categoryDao().getByLedger(second.id).size)
        assertEquals(1, db.accountDao().getByLedger(second.id).size)
    }
}

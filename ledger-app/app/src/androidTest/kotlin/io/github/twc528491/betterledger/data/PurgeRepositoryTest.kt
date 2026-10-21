package io.github.twc528491.betterledger.data

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.twc528491.betterledger.data.local.database.AppDatabase
import io.github.twc528491.betterledger.data.repository.AccountRepository
import io.github.twc528491.betterledger.data.repository.BudgetRepository
import io.github.twc528491.betterledger.data.repository.CategoryRepository
import io.github.twc528491.betterledger.data.repository.LedgerRepository
import io.github.twc528491.betterledger.data.repository.MerchantRepository
import io.github.twc528491.betterledger.data.repository.PurgeRepository
import io.github.twc528491.betterledger.data.repository.RecurringChargeDraft
import io.github.twc528491.betterledger.data.repository.RecurringChargeRepository
import io.github.twc528491.betterledger.data.repository.TagRepository
import io.github.twc528491.betterledger.data.repository.TransactionDraft
import io.github.twc528491.betterledger.data.repository.TransactionRepository
import io.github.twc528491.betterledger.data.settings.SettingsRepository
import io.github.twc528491.betterledger.domain.model.AccountType
import io.github.twc528491.betterledger.domain.model.CategoryType
import io.github.twc528491.betterledger.domain.model.RecurrenceFrequency
import io.github.twc528491.betterledger.domain.model.TransactionType
import io.github.twc528491.betterledger.util.TimeUtils
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

/** 彻底删除（PurgeRepository）的数据层测试：每条路径都只允许删除已归档项目，并保持引用完整。 */
@RunWith(AndroidJUnit4::class)
class PurgeRepositoryTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var ledgerRepo: LedgerRepository
    private lateinit var accountRepo: AccountRepository
    private lateinit var categoryRepo: CategoryRepository
    private lateinit var txRepo: TransactionRepository
    private lateinit var budgetRepo: BudgetRepository
    private lateinit var merchantRepo: MerchantRepository
    private lateinit var tagRepo: TagRepository
    private lateinit var recurringRepo: RecurringChargeRepository
    private lateinit var purgeRepo: PurgeRepository

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
        budgetRepo = BudgetRepository(db, categoryRepo)
        merchantRepo = MerchantRepository(db)
        tagRepo = TagRepository(db.tagDao())
        recurringRepo = RecurringChargeRepository(db, txRepo)
        purgeRepo = PurgeRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun seededLedgerId(): String {
        ledgerRepo.ensureDefaultLedger()
        return db.ledgerDao().getAll().first().id
    }

    /** 账户全局共享：种子“现金”账户只在没有同币种账户时创建，所有 CNY 账本共用。 */
    private suspend fun sharedCash() = db.accountDao().getAll().first().id

    private suspend fun expenseCategory(ledgerId: String, index: Int = 0) =
        db.categoryDao().getByLedgerAndType(ledgerId, CategoryType.EXPENSE)
            .filter { it.parentId == null }[index].id

    private suspend fun addExpense(
        ledgerId: String,
        accountId: String,
        categoryId: String,
        merchantId: String? = null,
        tagIds: List<String> = emptyList(),
    ): String = txRepo.add(
        TransactionDraft(
            ledgerId,
            TransactionType.EXPENSE,
            1000,
            "CNY",
            accountId,
            categoryId = categoryId,
            merchantId = merchantId,
            tagIds = tagIds,
            transactionTime = TimeUtils.now(),
        ),
    ).getOrThrow().id

    @Test
    fun `未归档的项目不能彻底删除`() = runTest {
        val ledgerId = seededLedgerId()
        val tag = tagRepo.addTag(ledgerId, "旅行").getOrThrow()
        val merchant = merchantRepo.addMerchant(ledgerId, "超市").getOrThrow()
        val categoryId = expenseCategory(ledgerId)

        assertTrue(purgeRepo.purgeTag(tag.id).isFailure)
        assertTrue(purgeRepo.purgeMerchant(merchant.id).isFailure)
        assertTrue(purgeRepo.purgeCategory(categoryId, null).isFailure)
        assertTrue(purgeRepo.purgeAccount(sharedCash()).isFailure)
        assertTrue(purgeRepo.purgeLedger(ledgerId).isFailure)
        assertEquals(1, db.tagDao().getAll().size)
        assertEquals(1, db.merchantDao().getAll().size)
        assertEquals(1, db.ledgerDao().getAll().size)
    }

    @Test
    fun `彻底删除标签只移除关联不删除账单`() = runTest {
        val ledgerId = seededLedgerId()
        val cash = sharedCash()
        val food = expenseCategory(ledgerId)
        val tag = tagRepo.addTag(ledgerId, "旅行").getOrThrow()
        val keep = tagRepo.addTag(ledgerId, "日常").getOrThrow()
        val txId = addExpense(ledgerId, cash, food, tagIds = listOf(tag.id, keep.id))
        val deletedTx = addExpense(ledgerId, cash, food, tagIds = listOf(tag.id))
        txRepo.softDelete(deletedTx).getOrThrow()
        tagRepo.setArchived(tag.id, true).getOrThrow()

        assertEquals(2, purgeRepo.tagUsage(tag.id))
        purgeRepo.purgeTag(tag.id).getOrThrow()

        assertNull(db.tagDao().getById(tag.id))
        assertEquals(listOf(keep.id), db.transactionTagDao().getTagIds(txId))
        assertTrue(db.transactionTagDao().getTagIds(deletedTx).isEmpty())
        assertEquals(2, db.transactionDao().getAll().size)
    }

    @Test
    fun `彻底删除商家时账单含回收站改为无商家`() = runTest {
        val ledgerId = seededLedgerId()
        val cash = sharedCash()
        val food = expenseCategory(ledgerId)
        val merchant = merchantRepo.addMerchant(ledgerId, "超市", setOf(food)).getOrThrow()
        val txId = addExpense(ledgerId, cash, food, merchantId = merchant.id)
        val deletedTx = addExpense(ledgerId, cash, food, merchantId = merchant.id)
        txRepo.softDelete(deletedTx).getOrThrow()
        merchantRepo.delete(merchant.id).getOrThrow()

        assertEquals(2, purgeRepo.merchantUsage(merchant.id))
        purgeRepo.purgeMerchant(merchant.id).getOrThrow()

        assertNull(db.merchantDao().getById(merchant.id))
        assertTrue(db.merchantCategoryDao().getByMerchant(merchant.id).isEmpty())
        assertNull(db.transactionDao().getById(txId)!!.merchantId)
        assertNull(db.transactionDao().getById(deletedTx)!!.merchantId)
    }

    @Test
    fun `彻底删除被引用的分类必须迁移到同类型替代分类`() = runTest {
        val ledgerId = seededLedgerId()
        val cash = sharedCash()
        val root = expenseCategory(ledgerId, 0)
        val replacement = expenseCategory(ledgerId, 1)
        val income = db.categoryDao().getByLedgerAndType(ledgerId, CategoryType.INCOME).first().id
        val child = categoryRepo.addCategory(ledgerId, "早餐", CategoryType.EXPENSE, root).getOrThrow().id
        val merchant = merchantRepo.addMerchant(ledgerId, "早餐店", setOf(root)).getOrThrow()
        val txRoot = addExpense(ledgerId, cash, root)
        val txChild = addExpense(ledgerId, cash, child)
        txRepo.softDelete(txChild).getOrThrow()
        val month = TimeUtils.currentMonthKey()
        budgetRepo.setBudget(ledgerId, month, root, 50000, "CNY").getOrThrow()
        val account = accountRepo.addAccount("订阅卡", AccountType.BANK, "CNY", 0, 0).getOrThrow()
        val charge = recurringRepo.save(
            null,
            RecurringChargeDraft(
                ledgerId = ledgerId,
                name = "会员",
                amount = 1500,
                accountId = account.id,
                categoryId = child,
                frequency = RecurrenceFrequency.MONTHLY,
                startDate = LocalDate.now().plusMonths(1),
                minuteOfDay = 9 * 60,
                zoneId = ZoneId.systemDefault(),
            ),
        ).getOrThrow()
        // 删除一级分类会一并归档其二级分类
        categoryRepo.delete(root).getOrThrow()

        val plan = purgeRepo.categoryPlan(root)
        assertFalse(plan.blocked)
        assertTrue(plan.needsReplacement)
        assertEquals(setOf(root, child), plan.removed.map { it.id }.toSet())
        assertEquals(2, plan.transactionCount)
        assertEquals(1, plan.recurringCount)
        assertEquals(1, plan.budgetCount)
        assertTrue(plan.replacementCandidates.none { it.id == root || it.id == child || it.id == income })

        // 缺少替代分类或替代分类类型不一致时拒绝，且不留下任何修改
        assertTrue(purgeRepo.purgeCategory(root, null).isFailure)
        assertTrue(purgeRepo.purgeCategory(root, income).isFailure)
        assertEquals(root, db.transactionDao().getById(txRoot)!!.categoryId)

        purgeRepo.purgeCategory(root, replacement).getOrThrow()

        assertNull(db.categoryDao().getById(root))
        assertNull(db.categoryDao().getById(child))
        assertEquals(replacement, db.transactionDao().getById(txRoot)!!.categoryId)
        assertEquals(replacement, db.transactionDao().getById(txChild)!!.categoryId)
        assertEquals(replacement, db.recurringChargeDao().getById(charge.id)!!.categoryId)
        assertTrue(db.budgetDao().getAll().none { it.categoryId == root })
        assertTrue(db.merchantCategoryDao().getByMerchant(merchant.id).none { it.categoryId == root })
    }

    @Test
    fun `一级分类下仍有使用中的二级分类时禁止删除`() = runTest {
        val ledgerId = seededLedgerId()
        val root = expenseCategory(ledgerId)
        val child = categoryRepo.addCategory(ledgerId, "午餐", CategoryType.EXPENSE, root).getOrThrow().id
        categoryRepo.setArchived(root, true).getOrThrow()

        val plan = purgeRepo.categoryPlan(root)
        assertTrue(plan.blocked)
        assertEquals(listOf(child), plan.activeChildren.map { it.id })
        assertTrue(purgeRepo.purgeCategory(root, null).isFailure)
        assertTrue(db.categoryDao().getById(root) != null)
    }

    @Test
    fun `未被引用的分类无需替代分类即可删除`() = runTest {
        val ledgerId = seededLedgerId()
        val unused = expenseCategory(ledgerId, 2)
        categoryRepo.delete(unused).getOrThrow()

        val plan = purgeRepo.categoryPlan(unused)
        assertFalse(plan.needsReplacement)
        purgeRepo.purgeCategory(unused, null).getOrThrow()
        assertNull(db.categoryDao().getById(unused))
    }

    @Test
    fun `彻底删除账户连同所有账本中的账单转账和定时扣款`() = runTest {
        val ledgerId = seededLedgerId()
        val cash = sharedCash()
        val food = expenseCategory(ledgerId)
        val other = ledgerRepo.createLedger("旅行")
        val card = accountRepo.addAccount("旧卡", AccountType.BANK, "CNY", 0, 0).getOrThrow()
        val tag = tagRepo.addTag(ledgerId, "旧卡消费").getOrThrow()
        addExpense(ledgerId, card.id, food, tagIds = listOf(tag.id))
        val cashExpense = addExpense(ledgerId, cash, food)
        // 现金 → 旧卡 的转账：删除后现金余额会变化
        txRepo.add(
            TransactionDraft(
                ledgerId, TransactionType.TRANSFER, 5000, "CNY", cash,
                targetAccountId = card.id, transactionTime = TimeUtils.now(),
            ),
        ).getOrThrow()
        val charge = recurringRepo.save(
            null,
            RecurringChargeDraft(
                ledgerId = ledgerId,
                name = "旧卡订阅",
                amount = 990,
                accountId = card.id,
                categoryId = food,
                frequency = RecurrenceFrequency.MONTHLY,
                startDate = LocalDate.of(2020, 1, 1),
                minuteOfDay = 9 * 60,
                zoneId = ZoneId.of("UTC"),
            ),
        ).getOrThrow()
        recurringRepo.processDue(charge.nextDueAt)
        // 另一个账本中使用同一账户的账单（含回收站）也会被删除
        val otherTx = addExpense(other.id, card.id, expenseCategory(other.id))
        txRepo.softDelete(otherTx).getOrThrow()
        accountRepo.setArchived(card.id, true).getOrThrow()

        val impact = purgeRepo.accountImpact(card.id)
        assertEquals(1, impact.transferCount)
        assertEquals(1, impact.recurringCount)
        assertEquals(4, impact.transactionCount)
        assertEquals(2, impact.ledgerCount)

        purgeRepo.purgeAccount(card.id).getOrThrow()

        assertNull(db.accountDao().getById(card.id))
        assertTrue(db.transactionDao().getAll().none { it.accountId == card.id || it.targetAccountId == card.id })
        assertEquals(listOf(cashExpense), db.transactionDao().getAll().map { it.id })
        assertNull(db.recurringChargeDao().getById(charge.id))
        assertTrue(db.recurringChargeDao().getAllOccurrences().isEmpty())
        assertTrue(db.transactionTagDao().getAll().isEmpty())
    }

    @Test
    fun `彻底删除归档账本删除其全部数据但保留共享账户`() = runTest {
        val keepId = seededLedgerId()
        val keepFood = expenseCategory(keepId)
        val keepTx = addExpense(keepId, sharedCash(), keepFood)
        val old = ledgerRepo.createLedger("旧账本")
        // 已有 CNY 账户，新账本不再自建现金账户，直接使用共享账户
        assertEquals(1, db.accountDao().getAll().size)
        val oldCash = sharedCash()
        val unused = accountRepo.addAccount("未使用", AccountType.BANK, "CNY", 0, 0).getOrThrow()
        val oldFood = expenseCategory(old.id)
        val oldTag = tagRepo.addTag(old.id, "旧标签").getOrThrow()
        val oldMerchant = merchantRepo.addMerchant(old.id, "旧商家", setOf(oldFood)).getOrThrow()
        addExpense(old.id, oldCash, oldFood, merchantId = oldMerchant.id, tagIds = listOf(oldTag.id))
        budgetRepo.setBudget(old.id, TimeUtils.currentMonthKey(), null, 100000, "CNY").getOrThrow()
        val charge = recurringRepo.save(
            null,
            RecurringChargeDraft(
                ledgerId = old.id,
                name = "旧订阅",
                amount = 990,
                accountId = oldCash,
                categoryId = oldFood,
                frequency = RecurrenceFrequency.MONTHLY,
                startDate = LocalDate.of(2020, 1, 1),
                minuteOfDay = 9 * 60,
                zoneId = ZoneId.of("UTC"),
            ),
        )
        // 现金账户初始时间为创建时刻，早于该时刻的扣款会失败；规则本身仍会保存
        if (charge.isSuccess) recurringRepo.processDue(charge.getOrThrow().nextDueAt)
        ledgerRepo.setLedgerArchived(old.id, true)

        val counts = purgeRepo.ledgerCounts(old.id)
        assertTrue(counts.transactions >= 1)
        assertEquals(1, counts.affectedAccounts) // 只有共享现金账户的余额会变化
        assertEquals(1, counts.tags)
        assertEquals(1, counts.merchants)
        assertEquals(1, counts.budgets)

        purgeRepo.purgeLedger(old.id).getOrThrow()

        assertNull(db.ledgerDao().getById(old.id))
        // 账户全局共享，不随账本删除；余额只剩保留账本中的账单
        assertEquals(setOf(oldCash, unused.id), db.accountDao().getAll().map { it.id }.toSet())
        assertEquals(-1000L, accountRepo.observeWithBalances().first().first { it.account.id == oldCash }.balance)
        assertTrue(db.categoryDao().getAll().none { it.ledgerId == old.id })
        assertTrue(db.transactionDao().getAll().none { it.ledgerId == old.id })
        assertTrue(db.tagDao().getAll().isEmpty())
        assertTrue(db.merchantDao().getAll().isEmpty())
        assertTrue(db.merchantCategoryDao().getAll().isEmpty())
        assertTrue(db.budgetDao().getAll().isEmpty())
        assertTrue(db.recurringChargeDao().getAllCharges().isEmpty())
        assertTrue(db.recurringChargeDao().getAllOccurrences().isEmpty())
        // 其他账本不受影响
        assertEquals(listOf(keepTx), db.transactionDao().getAll().map { it.id })
        assertEquals(listOf(keepId), db.ledgerDao().getAll().map { it.id })
    }

    @Test
    fun `至少保留一个未归档账本`() = runTest {
        val onlyId = seededLedgerId()
        val other = ledgerRepo.createLedger("另一个")
        ledgerRepo.setLedgerArchived(onlyId, true)
        ledgerRepo.setLedgerArchived(other.id, true)

        assertTrue(purgeRepo.purgeLedger(onlyId).isFailure)
        assertEquals(2, db.ledgerDao().getAll().size)
    }
}

package io.github.twc528491.betterledger.domain.calc

import io.github.twc528491.betterledger.R
import io.github.twc528491.betterledger.domain.calc.AccountRules.MergeTx
import io.github.twc528491.betterledger.domain.model.Account
import io.github.twc528491.betterledger.domain.model.AccountType
import io.github.twc528491.betterledger.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountRulesTest {

    private fun account(id: String, currency: String = "CNY", archived: Boolean = false) = Account(
        id = id, name = "名$id", type = AccountType.CASH, currency = currency,
        initialBalance = 0, initialBalanceTime = 0, createdAt = 0, updatedAt = 0, isArchived = archived,
    )

    private val cash = account("cash")
    private val card = account("card")
    private val oldCard = account("old", archived = true)
    private val usd = account("usd", currency = "USD")
    private val all = listOf(cash, card, oldCard, usd)

    @Test
    fun `账本只提供同币种且未归档的账户`() {
        assertEquals(listOf(cash, card), AccountRules.usableIn(all, "CNY"))
        assertEquals(listOf(usd), AccountRules.usableIn(all, "USD"))
        assertTrue(AccountRules.usableIn(all, "JPY").isEmpty())
    }

    @Test
    fun `编辑旧账单时保留其已归档账户但不放开币种`() {
        assertEquals(
            listOf(cash, card, oldCard),
            AccountRules.usableIn(all, "CNY", keepIds = setOf("old", "usd")),
        )
    }

    @Test
    fun `合并目标为同币种未归档的其他账户`() {
        assertEquals(listOf(card), AccountRules.mergeCandidates(cash, all))
        assertEquals(listOf(cash, card), AccountRules.mergeCandidates(oldCard, all))
        assertTrue(AccountRules.mergeCandidates(usd, all).isEmpty())
    }

    @Test
    fun `合并校验`() {
        assertNull(AccountRules.validateMerge(cash, card))
        assertNull(AccountRules.validateMerge(oldCard, card)) // 已归档账户可以并入其他账户
        assertEquals(R.string.err_account_not_found, AccountRules.validateMerge(null, card))
        assertEquals(R.string.err_merge_target_not_found, AccountRules.validateMerge(cash, null))
        assertEquals(R.string.err_merge_same_account, AccountRules.validateMerge(cash, cash))
        assertEquals(R.string.err_merge_currency, AccountRules.validateMerge(cash, usd))
        assertEquals(R.string.err_merge_target_archived, AccountRules.validateMerge(cash, oldCard))
    }

    @Test
    fun `合并计划区分改挂交易与两者之间的互转`() {
        val txs = listOf(
            MergeTx("e1", TransactionType.EXPENSE, "cash", null),
            MergeTx("i1", TransactionType.INCOME, "cash", null),
            MergeTx("t-out", TransactionType.TRANSFER, "cash", "other"),
            MergeTx("t-in", TransactionType.TRANSFER, "other", "cash"),
            MergeTx("self1", TransactionType.TRANSFER, "cash", "card"),
            MergeTx("self2", TransactionType.TRANSFER, "card", "cash"),
            MergeTx("unrelated", TransactionType.TRANSFER, "card", "other"),
            MergeTx("target-only", TransactionType.EXPENSE, "card", null),
        )
        val plan = AccountRules.mergePlan("cash", "card", txs)
        assertEquals(listOf("e1", "i1", "t-out", "t-in"), plan.movedIds)
        assertEquals(listOf("self1", "self2"), plan.selfTransferIds)
    }

    @Test
    fun `合并后初始余额相加且初始时间取较早者`() {
        val (opening, time) = AccountRules.mergedOpening(100, 2_000, -50, 1_000)
        assertEquals(50L, opening)
        assertEquals(1_000L, time)
        assertEquals(500L, AccountRules.mergedOpening(0, 500, 0, 900).second)
    }

    @Test
    fun `删除互转后合并账户余额等于两账户余额之和`() {
        // cash：初始 100，支出 20，转给 card 30 → 50；card：初始 -50，收入 10，收到 30 → -10。
        val cashBalance = BalanceCalculator.compute(100, listOf(-20L, -30L))
        val cardBalance = BalanceCalculator.compute(-50, listOf(10L, 30L))
        val (opening, _) = AccountRules.mergedOpening(100, 0, -50, 0)
        // 合并后：互转删除，剩余支出 20 与收入 10 改挂到 card。
        val merged = BalanceCalculator.compute(opening, listOf(-20L, 10L))
        assertEquals(cashBalance + cardBalance, merged)
    }

    @Test(expected = ArithmeticException::class)
    fun `初始余额相加溢出时报错`() {
        AccountRules.mergedOpening(Long.MAX_VALUE, 0, 1, 0)
    }
}

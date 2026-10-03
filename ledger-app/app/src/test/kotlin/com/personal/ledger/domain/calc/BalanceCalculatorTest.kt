package com.personal.ledger.domain.calc

import com.personal.ledger.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BalanceCalculatorTest {

    @Test
    fun `支出减少余额`() {
        val d = BalanceCalculator.deltaFor(TransactionType.EXPENSE, 2500, "A", null, "A")
        assertEquals(-2500L, d)
    }

    @Test
    fun `收入增加余额`() {
        val d = BalanceCalculator.deltaFor(TransactionType.INCOME, 300000, "A", null, "A")
        assertEquals(300000L, d)
    }

    @Test
    fun `转账转出减少、转入增加`() {
        assertEquals(-10000L, BalanceCalculator.deltaFor(TransactionType.TRANSFER, 10000, "A", "B", "A"))
        assertEquals(10000L, BalanceCalculator.deltaFor(TransactionType.TRANSFER, 10000, "A", "B", "B"))
        assertNull(BalanceCalculator.deltaFor(TransactionType.TRANSFER, 10000, "A", "B", "C"))
    }

    @Test
    fun `无关账户返回 null`() {
        assertNull(BalanceCalculator.deltaFor(TransactionType.EXPENSE, 100, "A", null, "B"))
    }

    @Test
    fun `余额由初始余额与增减量计算`() {
        assertEquals(100L, BalanceCalculator.compute(0, listOf(500L, -400L)))
        assertEquals(500L, BalanceCalculator.compute(500, emptyList()))
    }

    @Test
    fun `负债账户余额可以为负`() {
        assertEquals(-198000L, BalanceCalculator.compute(0, listOf(-198000L)))
    }

    @Test
    fun `转账不影响收入支出合计（转账不计入）`() {
        // 转账类型的 delta 只影响账户，不进入收入/支出聚合（聚合在 Repository 层按类型过滤）。
        // 这里验证转账 delta 与账户侧一致：转出 -，转入 +。
        val out = BalanceCalculator.deltaFor(TransactionType.TRANSFER, 500, "A", "B", "A")!!
        val inSide = BalanceCalculator.deltaFor(TransactionType.TRANSFER, 500, "A", "B", "B")!!
        assertEquals(0L, out + inSide)
    }

    @Test
    fun `按类型聚合的合计折算方向与逐笔一致`() {
        assertEquals(300L, BalanceCalculator.deltaForTotal(TransactionType.INCOME, incoming = false, total = 300))
        assertEquals(-200L, BalanceCalculator.deltaForTotal(TransactionType.EXPENSE, incoming = false, total = 200))
        assertEquals(-50L, BalanceCalculator.deltaForTotal(TransactionType.TRANSFER, incoming = false, total = 50))
        assertEquals(50L, BalanceCalculator.deltaForTotal(TransactionType.TRANSFER, incoming = true, total = 50))
        // 只有转账有转入方向；其他类型的转入合计不计入
        assertEquals(0L, BalanceCalculator.deltaForTotal(TransactionType.INCOME, incoming = true, total = 50))
    }
}

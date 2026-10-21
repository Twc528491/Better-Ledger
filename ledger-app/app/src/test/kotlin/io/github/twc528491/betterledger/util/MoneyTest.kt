package io.github.twc528491.betterledger.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {

    @Test
    fun `formatMinor 千分位与两位小数`() {
        assertEquals("25.36", Money.formatMinor(2536))
        assertEquals("0.00", Money.formatMinor(0))
        assertEquals("1,234,567.89", Money.formatMinor(123456789))
        assertEquals("-1,980.00", Money.formatMinor(-198000))
    }

    @Test
    fun `formatSignedMinor 符号规则`() {
        assertEquals("-25.36", Money.formatSignedMinor(-2536))
        assertEquals("25.36", Money.formatSignedMinor(2536))
        assertEquals("+25.36", Money.formatSignedMinor(2536, explicitPlus = true))
        assertEquals("-0.01", Money.formatSignedMinor(-1))
        assertEquals("0.00", Money.formatSignedMinor(0))
        assertEquals("0.00", Money.formatSignedMinor(0, explicitPlus = true))
        assertEquals("¥0.00", Money.formatWithSymbol(0))
    }

    @Test
    fun `formatWithCurrency 使用币种而非固定人民币符号`() {
        assertEquals("¥25.36", Money.formatWithCurrency(2536, "CNY"))
        assertEquals("$25.36", Money.formatWithCurrency(2536, "usd"))
        assertEquals("€25.36", Money.formatWithCurrency(2536, "EUR"))
        assertEquals("GBP 25.36", Money.formatWithCurrency(2536, "gbp"))
    }

    @Test
    fun `parseToMinor 解析与舍入`() {
        assertEquals(2536L, Money.parseToMinor("25.36"))
        assertEquals(100L, Money.parseToMinor("1"))
        assertEquals(105L, Money.parseToMinor("1.05"))
        assertEquals(0L, Money.parseToMinor("0"))
        assertNull(Money.parseToMinor(""))
        assertNull(Money.parseToMinor("abc"))
        assertNull(Money.parseToMinor("1.234"))
        assertNull(Money.parseToMinor("25.355"))
        assertNull(Money.parseToMinor("-5"))
    }

    @Test
    fun `交易金额不允许负数，账户初始余额允许负数`() {
        assertNull(Money.parseToMinor("-5"))
        assertNull(Money.parseToMinor("-5", allowNegative = false))
        assertEquals(-198000L, Money.parseToMinor("-1980.00", allowNegative = true))
        assertEquals(-5L, Money.parseToMinor("-0.05", allowNegative = true))
    }

    @Test
    fun `parseToMinor 支持千分位逗号`() {
        assertEquals(123456789L, Money.parseToMinor("1,234,567.89"))
    }
}

package com.personal.ledger.ui.common.charts

import org.junit.Assert.assertEquals
import org.junit.Test

class ChartAxisTest {

    @Test
    fun `刻度步长落在 1 2 5 序列上`() {
        assertEquals(500_000L, ChartAxis.niceStep(700_000L, 3))
        assertEquals(100L, ChartAxis.niceStep(250L, 3))
        assertEquals(2_000_000L, ChartAxis.niceStep(5_400_000L, 3))
        assertEquals(1L, ChartAxis.niceStep(1L, 3))
        assertEquals(1L, ChartAxis.niceStep(0L, 3))
    }

    @Test
    fun `刻度从零开始且覆盖最大值`() {
        assertEquals(listOf(0L, 500_000L, 1_000_000L), ChartAxis.ticks(700_000L))
        assertEquals(listOf(0L, 2_000_000L, 4_000_000L, 6_000_000L), ChartAxis.ticks(5_400_000L))
        assertEquals(listOf(0L, 50_000L, 100_000L), ChartAxis.ticks(100_000L))
        assertEquals(listOf(0L), ChartAxis.ticks(0L))
        assertEquals(listOf(0L), ChartAxis.ticks(-500L))
    }

    @Test
    fun `紧凑标签使用万和亿`() {
        assertEquals("0", ChartAxis.compactLabel(0L))
        assertEquals("8000", ChartAxis.compactLabel(800_000L))
        assertEquals("2.5", ChartAxis.compactLabel(250L))
        assertEquals("1万", ChartAxis.compactLabel(1_000_000L))
        assertEquals("1.2万", ChartAxis.compactLabel(1_234_500L))
        assertEquals("12.5万", ChartAxis.compactLabel(12_500_000L))
        assertEquals("3亿", ChartAxis.compactLabel(30_000_000_000L))
        assertEquals("1亿", ChartAxis.compactLabel(9_999_999_900L))
        assertEquals("-1.5万", ChartAxis.compactLabel(-1_500_000L))
    }

    @Test
    fun `读数金额过大时改用紧凑写法`() {
        assertEquals("¥1,234.50", ChartAxis.readoutMoney(123_450L, "CNY"))
        assertEquals("¥999,999.99", ChartAxis.readoutMoney(99_999_999L, "CNY"))
        assertEquals("¥100万", ChartAxis.readoutMoney(100_000_000L, "CNY"))
        assertEquals("-$123.5万", ChartAxis.readoutMoney(-123_456_789L, "USD"))
        assertEquals("-¥25.00", ChartAxis.readoutMoney(-2_500L, "CNY"))
        assertEquals("+¥25.00", ChartAxis.readoutMoney(2_500L, "CNY", explicitPlus = true))
        assertEquals("¥0.00", ChartAxis.readoutMoney(0L, "CNY", explicitPlus = true))
    }
}

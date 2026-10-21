package io.github.twc528491.betterledger.domain.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryChangeCalculatorTest {

    @Test
    fun `按变化幅度绝对值降序并合并两月分类`() {
        val result = CategoryChangeCalculator.compare(
            current = mapOf("food" to 12_000L, "taxi" to 1_000L, "new" to 5_000L),
            previous = mapOf("food" to 9_000L, "taxi" to 8_000L, "gone" to 2_000L),
        )
        assertEquals(listOf("taxi", "new", "food", "gone"), result.map { it.categoryId })
        assertEquals(-7_000L, result[0].delta)
        assertEquals(5_000L, result[1].delta)
        assertEquals(0L, result[3].current)
        assertEquals(2_000L, result[3].previous)
    }

    @Test
    fun `环比百分比四舍五入且上月为零时为空`() {
        val result = CategoryChangeCalculator.compare(
            current = mapOf("a" to 400L, "b" to 0L, "c" to 300L),
            previous = mapOf("a" to 300L, "b" to 300L),
        ).associateBy { it.categoryId }
        assertEquals(33L, result.getValue("a").percent)
        assertEquals(-100L, result.getValue("b").percent)
        assertNull(result.getValue("c").percent)
    }

    @Test
    fun `默认剔除金额不变的分类`() {
        val current = mapOf("same" to 500L, "up" to 600L, "zero" to 0L)
        val previous = mapOf("same" to 500L, "up" to 100L, "zero" to 0L)
        assertEquals(listOf("up"), CategoryChangeCalculator.compare(current, previous).map { it.categoryId })
        val all = CategoryChangeCalculator.compare(current, previous, includeUnchanged = true)
        assertEquals(listOf("up", "same"), all.map { it.categoryId })
    }

    @Test
    fun `幅度相同时本月金额大的在前`() {
        val result = CategoryChangeCalculator.compare(
            current = mapOf("x" to 1_000L, "y" to 3_000L),
            previous = mapOf("x" to 2_000L, "y" to 2_000L),
        )
        assertEquals(listOf("y", "x"), result.map { it.categoryId })
    }

    @Test
    fun `两月都没有数据时返回空列表`() {
        assertTrue(CategoryChangeCalculator.compare(emptyMap(), emptyMap()).isEmpty())
    }
}

package io.github.twc528491.betterledger.domain.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BudgetCalculatorTest {

    @Test
    fun `父分类预算包含所有子分类支出`() {
        val parentMap = mapOf(
            "food" to null,
            "meal" to "food",
            "snack" to "food",
            "transport" to null,
        )
        val expense = mapOf(
            "meal" to 1000L,
            "snack" to 500L,
            "transport" to 300L,
        )
        val rolled = BudgetCalculator.rollUpToParents(parentMap, expense)
        assertEquals(1500L, rolled["food"])
        assertEquals(1000L, rolled["meal"])
        assertEquals(500L, rolled["snack"])
        assertEquals(300L, rolled["transport"])
    }

    @Test
    fun `二级分类下仍正确上卷（两级结构）`() {
        val parentMap = mapOf(
            "a" to null,
            "b" to "a",
        )
        val expense = mapOf("b" to 700L)
        val rolled = BudgetCalculator.rollUpToParents(parentMap, expense)
        assertEquals(700L, rolled["a"])
        assertEquals(700L, rolled["b"])
    }

    @Test
    fun `进度千分比计算与超支`() {
        assertEquals(630, BudgetCalculator.progressPermille(1000, 630))
        assertEquals(1000, BudgetCalculator.progressPermille(1000, 1000))
        assertEquals(1200, BudgetCalculator.progressPermille(1000, 1200))
        assertNull(BudgetCalculator.progressPermille(0, 100))
        assertNull(BudgetCalculator.progressPermille(-1, 100))
    }
}

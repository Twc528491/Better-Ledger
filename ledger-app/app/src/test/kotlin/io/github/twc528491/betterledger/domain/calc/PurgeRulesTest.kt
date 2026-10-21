package io.github.twc528491.betterledger.domain.calc

import io.github.twc528491.betterledger.R
import io.github.twc528491.betterledger.domain.model.Category
import io.github.twc528491.betterledger.domain.model.CategoryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PurgeRulesTest {

    private fun cat(
        id: String,
        parentId: String? = null,
        archived: Boolean = false,
        type: CategoryType = CategoryType.EXPENSE,
        ledgerId: String = "L1",
        sort: Int = 0,
    ) = Category(id, ledgerId, "名$id", type, parentId, sort, archived)

    private val food = cat("food", archived = true, sort = 0)
    private val breakfast = cat("breakfast", parentId = "food", archived = true, sort = 0)
    private val lunch = cat("lunch", parentId = "food", archived = false, sort = 1)
    private val traffic = cat("traffic", sort = 1)
    private val taxi = cat("taxi", parentId = "traffic", sort = 0)
    private val old = cat("old", archived = true, sort = 2)
    private val orphan = cat("orphan", parentId = "old", sort = 0)
    private val salary = cat("salary", type = CategoryType.INCOME)
    private val otherLedger = cat("other", ledgerId = "L2")
    private val all = listOf(food, breakfast, lunch, traffic, taxi, old, orphan, salary, otherLedger)

    @Test
    fun `二级分类只删除自身`() {
        val scope = PurgeRules.categoryScope(breakfast, all)
        assertEquals(listOf("breakfast"), scope.removedIds)
        assertTrue(scope.activeChildIds.isEmpty())
    }

    @Test
    fun `一级分类连同已归档的二级分类一起删除并报告使用中的子分类`() {
        val scope = PurgeRules.categoryScope(food, all)
        assertEquals(listOf("food", "breakfast"), scope.removedIds)
        assertEquals(listOf("lunch"), scope.activeChildIds)
    }

    @Test
    fun `替代分类限定同账本同类型且未归档`() {
        val candidates = PurgeRules.replacementCandidates(food, all, listOf("food", "breakfast")).map { it.id }
        // traffic 及其子分类 taxi；old 已归档，其子分类 orphan 也不能作为替代；lunch 的父分类已归档
        assertEquals(listOf("traffic", "taxi"), candidates)
    }

    @Test
    fun `替代分类校验`() {
        val removed = listOf("food", "breakfast")
        assertNull(PurgeRules.validateReplacement(food, traffic, all, removed))
        assertNull(PurgeRules.validateReplacement(food, taxi, all, removed))
        assertEquals(R.string.err_select_replacement, PurgeRules.validateReplacement(food, null, all, removed))
        assertEquals(R.string.err_replacement_removed, PurgeRules.validateReplacement(food, breakfast, all, removed))
        assertEquals(R.string.err_replacement_type, PurgeRules.validateReplacement(food, salary, all, removed))
        assertEquals(R.string.err_replacement_other_ledger, PurgeRules.validateReplacement(food, otherLedger, all, removed))
        assertEquals(R.string.err_replacement_archived, PurgeRules.validateReplacement(food, old, all, removed))
        assertEquals(R.string.err_replacement_parent_archived, PurgeRules.validateReplacement(food, orphan, all, removed))
    }

    @Test
    fun `二级分类显示父子名称`() {
        assertEquals("名traffic / 名taxi", PurgeRules.displayName(taxi, all))
        assertEquals("名traffic", PurgeRules.displayName(traffic, all))
    }

    @Test
    fun `账本名称确认`() {
        assertTrue(PurgeRules.ledgerNameConfirmed(" 旧账本 ", "旧账本"))
        assertFalse(PurgeRules.ledgerNameConfirmed("旧账", "旧账本"))
        assertFalse(PurgeRules.ledgerNameConfirmed("", ""))
    }
}

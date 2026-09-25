package com.personal.ledger.ui.common

import com.personal.ledger.data.repository.DefaultSeed
import com.personal.ledger.domain.model.CategoryType
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryVisualsTest {

    @Test
    fun seededCategoriesUseDistinctMeaningfulIconKeys() {
        assertEquals(32, DefaultSeed.EXPENSE_CATEGORIES.size)
        assertEquals(17, DefaultSeed.INCOME_CATEGORIES.size)
        val expenseKeys = DefaultSeed.EXPENSE_CATEGORIES.map {
            CategoryVisuals.iconKeyFor(it, CategoryType.EXPENSE)
        }
        val incomeKeys = DefaultSeed.INCOME_CATEGORIES.map {
            CategoryVisuals.iconKeyFor(it, CategoryType.INCOME)
        }
        val allKeys = expenseKeys + incomeKeys

        assertEquals(allKeys.size, allKeys.distinct().size)
    }

    @Test
    fun customNamesHaveStableTypeSpecificFallbacks() {
        assertEquals("food", CategoryVisuals.iconKeyFor("午餐", CategoryType.EXPENSE))
        assertEquals("coffee", CategoryVisuals.iconKeyFor("咖啡豆", CategoryType.EXPENSE))
        assertEquals("subscription", CategoryVisuals.iconKeyFor("视频会员订阅", CategoryType.EXPENSE))
        assertEquals("delivery", CategoryVisuals.iconKeyFor("邮寄包裹", CategoryType.EXPENSE))
        assertEquals("other", CategoryVisuals.iconKeyFor("手作收藏", CategoryType.EXPENSE))
        assertEquals("other", CategoryVisuals.iconKeyFor("手作收藏", CategoryType.EXPENSE))
        assertEquals("other_income", CategoryVisuals.iconKeyFor("手作收藏", CategoryType.INCOME))
    }
}

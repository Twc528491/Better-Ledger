package io.github.twc528491.betterledger.ui.common

import io.github.twc528491.betterledger.data.repository.DefaultSeed
import io.github.twc528491.betterledger.domain.model.CategoryType
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

    /** 读取英文资源中的预置分类名（单元测试工作目录为 app 模块）。 */
    private fun englishSeedNames(): Map<String, String> {
        val xml = java.io.File("src/main/res/values-en/strings_core.xml").readText()
        return Regex("""<string name="seed_cat_([a-z_]+)">([^<]*)</string>""").findAll(xml)
            .associate { it.groupValues[1] to it.groupValues[2].replace("&amp;", "&").replace("\\'", "'") }
    }

    @Test
    fun englishSeededCategoriesMapToSameIconsAsChinese() {
        val english = englishSeedNames()
        (DefaultSeed.EXPENSE_CATEGORIES.map { it to CategoryType.EXPENSE } +
            DefaultSeed.INCOME_CATEGORIES.map { it to CategoryType.INCOME }).forEach { (key, type) ->
            // 资源名 seed_cat_<iconKey> 与中文种子的图标 key 一致
            val iconKey = CategoryVisuals.iconKeyFor(key, type)
            val englishName = english[iconKey] ?: error("缺少英文种子名：seed_cat_$iconKey")
            assertEquals(englishName, iconKey, CategoryVisuals.iconKeyFor(englishName, type))
            assertEquals(englishName, iconKey, CategoryVisuals.iconKeyFor(englishName.uppercase(), type))
        }
    }

    @Test
    fun englishKeywordsPickMeaningfulIcons() {
        val e = CategoryType.EXPENSE
        assertEquals("food", CategoryVisuals.iconKeyFor("Dining out", e))
        assertEquals("food", CategoryVisuals.iconKeyFor("lunch", e))
        assertEquals("fresh", CategoryVisuals.iconKeyFor("Weekly groceries", e))
        assertEquals("taxi", CategoryVisuals.iconKeyFor("Taxi", e))
        assertEquals("fuel", CategoryVisuals.iconKeyFor("Fuel", e))
        assertEquals("transit", CategoryVisuals.iconKeyFor("Public transit", e))
        assertEquals("shopping", CategoryVisuals.iconKeyFor("Online shopping", e))
        assertEquals("home", CategoryVisuals.iconKeyFor("Rent", e))
        assertEquals("movie", CategoryVisuals.iconKeyFor("Entertainment", e))
        assertEquals("medical", CategoryVisuals.iconKeyFor("Health care", e))
        assertEquals("education", CategoryVisuals.iconKeyFor("Tuition", e))
        assertEquals("travel", CategoryVisuals.iconKeyFor("Summer trip", e))
        assertEquals("coffee", CategoryVisuals.iconKeyFor("COFFEE BEANS", e))
        assertEquals("gift", CategoryVisuals.iconKeyFor("Birthday gift", e))
        // 短词干只匹配整词："Credit card" 不是汽车
        assertEquals("other", CategoryVisuals.iconKeyFor("Credit card", e))
        assertEquals("car", CategoryVisuals.iconKeyFor("Car wash", e))
        val i = CategoryType.INCOME
        assertEquals("salary", CategoryVisuals.iconKeyFor("Monthly salary", i))
        assertEquals("salary", CategoryVisuals.iconKeyFor("Year-end bonus", i))
        assertEquals("invest", CategoryVisuals.iconKeyFor("Investment returns", i))
        assertEquals("refund", CategoryVisuals.iconKeyFor("Order refund", i))
        assertEquals("other_income", CategoryVisuals.iconKeyFor("Misc", i))
    }
}

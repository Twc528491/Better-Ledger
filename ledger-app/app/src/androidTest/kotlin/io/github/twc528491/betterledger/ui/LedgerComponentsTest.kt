package io.github.twc528491.betterledger.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.twc528491.betterledger.R
import io.github.twc528491.betterledger.data.settings.ThemeMode
import io.github.twc528491.betterledger.domain.model.CategoryType
import io.github.twc528491.betterledger.ui.common.CategoryBadge
import io.github.twc528491.betterledger.ui.common.LedgerSectionHeader
import io.github.twc528491.betterledger.ui.common.charts.ChartSlice
import io.github.twc528491.betterledger.ui.common.charts.RingChartWithLegend
import io.github.twc528491.betterledger.ui.theme.LedgerTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LedgerComponentsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun sectionHeader_exposesAndRunsAction() {
        var clicked = false
        composeRule.setContent {
            LedgerTheme(mode = ThemeMode.LIGHT) {
                LedgerSectionHeader("最近交易", actionLabel = "查看全部", onAction = { clicked = true })
            }
        }

        composeRule.onNodeWithText("最近交易").assertExists()
        composeRule.onNodeWithText("查看全部").performClick()
        assertTrue(clicked)
    }

    @Test
    fun ringChart_exposesReadableSummaryForAccessibility() {
        composeRule.setContent {
            LedgerTheme(mode = ThemeMode.LIGHT) {
                RingChartWithLegend(
                    slices = listOf(ChartSlice("餐饮", 2536, Color.Green)),
                    total = 2536,
                )
            }
        }

        // 按设备当前语言拼出期望文本：中文为“支出，餐饮，¥25.36，100%”，英文为“Expense: 餐饮, ¥25.36, 100%”。
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val expected = context.getString(
            io.github.twc528491.betterledger.R.string.chart_ring_description,
            context.getString(io.github.twc528491.betterledger.R.string.chart_expense),
            context.getString(io.github.twc528491.betterledger.R.string.chart_ring_item, "餐饮", "¥25.36", 100),
        )
        composeRule.onNodeWithContentDescription(expected)
            .assertContentDescriptionEquals(expected)
    }

    @Test
    fun categoryBadge_exposesCategoryNameForAccessibility() {
        composeRule.setContent {
            LedgerTheme(mode = ThemeMode.LIGHT) {
                CategoryBadge("咖啡", CategoryType.EXPENSE)
            }
        }

        val expected = InstrumentationRegistry.getInstrumentation().targetContext
            .getString(R.string.core_cd_category_icon, "咖啡")
        composeRule.onNodeWithContentDescription(expected)
            .assertContentDescriptionEquals(expected)
    }
}

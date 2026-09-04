package com.personal.ledger.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.personal.ledger.data.settings.ThemeMode
import com.personal.ledger.ui.common.LedgerSectionHeader
import com.personal.ledger.ui.common.charts.ChartSlice
import com.personal.ledger.ui.common.charts.RingChartWithLegend
import com.personal.ledger.ui.theme.LedgerTheme
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

        composeRule.onNodeWithContentDescription("支出，餐饮¥25.36")
            .assertContentDescriptionEquals("支出，餐饮¥25.36")
    }
}

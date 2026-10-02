package com.personal.ledger.ui.common.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.personal.ledger.R
import com.personal.ledger.domain.calc.MonthTrendCalculator
import com.personal.ledger.util.DateLabels
import com.personal.ledger.ui.common.cardColor
import com.personal.ledger.ui.common.expenseAmountColor
import com.personal.ledger.ui.theme.LedgerSpacing
import java.time.YearMonth

/**
 * 收支趋势：最近 6 / 12 个月的收入（左）与支出（右）细柱对比。
 * - 选中月份（默认当前查看的月份）实色显示并在上方读出收入 / 支出 / 结余，其余月份淡化；
 * - 虚线为窗口内有记录月份的月均支出；
 * - 当前查看的月份在横轴上加粗。
 */
@Composable
fun IncomeExpenseTrendChart(
    trend: List<MonthTrendCalculator.MonthTotals>,
    viewedMonth: YearMonth,
    currency: String,
    modifier: Modifier = Modifier,
) {
    var span by rememberSaveable { mutableIntStateOf(6) }
    var selectedMonth by rememberSaveable(viewedMonth) { mutableStateOf(viewedMonth) }
    val months = trend.takeLast(span)
    val selectedIndex = months.indexOfFirst { it.yearMonth == selectedMonth }
        .takeIf { it >= 0 } ?: months.lastIndex
    val selected = months.getOrNull(selectedIndex)
    val average = MonthTrendCalculator.averageExpense(months)
    val maxValue = maxOf(
        months.maxOfOrNull { maxOf(it.income, it.expense) } ?: 0L,
        average,
    )
    val ticks = ChartAxis.ticks(maxValue)
    val locale = chartLocale()

    val incomeColor = chartIncomeColor()
    val expenseColor = chartExpenseColor()
    val contextColor = chartContextColor()
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val columnHighlight = MaterialTheme.colorScheme.surfaceContainerHigh
    val labelStyle = axisLabelStyle()
    val strongLabelStyle = labelStyle.copy(
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold,
    )
    val textMeasurer = rememberTextMeasurer()
    val tickLabels = remember(ticks, labelStyle, locale) { textMeasurer.measureTicks(ticks, labelStyle, locale) }
    val density = LocalDensity.current
    val gutterPx = tickLabels.maxLabelWidth() + with(density) { 10.dp.toPx() }
    val reveal by rememberChartReveal(viewedMonth to span)
    val tick = rememberSelectionTick()

    val indexAt = rememberUpdatedState { x: Float, width: Int ->
        if (months.isEmpty()) null
        else (((x - gutterPx) / ((width - gutterPx) / months.size)).toInt()).coerceIn(0, months.lastIndex)
    }
    val onSelect = rememberUpdatedState { index: Int ->
        val target = months[index].yearMonth
        tick(target != selected?.yearMonth)
        selectedMonth = target
    }

    val descriptionIntro = pluralStringResource(R.plurals.chart_trend_cd_intro, months.size, months.size)
    val monthFormat = stringResource(R.string.chart_trend_cd_month)
    val averageDescription = if (average > 0L) {
        stringResource(R.string.chart_trend_cd_average, ChartAxis.readoutMoney(average, currency, locale = locale))
    } else {
        ""
    }
    val description = buildString {
        append(descriptionIntro)
        months.forEach { m ->
            append(
                String.format(
                    monthFormat,
                    DateLabels.yearMonth(m.yearMonth, locale),
                    ChartAxis.readoutMoney(m.income, currency, locale = locale),
                    ChartAxis.readoutMoney(m.expense, currency, locale = locale),
                ),
            )
        }
        append(averageDescription)
    }

    ChartCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize().padding(LedgerSpacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    selected?.let { DateLabels.yearMonth(it.yearMonth, locale) } ?: "",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                )
                SpanToggle(span = span, onChange = { span = it })
            }
            Spacer(Modifier.height(LedgerSpacing.xs))
            val balance = (selected?.income ?: 0L) - (selected?.expense ?: 0L)
            Row(horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.xs)) {
                ReadoutFigure(
                    label = stringResource(R.string.type_income),
                    value = ChartAxis.readoutMoney(selected?.income ?: 0L, currency, locale = locale),
                    keyColor = incomeColor,
                    modifier = Modifier.weight(1f),
                )
                ReadoutFigure(
                    label = stringResource(R.string.type_expense),
                    value = ChartAxis.readoutMoney(selected?.expense ?: 0L, currency, locale = locale),
                    keyColor = expenseColor,
                    modifier = Modifier.weight(1f),
                )
                ReadoutFigure(
                    label = stringResource(R.string.label_balance),
                    value = ChartAxis.readoutMoney(balance, currency, explicitPlus = true, locale = locale),
                    valueColor = if (balance < 0L) expenseAmountColor() else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(LedgerSpacing.sm))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .semantics { contentDescription = description }
                    .chartSelection(indexAt, onSelect),
            ) {
                if (months.isEmpty()) return@Canvas
                val xLabelHeight = tickLabels.firstOrNull()?.size?.height?.toFloat() ?: 0f
                val top = xLabelHeight / 2f
                val bottom = size.height - xLabelHeight - 6.dp.toPx()
                val plotHeight = bottom - top
                val left = gutterPx
                val topValue = ticks.last().coerceAtLeast(1L).toDouble()
                val groupWidth = (size.width - left) / months.size
                val barWidth = (groupWidth * 0.3f).coerceAtMost(12.dp.toPx())
                val pairGap = 2.dp.toPx()

                // 选中列：浅底色衬托，柱形本身保持细。
                drawRoundRect(
                    color = columnHighlight,
                    topLeft = Offset(left + groupWidth * selectedIndex + 1.dp.toPx(), top),
                    size = Size(groupWidth - 2.dp.toPx(), plotHeight),
                    cornerRadius = CornerRadius(6.dp.toPx()),
                )
                drawValueGrid(ticks, tickLabels, top, bottom, left, gridColor)

                months.forEachIndexed { index, m ->
                    val centerX = left + groupWidth * index + groupWidth / 2f
                    val emphasis = index == selectedIndex
                    val incomeHeight = (m.income.coerceAtLeast(0L) / topValue).toFloat() * plotHeight * reveal
                    val expenseHeight = (m.expense.coerceAtLeast(0L) / topValue).toFloat() * plotHeight * reveal
                    drawBar(
                        color = if (emphasis) incomeColor else incomeColor.copy(alpha = 0.4f),
                        left = centerX - pairGap / 2f - barWidth,
                        top = bottom - incomeHeight,
                        width = barWidth,
                        height = incomeHeight,
                    )
                    drawBar(
                        color = if (emphasis) expenseColor else expenseColor.copy(alpha = 0.4f),
                        left = centerX + pairGap / 2f,
                        top = bottom - expenseHeight,
                        width = barWidth,
                        height = expenseHeight,
                    )
                    // 12 个月时隔月标注，始终保留最后（当前查看）月份。
                    val showLabel = months.size <= 6 || (months.lastIndex - index) % 2 == 0
                    if (showLabel) {
                        val isViewed = m.yearMonth == viewedMonth
                        val label = textMeasurer.measure(
                            DateLabels.month(m.yearMonth.monthValue, locale),
                            if (isViewed) strongLabelStyle else labelStyle,
                        )
                        drawText(
                            label,
                            topLeft = Offset(
                                (centerX - label.size.width / 2f).coerceIn(0f, size.width - label.size.width),
                                size.height - label.size.height,
                            ),
                        )
                    }
                }

                if (average > 0L) {
                    val y = bottom - (average / topValue).toFloat() * plotHeight
                    drawLine(
                        color = contextColor,
                        start = Offset(left, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
                    )
                }
            }
            Spacer(Modifier.height(LedgerSpacing.xs))
            Row(
                horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LegendKey(incomeColor, stringResource(R.string.type_income))
                LegendKey(expenseColor, stringResource(R.string.type_expense))
                if (average > 0L) {
                    LegendKey(
                        contextColor,
                        stringResource(R.string.chart_monthly_average, ChartAxis.readoutMoney(average, currency, locale = locale)),
                        LegendShape.DASHED,
                    )
                }
            }
        }
    }
}

/** 6 / 12 个月切换（小号分段按钮）。 */
@Composable
private fun SpanToggle(span: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(2.dp),
    ) {
        listOf(6, 12).forEach { option ->
            val isSelected = option == span
            Text(
                stringResource(R.string.chart_span_months, option),
                modifier = Modifier
                    .heightIn(min = 28.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) cardColor() else Color.Transparent)
                    .clickable(role = Role.RadioButton) { onChange(option) }
                    .semantics { selected = isSelected }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}


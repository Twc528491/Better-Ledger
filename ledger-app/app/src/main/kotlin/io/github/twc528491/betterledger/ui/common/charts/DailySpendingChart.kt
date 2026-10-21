package io.github.twc528491.betterledger.ui.common.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import io.github.twc528491.betterledger.R
import io.github.twc528491.betterledger.domain.calc.DailyExpenseComparison
import io.github.twc528491.betterledger.util.DateLabels
import io.github.twc528491.betterledger.ui.common.cardColor
import io.github.twc528491.betterledger.ui.common.expenseAmountColor
import io.github.twc528491.betterledger.ui.common.incomeAmountColor
import io.github.twc528491.betterledger.ui.theme.LedgerNumerals
import io.github.twc528491.betterledger.ui.theme.LedgerSpacing

/**
 * 每日支出：上方为本月累计支出曲线与上月累计曲线（按几号对齐，未来日期不绘制），
 * 下方为每日支出细柱（独立刻度，不与累计曲线共用纵轴）。
 * 点按 / 长按拖动选择某天，顶部读出当日支出、累计与比上月同期。
 */
@Composable
fun DailySpendingChart(
    comparison: DailyExpenseComparison,
    currency: String,
    modifier: Modifier = Modifier,
) {
    val elapsed = comparison.elapsedDays.coerceIn(1, comparison.days.size)
    var selectedDay by rememberSaveable(comparison.month, elapsed) { mutableIntStateOf(elapsed) }
    val day = selectedDay.coerceIn(1, elapsed)
    val locale = chartLocale()
    val hasPrevious = comparison.previousTotal > 0L
    val dayExpense = comparison.days[day - 1].expense
    val cumulative = comparison.cumulativeAt(day)
    val delta = comparison.deltaAt(day)
    val peak = comparison.peak

    val lineColor = chartExpenseColor()
    val contextColor = chartContextColor()
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val surfaceColor = cardColor()
    val crosshairColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    val labelStyle = axisLabelStyle()
    val textMeasurer = rememberTextMeasurer()
    val maxCumulative = maxOf(comparison.cumulativeAt(elapsed), comparison.previousTotal)
    val ticks = ChartAxis.ticks(maxCumulative)
    val tickLabels = remember(ticks, labelStyle, locale) { textMeasurer.measureTicks(ticks, labelStyle, locale) }
    val stripText = stringResource(R.string.chart_daily_strip)
    val stripLabel = remember(labelStyle, stripText) { textMeasurer.measure(stripText, labelStyle) }
    val dayAxisFormat = stringResource(R.string.chart_day_axis)
    val density = LocalDensity.current
    val gutterPx = maxOf(tickLabels.maxLabelWidth(), stripLabel.size.width) + with(density) { 10.dp.toPx() }
    // 横轴按两个月中较长的一个铺满，保证 31 号也有位置。
    val domainDays = maxOf(comparison.days.size, comparison.previousCumulative.size)
    val reveal by rememberChartReveal(comparison.month)
    val tick = rememberSelectionTick()

    val indexAt = rememberUpdatedState { x: Float, width: Int ->
        val dayWidth = (width - gutterPx) / domainDays
        ((x - gutterPx) / dayWidth).toInt().coerceIn(0, elapsed - 1)
    }
    val onSelect = rememberUpdatedState { index: Int ->
        tick(index + 1 != day)
        selectedDay = index + 1
    }

    val deltaText = when {
        !hasPrevious -> stringResource(R.string.chart_no_prev_expense)
        delta > 0L -> "▲${ChartAxis.readoutMoney(delta, currency, locale = locale)}"
        delta < 0L -> "▼${ChartAxis.readoutMoney(-delta, currency, locale = locale)}"
        else -> stringResource(R.string.chart_flat)
    }
    val deltaColor = when {
        !hasPrevious || delta == 0L -> MaterialTheme.colorScheme.onSurfaceVariant
        delta > 0L -> expenseAmountColor()
        else -> incomeAmountColor()
    }
    val descriptionBase = stringResource(
        R.string.chart_daily_cd_base,
        DateLabels.monthDay(comparison.month.atDay(elapsed), locale),
        ChartAxis.readoutMoney(comparison.cumulativeAt(elapsed), currency, locale = locale),
    )
    val descriptionPrevious = if (hasPrevious) {
        stringResource(
            R.string.chart_daily_cd_previous,
            ChartAxis.readoutMoney(comparison.previousAt(elapsed), currency, locale = locale),
        )
    } else {
        ""
    }
    val descriptionAverage = stringResource(
        R.string.chart_daily_cd_average,
        ChartAxis.readoutMoney(comparison.dailyAverage, currency, locale = locale),
    )
    val descriptionPeak = peak?.let {
        stringResource(
            R.string.chart_daily_cd_peak,
            DateLabels.monthDay(comparison.month.atDay(it.day), locale),
            ChartAxis.readoutMoney(it.expense, currency, locale = locale),
        )
    }.orEmpty()
    val description = descriptionBase + descriptionPrevious + descriptionAverage + descriptionPeak

    ChartCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize().padding(LedgerSpacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    DateLabels.monthDay(comparison.month.atDay(day), locale),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LegendKey(lineColor, stringResource(R.string.chart_this_month), LegendShape.LINE)
                    if (hasPrevious) LegendKey(contextColor, stringResource(R.string.chart_last_month), LegendShape.LINE)
                }
            }
            Spacer(Modifier.height(LedgerSpacing.xs))
            Row(horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.xs)) {
                ReadoutFigure(
                    label = stringResource(R.string.chart_day_expense),
                    value = ChartAxis.readoutMoney(dayExpense, currency, locale = locale),
                    modifier = Modifier.weight(1f),
                )
                ReadoutFigure(
                    label = stringResource(R.string.chart_cumulative),
                    value = ChartAxis.readoutMoney(cumulative, currency, locale = locale),
                    modifier = Modifier.weight(1f),
                )
                ReadoutFigure(
                    label = stringResource(R.string.chart_vs_last_month),
                    value = deltaText,
                    valueColor = deltaColor,
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
                val left = gutterPx
                val labelHeight = stripLabel.size.height.toFloat()
                val axisBand = labelHeight + 6.dp.toPx()
                val stripHeight = (size.height * 0.2f).coerceAtLeast(28.dp.toPx())
                val stripBottom = size.height - axisBand
                val stripTop = stripBottom - stripHeight
                val top = labelHeight / 2f
                val bottom = stripTop - 12.dp.toPx()
                val plotHeight = bottom - top
                val dayWidth = (size.width - left) / domainDays
                fun xOf(d: Int) = left + dayWidth * (d - 0.5f)
                val topValue = ticks.last().coerceAtLeast(1L).toDouble()
                fun yOf(value: Long) = bottom - (value / topValue).toFloat() * plotHeight

                drawValueGrid(ticks, tickLabels, top, bottom, left, gridColor)

                // 上月累计：中性色作为参照。
                if (hasPrevious) {
                    val previous = Path().apply {
                        moveTo(left, bottom)
                        comparison.previousCumulative.forEachIndexed { i, v -> lineTo(xOf(i + 1), yOf(v)) }
                    }
                    clipRect(right = left + (size.width - left) * reveal) {
                        drawPath(
                            previous,
                            contextColor,
                            style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                        )
                    }
                }

                // 本月累计：折线 + 10% 面积底色，只画到已过的天数。
                val current = Path().apply {
                    moveTo(left, bottom)
                    for (d in 1..elapsed) lineTo(xOf(d), yOf(comparison.cumulativeAt(d)))
                }
                val area = Path().apply {
                    addPath(current)
                    lineTo(xOf(elapsed), bottom)
                    close()
                }
                clipRect(right = left + (size.width - left) * reveal) {
                    drawPath(area, lineColor.copy(alpha = 0.1f))
                    drawPath(
                        current,
                        lineColor,
                        style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }

                // 每日细柱：独立刻度，选中日实色。
                drawLine(gridColor, Offset(left, stripBottom), Offset(size.width, stripBottom), strokeWidth = 1.dp.toPx())
                drawText(
                    stripLabel,
                    topLeft = Offset(
                        (left - 6.dp.toPx() - stripLabel.size.width).coerceAtLeast(0f),
                        stripBottom - stripLabel.size.height,
                    ),
                )
                val maxDaily = comparison.days.take(elapsed).maxOfOrNull { it.expense }?.coerceAtLeast(1L) ?: 1L
                val barWidth = (dayWidth * 0.56f).coerceIn(1.dp.toPx(), 8.dp.toPx())
                for (d in 1..elapsed) {
                    val value = comparison.days[d - 1].expense
                    if (value <= 0L) continue
                    val h = (value.toDouble() / maxDaily).toFloat() * (stripHeight - 2.dp.toPx()) * reveal
                    drawBar(
                        color = if (d == day) lineColor else lineColor.copy(alpha = 0.4f),
                        left = xOf(d) - barWidth / 2f,
                        top = stripBottom - h,
                        width = barWidth,
                        height = h,
                    )
                }

                // 选中日：竖向参考线 + 带表面色描边的圆点。
                val x = xOf(day)
                drawLine(crosshairColor, Offset(x, top), Offset(x, stripBottom), strokeWidth = 1.dp.toPx())
                if (hasPrevious) {
                    val prevY = yOf(comparison.previousAt(day))
                    drawCircle(surfaceColor, radius = 5.dp.toPx(), center = Offset(x, prevY))
                    drawCircle(contextColor, radius = 3.dp.toPx(), center = Offset(x, prevY))
                }
                val y = yOf(cumulative)
                drawCircle(surfaceColor, radius = 6.dp.toPx(), center = Offset(x, y))
                drawCircle(lineColor, radius = 4.dp.toPx(), center = Offset(x, y))

                // 横轴：1 / 8 / 15 / 22 / 月末。
                val lastDay = comparison.days.size
                listOf(1, 8, 15, 22, lastDay).distinct().forEach { d ->
                    val label = textMeasurer.measure(String.format(locale, dayAxisFormat, d), labelStyle)
                    drawText(
                        label,
                        topLeft = Offset(
                            (xOf(d) - label.size.width / 2f).coerceIn(0f, size.width - label.size.width),
                            size.height - label.size.height,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(LedgerSpacing.xs))
            Text(
                if (peak != null) {
                    stringResource(
                        R.string.chart_daily_footer_with_peak,
                        ChartAxis.readoutMoney(comparison.dailyAverage, currency, locale = locale),
                        ChartAxis.readoutMoney(peak.expense, currency, locale = locale),
                        peak.day,
                    )
                } else {
                    stringResource(
                        R.string.chart_daily_footer,
                        ChartAxis.readoutMoney(comparison.dailyAverage, currency, locale = locale),
                    )
                },
                style = LedgerNumerals.of(MaterialTheme.typography.labelMedium),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}


package com.personal.ledger.ui.common.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.personal.ledger.domain.calc.DailyExpenseTotal
import com.personal.ledger.ui.common.cardColor
import com.personal.ledger.ui.common.expenseAmountColor
import com.personal.ledger.ui.common.incomeAmountColor
import com.personal.ledger.util.Money
import java.time.YearMonth

/**
 * Compose Canvas 自绘图表（UI.md 第 6 节 / ARCHITECTURE.md 第 10 节）：
 * 只使用环形图 / 柱状图 / 折线图 / 进度条；不引入图表库。
 */

/** 环形图扇区。 */
data class ChartSlice(
    val label: String,
    val value: Long,
    val color: Color,
)

private val chartPalette = listOf(
    Color(0xFF4CAF50), Color(0xFFFF9800), Color(0xFF2196F3),
    Color(0xFF9C27B0), Color(0xFFF44336), Color(0xFF00BCD4),
    Color(0xFF795548), Color(0xFF607D8B),
)

fun paletteFor(index: Int): Color = chartPalette[index % chartPalette.size]

/** 环形图：分类占比。空数据显示占位文本。 */
@Composable
fun RingChart(
    slices: List<ChartSlice>,
    total: Long,
    modifier: Modifier = Modifier,
    centerLabel: String = "支出",
    currency: String = "CNY",
) {
    if (slices.isEmpty() || total <= 0) {
        Text(
            "暂无数据",
            modifier = modifier.padding(24.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
        )
        return
    }
    val chartDescription = slices.joinToString("，") {
        val percentage = (it.value.toDouble() / total.toDouble() * 100).toInt()
        "${it.label}，${Money.formatWithCurrency(it.value, currency)}，$percentage%"
    }
    Canvas(modifier = modifier.semantics { contentDescription = "$centerLabel，$chartDescription" }) {
        val stroke = Stroke(width = size.minDimension * 0.16f, cap = StrokeCap.Butt)
        val inset = stroke.width / 2
        val arcSize = Size(size.minDimension - stroke.width, size.minDimension - stroke.width)
        val topLeft = Offset(inset, inset)
        var startAngle = -90f
        slices.forEach { slice ->
            val sweep = 360f * slice.value.coerceAtLeast(0).toDouble().div(total.toDouble()).toFloat()
            drawArc(
                color = slice.color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )
            startAngle += sweep
        }
    }
}

/** 环形图 + 图例（占一行，图在左、图例在右）。 */
@Composable
fun RingChartWithLegend(
    slices: List<ChartSlice>,
    total: Long,
    centerLabel: String = "支出",
    currency: String = "CNY",
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = cardColor(),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(132.dp), contentAlignment = Alignment.Center) {
                RingChart(
                    slices = slices,
                    total = total,
                    centerLabel = centerLabel,
                    currency = currency,
                    modifier = Modifier.fillMaxSize(),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(centerLabel, style = MaterialTheme.typography.labelMedium)
                    Text(
                        Money.formatWithCurrency(total, currency),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                slices.forEach { slice ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = slice.color.copy(alpha = 0.14f),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                .semantics(mergeDescendants = true) {
                                    contentDescription =
                                        "${slice.label}，${Money.formatWithCurrency(slice.value, currency)}"
                                },
                        ) {
                            Canvas(modifier = Modifier.size(10.dp)) {
                                drawCircle(color = slice.color)
                            }
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "${slice.label}  ${Money.formatWithCurrency(slice.value, currency)}",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 分组柱状图：每月收入（绿）/ 支出（红）双柱。 */
@Composable
fun TrendBarChart(
    months: List<YearMonth>,
    incomes: List<Long>,
    expenses: List<Long>,
    currency: String = "CNY",
    modifier: Modifier = Modifier,
) {
    val count = minOf(months.size, incomes.size, expenses.size)
    val maxValue = (0 until count).maxOfOrNull { maxOf(incomes[it], expenses[it]).coerceAtLeast(0L) } ?: 0L
    val incomeColor = incomeAmountColor()
    val expenseColor = expenseAmountColor()
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = cardColor(),
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Text("● 收入", color = incomeColor, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.width(12.dp))
                Text("● 支出", color = expenseColor, style = MaterialTheme.typography.labelSmall)
            }
            Text(
                "金额（${Money.symbolFor(currency)}） · 最高 ${Money.formatWithCurrency(maxValue, currency)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .semantics {
                        contentDescription = (0 until count).joinToString("，") { index ->
                            "${months[index].year}年${months[index].monthValue}月收入${Money.formatWithCurrency(incomes[index], currency)}，支出${Money.formatWithCurrency(expenses[index], currency)}"
                        }
                    },
            ) {
                if (count == 0 || maxValue <= 0) return@Canvas
                val groupCount = count
                val groupWidth = size.width / groupCount
                val barWidth = groupWidth * 0.28f
                val baseline = size.height * 0.92f
                for (gridStep in 0..2) {
                    val y = size.height * (0.12f + gridStep * 0.4f)
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                }
                drawLine(gridColor, Offset(0f, baseline), Offset(size.width, baseline), strokeWidth = 1.dp.toPx())
                for (i in 0 until count) {
                    val centerX = groupWidth * i + groupWidth / 2
                    val incH = (incomes[i].coerceAtLeast(0).toDouble() / maxValue.toDouble()).toFloat() * size.height * 0.76f
                    val expH = (expenses[i].coerceAtLeast(0).toDouble() / maxValue.toDouble()).toFloat() * size.height * 0.76f
                    // 收入柱（左）
                    if (incomes[i] > 0) {
                        drawRect(
                            color = incomeColor,
                            topLeft = Offset(centerX - barWidth - barWidth / 2, baseline - incH),
                            size = Size(barWidth, incH),
                        )
                    }
                    // 支出柱（右）
                    if (expenses[i] > 0) {
                        drawRect(
                            color = expenseColor,
                            topLeft = Offset(centerX + barWidth / 2, baseline - expH),
                            size = Size(barWidth, expH),
                        )
                    }
                }
            }
            if (count == 0 || maxValue <= 0L) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("近 6 个月暂无收支", style = MaterialTheme.typography.bodyMedium)
                }
            }
            // X 轴月份标签
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                months.take(count).forEach { ym ->
                    Text(
                        "${ym.monthValue}月",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** Compact horizontal bars for the highest spending categories. */
@Composable
fun HorizontalRankingChart(
    items: List<ChartSlice>,
    currency: String,
    modifier: Modifier = Modifier,
    limit: Int = 5,
) {
    val visibleItems = items.take(limit)
    val maxValue = visibleItems.maxOfOrNull { it.value } ?: 0L
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = cardColor(),
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text("本月支出前 ${visibleItems.size} 项", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            if (visibleItems.isEmpty() || maxValue <= 0L) {
                Text("本月暂无分类支出", style = MaterialTheme.typography.bodyMedium)
            } else {
                visibleItems.forEachIndexed { index, slice ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .semantics(mergeDescendants = true) {
                                contentDescription =
                                    "${index + 1}，${slice.label}，${Money.formatWithCurrency(slice.value, currency)}"
                            },
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${index + 1}. ${slice.label}",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                Money.formatWithCurrency(slice.value, currency),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.height(5.dp))
                        val fraction = (slice.value.toDouble() / maxValue.toDouble()).toFloat().coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction)
                                    .height(7.dp)
                                    .clip(CircleShape)
                                    .background(slice.color),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Daily expense bars for the selected month. Zero-value days remain visible on the x-axis. */
@Composable
fun DailyExpenseChart(
    days: List<DailyExpenseTotal>,
    currency: String,
    modifier: Modifier = Modifier,
) {
    val maxValue = days.maxOfOrNull { it.expense } ?: 0L
    val expenseColor = expenseAmountColor()
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val chartDescription = if (days.isEmpty()) {
        "本月每日支出暂无数据"
    } else {
        days.joinToString("，") { "${it.day}日${Money.formatWithCurrency(it.expense, currency)}" }
    }
    Surface(
        modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) {
            contentDescription = "每日支出趋势，$chartDescription"
        },
        shape = RoundedCornerShape(16.dp),
        color = cardColor(),
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
            if (days.isEmpty() || maxValue <= 0L) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("本月暂无支出", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                val peak = days.maxByOrNull { it.expense }!!
                Text(
                    "金额（${Money.symbolFor(currency)}） · 峰值 ${Money.formatWithCurrency(peak.expense, currency)}（${peak.day}日）",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .semantics { contentDescription = chartDescription },
                ) {
                    val groupWidth = size.width / days.size
                    val barWidth = (groupWidth * 0.62f).coerceAtLeast(1f)
                    val baseline = size.height * 0.92f
                    for (gridStep in 0..2) {
                        val y = size.height * (0.12f + gridStep * 0.4f)
                        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                    }
                    drawLine(gridColor, Offset(0f, baseline), Offset(size.width, baseline), strokeWidth = 1.dp.toPx())
                    days.forEachIndexed { index, point ->
                        if (point.expense > 0L) {
                            val barHeight =
                                (point.expense.toDouble() / maxValue.toDouble()).toFloat() * size.height * 0.76f
                            val centerX = groupWidth * index + groupWidth / 2
                            drawRect(
                                color = expenseColor,
                                topLeft = Offset(centerX - barWidth / 2, baseline - barHeight),
                                size = Size(barWidth, barHeight),
                            )
                        }
                    }
                }
                val tickIndices = when {
                    days.size <= 1 -> days.indices.toList()
                    else -> (0..3).map { it * days.lastIndex / 3 }.distinct()
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    tickIndices.forEach { index ->
                        Text(
                            "${days[index].day}日",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

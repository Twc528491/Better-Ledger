package com.personal.ledger.ui.common.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.personal.ledger.domain.calc.DailyExpenseTotal
import com.personal.ledger.ui.common.LedgerProgressBar
import com.personal.ledger.ui.common.cardColor
import com.personal.ledger.ui.common.expenseAmountColor
import com.personal.ledger.ui.common.incomeAmountColor
import com.personal.ledger.ui.theme.LedgerNumerals
import com.personal.ledger.util.Money
import java.time.YearMonth

/**
 * Compose Canvas 自绘图表（UI.md 第 6 节 / ARCHITECTURE.md 第 10 节）：
 * 只使用环形图 / 柱状图 / 折线图 / 进度条；不引入图表库。
 * 配色取自主题与分类色相（categoryChartColor），网格线使用 outlineVariant，保持低存在感。
 */

/** 环形图扇区。 */
data class ChartSlice(
    val label: String,
    val value: Long,
    val color: Color,
)

/** 备用色板：与分类色相同一明度 / 饱和度区间，不使用高饱和 Material 基础色。 */
private val chartPalette = listOf(
    Color(0xFF3F9C7E), Color(0xFFD08A3C), Color(0xFF4F7FC0),
    Color(0xFF8E6CB8), Color(0xFFC75B55), Color(0xFF3A9AA8),
    Color(0xFF9A7B5C), Color(0xFF7B8784),
)

fun paletteFor(index: Int): Color = chartPalette[index % chartPalette.size]

/** 图表卡片统一容器。 */
@Composable
private fun ChartCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = cardColor(),
        tonalElevation = 0.dp,
        content = content,
    )
}

/** 顶部圆角、底部平直的柱形，贴合基线。 */
private fun DrawScope.drawBar(color: Color, left: Float, top: Float, width: Float, height: Float) {
    if (height <= 0f || width <= 0f) return
    val radius = minOf(width / 2f, 3.dp.toPx(), height)
    drawRoundRect(
        color = color,
        topLeft = Offset(left, top),
        size = Size(width, height),
        cornerRadius = CornerRadius(radius, radius),
    )
    if (height > radius) {
        drawRect(
            color = color,
            topLeft = Offset(left, top + height - radius),
            size = Size(width, radius),
        )
    }
}

/** 三条浅网格线 + 基线。 */
private fun DrawScope.drawGrid(gridColor: Color, baseline: Float) {
    val stroke = 1.dp.toPx()
    for (gridStep in 0..2) {
        val y = size.height * (0.12f + gridStep * 0.4f)
        if (y < baseline - stroke) {
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = stroke)
        }
    }
    drawLine(gridColor, Offset(0f, baseline), Offset(size.width, baseline), strokeWidth = stroke)
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(8.dp)) { drawCircle(color = color) }
        Spacer(Modifier.width(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val chartDescription = slices.joinToString("，") {
        val percentage = (it.value.toDouble() / total.toDouble() * 100).toInt()
        "${it.label}，${Money.formatWithCurrency(it.value, currency)}，$percentage%"
    }
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
    Canvas(modifier = modifier.semantics { contentDescription = "$centerLabel，$chartDescription" }) {
        val stroke = Stroke(width = size.minDimension * 0.14f, cap = StrokeCap.Butt)
        val inset = stroke.width / 2
        val arcSize = Size(size.minDimension - stroke.width, size.minDimension - stroke.width)
        val topLeft = Offset(inset, inset)
        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke,
        )
        // 多个扇区之间留 1.5° 细缝，避免相邻色块糊在一起。
        val gap = if (slices.size > 1) 1.5f else 0f
        var startAngle = -90f
        slices.forEach { slice ->
            val sweep = 360f * slice.value.coerceAtLeast(0).toDouble().div(total.toDouble()).toFloat()
            val drawn = (sweep - gap).coerceAtLeast(0f)
            if (drawn > 0f) {
                drawArc(
                    color = slice.color,
                    startAngle = startAngle + gap / 2,
                    sweepAngle = drawn,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke,
                )
            }
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
    ChartCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(136.dp), contentAlignment = Alignment.Center) {
                RingChart(
                    slices = slices,
                    total = total,
                    centerLabel = centerLabel,
                    currency = currency,
                    modifier = Modifier.fillMaxSize(),
                )
                Column(
                    modifier = Modifier.widthIn(max = 96.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        centerLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        Money.formatWithCurrency(total, currency),
                        style = LedgerNumerals.of(MaterialTheme.typography.labelLarge),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                slices.forEach { slice ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics(mergeDescendants = true) {
                                contentDescription =
                                    "${slice.label}，${Money.formatWithCurrency(slice.value, currency)}"
                            },
                    ) {
                        Canvas(modifier = Modifier.size(8.dp)) {
                            drawCircle(color = slice.color)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            slice.label,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            Money.formatWithCurrency(slice.value, currency),
                            style = LedgerNumerals.of(MaterialTheme.typography.bodySmall),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                        )
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
    ChartCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "金额（${Money.symbolFor(currency)}） · 最高 ${Money.formatWithCurrency(maxValue, currency)}",
                    modifier = Modifier.weight(1f),
                    style = LedgerNumerals.of(MaterialTheme.typography.labelSmall),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(8.dp))
                LegendDot(incomeColor, "收入")
                Spacer(Modifier.width(12.dp))
                LegendDot(expenseColor, "支出")
            }
            Spacer(Modifier.height(8.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
                    .semantics {
                        contentDescription = (0 until count).joinToString("，") { index ->
                            "${months[index].year}年${months[index].monthValue}月收入${Money.formatWithCurrency(incomes[index], currency)}，支出${Money.formatWithCurrency(expenses[index], currency)}"
                        }
                    },
            ) {
                val baseline = size.height * 0.92f
                drawGrid(gridColor, baseline)
                if (count == 0 || maxValue <= 0) return@Canvas
                val groupWidth = size.width / count
                val barWidth = (groupWidth * 0.26f).coerceAtMost(14.dp.toPx())
                val pairGap = 3.dp.toPx()
                for (i in 0 until count) {
                    val centerX = groupWidth * i + groupWidth / 2
                    val incH = (incomes[i].coerceAtLeast(0).toDouble() / maxValue.toDouble()).toFloat() * size.height * 0.76f
                    val expH = (expenses[i].coerceAtLeast(0).toDouble() / maxValue.toDouble()).toFloat() * size.height * 0.76f
                    // 收入柱（左）
                    if (incomes[i] > 0) {
                        drawBar(incomeColor, centerX - pairGap / 2 - barWidth, baseline - incH, barWidth, incH)
                    }
                    // 支出柱（右）
                    if (expenses[i] > 0) {
                        drawBar(expenseColor, centerX + pairGap / 2, baseline - expH, barWidth, expH)
                    }
                }
            }
            if (count == 0 || maxValue <= 0L) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "近 6 个月暂无收支",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // X 轴月份标签
            Row(modifier = Modifier.fillMaxWidth()) {
                months.take(count).forEach { ym ->
                    Text(
                        "${ym.monthValue}月",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    ChartCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("本月支出前 ${visibleItems.size} 项", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(12.dp))
            if (visibleItems.isEmpty() || maxValue <= 0L) {
                Text(
                    "本月暂无分类支出",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    visibleItems.forEachIndexed { index, slice ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics(mergeDescendants = true) {
                                    contentDescription =
                                        "${index + 1}，${slice.label}，${Money.formatWithCurrency(slice.value, currency)}"
                                },
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${index + 1}",
                                    modifier = Modifier.width(20.dp),
                                    style = LedgerNumerals.of(MaterialTheme.typography.labelMedium),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    slice.label,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    Money.formatWithCurrency(slice.value, currency),
                                    style = LedgerNumerals.of(MaterialTheme.typography.bodyMedium),
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            val fraction = (slice.value.toDouble() / maxValue.toDouble()).toFloat()
                            LedgerProgressBar(
                                progress = fraction,
                                color = slice.color,
                                modifier = Modifier.padding(start = 20.dp),
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
    ChartCard(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "每日支出趋势，$chartDescription"
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            if (days.isEmpty() || maxValue <= 0L) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "本月暂无支出",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                val peak = days.maxByOrNull { it.expense }!!
                Text(
                    "金额（${Money.symbolFor(currency)}） · 峰值 ${Money.formatWithCurrency(peak.expense, currency)}（${peak.day}日）",
                    style = LedgerNumerals.of(MaterialTheme.typography.labelSmall),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp)
                        .semantics { contentDescription = chartDescription },
                ) {
                    val groupWidth = size.width / days.size
                    val barWidth = (groupWidth * 0.6f).coerceAtLeast(1f)
                    val baseline = size.height * 0.92f
                    drawGrid(gridColor, baseline)
                    days.forEachIndexed { index, point ->
                        if (point.expense > 0L) {
                            val barHeight =
                                (point.expense.toDouble() / maxValue.toDouble()).toFloat() * size.height * 0.76f
                            val centerX = groupWidth * index + groupWidth / 2
                            drawBar(
                                color = if (point == peak) expenseColor else expenseColor.copy(alpha = 0.72f),
                                left = centerX - barWidth / 2,
                                top = baseline - barHeight,
                                width = barWidth,
                                height = barHeight,
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
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

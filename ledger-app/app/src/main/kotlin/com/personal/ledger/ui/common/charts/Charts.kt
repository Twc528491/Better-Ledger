package com.personal.ledger.ui.common.charts

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
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
    val chartDescription = slices.joinToString(", ") { "${it.label}${Money.formatWithCurrency(it.value, currency)}" }
    Canvas(modifier = modifier.semantics { contentDescription = "$centerLabel，$chartDescription" }) {
        val stroke = Stroke(width = size.minDimension * 0.16f, cap = StrokeCap.Butt)
        val inset = stroke.width / 2
        val arcSize = Size(size.minDimension - stroke.width, size.minDimension - stroke.width)
        val topLeft = Offset(inset, inset)
        var startAngle = -90f
        slices.forEach { slice ->
            val sweep = 360f * slice.value / total
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
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
                Text(Money.formatWithCurrency(total, currency), style = MaterialTheme.typography.labelLarge)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column {
            slices.forEach { slice ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                    Canvas(modifier = Modifier.size(10.dp)) {
                        drawCircle(color = slice.color)
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${slice.label}  ${Money.formatWithCurrency(slice.value, currency)}",
                        style = MaterialTheme.typography.bodySmall,
                    )
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
    modifier: Modifier = Modifier,
) {
    val maxValue = (incomes + expenses).maxOrNull() ?: 0L
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Text("● 收入", color = Color(0xFF4CAF50), style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.width(12.dp))
            Text("● 支出", color = Color(0xFFF44336), style = MaterialTheme.typography.labelSmall)
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .semantics {
                    contentDescription = months.indices.joinToString(", ") { index ->
                        "${months[index].monthValue}月收入${Money.formatMinor(incomes[index])}，支出${Money.formatMinor(expenses[index])}"
                    }
                },
        ) {
            if (months.isEmpty() || maxValue <= 0) return@Canvas
            val groupCount = months.size
            val groupWidth = size.width / groupCount
            val barWidth = groupWidth * 0.28f
            val baseline = size.height * 0.92f
            months.forEachIndexed { i, ym ->
                val centerX = groupWidth * i + groupWidth / 2
                val incH = (incomes[i].toFloat() / maxValue) * size.height * 0.85f
                val expH = (expenses[i].toFloat() / maxValue) * size.height * 0.85f
                // 收入柱（左）
                if (incomes[i] > 0) {
                    drawRect(
                        color = Color(0xFF4CAF50),
                        topLeft = Offset(centerX - barWidth - barWidth / 2, baseline - incH),
                        size = Size(barWidth, incH),
                    )
                }
                // 支出柱（右）
                if (expenses[i] > 0) {
                    drawRect(
                        color = Color(0xFFF44336),
                        topLeft = Offset(centerX + barWidth / 2, baseline - expH),
                        size = Size(barWidth, expH),
                    )
                }
            }
        }
        // X 轴月份标签
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            months.forEach { ym ->
                Text(
                    "${ym.monthValue}月",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

package com.personal.ledger.ui.common.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.personal.ledger.ui.common.expenseAmountColor
import com.personal.ledger.ui.common.incomeAmountColor
import com.personal.ledger.ui.theme.LedgerNumerals
import com.personal.ledger.ui.theme.LedgerSpacing
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.personal.ledger.R
import com.personal.ledger.util.Money
import java.util.Locale
import kotlin.math.abs

/** 分类环比的一行。 */
data class CategoryChangeRow(
    val label: String,
    val color: Color,
    val current: Long,
    val previous: Long,
    /** 环比百分比；上月为 0 时为 null。 */
    val percent: Long?,
) {
    val delta: Long get() = current - previous
}

/**
 * 分类环比：本月各分类支出与上月相比的变化，按变化幅度排序。
 * 中线向右（支出色）表示多花、向左（收入色）表示少花，并配 ▲ / ▼ 与正负号，不只依赖颜色。
 */
@Composable
fun CategoryChangeChart(
    rows: List<CategoryChangeRow>,
    currentTotal: Long,
    previousTotal: Long,
    currency: String,
    modifier: Modifier = Modifier,
    revealKey: Any? = Unit,
    maxRows: Int = 5,
) {
    val visible = rows.take(maxRows)
    val maxAbs = visible.maxOfOrNull { abs(it.delta) }?.coerceAtLeast(1L) ?: 1L
    val increaseColor = chartExpenseColor()
    val decreaseColor = chartIncomeColor()
    val axisColor = MaterialTheme.colorScheme.outline
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val reveal by rememberChartReveal(revealKey)
    val totalDelta = currentTotal - previousTotal
    val totalPercent = if (previousTotal > 0L) Math.round(totalDelta * 100.0 / previousTotal) else null
    val locale = chartLocale()

    ChartCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize().padding(LedgerSpacing.md)) {
            Row(horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.xs)) {
                ReadoutFigure(
                    label = stringResource(R.string.chart_this_month_expense),
                    value = ChartAxis.readoutMoney(currentTotal, currency, locale = locale),
                    modifier = Modifier.weight(1f),
                )
                ReadoutFigure(
                    label = stringResource(R.string.chart_last_month_expense),
                    value = ChartAxis.readoutMoney(previousTotal, currency, locale = locale),
                    modifier = Modifier.weight(1f),
                )
                ReadoutFigure(
                    label = stringResource(R.string.chart_change),
                    value = when {
                        previousTotal <= 0L -> stringResource(R.string.chart_no_prev_expense)
                        totalPercent == null || totalDelta == 0L -> stringResource(R.string.chart_flat)
                        totalDelta > 0L -> "▲${totalPercent}%"
                        else -> "▼${-totalPercent}%"
                    },
                    valueColor = changeTextColor(if (previousTotal <= 0L) 0L else totalDelta),
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(LedgerSpacing.sm))
            if (visible.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        stringResource(R.string.chart_all_flat),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                return@Column
            }
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                visible.forEach { row ->
                    ChangeRow(
                        row = row,
                        currency = currency,
                        locale = locale,
                        barFraction = (abs(row.delta).toDouble() / maxAbs.toDouble()).toFloat() * reveal,
                        barColor = if (row.delta > 0L) increaseColor else decreaseColor,
                        axisColor = axisColor,
                        trackColor = trackColor,
                    )
                }
            }
            if (rows.size > visible.size) {
                Text(
                    pluralStringResource(R.plurals.chart_more_changes, rows.size - visible.size, rows.size - visible.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun shortMoney(minor: Long, currency: String, locale: Locale): String =
    Money.symbolFor(currency) + ChartAxis.compactLabel(minor, locale)

@Composable
private fun changeTextColor(delta: Long): Color = when {
    delta > 0L -> expenseAmountColor()
    delta < 0L -> incomeAmountColor()
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun ChangeRow(
    row: CategoryChangeRow,
    currency: String,
    locale: Locale,
    barFraction: Float,
    barColor: Color,
    axisColor: Color,
    trackColor: Color,
) {
    val deltaMoney = ChartAxis.readoutMoney(abs(row.delta), currency, locale = locale)
    val percentText = when {
        row.percent != null -> "${if (row.percent > 0) "+" else ""}${row.percent}%"
        else -> stringResource(R.string.chart_none_last_month)
    }
    val description = stringResource(
        if (row.delta > 0L) R.string.chart_change_row_up else R.string.chart_change_row_down,
        row.label,
        ChartAxis.readoutMoney(row.current, currency, locale = locale),
        ChartAxis.readoutMoney(row.previous, currency, locale = locale),
        deltaMoney,
        percentText,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 46.dp)
            .clearAndSetSemantics {
                contentDescription = description
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(8.dp)) { drawCircle(row.color) }
        Spacer(Modifier.width(LedgerSpacing.xs))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                row.label,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                // 次要说明用紧凑金额，窄屏下也能完整显示。
                stringResource(
                    R.string.chart_change_row_detail,
                    shortMoney(row.current, currency, locale),
                    shortMoney(row.previous, currency, locale),
                ),
                style = LedgerNumerals.of(MaterialTheme.typography.labelSmall),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(LedgerSpacing.xs))
        // 发散条：中线为 0，右侧多花、左侧少花。
        Canvas(Modifier.width(56.dp).height(16.dp)) {
            val center = size.width / 2f
            val barHeight = 6.dp.toPx()
            val y = (size.height - barHeight) / 2f
            drawRoundRect(trackColor, Offset(0f, y), Size(size.width, barHeight), CornerRadius(barHeight / 2))
            val length = (size.width / 2f) * barFraction
            if (length > 0f) {
                val left = if (row.delta > 0L) center else center - length
                drawRoundRect(barColor, Offset(left, y), Size(length, barHeight), CornerRadius(barHeight / 2))
            }
            drawLine(axisColor, Offset(center, 0f), Offset(center, size.height), strokeWidth = 1.dp.toPx())
        }
        Spacer(Modifier.width(LedgerSpacing.xs))
        Column(
            modifier = Modifier.widthIn(min = 76.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (row.delta > 0L) "▲" else "▼",
                    style = MaterialTheme.typography.labelSmall,
                    color = changeTextColor(row.delta),
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    deltaMoney,
                    style = LedgerNumerals.of(MaterialTheme.typography.labelLarge),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                )
            }
            Text(
                percentText,
                style = LedgerNumerals.of(MaterialTheme.typography.labelSmall),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

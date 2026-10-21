package io.github.twc528491.betterledger.ui.common.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.twc528491.betterledger.ui.common.cardColor
import io.github.twc528491.betterledger.ui.common.rememberHapticTick
import io.github.twc528491.betterledger.ui.theme.LedgerNumerals
import io.github.twc528491.betterledger.ui.theme.LedgerSpacing
import io.github.twc528491.betterledger.ui.theme.LocalLedgerDarkTheme
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import io.github.twc528491.betterledger.R
import io.github.twc528491.betterledger.util.Money
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Compose Canvas 自绘图表（UI.md 第 6 节 / ARCHITECTURE.md 第 10 节）：不引入图表库。
 * 统一约定：细柱（≤ 24dp、4dp 圆角顶端、贴基线）、2dp 折线、1dp 实线网格、
 * 文字只用文字色，颜色只落在图形上；所有图表页高度一致，避免分页切换时跳动。
 */

/** 统计页每个图表分页的统一高度。 */
val ChartPageHeight = 368.dp

/** 环形图扇区。 */
data class ChartSlice(
    val label: String,
    val value: Long,
    val color: Color,
)

/**
 * 图表专用收入 / 支出色：与全局收支语义色同色相，但拉开明度，
 * 使红绿色弱用户也能区分（配合固定的左右位置与图例）。
 */
@Composable
fun chartIncomeColor(): Color =
    if (LocalLedgerDarkTheme.current) Color(0xFF2F8A50) else Color(0xFF1E7B3A)

@Composable
fun chartExpenseColor(): Color =
    if (LocalLedgerDarkTheme.current) Color(0xFFF2907F) else Color(0xFFE5715C)

/** 对比 / 背景序列（上月、非选中）使用的中性色。 */
@Composable
fun chartContextColor(): Color = MaterialTheme.colorScheme.outline

/** 图表卡片统一容器。 */
@Composable
internal fun ChartCard(
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

/** 入场动画进度（0→1）；[key] 变化时重播。只在绘制阶段读取，避免逐帧重组。 */
@Composable
internal fun rememberChartReveal(key: Any?): State<Float> {
    val progress = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        progress.animateTo(1f, tween(durationMillis = 560, easing = FastOutSlowInEasing))
    }
    return progress.asState()
}

/** 选中项变化时触发一次轻刻度触感。 */
@Composable
internal fun rememberSelectionTick(): (changed: Boolean) -> Unit {
    val tick = rememberHapticTick()
    return remember(tick) { { changed -> if (changed) tick() } }
}

/** 顶部 4dp 圆角、底部平直的柱形，贴合基线。 */
internal fun DrawScope.drawBar(color: Color, left: Float, top: Float, width: Float, height: Float) {
    if (height <= 0f || width <= 0f) return
    val radius = minOf(width / 2f, 4.dp.toPx(), height)
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

/** 图例键：柱 / 区域用小方块，折线用短线，参考线用虚线。 */
internal enum class LegendShape { BAR, LINE, DASHED }

@Composable
internal fun LegendKey(color: Color, label: String, shape: LegendShape = LegendShape.BAR) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(width = if (shape == LegendShape.BAR) 8.dp else 14.dp, height = 8.dp)) {
            when (shape) {
                LegendShape.BAR -> drawRoundRect(color, cornerRadius = CornerRadius(2.dp.toPx()))
                LegendShape.LINE -> drawLine(
                    color,
                    Offset(0f, size.height / 2),
                    Offset(size.width, size.height / 2),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                LegendShape.DASHED -> drawLine(
                    color,
                    Offset(0f, size.height / 2),
                    Offset(size.width, size.height / 2),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.dp.toPx())),
                )
            }
        }
        Spacer(Modifier.width(4.dp))
        Text(
            label,
            style = LedgerNumerals.of(MaterialTheme.typography.labelSmall),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
        )
    }
}

/** 图表顶部的读数块：小标签 + 金额（等宽数字，拖动选择时不抖动）。 */
@Composable
internal fun ReadoutFigure(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    keyColor: Color? = null,
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (keyColor != null) {
                Box(Modifier.size(6.dp).background(keyColor, RoundedCornerShape(1.5.dp)))
                Spacer(Modifier.width(4.dp))
            }
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            style = LedgerNumerals.of(MaterialTheme.typography.titleSmall),
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 图表文字使用的当前界面语言（轴标签、读数的紧凑写法随语言变化）。 */
@Composable
internal fun chartLocale(): Locale = LocalConfiguration.current.locales[0]

/** 坐标轴文字样式（等宽数字）。 */
@Composable
internal fun axisLabelStyle(): TextStyle =
    LedgerNumerals.of(MaterialTheme.typography.labelSmall).copy(color = MaterialTheme.colorScheme.onSurfaceVariant)

/** 环形图：分类占比。[selectedIndex] 非空时其余扇区淡化。 */
@Composable
fun RingChart(
    slices: List<ChartSlice>,
    total: Long,
    modifier: Modifier = Modifier,
    centerLabel: String = stringResource(R.string.chart_expense),
    currency: String = "CNY",
    selectedIndex: Int? = null,
    revealKey: Any? = Unit,
) {
    if (slices.isEmpty() || total <= 0) {
        Text(
            stringResource(R.string.chart_no_data),
            modifier = modifier.padding(24.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val separator = stringResource(R.string.chart_list_separator)
    val itemFormat = stringResource(R.string.chart_ring_item)
    val chartDescription = slices.joinToString(separator) {
        val percentage = (it.value.toDouble() / total.toDouble() * 100).toInt()
        String.format(itemFormat, it.label, Money.formatWithCurrency(it.value, currency), percentage)
    }
    val fullDescription = stringResource(R.string.chart_ring_description, centerLabel, chartDescription)
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val reveal by rememberChartReveal(revealKey)
    Canvas(modifier = modifier.semantics { contentDescription = fullDescription }) {
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
        // 多个扇区之间留 1.5° 细缝，避免相邻色块糊在一起；入场时整体顺时针展开。
        val gap = if (slices.size > 1) 1.5f else 0f
        var startAngle = -90f
        slices.forEachIndexed { index, slice ->
            val sweep = 360f * reveal *
                slice.value.coerceAtLeast(0).toDouble().div(total.toDouble()).toFloat()
            val drawn = (sweep - gap).coerceAtLeast(0f)
            if (drawn > 0f) {
                val dimmed = selectedIndex != null && selectedIndex != index
                drawArc(
                    color = if (dimmed) slice.color.copy(alpha = 0.28f) else slice.color,
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

/** 根据点击位置找到环上的扇区；点在圆心区域返回 -1（取消选择），环外返回 null。 */
private fun ringSliceAt(offset: Offset, size: Size, slices: List<ChartSlice>, total: Long): Int? {
    val center = Offset(size.width / 2, size.height / 2)
    val radius = minOf(size.width, size.height) / 2
    val distance = hypot(offset.x - center.x, offset.y - center.y)
    if (distance < radius * 0.62f) return -1
    if (distance > radius * 1.1f) return null
    val degrees = Math.toDegrees(atan2((offset.y - center.y).toDouble(), (offset.x - center.x).toDouble()))
    val fromTop = ((degrees + 90.0) % 360.0 + 360.0) % 360.0
    var cursor = 0.0
    slices.forEachIndexed { index, slice ->
        cursor += 360.0 * slice.value.coerceAtLeast(0).toDouble() / total.toDouble()
        if (fromTop < cursor) return index
    }
    return slices.lastIndex
}

/**
 * 环形图 + 图例（图在左、图例在右）。
 * 点击扇区或图例行可选中该分类，环心显示其金额与占比；再次点击或点环心取消。
 */
@Composable
fun RingChartWithLegend(
    slices: List<ChartSlice>,
    total: Long,
    centerLabel: String = stringResource(R.string.chart_expense),
    currency: String = "CNY",
    modifier: Modifier = Modifier,
    revealKey: Any? = Unit,
) {
    val locale = chartLocale()
    val legendItemFormat = stringResource(R.string.chart_ring_legend_item)
    var selected by rememberSaveable(revealKey, slices.size) { mutableStateOf<Int?>(null) }
    val tick = rememberSelectionTick()
    val select: (Int?) -> Unit = { index ->
        val next = if (index == null || index < 0 || index == selected) null else index
        tick(next != selected)
        selected = next
    }
    val selectedSlice = selected?.let { slices.getOrNull(it) }
    ChartCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(LedgerSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(136.dp), contentAlignment = Alignment.Center) {
                RingChart(
                    slices = slices,
                    total = total,
                    centerLabel = centerLabel,
                    currency = currency,
                    selectedIndex = selected,
                    revealKey = revealKey,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(slices, total) {
                            detectTapGestures { offset ->
                                if (total > 0L) {
                                    ringSliceAt(offset, Size(size.width.toFloat(), size.height.toFloat()), slices, total)
                                        ?.let(select)
                                }
                            }
                        },
                )
                Column(
                    modifier = Modifier.widthIn(max = 92.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        selectedSlice?.label ?: centerLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        ChartAxis.readoutMoney(selectedSlice?.value ?: total, currency, locale = locale),
                        style = LedgerNumerals.of(MaterialTheme.typography.labelLarge),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (selectedSlice != null && total > 0L) {
                        Text(
                            stringResource(R.string.chart_share, percentOf(selectedSlice.value, total)),
                            style = LedgerNumerals.of(MaterialTheme.typography.labelSmall),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
            Spacer(Modifier.width(LedgerSpacing.sm))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                slices.forEachIndexed { index, slice ->
                    val isSelected = selected == index
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 30.dp)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent,
                                MaterialTheme.shapes.small,
                            )
                            .clickable(role = Role.Button) { select(index) }
                            .semantics(mergeDescendants = true) {
                                this.selected = isSelected
                                contentDescription = String.format(
                                    legendItemFormat,
                                    slice.label,
                                    Money.formatWithCurrency(slice.value, currency),
                                    percentOf(slice.value, total),
                                )
                            }
                            .padding(horizontal = 6.dp),
                    ) {
                        Canvas(modifier = Modifier.size(8.dp)) {
                            drawCircle(
                                color = if (selected != null && !isSelected) slice.color.copy(alpha = 0.35f) else slice.color,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            slice.label,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            ChartAxis.readoutMoney(slice.value, currency, locale = locale),
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

internal fun percentOf(value: Long, total: Long): Int =
    if (total <= 0L) 0 else Math.round(value.toDouble() * 100.0 / total.toDouble()).toInt()

/**
 * 图表选择手势：点按选中；长按后左右拖动连续选择。
 * 不使用普通横向拖动，避免与外层 HorizontalPager 的翻页手势冲突。
 */
internal fun Modifier.chartSelection(
    indexAt: State<(x: Float, width: Int) -> Int?>,
    onSelect: State<(Int) -> Unit>,
): Modifier = this
    .pointerInput(Unit) {
        detectTapGestures { offset ->
            indexAt.value(offset.x, size.width)?.let { onSelect.value(it) }
        }
    }
    .pointerInput(Unit) {
        detectDragGesturesAfterLongPress(
            onDragStart = { offset -> indexAt.value(offset.x, size.width)?.let { onSelect.value(it) } },
            onDrag = { change, _ ->
                change.consume()
                indexAt.value(change.position.x, size.width)?.let { onSelect.value(it) }
            },
        )
    }

/** 数值网格：1dp 实线横线 + 左侧紧凑刻度文字（右对齐、与网格线垂直居中）。 */
internal fun DrawScope.drawValueGrid(
    ticks: List<Long>,
    tickLabels: List<TextLayoutResult>,
    top: Float,
    bottom: Float,
    left: Float,
    gridColor: Color,
) {
    val topValue = ticks.lastOrNull()?.takeIf { it > 0L } ?: return
    val stroke = 1.dp.toPx()
    val labelGap = 6.dp.toPx()
    ticks.forEachIndexed { index, tick ->
        val y = bottom - (tick.toDouble() / topValue.toDouble()).toFloat() * (bottom - top)
        drawLine(gridColor, Offset(left, y), Offset(size.width, y), strokeWidth = stroke)
        tickLabels.getOrNull(index)?.let { label ->
            drawText(
                label,
                topLeft = Offset(
                    (left - labelGap - label.size.width).coerceAtLeast(0f),
                    (y - label.size.height / 2f).coerceIn(0f, size.height - label.size.height),
                ),
            )
        }
    }
}

/** 刻度文字的最大宽度（px），用于预留左侧刻度区。 */
internal fun List<TextLayoutResult>.maxLabelWidth(): Int = maxOfOrNull { it.size.width } ?: 0

/** 在 [ticks] 上测量刻度文字。 */
internal fun TextMeasurer.measureTicks(
    ticks: List<Long>,
    style: TextStyle,
    locale: Locale = Locale.getDefault(),
): List<TextLayoutResult> =
    ticks.map { measure(ChartAxis.compactLabel(it, locale), style) }

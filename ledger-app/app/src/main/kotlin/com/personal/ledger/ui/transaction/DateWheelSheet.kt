package com.personal.ledger.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.personal.ledger.R
import com.personal.ledger.ui.common.rememberHapticTick
import com.personal.ledger.util.DateLabels
import com.personal.ledger.ui.theme.LedgerNumerals
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.distinctUntilChanged

private val WheelItemHeight = 44.dp
private const val WheelVisibleItems = 5

/**
 * 年 / 月 / 日三列滚轮日期选择。只改日期，保留原账单的本地时分。
 * 三列共享一条选中带；快捷项（今天 / 昨天 / 前天）会带动滚轮平滑滚到目标日期。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateWheelSheet(
    initialTime: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val zone = remember { ZoneId.systemDefault() }
    val initialDate = remember(initialTime, zone) {
        Instant.ofEpochMilli(initialTime).atZone(zone).toLocalDate()
    }
    val today = remember(zone) { LocalDate.now(zone) }
    val years = remember(initialDate, today) {
        (minOf(initialDate.year, today.year) - 20)..(maxOf(initialDate.year, today.year) + 5)
    }
    var year by remember(initialDate) { mutableIntStateOf(initialDate.year) }
    var month by remember(initialDate) { mutableIntStateOf(initialDate.monthValue) }
    var day by remember(initialDate) { mutableIntStateOf(initialDate.dayOfMonth) }
    val lastDay = DatePickerSelection.monthLength(year, month)
    val safeDay = day.coerceAtMost(lastDay)
    val selectedDate = LocalDate.of(year, month, safeDay)
    val shortcuts = remember(today) {
        listOf(
            R.string.label_today to today,
            R.string.label_yesterday to today.minusDays(1),
            R.string.label_day_before_yesterday to today.minusDays(2),
        )
    }
    val locale = LocalConfiguration.current.locales[0]
    val yearFormat = stringResource(R.string.add_wheel_year_value)
    val dayFormat = stringResource(R.string.add_wheel_day_value)
    val tick = rememberHapticTick()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 20.dp),
        ) {
            Text(stringResource(R.string.add_choose_date), style = MaterialTheme.typography.titleLarge)
            Text(
                DatePickerSelection.longLabel(selectedDate, locale),
                style = LedgerNumerals.of(MaterialTheme.typography.bodyLarge),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                shortcuts.forEach { (label, date) ->
                    FilterChip(
                        selected = selectedDate == date,
                        onClick = {
                            year = date.year
                            month = date.monthValue
                            day = date.dayOfMonth
                        },
                        label = {
                            Text(stringResource(label), modifier = Modifier.fillMaxWidth(), maxLines = 1,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        },
                        modifier = Modifier.weight(1f).height(40.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(WheelItemHeight * WheelVisibleItems)) {
                // 三列共用一条选中带，视觉上是一行“当前日期”。
                Box(
                    Modifier.align(Alignment.Center).fillMaxWidth().height(WheelItemHeight)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.medium),
                )
                Row(Modifier.fillMaxWidth()) {
                    WheelColumn(
                        count = years.count(),
                        selectedIndex = year - years.first,
                        label = { yearFormat.format(years.first + it) },
                        description = stringResource(R.string.add_wheel_year),
                        onSelected = { index ->
                            val y = years.first + index
                            if (y != year) { year = y; tick() }
                        },
                        modifier = Modifier.weight(1.3f),
                    )
                    WheelColumn(
                        count = 12,
                        selectedIndex = month - 1,
                        label = { DateLabels.month(it + 1, locale) },
                        description = stringResource(R.string.add_wheel_month),
                        onSelected = { index ->
                            if (index + 1 != month) { month = index + 1; tick() }
                        },
                        modifier = Modifier.weight(1f),
                    )
                    WheelColumn(
                        count = lastDay,
                        selectedIndex = safeDay - 1,
                        label = { dayFormat.format(it + 1) },
                        description = stringResource(R.string.add_wheel_day),
                        onSelected = { index ->
                            if (index + 1 != safeDay) { day = index + 1; tick() }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(48.dp)) {
                    Text(stringResource(R.string.action_cancel))
                }
                Button(
                    onClick = {
                        onConfirm(DatePickerSelection.withPickedDate(initialTime, selectedDate, zone))
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                ) { Text(stringResource(R.string.action_confirm)) }
            }
        }
    }
}

/**
 * 单列滚轮。滚动中实时回报居中项；外部选中值变化（快捷项、月份天数收缩）时平滑滚动对齐。
 */
@Composable
private fun WheelColumn(
    count: Int,
    selectedIndex: Int,
    label: (Int) -> String,
    description: String,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val target = selectedIndex.coerceIn(0, count - 1)
    val state = rememberLazyListState(initialFirstVisibleItemIndex = target)
    val itemPx = with(LocalDensity.current) { WheelItemHeight.toPx() }
    val centered by remember(state, itemPx) { derivedStateOf { state.centeredIndex(itemPx) } }
    val latestOnSelected by rememberUpdatedState(onSelected)
    val latestCount by rememberUpdatedState(count)

    // 程序滚动（快捷项 / 天数收缩）期间不回报中间值，避免动画途经的年份改写选择。
    val programmatic = remember { booleanArrayOf(false) }

    // 用户滚动时持续更新选中值（居中项）；滚动停止时再确认一次最终项。
    LaunchedEffect(state) {
        snapshotFlow { state.centeredIndex(itemPx) to state.isScrollInProgress }
            .distinctUntilChanged()
            .collect { (index, _) ->
                if (!programmatic[0]) latestOnSelected(index.coerceIn(0, latestCount - 1))
            }
    }
    // 外部改值：非用户滚动时平滑对齐到目标项。
    LaunchedEffect(target, count) {
        if (state.isScrollInProgress && !programmatic[0]) return@LaunchedEffect
        if (state.centeredIndex(itemPx) != target) {
            programmatic[0] = true
            try {
                state.animateScrollToItem(target)
            } finally {
                programmatic[0] = false
            }
        }
    }

    val padding: Dp = WheelItemHeight * (WheelVisibleItems / 2)
    LazyColumn(
        state = state,
        flingBehavior = rememberSnapFlingBehavior(state, SnapPosition.Center),
        contentPadding = PaddingValues(vertical = padding),
        modifier = modifier.height(WheelItemHeight * WheelVisibleItems)
            .semantics { contentDescription = description }
            .wheelEdgeFade(),
    ) {
        items(count = count, key = { it }) { index ->
            val selected = index == centered
            Box(Modifier.fillMaxWidth().height(WheelItemHeight), contentAlignment = Alignment.Center) {
                Text(
                    label(index),
                    style = LedgerNumerals.of(
                        if (selected) MaterialTheme.typography.titleMedium
                        else MaterialTheme.typography.bodyLarge,
                    ),
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** 内容前后留白让第 N 项滚到顶部时恰好位于选中带；据此由首项与偏移推算居中项。 */
private fun LazyListState.centeredIndex(itemPx: Float): Int =
    firstVisibleItemIndex + if (firstVisibleItemScrollOffset > itemPx / 2) 1 else 0

/** 上下边缘渐隐，用透明度遮罩实现，与任何背景色都协调。 */
private fun Modifier.wheelEdgeFade(): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.3f to Color.Black,
                    0.7f to Color.Black,
                    1f to Color.Transparent,
                ),
                blendMode = BlendMode.DstIn,
            )
        }

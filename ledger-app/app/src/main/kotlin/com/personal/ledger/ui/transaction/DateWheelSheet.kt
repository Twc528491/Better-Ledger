package com.personal.ledger.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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
    var year by remember(initialDate) { mutableIntStateOf(initialDate.year) }
    var month by remember(initialDate) { mutableIntStateOf(initialDate.monthValue) }
    var day by remember(initialDate) { mutableIntStateOf(initialDate.dayOfMonth) }
    var yearScrolling by remember { mutableStateOf(false) }
    var monthScrolling by remember { mutableStateOf(false) }
    var dayScrolling by remember { mutableStateOf(false) }
    var scrollRequest by remember { mutableIntStateOf(0) }
    val today = remember(zone) { LocalDate.now(zone) }
    val lastDay = DatePickerSelection.monthLength(year, month)
    val safeDay = day.coerceAtMost(lastDay)
    val selectedDate = remember(year, month, safeDay) { LocalDate.of(year, month, safeDay) }
    val years = remember(initialDate, today) {
        val first = minOf(1900, initialDate.year - 80, today.year - 80).coerceAtLeast(1)
        val last = maxOf(2100, initialDate.year + 80, today.year + 80)
        first..last
    }
    val anyScrolling = yearScrolling || monthScrolling || dayScrolling

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val preferredHeight = (maxHeight * 0.62f).coerceAtMost(560.dp)
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(min = preferredHeight, max = maxHeight)
                    .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 24.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text("选择日期", style = MaterialTheme.typography.titleLarge)
                        Text(selectedDate.toString(), style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = {
                        year = today.year
                        month = today.monthValue
                        day = today.dayOfMonth
                        scrollRequest++
                    }) { Text("今天") }
                }
                Spacer(Modifier.height(12.dp))
                key(scrollRequest) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DateWheelColumn(
                            label = "年", modifier = Modifier.weight(1.2f),
                            itemCount = years.count(), initialIndex = year - years.first,
                            indexValue = { years.first + it }, selectedValue = year,
                            onValue = { year = it; day = DatePickerSelection.clampDay(day, it, month) },
                            onScrolling = { yearScrolling = it },
                        )
                        DateWheelColumn(
                            label = "月", modifier = Modifier.weight(1f),
                            itemCount = 12, initialIndex = month - 1,
                            indexValue = { it + 1 }, selectedValue = month,
                            onValue = { month = it; day = DatePickerSelection.clampDay(day, year, it) },
                            onScrolling = { monthScrolling = it },
                        )
                        DateWheelColumn(
                            label = "日", modifier = Modifier.weight(1f),
                            itemCount = lastDay, initialIndex = safeDay - 1,
                            indexValue = { it + 1 }, selectedValue = safeDay,
                            onValue = { day = it },
                            onScrolling = { dayScrolling = it },
                        )
                    }
                }
                Spacer(Modifier.weight(1f).height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.height(48.dp)) { Text("取消") }
                    Button(
                        enabled = !anyScrolling,
                        onClick = {
                            onConfirm(DatePickerSelection.withPickedDate(initialTime, selectedDate, zone))
                        },
                        modifier = Modifier.height(48.dp),
                    ) { Text("确定") }
                }
            }
        }
    }
}

@Composable
private fun DateWheelColumn(
    label: String,
    modifier: Modifier,
    itemCount: Int,
    initialIndex: Int,
    indexValue: (Int) -> Int,
    selectedValue: Int,
    onValue: (Int) -> Unit,
    onScrolling: (Boolean) -> Unit,
) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex.coerceIn(0, itemCount - 1))
    val flingBehavior = rememberSnapFlingBehavior(state, SnapPosition.Center)
    val centerIndex by remember(state) {
        derivedStateOf {
            val layout = state.layoutInfo
            val center = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
            layout.visibleItemsInfo.minByOrNull { item ->
                kotlin.math.abs(item.offset + item.size / 2 - center)
            }?.index ?: state.firstVisibleItemIndex
        }
    }
    val scrolling by remember(state) { derivedStateOf { state.isScrollInProgress } }
    var suppressCenterUpdates by remember { mutableStateOf(false) }
    var previousItemCount by remember { mutableIntStateOf(itemCount) }

    LaunchedEffect(centerIndex) {
        if (!suppressCenterUpdates && centerIndex in 0 until itemCount) onValue(indexValue(centerIndex))
    }
    LaunchedEffect(itemCount) {
        if (previousItemCount != itemCount) {
            previousItemCount = itemCount
            suppressCenterUpdates = true
            state.scrollToItem((selectedValue - indexValue(0)).coerceIn(0, itemCount - 1))
            suppressCenterUpdates = false
        }
    }
    LaunchedEffect(scrolling) { onScrolling(scrolling) }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Box {
            Box(
                Modifier.align(Alignment.Center).fillMaxWidth().height(48.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp)),
            )
            LazyColumn(
                state = state,
                flingBehavior = flingBehavior,
                modifier = Modifier.fillMaxWidth().height(240.dp)
                    .wheelEdgeFade(MaterialTheme.colorScheme.surface),
                contentPadding = PaddingValues(vertical = 96.dp),
            ) {
                items(count = itemCount, key = { indexValue(it) }) { index ->
                    val value = indexValue(index)
                    Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = when (label) {
                                "年" -> "${value}年"
                                "月" -> "%02d月".format(value)
                                else -> "%02d日".format(value)
                            },
                            style = if (index == centerIndex) MaterialTheme.typography.titleMedium
                                else MaterialTheme.typography.bodyMedium,
                            color = if (index == centerIndex) MaterialTheme.colorScheme.onSecondaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun Modifier.wheelEdgeFade(surface: Color): Modifier = drawWithContent {
    drawContent()
    drawRect(
        brush = Brush.verticalGradient(
            0f to surface,
            0.16f to Color.Transparent,
            0.84f to Color.Transparent,
            1f to surface,
            startY = 0f,
            endY = size.height,
        ),
        topLeft = Offset.Zero,
    )
}

package com.personal.ledger.ui.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateCalendarDialog(
    initialTime: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val zone = remember { ZoneId.systemDefault() }
    val initialDate = remember(initialTime, zone) {
        Instant.ofEpochMilli(initialTime).atZone(zone).toLocalDate()
    }
    val yearRange = remember(initialDate) { minOf(1900, initialDate.year)..maxOf(2100, initialDate.year) }
    val initialUtc = remember(initialDate) {
        DatePickerSelection.initialUtcMillis(initialTime, zone)
    }
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialUtc,
        yearRange = yearRange,
    )
    val selectedDate = pickerState.selectedDateMillis?.let {
        Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
    } ?: initialDate
    val shortcuts = remember(zone) {
        val today = LocalDate.now(zone)
        listOf("今天" to today, "昨天" to today.minusDays(1), "前天" to today.minusDays(2))
    }

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                pickerState.selectedDateMillis?.let {
                    onConfirm(DatePickerSelection.withPickedDate(initialTime, it, zone))
                }
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text("选择日期", style = MaterialTheme.typography.titleMedium)
            Text(
                "%04d-%02d-%02d".format(selectedDate.year, selectedDate.monthValue, selectedDate.dayOfMonth),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                shortcuts.forEach { (label, date) ->
                    val dateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                    FilterChip(
                        selected = pickerState.selectedDateMillis == dateMillis,
                        onClick = {
                            pickerState.selectedDateMillis = dateMillis
                            pickerState.displayedMonthMillis = date.withDayOfMonth(1)
                                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                        },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            DatePicker(
                state = pickerState,
                title = null,
                headline = null,
                showModeToggle = false,
            )
        }
    }
}

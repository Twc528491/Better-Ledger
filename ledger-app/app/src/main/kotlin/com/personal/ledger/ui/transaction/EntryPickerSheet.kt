package com.personal.ledger.ui.transaction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal data class EntryPickerOption(
    val id: String,
    val title: String,
    val detail: String? = null,
    val emoji: String = "•",
    val selected: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EntryPickerSheet(
    title: String,
    options: List<EntryPickerOption>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
    subtitle: String? = null,
    hint: String? = null,
    multiSelect: Boolean = false,
    onDone: (() -> Unit)? = null,
    clearLabel: String? = null,
    onClear: (() -> Unit)? = null,
    addLabel: String? = null,
    onAdd: ((String, (Result<Unit>) -> Unit) -> Unit)? = null,
    closeOnAddSuccess: Boolean = false,
) {
    var query by remember { mutableStateOf("") }
    var newName by remember { mutableStateOf("") }
    var addError by remember { mutableStateOf<String?>(null) }
    var isAdding by remember { mutableStateOf(false) }
    val filtered = remember(options, query) {
        if (query.isBlank()) options else options.filter {
            it.title.contains(query.trim(), ignoreCase = true) ||
                it.detail.orEmpty().contains(query.trim(), ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().imePadding()
                .padding(horizontal = 18.dp).padding(bottom = 20.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp))
            }
            hint?.let {
                Text(it, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp))
            }
            Spacer(Modifier.height(10.dp))
            if (options.size > 8) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("搜索") },
                )
                Spacer(Modifier.height(8.dp))
            }
            if (clearLabel != null && onClear != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (options.none { it.selected }) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                        .clickable(onClick = onClear),
                ) {
                    Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("○", fontSize = 22.sp, modifier = Modifier.size(38.dp))
                        Text(clearLabel, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        RadioButton(selected = options.none { it.selected }, onClick = null)
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            if (filtered.isEmpty()) {
                Text(
                    if (options.isEmpty()) "暂无可选内容" else "没有匹配的内容",
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp, max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(filtered, key = { it.id }, contentType = { "entry-choice" }) { option ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (option.selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                                .selectable(
                                    selected = option.selected,
                                    role = if (multiSelect) Role.Checkbox else Role.RadioButton,
                                    onClick = { onPick(option.id) },
                                ),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.size(38.dp),
                                ) {
                                    androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                                        Text(option.emoji, fontSize = 19.sp)
                                    }
                                }
                                Column(Modifier.weight(1f).padding(start = 11.dp, end = 8.dp)) {
                                    Text(option.title, style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    option.detail?.let {
                                        Text(it, style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                if (multiSelect) {
                                    Checkbox(checked = option.selected, onCheckedChange = null)
                                } else {
                                    RadioButton(selected = option.selected, onClick = null)
                                }
                            }
                        }
                    }
                }
            }
            if (onAdd != null && addLabel != null) {
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it; addError = null },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isAdding,
                    singleLine = true,
                    label = { Text("名称") },
                )
                addError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp))
                }
                Button(
                    enabled = !isAdding && newName.isNotBlank(),
                    onClick = {
                        isAdding = true
                        onAdd(newName.trim()) { result ->
                            isAdding = false
                            result.onSuccess {
                                newName = ""
                                if (closeOnAddSuccess) onDismiss()
                            }
                                .onFailure { addError = it.message ?: "新建失败，请重试" }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = 4.dp),
                ) { Text(if (isAdding) "创建中…" else addLabel) }
            }
            if (multiSelect) {
                Button(onClick = { onDone?.invoke() }, modifier = Modifier.fillMaxWidth()
                    .heightIn(min = 48.dp).padding(top = 4.dp)) { Text("完成") }
            }
        }
    }
}

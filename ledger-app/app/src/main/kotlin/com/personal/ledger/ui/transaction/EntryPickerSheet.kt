package com.personal.ledger.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction

internal data class EntryPickerOption(
    val id: String,
    val title: String,
    val detail: String? = null,
    val emoji: String = "•",
    val selected: Boolean = false,
    /** 自定义前置图标（如账户圆标）；为空时显示 [emoji] 文字圆标。 */
    val leading: (@Composable () -> Unit)? = null,
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
    onDone: ((Set<String>) -> Unit)? = null,
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
    var stagedIds by remember(title) { mutableStateOf(options.filter { it.selected }.map { it.id }.toSet()) }
    val visibleOptions = remember(options, stagedIds, multiSelect) {
        if (multiSelect) options.map { it.copy(selected = it.id in stagedIds) } else options
    }
    val filtered = remember(visibleOptions, query) {
        if (query.isBlank()) visibleOptions else visibleOptions.filter {
            it.title.contains(query.trim(), ignoreCase = true) ||
                it.detail.orEmpty().contains(query.trim(), ignoreCase = true)
        }
    }
    val focusManager = LocalFocusManager.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val maxSheetHeight = maxHeight * 0.75f
            val maxListHeight = (maxHeight * 0.55f).coerceAtMost(320.dp)
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = maxSheetHeight)
                    .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 16.dp),
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
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    )
                    Spacer(Modifier.height(8.dp))
                }
                if (clearLabel != null && onClear != null) {
                    val noneSelected = visibleOptions.none { it.selected }
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = if (noneSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                            .selectable(selected = noneSelected, role = Role.RadioButton, onClick = onClear),
                    ) {
                        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(36.dp).border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                            )
                            Text(clearLabel, modifier = Modifier.weight(1f).padding(start = 12.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            RadioButton(selected = noneSelected, onClick = null)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
                Box(Modifier.fillMaxWidth().heightIn(max = maxListHeight)) {
                    when {
                        filtered.isEmpty() -> Text(
                            if (options.isEmpty()) "暂无可选内容" else "没有匹配的内容",
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        filtered.size <= 5 -> Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            filtered.forEach { option ->
                                EntryChoiceRow(option, multiSelect) {
                                    if (multiSelect) stagedIds = if (option.id in stagedIds)
                                        stagedIds - option.id else stagedIds + option.id
                                    else onPick(option.id)
                                }
                            }
                        }
                        else -> LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = maxListHeight),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(filtered, key = { it.id }, contentType = { "entry-choice" }) { option ->
                                EntryChoiceRow(option, multiSelect) {
                                    if (multiSelect) stagedIds = if (option.id in stagedIds)
                                        stagedIds - option.id else stagedIds + option.id
                                    else onPick(option.id)
                                }
                            }
                        }
                    }
                }
                if (onAdd != null && addLabel != null) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it; addError = null },
                            modifier = Modifier.weight(1f),
                            enabled = !isAdding,
                            singleLine = true,
                            label = { Text("名称") },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        )
                        Button(
                            enabled = !isAdding && newName.isNotBlank(),
                            onClick = {
                                focusManager.clearFocus()
                                isAdding = true
                                onAdd(newName.trim()) { result ->
                                    isAdding = false
                                    result.onSuccess {
                                        newName = ""
                                        if (closeOnAddSuccess) onDismiss()
                                    }.onFailure { addError = it.message ?: "新建失败，请重试" }
                                }
                            },
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) { Text(if (isAdding) "创建中…" else addLabel) }
                    }
                    addError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { focusManager.clearFocus(); onDismiss() },
                        modifier = Modifier.heightIn(min = 48.dp)) { Text("取消") }
                    if (multiSelect) {
                        Button(onClick = { focusManager.clearFocus(); onDone?.invoke(stagedIds) },
                            modifier = Modifier.heightIn(min = 48.dp)) { Text("完成") }
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryChoiceRow(option: EntryPickerOption, multiSelect: Boolean, onClick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (option.selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .selectable(
                selected = option.selected,
                role = if (multiSelect) Role.Checkbox else Role.RadioButton,
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val leading = option.leading
            if (leading != null) {
                leading()
            } else {
                Box(
                    Modifier.size(36.dp).background(MaterialTheme.colorScheme.surfaceContainerLowest, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(option.emoji, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
            Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
                Text(option.title, style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (option.selected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                option.detail?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (multiSelect) Checkbox(checked = option.selected, onCheckedChange = null)
            else RadioButton(selected = option.selected, onClick = null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NoteEditorSheet(
    initialNote: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var note by remember(initialNote) { mutableStateOf(initialNote) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val close = {
        focusManager.clearFocus()
        keyboardController?.hide()
        onDismiss()
    }
    ModalBottomSheet(
        onDismissRequest = close,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val maxSheetHeight = maxHeight * 0.75f
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = maxSheetHeight)
                    .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 16.dp),
            ) {
                Text("备注", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("为这笔账单添加说明", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp))
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 180.dp),
                    minLines = 3,
                    maxLines = 5,
                    label = { Text("备注内容") },
                    keyboardOptions = KeyboardOptions(
                        capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences,
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }),
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = close, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("取消")
                    }
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            onConfirm(note)
                        },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) { Text("完成") }
                }
            }
        }
    }
}

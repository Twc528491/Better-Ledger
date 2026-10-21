package io.github.twc528491.betterledger.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.ui.res.stringResource
import io.github.twc528491.betterledger.R
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.twc528491.betterledger.domain.model.CategoryType
import io.github.twc528491.betterledger.ui.common.CategoryBadge
import io.github.twc528491.betterledger.ui.common.CategoryIconLibrary
import io.github.twc528491.betterledger.ui.common.CategoryVisuals
import io.github.twc528491.betterledger.ui.common.LibraryIcon
import io.github.twc528491.betterledger.ui.common.insetColor
import io.github.twc528491.betterledger.ui.theme.LedgerSpacing

/**
 * 分类编辑面板：名称（可选）+ 图标库选择。
 * 新建分类与“更换图标”共用；[selectedIconKey] 为 null 表示“自动”（按名称匹配图标）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CategoryIconSheet(
    title: String,
    type: CategoryType,
    initialName: String,
    initialIconKey: String?,
    nameEditable: Boolean,
    confirmLabel: String,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, iconKey: String?) -> Unit,
    visualKey: String? = null,
    subtitle: String? = null,
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedIconKey by remember { mutableStateOf(initialIconKey) }
    val defaultPreviewName = stringResource(R.string.set_category_preview_name)
    val previewName = name.trim().ifEmpty { defaultPreviewName }
    val canConfirm = !nameEditable || name.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val maxSheetHeight = maxHeight * 0.9f
            val fontScale = LocalDensity.current.fontScale
            // 常规宽度 6 列；窄屏或大字体减少列数，保证每格 ≥ 48dp 且标签可读。
            val columns = when {
                maxWidth >= 352.dp && fontScale < 1.3f -> 6
                maxWidth >= 300.dp && fontScale < 1.6f -> 5
                else -> 4
            }
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = maxSheetHeight)
                    .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 16.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                subtitle?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
                Spacer(Modifier.height(LedgerSpacing.sm))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryBadge(
                        name = previewName,
                        type = type,
                        sizeDp = 52,
                        visualKey = visualKey ?: previewName,
                        iconKey = selectedIconKey,
                    )
                    Spacer(Modifier.width(LedgerSpacing.sm))
                    if (nameEditable) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(stringResource(R.string.set_category_name)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Column(Modifier.weight(1f)) {
                            Text(
                                previewName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                CategoryIconLibrary.find(selectedIconKey)?.label ?: stringResource(R.string.set_icon_auto_match),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = LedgerSpacing.xs),
                    )
                }
                Spacer(Modifier.height(LedgerSpacing.sm))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                    contentPadding = PaddingValues(bottom = LedgerSpacing.xs),
                ) {
                    item(key = "auto", span = { GridItemSpan(maxLineSpan) }) {
                        AutoIconOption(
                            icon = CategoryVisuals.iconFor(previewName, type, null),
                            selected = selectedIconKey == null,
                            onClick = { selectedIconKey = null },
                        )
                    }
                    CategoryIconLibrary.groups.forEach { (groupTitle, icons) ->
                        item(key = "group-$groupTitle", span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                stringResource(groupTitle),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(
                                    start = LedgerSpacing.xxs,
                                    top = LedgerSpacing.sm,
                                    bottom = LedgerSpacing.xxs,
                                ),
                            )
                        }
                        items(icons, key = { it.key }) { icon ->
                            LibraryIconCell(
                                icon = icon,
                                selected = selectedIconKey == icon.key,
                                onClick = { selectedIconKey = icon.key },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(LedgerSpacing.sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.sm, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    Button(
                        onClick = { if (canConfirm) onConfirm(name.trim(), selectedIconKey) },
                        enabled = canConfirm,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) { Text(confirmLabel) }
                }
            }
        }
    }
}

@Composable
private fun AutoIconOption(icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = LedgerSpacing.xxs, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconCircle(icon = icon, contentDescription = null, selected = selected)
        Column(Modifier.weight(1f).padding(start = LedgerSpacing.sm)) {
            Text(
                stringResource(R.string.set_icon_auto),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                stringResource(R.string.set_icon_auto_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LibraryIconCell(icon: LibraryIcon, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconCircle(icon = icon.icon, contentDescription = icon.label, selected = selected)
        Text(
            icon.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = LedgerSpacing.xxs, start = 2.dp, end = 2.dp)
                .clearAndSetSemantics { },
        )
    }
}

@Composable
private fun IconCircle(icon: ImageVector, contentDescription: String?, selected: Boolean) {
    val colors = MaterialTheme.colorScheme
    val base = Modifier
        .size(44.dp)
        .clip(CircleShape)
        .background(if (selected) colors.primaryContainer else insetColor(), CircleShape)
    Box(
        modifier = if (selected) base.border(2.dp, colors.primary, CircleShape) else base,
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
    }
}

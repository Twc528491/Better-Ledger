package com.personal.ledger.ui.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val MultiSelectionSaver = listSaver<MultiSelection, String>(
    save = { listOf(if (it.active) "1" else "0") + it.selected },
    restore = { saved -> MultiSelection(active = saved.firstOrNull() == "1", selected = saved.drop(1).toSet()) },
)

/** 多选状态：跨重组与配置变更保留。 */
@Composable
fun rememberMultiSelection(): MutableState<MultiSelection> =
    rememberSaveable(stateSaver = MultiSelectionSaver) { mutableStateOf(MultiSelection.None) }

/** 选中行的底色：主色浅调，深浅色主题下都不会压过金额颜色。 */
@Composable
fun selectionHighlightColor(): Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)

/** 选择模式的上下文顶栏：关闭、已选数量、全选 / 取消全选，以及调用方的批量操作。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopBar(
    count: Int,
    allSelected: Boolean,
    onClose: () -> Unit,
    onToggleAll: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    TopAppBar(
        title = { Text("已选 $count 项") },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "退出多选")
            }
        },
        actions = {
            TextButton(onClick = onToggleAll) {
                Text(if (allSelected) "取消全选" else "全选")
            }
            actions()
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    )
}

/** 选择圈：选中为主色实心圆加对勾，未选中为描边空心圆。 */
@Composable
fun SelectionCheckCircle(selected: Boolean, modifier: Modifier = Modifier, sizeDp: Int = 22) {
    AnimatedContent(
        targetState = selected,
        transitionSpec = { (fadeIn(tween(120)) + scaleIn(tween(160), initialScale = 0.7f)) togetherWith fadeOut(tween(90)) },
        modifier = modifier.size(sizeDp.dp),
        contentAlignment = Alignment.Center,
        label = "selection-check",
    ) { checked ->
        if (checked) {
            Box(
                Modifier.size(sizeDp.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size((sizeDp * 0.62f).dp),
                )
            }
        } else {
            Box(
                Modifier.size(sizeDp.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape),
            )
        }
    }
}

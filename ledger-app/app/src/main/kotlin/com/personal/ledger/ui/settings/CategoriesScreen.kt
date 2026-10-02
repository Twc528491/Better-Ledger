package com.personal.ledger.ui.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.personal.ledger.data.AppContainer
import com.personal.ledger.data.repository.CategoryPurgePlan
import com.personal.ledger.domain.calc.PurgeRules
import com.personal.ledger.domain.model.Category
import com.personal.ledger.domain.model.CategoryType
import com.personal.ledger.ui.common.CategoryBadge
import com.personal.ledger.ui.common.LedgerGroupItem
import com.personal.ledger.ui.common.LedgerGroupLabel
import com.personal.ledger.ui.theme.LedgerSpacing
import com.personal.ledger.ui.common.rememberHapticConfirm
import com.personal.ledger.ui.common.rememberHapticDelete
import com.personal.ledger.ui.common.rememberHapticLight
import com.personal.ledger.ui.common.rememberHapticLongPress
import com.personal.ledger.ui.common.rememberHapticNavigation
import com.personal.ledger.ui.common.rememberHapticReject
import com.personal.ledger.viewmodel.CategoriesViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CategoriesScreen(container: AppContainer, navController: NavHostController) {
    val vm: CategoriesViewModel = viewModel {
        CategoriesViewModel(container.ledgerRepository, container.categoryRepository)
    }
    val state by vm.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var addParentId by remember { mutableStateOf<String?>(null) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var iconCategory by remember { mutableStateOf<Category?>(null) }
    var actionCategory by remember { mutableStateOf<Category?>(null) }
    var deleteCandidate by remember { mutableStateOf<Category?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var showSupplementDialog by remember { mutableStateOf(false) }
    var supplementMessage by remember { mutableStateOf<String?>(null) }
    val hapticLight = rememberHapticLight()
    val hapticLongPress = rememberHapticLongPress()
    val hapticNavigation = rememberHapticNavigation()
    val hapticConfirm = rememberHapticConfirm()
    val hapticDelete = rememberHapticDelete()
    val hapticReject = rememberHapticReject()
    val activeCategories = remember(state.categories) {
        state.categories.filterNot { it.isArchived }
    }
    // 已归档的一级分类在前，其已归档二级分类紧随其后；父分类仍在使用的已归档二级分类排在最后
    val archivedCategories = remember(state.categories) {
        val archived = state.categories.filter { it.isArchived }
        val roots = archived.filter { it.parentId == null }
        val grouped = roots.flatMap { root -> listOf(root) + archived.filter { it.parentId == root.id } }
        grouped + archived.filter { it !in grouped }
    }
    var purgeCategory by remember { mutableStateOf<Category?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("分类管理") },
                navigationIcon = {
                    IconButton(onClick = {
                        hapticNavigation()
                        navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        hapticNavigation()
                        addParentId = null
                        error = null
                        showAddDialog = true
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "新建一级分类")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = LedgerSpacing.md, end = LedgerSpacing.xs, top = LedgerSpacing.xxs, bottom = LedgerSpacing.xxs),
                horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = state.type == CategoryType.EXPENSE,
                    onClick = {
                        hapticLight()
                        vm.setType(CategoryType.EXPENSE)
                    },
                    label = { Text("支出") },
                )
                FilterChip(
                    selected = state.type == CategoryType.INCOME,
                    onClick = {
                        hapticLight()
                        vm.setType(CategoryType.INCOME)
                    },
                    label = { Text("收入") },
                )
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = {
                        supplementMessage = null
                        showSupplementDialog = true
                    },
                ) {
                    Text("补充常用分类")
                }
            }
            supplementMessage?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = LedgerSpacing.md, vertical = 2.dp),
                )
            }

            // 分组展示：每个一级分类与其二级分类组成一张卡片
            val groups = remember(activeCategories) {
                activeCategories.filter { it.parentId == null }.map { root ->
                    root to activeCategories.filter { it.parentId == root.id }
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(
                    start = LedgerSpacing.md,
                    top = LedgerSpacing.xs,
                    end = LedgerSpacing.md,
                    bottom = LedgerSpacing.xl,
                ),
            ) {
                groups.forEachIndexed { groupIndex, (root, children) ->
                    val members = listOf(root) + children
                    members.forEachIndexed { memberIndex, cat ->
                        item(key = cat.id) {
                            val siblings = activeCategories.filter {
                                it.parentId == cat.parentId
                            }
                            val index = siblings.indexOfFirst { it.id == cat.id }
                            LedgerGroupItem(
                                index = memberIndex,
                                count = members.size,
                                modifier = if (memberIndex == 0 && groupIndex > 0) {
                                    Modifier.padding(top = LedgerSpacing.sm)
                                } else {
                                    Modifier
                                },
                            ) {
                                CategoryRow(
                                    category = cat,
                                    isSub = cat.parentId != null,
                                    canMoveUp = index > 0,
                                    canMoveDown = index >= 0 && index < siblings.lastIndex,
                                    onMoveUp = {
                                        hapticLight()
                                        vm.move(cat.id, -1)
                                    },
                                    onMoveDown = {
                                        hapticLight()
                                        vm.move(cat.id, 1)
                                    },
                                    onLongClick = {
                                        hapticLongPress()
                                        actionCategory = cat
                                    },
                                    onAddChild = if (cat.parentId == null && !cat.isArchived) ({
                                        hapticNavigation()
                                        addParentId = cat.id
                                        error = null
                                        showAddDialog = true
                                    }) else null,
                                )
                            }
                        }
                    }
                }

                // 已删除（归档）的分类：可恢复或彻底删除
                if (archivedCategories.isNotEmpty()) {
                    item(key = "archived_label") {
                        LedgerGroupLabel("已删除的分类", modifier = Modifier.padding(top = LedgerSpacing.md))
                    }
                    archivedCategories.forEachIndexed { index, cat ->
                        item(key = "archived_" + cat.id) {
                            LedgerGroupItem(index = index, count = archivedCategories.size) {
                                ArchivedCategoryRow(
                                    category = cat,
                                    displayName = PurgeRules.displayName(cat, state.categories),
                                    onRestore = {
                                        vm.restoreCategory(cat.id) { result ->
                                            result.onSuccess { hapticConfirm() }.onFailure { hapticReject() }
                                        }
                                    },
                                    onPurge = {
                                        hapticConfirm()
                                        purgeCategory = cat
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    purgeCategory?.let { category ->
        CategoryPurgeDialog(
            container = container,
            category = category,
            onDismiss = { purgeCategory = null },
        )
    }

    if (showSupplementDialog) {
        AlertDialog(
            onDismissRequest = { showSupplementDialog = false },
            title = { Text("补充常用分类") },
            text = {
                Text("只添加账本中尚未存在的常用分类。已归档或已改名的分类会保留，不会恢复或覆盖。")
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.supplementCommonCategories { result ->
                        result.onSuccess { count ->
                            supplementMessage = if (count == 0) {
                                "常用分类已齐全"
                            } else {
                                "已补充 " + count + " 个常用分类"
                            }
                            showSupplementDialog = false
                        }.onFailure {
                            supplementMessage = it.message ?: "补充失败"
                            showSupplementDialog = false
                        }
                    }
                }) {
                    Text("补充")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSupplementDialog = false }) { Text("取消") }
            },
        )
    }

    if (showAddDialog) {
        val parentName = addParentId?.let { id -> state.categories.firstOrNull { it.id == id }?.name }
        CategoryIconSheet(
            title = if (addParentId == null) "新建一级分类" else "新建二级分类",
            subtitle = parentName?.let { "属于“$it”" },
            type = state.type,
            initialName = "",
            initialIconKey = null,
            nameEditable = true,
            confirmLabel = "创建",
            error = error,
            onDismiss = {
                showAddDialog = false
                error = null
            },
            onConfirm = { name, iconKey ->
                vm.addCategory(name, addParentId, iconKey) { result ->
                    result.onSuccess {
                        hapticConfirm()
                        showAddDialog = false
                        error = null
                    }
                        .onFailure {
                            hapticReject()
                            error = it.message ?: "创建失败"
                        }
                }
            },
        )
    }

    iconCategory?.let { category ->
        key(category.id) {
            CategoryIconSheet(
                title = "更换图标",
                type = category.type,
                initialName = category.name,
                initialIconKey = category.iconKey,
                nameEditable = false,
                visualKey = category.id,
                confirmLabel = "保存",
                error = error,
                onDismiss = {
                    iconCategory = null
                    error = null
                },
                onConfirm = { _, iconKey ->
                    vm.setIconKey(category.id, iconKey) { result ->
                        result.onSuccess {
                            hapticConfirm()
                            iconCategory = null
                            error = null
                        }.onFailure {
                            hapticReject()
                            error = it.message ?: "保存失败"
                        }
                    }
                },
            )
        }
    }

    editingCategory?.let { category ->
        var name by remember(category.id) { mutableStateOf(category.name) }
        AlertDialog(
            onDismissRequest = { editingCategory = null },
            title = { Text("重命名分类") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("分类名称") },
                    singleLine = true,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        vm.rename(category.id, name.trim()) { result ->
                            result.onSuccess {
                                hapticConfirm()
                                editingCategory = null
                                error = null
                            }
                                .onFailure {
                                    hapticReject()
                                    error = it.message ?: "保存失败"
                                }
                        }
                    }
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { editingCategory = null }) { Text("取消") }
            },
        )
    }

    actionCategory?.let { category ->
        AlertDialog(
            onDismissRequest = { actionCategory = null },
            title = { Text(category.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            text = {
                Column {
                    CategoryActionItem(icon = Icons.Outlined.Palette, label = "更换图标") {
                        hapticNavigation()
                        actionCategory = null
                        error = null
                        iconCategory = category
                    }
                    CategoryActionItem(icon = Icons.Outlined.Edit, label = "重命名") {
                        hapticNavigation()
                        actionCategory = null
                        error = null
                        editingCategory = category
                    }
                    CategoryActionItem(
                        icon = Icons.Outlined.Delete,
                        label = "删除",
                        color = MaterialTheme.colorScheme.error,
                    ) {
                        hapticConfirm()
                        actionCategory = null
                        error = null
                        deleteCandidate = category
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { actionCategory = null }) { Text("取消") }
            },
        )
    }

    deleteCandidate?.let { category ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("确认删除") },
            text = {
                Text(
                    "删除“${category.name}”后，历史账单和预算仍会保留分类引用。" +
                        (if (category.parentId == null) "其二级分类也会一并停用。" else "") +
                        "之后可在列表底部“已删除的分类”中恢复或彻底删除。",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    hapticDelete()
                    vm.deleteCategory(category.id) { result ->
                        result.onSuccess {
                            deleteCandidate = null
                            error = null
                        }.onFailure {
                            hapticReject()
                            error = it.message ?: "删除失败"
                        }
                    }
                }) {
                    Text("确认删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun CategoryRow(
    category: Category,
    isSub: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onLongClick: () -> Unit,
    onAddChild: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick,
                hapticFeedbackEnabled = false,
            )
            .heightIn(min = if (isSub) 52.dp else 60.dp)
            .padding(start = if (isSub) 28.dp else LedgerSpacing.md, end = LedgerSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryBadge(
            name = category.name,
            type = category.type,
            visualKey = category.id,
            sizeDp = if (isSub) 32 else 40,
            iconKey = category.iconKey,
        )
        Text(
            category.name,
            modifier = Modifier.weight(1f).padding(start = LedgerSpacing.sm),
            style = if (isSub) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSub) FontWeight.Normal else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val actionTint = MaterialTheme.colorScheme.onSurfaceVariant
        IconButton(onClick = onMoveUp, enabled = canMoveUp) {
            Icon(
                Icons.Filled.KeyboardArrowUp,
                contentDescription = "上移${category.name}",
                tint = if (canMoveUp) actionTint else actionTint.copy(alpha = 0.3f),
            )
        }
        IconButton(onClick = onMoveDown, enabled = canMoveDown) {
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = "下移${category.name}",
                tint = if (canMoveDown) actionTint else actionTint.copy(alpha = 0.3f),
            )
        }
        if (onAddChild != null) {
            IconButton(onClick = onAddChild) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "为${category.name}添加二级分类",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun CategoryActionItem(
    icon: ImageVector,
    label: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .heightIn(min = 52.dp)
            .padding(horizontal = LedgerSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = color)
        Text(
            label,
            color = color,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = LedgerSpacing.md),
        )
    }
}

@Composable
private fun ArchivedCategoryRow(
    category: Category,
    displayName: String,
    onRestore: () -> Unit,
    onPurge: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(start = LedgerSpacing.md, end = LedgerSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryBadge(
            name = category.name,
            type = category.type,
            visualKey = category.id,
            sizeDp = 32,
            iconKey = category.iconKey,
        )
        Text(
            displayName,
            modifier = Modifier.weight(1f).padding(start = LedgerSpacing.sm),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        TextButton(onClick = onRestore) { Text("恢复") }
        TextButton(onClick = onPurge) { Text("彻底删除", color = MaterialTheme.colorScheme.error) }
    }
}

/** 彻底删除分类：被交易 / 定时扣款引用时必须选择同类型的替代分类。 */
@Composable
private fun CategoryPurgeDialog(
    container: AppContainer,
    category: Category,
    onDismiss: () -> Unit,
) {
    var plan by remember(category.id) { mutableStateOf<CategoryPurgePlan?>(null) }
    var loadError by remember(category.id) { mutableStateOf<String?>(null) }
    var replacementId by remember(category.id) { mutableStateOf<String?>(null) }
    val hapticLight = rememberHapticLight()
    LaunchedEffect(category.id) {
        runCatching { container.purgeRepository.categoryPlan(category.id) }
            .onSuccess { plan = it }
            .onFailure { loadError = it.message ?: "无法读取分类" }
    }
    val current = plan
    val impact = when {
        loadError != null -> loadError.orEmpty()
        current == null -> "正在统计影响范围…"
        current.blocked -> "“${category.name}”下还有 ${current.activeChildren.size} 个使用中的二级分类（" +
            current.activeChildren.joinToString("、") { it.name } +
            "），请先删除或停用它们，再彻底删除这个一级分类。"
        else -> buildString {
            append("将永久删除分类“${category.name}”")
            val children = current.removed.filter { it.id != category.id }
            if (children.isNotEmpty()) {
                append("及其已删除的二级分类（")
                append(children.joinToString("、") { it.name })
                append("）")
            }
            append("。")
            if (current.needsReplacement) {
                append("有 ${current.transactionCount} 笔账单（含回收站）和 ${current.recurringCount} 条定时扣款使用这些分类，")
                append("需要迁移到下面选择的替代分类。")
            }
            if (current.budgetCount > 0) {
                append("相关的 ${current.budgetCount} 条分类预算将被删除。")
            } else {
                append("这些分类的预算（如有）将被删除。")
            }
            append("商家与这些分类的关联也会被移除。")
        }
    }
    val confirmEnabled = current != null && !current.blocked &&
        (!current.needsReplacement || replacementId != null)

    PurgeDialog(
        container = container,
        title = "彻底删除分类",
        impact = impact,
        confirmEnabled = confirmEnabled,
        onDismiss = onDismiss,
        onPurged = onDismiss,
        purge = { container.purgeRepository.purgeCategory(category.id, replacementId) },
    ) {
        if (current != null && !current.blocked && current.needsReplacement) {
            if (current.replacementCandidates.isEmpty()) {
                Text(
                    "没有可用的同类型分类，请先新建一个分类。",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                Text(
                    "替代分类",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(
                    modifier = Modifier
                        .heightIn(max = 220.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    current.replacementCandidates.forEach { candidate ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .clickable {
                                    hapticLight()
                                    replacementId = candidate.id
                                }
                                .heightIn(min = 44.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = replacementId == candidate.id, onClick = null)
                            Text(
                                PurgeRules.displayName(candidate, current.allCategories),
                                modifier = Modifier.padding(start = LedgerSpacing.xs),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}

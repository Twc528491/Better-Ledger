package com.personal.ledger.ui.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.personal.ledger.data.AppContainer
import com.personal.ledger.domain.model.Category
import com.personal.ledger.domain.model.CategoryType
import com.personal.ledger.ui.common.CategoryBadge
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
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
            }
            TextButton(
                onClick = {
                    supplementMessage = null
                    showSupplementDialog = true
                },
                modifier = Modifier.padding(start = 8.dp),
            ) {
                Text("补充常用分类")
            }
            supplementMessage?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                )
            }

            // 平铺展示：一级分类后紧跟其二级分类
            val display = remember(activeCategories) {
                buildList {
                    activeCategories.filter { it.parentId == null }.forEach { root ->
                        add(root)
                        activeCategories.filter { it.parentId == root.id }.forEach { add(it) }
                    }
                }
            }

            LazyColumn {
                items(display, key = { it.id }) { cat ->
                    val siblings = activeCategories.filter {
                        it.parentId == cat.parentId
                    }
                    val index = siblings.indexOfFirst { it.id == cat.id }
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
                            showAddDialog = true
                        }) else null,
                    )
                    HorizontalDivider()
                }
            }
        }
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
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(if (addParentId == null) "新建一级分类" else "新建二级分类") },
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
                        vm.addCategory(name, addParentId) { result ->
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
                    }
                }) { Text("创建") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("取消") }
            },
        )
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
            title = { Text("分类操作") },
            text = { Text(category.name) },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = {
                        hapticNavigation()
                        actionCategory = null
                        error = null
                        editingCategory = category
                    }) { Text("重命名") }
                    TextButton(onClick = {
                        hapticConfirm()
                        actionCategory = null
                        error = null
                        deleteCandidate = category
                    }) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
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
                        if (category.parentId == null) "其二级分类也会一并停用。" else "",
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
            .padding(start = if (isSub) 36.dp else 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryBadge(
            name = category.name,
            type = category.type,
            visualKey = category.id,
            sizeDp = if (isSub) 32 else 38,
        )
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                category.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSub) FontWeight.Normal else FontWeight.SemiBold,
            )
            Text(
                if (isSub) "二级分类" else "一级分类",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        IconButton(onClick = onMoveUp, enabled = canMoveUp) {
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "上移${category.name}")
        }
        IconButton(onClick = onMoveDown, enabled = canMoveDown) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "下移${category.name}")
        }
        if (onAddChild != null) {
            IconButton(onClick = onAddChild) {
                Icon(Icons.Filled.Add, contentDescription = "为${category.name}添加二级分类")
            }
        }
    }
}

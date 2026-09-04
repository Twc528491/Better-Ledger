package com.personal.ledger.ui.settings

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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.personal.ledger.ui.common.rememberHapticLight
import com.personal.ledger.ui.common.rememberHapticNavigation
import com.personal.ledger.ui.common.rememberHapticReject
import com.personal.ledger.viewmodel.CategoriesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(container: AppContainer, navController: NavHostController) {
    val vm: CategoriesViewModel = viewModel {
        CategoriesViewModel(container.ledgerRepository, container.categoryRepository)
    }
    val state by vm.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var addParentId by remember { mutableStateOf<String?>(null) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    val hapticLight = rememberHapticLight()
    val hapticNavigation = rememberHapticNavigation()
    val hapticConfirm = rememberHapticConfirm()
    val hapticReject = rememberHapticReject()

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

            // 平铺展示：一级分类后紧跟其二级分类
            val display = remember(state.categories) {
                buildList {
                    state.categories.filter { it.parentId == null }.forEach { root ->
                        add(root)
                        state.categories.filter { it.parentId == root.id }.forEach { add(it) }
                    }
                }
            }

            LazyColumn {
                items(display, key = { it.id }) { cat ->
                    val siblings = state.categories.filter {
                        it.parentId == cat.parentId && it.isArchived == cat.isArchived
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
                        onEdit = {
                            hapticNavigation()
                            editingCategory = cat
                        },
                        onToggleArchive = {
                            vm.setArchived(cat.id, !cat.isArchived) { result ->
                                result.onSuccess { hapticConfirm() }
                                    .onFailure { hapticReject() }
                            }
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

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = {
                hapticNavigation()
                showAddDialog = false
            },
            title = { Text(if (addParentId == null) "新建一级分类" else "新建二级分类") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("分类名称") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        vm.addCategory(name, addParentId) { result ->
                            result.onSuccess {
                                hapticConfirm()
                                showAddDialog = false
                            }
                                .onFailure { hapticReject() }
                        }
                    }
                }) { Text("创建") }
            },
            dismissButton = {
                TextButton(onClick = {
                    hapticNavigation()
                    showAddDialog = false
                }) { Text("取消") }
            },
        )
    }

    editingCategory?.let { category ->
        var name by remember(category.id) { mutableStateOf(category.name) }
        AlertDialog(
            onDismissRequest = {
                hapticNavigation()
                editingCategory = null
            },
            title = { Text("重命名分类") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("分类名称") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        vm.rename(category.id, name.trim()) { result ->
                            result.onSuccess {
                                hapticConfirm()
                                editingCategory = null
                            }
                                .onFailure { hapticReject() }
                        }
                    }
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = {
                    hapticNavigation()
                    editingCategory = null
                }) { Text("取消") }
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
    onEdit: () -> Unit,
    onToggleArchive: () -> Unit,
    onAddChild: (() -> Unit)?,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val hapticNavigation = rememberHapticNavigation()
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
                when {
                    category.isArchived -> "已归档"
                    isSub -> "二级分类"
                    else -> "一级分类"
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }
        IconButton(onClick = onMoveUp, enabled = canMoveUp) {
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "上移${category.name}")
        }
        IconButton(onClick = onMoveDown, enabled = canMoveDown) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "下移${category.name}")
        }
        androidx.compose.foundation.layout.Box {
        IconButton(onClick = {
            hapticNavigation()
            menuExpanded = true
        }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "${category.name}更多操作")
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text("重命名") },
                    onClick = {
                        menuExpanded = false
                        onEdit()
                    },
                )
                if (onAddChild != null) {
                    DropdownMenuItem(
                        text = { Text("新建二级分类") },
                        onClick = {
                            menuExpanded = false
                            onAddChild()
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text(if (category.isArchived) "恢复" else "归档") },
                    onClick = {
                        menuExpanded = false
                        onToggleArchive()
                    },
                )
            }
        }
    }
}

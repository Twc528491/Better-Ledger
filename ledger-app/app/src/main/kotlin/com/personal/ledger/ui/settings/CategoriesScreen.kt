package com.personal.ledger.ui.settings

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.personal.ledger.data.AppContainer
import com.personal.ledger.domain.model.Category
import com.personal.ledger.domain.model.CategoryType
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("分类管理") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
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
                    onClick = { vm.setType(CategoryType.EXPENSE) },
                    label = { Text("支出") },
                )
                FilterChip(
                    selected = state.type == CategoryType.INCOME,
                    onClick = { vm.setType(CategoryType.INCOME) },
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
                    CategoryRow(cat, isSub = cat.parentId != null) {
                        vm.setArchived(cat.id, !cat.isArchived) {}
                    }
                    if (cat.parentId == null) {
                        TextButton(onClick = {
                            addParentId = cat.id
                            showAddDialog = true
                        }) { Text("+ 二级分类") }
                    }
                    HorizontalDivider()
                }
            }
        }
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
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        vm.addCategory(name, addParentId) { result ->
                            result.onSuccess { showAddDialog = false }
                        }
                    }
                }) { Text("创建") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun CategoryRow(category: Category, isSub: Boolean, onToggleArchive: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleArchive)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            (if (isSub) "    " else "") + category.name + if (category.isArchived) "（已归档）" else "",
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            if (category.isArchived) "恢复" else "归档",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

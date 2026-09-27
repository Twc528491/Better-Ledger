package com.personal.ledger.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.personal.ledger.data.AppContainer
import com.personal.ledger.ui.common.EmptyState
import com.personal.ledger.ui.common.LedgerGroupItem
import com.personal.ledger.ui.common.LedgerIconTile
import com.personal.ledger.ui.common.LedgerListRow
import com.personal.ledger.ui.theme.LedgerSpacing
import com.personal.ledger.ui.common.rememberHapticConfirm
import com.personal.ledger.ui.common.rememberHapticNavigation
import com.personal.ledger.ui.common.rememberHapticReject
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsScreen(container: AppContainer, navController: NavHostController) {
    val scope = rememberCoroutineScope()
    val currentLedger by container.ledgerRepository.currentLedger.collectAsStateWithLifecycle(initialValue = null)
    val tags by container.tagRepository.observeByLedger(
        currentLedger?.id ?: ""
    ).collectAsStateWithLifecycle(initialValue = emptyList())
    var showAdd by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val hapticNavigation = rememberHapticNavigation()
    val hapticConfirm = rememberHapticConfirm()
    val hapticReject = rememberHapticReject()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("标签管理") },
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
                        showAdd = true
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "新建标签")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(
                start = LedgerSpacing.md,
                top = LedgerSpacing.xs,
                end = LedgerSpacing.md,
                bottom = LedgerSpacing.xl,
            ),
        ) {
            if (tags.isEmpty()) {
                item(key = "tags_empty") {
                    EmptyState(
                        icon = Icons.Outlined.Sell,
                        title = "暂无标签",
                        subtitle = "点右上角 + 新建",
                    )
                }
            }
            val count = tags.size
            itemsIndexed(tags, key = { _, tag -> tag.id }) { index, tag ->
                LedgerGroupItem(index = index, count = count) {
                    LedgerListRow(
                        title = tag.name + if (tag.isArchived) "（已归档）" else "",
                        titleColor = if (tag.isArchived) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        leading = {
                            LedgerIconTile(
                                Icons.Outlined.Sell,
                                tint = if (tag.isArchived) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                            )
                        },
                        trailing = {
                            Text(
                                if (tag.isArchived) "恢复" else "归档",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        },
                        onClick = {
                            scope.launch {
                                container.tagRepository.setArchived(tag.id, !tag.isArchived)
                                    .onSuccess { hapticConfirm() }
                                    .onFailure { hapticReject() }
                            }
                        },
                    )
                }
            }
        }
    }

    if (showAdd) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("新建标签") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("标签名称（同账本内唯一）") },
                        singleLine = true,
                    )
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val ledgerId = currentLedger?.id ?: return@TextButton
                    scope.launch {
                        container.tagRepository.addTag(ledgerId, name)
                            .onSuccess {
                                hapticConfirm()
                                showAdd = false
                                error = null
                            }
                            .onFailure {
                                hapticReject()
                                error = it.message
                            }
                    }
                }) { Text("创建") }
            },
            dismissButton = {
                TextButton(onClick = { showAdd = false }) { Text("取消") }
            },
        )
    }
}

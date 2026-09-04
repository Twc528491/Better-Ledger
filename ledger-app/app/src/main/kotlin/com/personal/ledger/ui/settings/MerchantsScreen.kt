package com.personal.ledger.ui.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.personal.ledger.data.AppContainer
import com.personal.ledger.domain.model.Merchant
import com.personal.ledger.ui.common.rememberHapticConfirm
import com.personal.ledger.ui.common.rememberHapticDelete
import com.personal.ledger.ui.common.rememberHapticLongPress
import com.personal.ledger.ui.common.rememberHapticNavigation
import com.personal.ledger.ui.common.rememberHapticReject
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MerchantsScreen(container: AppContainer, navController: NavHostController) {
    val scope = rememberCoroutineScope()
    val currentLedger by container.ledgerRepository.currentLedger.collectAsStateWithLifecycle(initialValue = null)
    val merchants by container.merchantRepository.observeByLedger(
        currentLedger?.id ?: "",
    ).collectAsStateWithLifecycle(initialValue = emptyList())
    var showAdd by remember { mutableStateOf(false) }
    var editingMerchant by remember { mutableStateOf<Merchant?>(null) }
    var actionMerchant by remember { mutableStateOf<Merchant?>(null) }
    var deleteCandidate by remember { mutableStateOf<Merchant?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val hapticNavigation = rememberHapticNavigation()
    val hapticLongPress = rememberHapticLongPress()
    val hapticConfirm = rememberHapticConfirm()
    val hapticDelete = rememberHapticDelete()
    val hapticReject = rememberHapticReject()
    val activeMerchants = remember(merchants) { merchants.filterNot { it.isArchived } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("商家管理") },
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
                        error = null
                        showAdd = true
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "新建商家")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            items(activeMerchants, key = { it.id }) { merchant ->
                MerchantRow(
                    merchant = merchant,
                    onLongClick = {
                        hapticLongPress()
                        actionMerchant = merchant
                    },
                )
                HorizontalDivider()
            }
        }
    }

    if (showAdd) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("新建商家") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("商家名称") },
                        singleLine = true,
                    )
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val ledgerId = currentLedger?.id ?: return@TextButton
                    scope.launch {
                        container.merchantRepository.addMerchant(ledgerId, name)
                            .onSuccess {
                                hapticConfirm()
                                showAdd = false
                                error = null
                            }
                            .onFailure {
                                hapticReject()
                                error = it.message ?: "创建失败"
                            }
                    }
                }) { Text("创建") }
            },
            dismissButton = {
                TextButton(onClick = { showAdd = false }) { Text("取消") }
            },
        )
    }

    actionMerchant?.let { merchant ->
        AlertDialog(
            onDismissRequest = { actionMerchant = null },
            title = { Text("商家操作") },
            text = { Text(merchant.name) },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = {
                        hapticNavigation()
                        actionMerchant = null
                        error = null
                        editingMerchant = merchant
                    }) { Text("重命名") }
                    TextButton(onClick = {
                        hapticConfirm()
                        actionMerchant = null
                        error = null
                        deleteCandidate = merchant
                    }) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { actionMerchant = null }) { Text("取消") }
            },
        )
    }

    editingMerchant?.let { merchant ->
        var name by remember(merchant.id) { mutableStateOf(merchant.name) }
        AlertDialog(
            onDismissRequest = { editingMerchant = null },
            title = { Text("重命名商家") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("商家名称") },
                        singleLine = true,
                    )
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        container.merchantRepository.rename(merchant.id, name)
                            .onSuccess {
                                hapticConfirm()
                                editingMerchant = null
                                error = null
                            }
                            .onFailure {
                                hapticReject()
                                error = it.message ?: "保存失败"
                            }
                    }
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { editingMerchant = null }) { Text("取消") }
            },
        )
    }

    deleteCandidate?.let { merchant ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("确认删除") },
            text = { Text("删除“${merchant.name}”后，历史账单仍会保留商家名称。") },
            confirmButton = {
                TextButton(onClick = {
                    hapticDelete()
                    scope.launch {
                        container.merchantRepository.delete(merchant.id)
                            .onSuccess {
                                deleteCandidate = null
                                error = null
                            }
                            .onFailure {
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MerchantRow(
    merchant: Merchant,
    onLongClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick,
                hapticFeedbackEnabled = false,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(merchant.name, style = MaterialTheme.typography.bodyLarge)
        Text("长按操作", style = MaterialTheme.typography.bodySmall)
    }
}

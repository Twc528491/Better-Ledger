package com.personal.ledger.ui.transaction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.personal.ledger.data.AppContainer
import com.personal.ledger.domain.model.TransactionType
import com.personal.ledger.ui.common.CategoryVisuals
import com.personal.ledger.util.Money
import com.personal.ledger.util.TimeUtils
import com.personal.ledger.viewmodel.AddTransactionViewModel
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionScreen(container: AppContainer, navController: NavHostController, txId: String?) {
    val vm: AddTransactionViewModel = viewModel {
        AddTransactionViewModel(
            container.ledgerRepository,
            container.accountRepository,
            container.categoryRepository,
            container.merchantRepository,
            container.tagRepository,
            container.transactionRepository,
            container.settings,
        )
    }
    val state by vm.uiState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    var showAccountPicker by remember { mutableStateOf(false) }
    var pickTarget by remember { mutableStateOf(false) }
    var showMerchantPicker by remember { mutableStateOf(false) }
    var showTagPicker by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(txId) { vm.load(txId) }

    LaunchedEffect(state.saved) {
        if (state.saved) navController.popBackStack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.editing != null) "编辑账单" else "记一笔") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            // 类型
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.type == TransactionType.EXPENSE,
                    onClick = { vm.setType(TransactionType.EXPENSE) },
                    label = { Text("支出") },
                )
                FilterChip(
                    selected = state.type == TransactionType.INCOME,
                    onClick = { vm.setType(TransactionType.INCOME) },
                    label = { Text("收入") },
                )
                FilterChip(
                    selected = state.type == TransactionType.TRANSFER,
                    onClick = { vm.setType(TransactionType.TRANSFER) },
                    label = { Text("转账") },
                )
            }

            Spacer(Modifier.height(16.dp))

            // 金额（最高视觉优先级）
            OutlinedTextField(
                value = state.amountText,
                onValueChange = { vm.setAmountText(it) },
                label = { Text("金额") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))

            if (state.type != TransactionType.TRANSFER) {
                // 分类（必填）
                Text("分类（必填）", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                val categories = if (state.type == TransactionType.EXPENSE) state.expenseCategories else state.incomeCategories
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = state.categoryId == cat.id,
                            onClick = { vm.setCategoryId(cat.id) },
                            label = { Text("${CategoryVisuals.emojiFor(cat.name, cat.type)} ${cat.name}") },
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // 账户
            OutlinedTextField(
                value = state.accounts.firstOrNull { it.id == state.accountId }?.name ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text(if (state.type == TransactionType.TRANSFER) "转出账户" else "账户") },
                trailingIcon = {
                    TextButton(onClick = { pickTarget = false; showAccountPicker = true }) { Text("选择") }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.type == TransactionType.TRANSFER) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.accounts.firstOrNull { it.id == state.targetAccountId }?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("转入账户") },
                    trailingIcon = {
                        TextButton(onClick = { pickTarget = true; showAccountPicker = true }) { Text("选择") }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(16.dp))

            // 商家（选填）
            OutlinedTextField(
                value = state.merchants.firstOrNull { it.id == state.merchantId }?.name ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("商家（选填）") },
                trailingIcon = {
                    Row {
                        if (state.merchantId != null) {
                            TextButton(onClick = { vm.setMerchantId(null) }) { Text("清除") }
                        }
                        TextButton(onClick = { showMerchantPicker = true }) { Text("选择") }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))

            // 标签（选填，可多选）
            Text("标签（选填，可多选）", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.tags.forEach { tag ->
                    FilterChip(
                        selected = tag.id in state.selectedTagIds,
                        onClick = { vm.toggleTag(tag.id) },
                        label = { Text(tag.name) },
                    )
                }
                FilterChip(
                    selected = false,
                    onClick = { showTagPicker = true },
                    label = { Text("+ 新建标签") },
                )
            }

            Spacer(Modifier.height(16.dp))

            // 备注（选填）
            OutlinedTextField(
                value = state.note,
                onValueChange = { vm.setNote(it) },
                label = { Text("备注（选填）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))

            // 时间
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("时间", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { showDatePicker = true }) {
                    Text(TimeUtils.formatMonthDay(state.time) + " " + timeOfDay(state.time))
                }
            }

            Spacer(Modifier.height(8.dp))

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    // 保存成功后才返回；此处轻震提示，成功反馈由返回动作衔接
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.save()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("保存", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = state.time)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val picked = datePickerState.selectedDateMillis
                    if (picked != null) {
                        val date = Instant.ofEpochMilli(picked).atZone(ZoneId.systemDefault()).toLocalDate()
                        val current = Instant.ofEpochMilli(state.time).atZone(ZoneId.systemDefault())
                        val combined = date.atTime(current.toLocalTime())
                            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        vm.setTime(combined)
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showAccountPicker) {
        SimplePickerDialog(
            title = "选择账户",
            items = state.accounts.map { it.id to it.name },
            onPick = { id ->
                if (pickTarget) vm.setTargetAccountId(id) else vm.setAccountId(id)
                showAccountPicker = false
            },
            onDismiss = { showAccountPicker = false },
        )
    }

    if (showMerchantPicker) {
        SimplePickerDialog(
            title = "选择商家",
            items = state.merchants.map { it.id to it.name },
            addLabel = "+ 新建商家",
            onPick = { id ->
                vm.setMerchantId(id)
                showMerchantPicker = false
            },
            onAdd = { name ->
                vm.addMerchant(name) { result ->
                    result.onSuccess { showMerchantPicker = false }
                }
            },
            onDismiss = { showMerchantPicker = false },
        )
    }

    if (showTagPicker) {
        SimplePickerDialog(
            title = "新建标签",
            items = emptyList(),
            addLabel = "创建",
            onAdd = { name ->
                vm.addTag(name) { result ->
                    result.onSuccess { showTagPicker = false }
                }
            },
            onDismiss = { showTagPicker = false },
        )
    }
}

private fun timeOfDay(ms: Long): String {
    val t = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalTime()
    return "%02d:%02d".format(t.hour, t.minute)
}

/** 简单列表选择对话框；addLabel 非空时提供新建输入框。 */
@Composable
private fun SimplePickerDialog(
    title: String,
    items: List<Pair<String, String>>,
    onPick: (String) -> Unit = {},
    onAdd: ((String) -> Unit)? = null,
    addLabel: String = "新建",
    onDismiss: () -> Unit,
) {
    var newName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                items.forEach { (id, name) ->
                    Text(
                        name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(id) }
                            .padding(vertical = 10.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                if (onAdd != null) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("名称") },
                        singleLine = true,
                    )
                    TextButton(onClick = {
                        if (newName.isNotBlank()) onAdd(newName.trim())
                    }) { Text(addLabel) }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

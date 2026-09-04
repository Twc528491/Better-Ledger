package com.personal.ledger.ui.transaction

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.personal.ledger.data.AppContainer
import com.personal.ledger.domain.model.Account
import com.personal.ledger.domain.model.Category
import com.personal.ledger.domain.model.TransactionType
import com.personal.ledger.ui.common.AccountBadge
import com.personal.ledger.ui.common.CategoryBadge
import com.personal.ledger.ui.common.CategoryVisuals
import com.personal.ledger.ui.common.accountTypeLabel
import com.personal.ledger.ui.common.cardColor
import com.personal.ledger.ui.common.rememberHapticConfirm
import com.personal.ledger.ui.common.rememberHapticLight
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
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showAccountPicker by remember { mutableStateOf(false) }
    var pickTarget by remember { mutableStateOf(false) }
    var showMerchantPicker by remember { mutableStateOf(false) }
    var showTagPicker by remember { mutableStateOf(false) }
    var detailsExpanded by remember { mutableStateOf(false) }
    val amountFocusRequester = remember { FocusRequester() }
    val hapticConfirm = rememberHapticConfirm()
    val hapticLight = rememberHapticLight()

    LaunchedEffect(txId) { vm.load(txId) }

    LaunchedEffect(state.saved) {
        if (state.saved) navController.popBackStack()
    }

    LaunchedEffect(state.ledgerId, state.editing) {
        if (state.ledgerId != null && txId == null) amountFocusRequester.requestFocus()
        if (state.editing != null || state.merchantId != null || state.selectedTagIds.isNotEmpty() || state.note.isNotEmpty()) {
            detailsExpanded = true
        }
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
        bottomBar = {
            Surface(shadowElevation = 4.dp) {
                Button(
                    onClick = {
                        hapticConfirm()
                        vm.save()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(52.dp),
                ) {
                    Text("保存", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 类型分段选择（全宽）
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val types = TransactionType.entries
                types.forEachIndexed { index, t ->
                    SegmentedButton(
                        selected = state.type == t,
                        onClick = {
                            hapticLight()
                            vm.setType(t)
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = types.size),
                    ) {
                        Text(typeLabel(t))
                    }
                }
            }

            // 金额输入卡片
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = cardColor(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                    Text("金额", style = MaterialTheme.typography.bodySmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            Money.symbolFor(state.baseCurrency),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        OutlinedTextField(
                            value = state.amountText,
                            onValueChange = { vm.setAmountText(it) },
                            placeholder = { Text("0.00", fontSize = 28.sp) },
                            textStyle = MaterialTheme.typography.headlineMedium,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(amountFocusRequester)
                                .padding(start = 8.dp),
                        )
                    }
                }
            }

            if (state.type != TransactionType.TRANSFER) {
                // 分类选择卡片
                Column {
                    Text("分类（必填）", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    val categories = if (state.type == TransactionType.EXPENSE) {
                        state.expenseCategories
                    } else {
                        state.incomeCategories
                    }
                    val visibleCategories = primaryCategories(categories, state.categoryId)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        visibleCategories.forEach { cat ->
                            CategoryOptionCard(
                                category = cat,
                                selected = state.categoryId == cat.id,
                                onClick = {
                                    hapticLight()
                                    vm.setCategoryId(cat.id)
                                },
                            )
                        }
                        if (categories.size > visibleCategories.size) {
                            MoreCategoryCard(onClick = {
                                hapticLight()
                                showCategoryPicker = true
                            })
                        }
                    }
                }
            }

            // 账户选择卡片
            AccountPickerCard(
                title = if (state.type == TransactionType.TRANSFER) "转出账户" else "账户",
                account = state.accounts.firstOrNull { it.id == state.accountId },
                onClick = {
                    hapticLight()
                    pickTarget = false
                    showAccountPicker = true
                },
            )
            if (state.type == TransactionType.TRANSFER) {
                AccountPickerCard(
                    title = "转入账户",
                    account = state.accounts.firstOrNull { it.id == state.targetAccountId },
                    onClick = {
                        hapticLight()
                        pickTarget = true
                        showAccountPicker = true
                    },
                )
            }

            // 低频信息默认收起，让日常记账停留在首屏。
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = cardColor(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                hapticLight()
                                detailsExpanded = !detailsExpanded
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("更多信息", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(
                                detailsSummary(state.merchantId != null, state.selectedTagIds.size, state.note.isNotBlank()),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (detailsExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = if (detailsExpanded) "收起更多信息" else "展开更多信息",
                            )
                        }
                    }
                    AnimatedVisibility(visible = detailsExpanded) {
                        Column {
                            HorizontalDivider()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        hapticLight()
                                        showMerchantPicker = true
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("🏪 商家", style = MaterialTheme.typography.bodyLarge)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        state.merchants.firstOrNull { it.id == state.merchantId }?.name ?: "选填",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                                }
                            }
                            HorizontalDivider()
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                Text("🏷️ 标签（选填，可多选）", style = MaterialTheme.typography.bodyLarge)
                                Spacer(Modifier.height(8.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    state.tags.forEach { tag ->
                                        TagChip(
                                            name = tag.name,
                                            selected = tag.id in state.selectedTagIds,
                                            onClick = {
                                                hapticLight()
                                                vm.toggleTag(tag.id)
                                            },
                                        )
                                    }
                                    TextButton(onClick = {
                                        hapticLight()
                                        showTagPicker = true
                                    }) { Text("+ 新建标签") }
                                }
                            }
                            HorizontalDivider()
                            OutlinedTextField(
                                value = state.note,
                                onValueChange = { vm.setNote(it) },
                                label = { Text("备注（选填）") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                            HorizontalDivider()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        hapticLight()
                                        showDatePicker = true
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("🕐 日期", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    TimeUtils.formatMonthDay(state.time) + " " + timeOfDay(state.time),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
            }

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
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
            items = state.accounts
                .filter { !pickTarget || it.id != state.accountId }
                .map { it.id to it.name },
            onPick = { id ->
                hapticLight()
                if (pickTarget) vm.setTargetAccountId(id) else vm.setAccountId(id)
                showAccountPicker = false
            },
            onDismiss = { showAccountPicker = false },
        )
    }

    if (showCategoryPicker) {
        val categories = if (state.type == TransactionType.EXPENSE) state.expenseCategories else state.incomeCategories
        SimplePickerDialog(
            title = "选择分类",
            items = categories.map { it.id to it.name },
            onPick = { id ->
                hapticLight()
                vm.setCategoryId(id)
                showCategoryPicker = false
            },
            onDismiss = { showCategoryPicker = false },
        )
    }

    if (showMerchantPicker) {
        SimplePickerDialog(
            title = "选择商家",
            items = state.merchants.map { it.id to it.name },
            addLabel = "+ 新建商家",
            clearLabel = "不选择商家",
            onClear = {
                vm.setMerchantId(null)
                showMerchantPicker = false
            },
            onPick = { id ->
                hapticLight()
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

private fun typeLabel(t: TransactionType): String = when (t) {
    TransactionType.EXPENSE -> "支出"
    TransactionType.INCOME -> "收入"
    TransactionType.TRANSFER -> "转账"
}

private fun timeOfDay(ms: Long): String {
    val t = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalTime()
    return "%02d:%02d".format(t.hour, t.minute)
}

private fun primaryCategories(categories: List<Category>, selectedId: String?): List<Category> {
    val primary = categories.take(6).toMutableList()
    val selected = categories.firstOrNull { it.id == selectedId }
    if (selected != null && primary.none { it.id == selected.id }) {
        if (primary.isNotEmpty()) primary[primary.lastIndex] = selected else primary += selected
    }
    return primary.distinctBy { it.id }
}

private fun detailsSummary(hasMerchant: Boolean, tagCount: Int, hasNote: Boolean): String {
    val count = listOf(hasMerchant, tagCount > 0, hasNote).count { it }
    return if (count == 0) "商家、标签、备注与日期" else "已填写 $count 项 · 点击展开"
}

/** 分类选择卡片（emoji 圆标 + 名称，选中描边高亮）。 */
@Composable
private fun CategoryOptionCard(
    category: Category,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else cardColor(),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .width(86.dp)
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CategoryBadge(name = category.name, type = category.type, sizeDp = 40, visualKey = category.id)
            Spacer(Modifier.height(6.dp))
            Text(
                category.name,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun MoreCategoryCard(onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardColor(),
        modifier = Modifier.width(86.dp).clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("•••", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(6.dp))
            Text("更多", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** 标签选择 chip。 */
@Composable
private fun TagChip(name: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            (if (selected) "✓ " else "# ") + name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

/** 账户选择卡片行（图标 + 名称 + 类型 + 箭头）。 */
@Composable
private fun AccountPickerCard(
    title: String,
    account: Account?,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = cardColor(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AccountBadge(type = account?.type ?: com.personal.ledger.domain.model.AccountType.OTHER, sizeDp = 40)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(title, style = MaterialTheme.typography.bodySmall)
                if (account != null) {
                    Text(
                        "${account.name} · ${accountTypeLabel(account.type)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                } else {
                    Text("点击选择", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

/** 简单列表选择对话框；addLabel 非空时提供新建输入框。 */
@Composable
private fun SimplePickerDialog(
    title: String,
    items: List<Pair<String, String>>,
    onPick: (String) -> Unit = {},
    onAdd: ((String) -> Unit)? = null,
    addLabel: String = "新建",
    clearLabel: String? = null,
    onClear: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    var newName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (clearLabel != null && onClear != null) {
                    TextButton(onClick = onClear, modifier = Modifier.fillMaxWidth()) {
                        Text(clearLabel)
                    }
                }
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

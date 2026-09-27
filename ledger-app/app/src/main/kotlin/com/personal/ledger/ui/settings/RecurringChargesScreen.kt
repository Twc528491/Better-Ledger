package com.personal.ledger.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.personal.ledger.data.AppContainer
import com.personal.ledger.data.repository.RecurringChargeDraft
import com.personal.ledger.domain.model.Account
import com.personal.ledger.domain.model.Category
import com.personal.ledger.domain.model.RecurrenceFrequency
import com.personal.ledger.domain.model.RecurringCharge
import com.personal.ledger.ui.common.CategoryBadge
import com.personal.ledger.ui.common.EmptyState
import com.personal.ledger.ui.common.MoneyText
import com.personal.ledger.ui.common.cardColor
import com.personal.ledger.ui.common.insetColor
import com.personal.ledger.ui.common.rememberHapticDelete
import com.personal.ledger.ui.theme.LedgerNumerals
import com.personal.ledger.ui.theme.LedgerSpacing
import com.personal.ledger.util.Money
import com.personal.ledger.viewmodel.RecurringChargesViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringChargesScreen(container: AppContainer, navController: NavHostController) {
    val vm: RecurringChargesViewModel = viewModel {
        RecurringChargesViewModel(
            container.ledgerRepository,
            container.accountRepository,
            container.categoryRepository,
            container.recurringChargeRepository,
        )
    }
    val state by vm.uiState.collectAsStateWithLifecycle()
    val appContext = LocalContext.current.applicationContext
    var editorVisible by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<RecurringCharge?>(null) }
    var deleting by remember { mutableStateOf<RecurringCharge?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    val hapticDelete = rememberHapticDelete()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("定时扣款") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editing = null
                    editorVisible = true
                },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("添加扣款") },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = LedgerSpacing.md, top = LedgerSpacing.xs, end = LedgerSpacing.md, bottom = 112.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(LedgerSpacing.sm),
        ) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = insetColor(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(modifier = Modifier.padding(LedgerSpacing.md)) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(LedgerSpacing.sm))
                        Column(verticalArrangement = Arrangement.spacedBy(LedgerSpacing.xxs)) {
                            Text("只在本地账本生成普通支出记录，不会向商家或银行实际付款。", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "开始日期早于现在时会补记已到期扣款；每次最多补 200 笔。后台执行受系统省电和调度影响，打开应用时也会检查补记。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            if (notice != null) {
                item {
                    Text(
                        notice.orEmpty(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            if (state.ledger == null) {
                item {
                    Text(
                        "当前没有可用账本。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else if (state.charges.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.EventRepeat,
                        title = "还没有定时扣款",
                        subtitle = "添加后可设置金额、账户、支出分类、开始日期和重复周期。",
                    )
                }
            } else {
                items(state.charges, key = { it.id }) { charge ->
                    RecurringChargeCard(
                        charge = charge,
                        account = state.accounts.firstOrNull { it.id == charge.accountId },
                        category = state.expenseCategories.firstOrNull { it.id == charge.categoryId },
                        onEdit = {
                            editing = charge
                            editorVisible = true
                            notice = null
                        },
                        onPauseToggle = {
                            vm.setPaused(charge.id, !charge.isPaused) { result ->
                                notice = result.exceptionOrNull()?.message
                                    ?: if (charge.isPaused) "已恢复，将从当前时间起安排下一次扣款。" else "已暂停定时扣款。"
                                if (result.isSuccess && charge.isPaused) {
                                    com.personal.ledger.worker.RecurringChargeScheduler.enqueueCatchUp(
                                        appContext,
                                    )
                                }
                            }
                        },
                        onRetry = {
                            vm.retry(charge.id) { result ->
                                notice = result.exceptionOrNull()?.message ?: "已重新安排检查。"
                                if (result.isSuccess) {
                                    com.personal.ledger.worker.RecurringChargeScheduler.enqueueCatchUp(
                                        appContext,
                                    )
                                }
                            }
                        },
                        onDelete = { deleting = charge },
                    )
                }
            }
        }
    }

    if (editorVisible) {
        RecurringChargeEditor(
            charge = editing,
            accounts = state.accounts,
            categories = state.expenseCategories,
            currency = state.ledger?.baseCurrency ?: "CNY",
            ledgerId = state.ledger?.id.orEmpty(),
            onDismiss = { editorVisible = false },
            onSave = { id, draft, complete ->
                vm.save(id, draft) { result ->
                    complete(result)
                    if (result.isSuccess) {
                        editorVisible = false
                        notice = "定时扣款已保存。"
                        com.personal.ledger.worker.RecurringChargeScheduler.enqueueCatchUp(
                            appContext,
                        )
                    } else {
                        notice = null
                    }
                }
            },
        )
    }

    deleting?.let { charge ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("删除定时扣款？") },
            text = { Text("将停止后续生成；已经生成的支出记录会保留。") },
            confirmButton = {
                TextButton(onClick = {
                    hapticDelete()
                    vm.delete(charge.id) { result ->
                        notice = result.exceptionOrNull()?.message ?: "定时扣款已删除。"
                        if (result.isSuccess) deleting = null
                    }
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun RecurringChargeCard(
    charge: RecurringCharge,
    account: Account?,
    category: Category?,
    onEdit: () -> Unit,
    onPauseToggle: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = cardColor(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(start = LedgerSpacing.md, end = LedgerSpacing.md, top = 14.dp, bottom = LedgerSpacing.xxs),
            verticalArrangement = Arrangement.spacedBy(LedgerSpacing.xxs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (category != null) {
                    CategoryBadge(
                        name = category.name,
                        type = category.type,
                        sizeDp = 36,
                        visualKey = category.id,
                        iconKey = category.iconKey,
                    )
                    Spacer(Modifier.width(LedgerSpacing.sm))
                }
                Column(modifier = Modifier.weight(1f).padding(end = LedgerSpacing.xs)) {
                    Text(
                        charge.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (charge.isPaused) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${frequencyLabel(charge.frequency)} · ${account?.name ?: "账户不可用"} · ${category?.name ?: "分类不可用"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                MoneyText(
                    text = Money.formatWithCurrency(charge.amount, charge.currency),
                    color = if (charge.isPaused) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.height(LedgerSpacing.xxs))
            when {
                charge.lastError != null -> {
                    Text(
                        "生成失败：${charge.lastError}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    TextButton(onClick = onRetry) { Text("重新检查") }
                }
                charge.isPaused -> Text(
                    "已暂停",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> {
                    val formatter = remember(charge.zoneId) {
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(charge.zoneId)
                    }
                    Text(
                        "下次：${formatter.format(Instant.ofEpochMilli(charge.nextDueAt))}（${charge.zoneId.id}）",
                        style = LedgerNumerals.of(MaterialTheme.typography.bodySmall),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.xxs),
            ) {
                TextButton(onClick = onEdit) { Text("编辑") }
                TextButton(onClick = onPauseToggle) {
                    Text(if (charge.isPaused) "恢复" else "暂停")
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDelete) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun RecurringChargeEditor(
    charge: RecurringCharge?,
    accounts: List<Account>,
    categories: List<Category>,
    currency: String,
    ledgerId: String,
    onDismiss: () -> Unit,
    onSave: (String?, RecurringChargeDraft, (Result<RecurringCharge>) -> Unit) -> Unit,
) {
    val zone = charge?.zoneId ?: ZoneId.systemDefault()
    var name by remember(charge?.id) { mutableStateOf(charge?.name.orEmpty()) }
    var amountText by remember(charge?.id) {
        mutableStateOf(charge?.let { Money.formatMinor(it.amount) }.orEmpty())
    }
    var dateText by remember(charge?.id) {
        mutableStateOf(charge?.startDate?.toString() ?: LocalDate.now(zone).toString())
    }
    var timeText by remember(charge?.id) {
        mutableStateOf(
            charge?.let {
                "%02d:%02d".format(it.minuteOfDay / 60, it.minuteOfDay % 60)
            } ?: LocalTime.now(zone).withSecond(0).withNano(0).format(DateTimeFormatter.ofPattern("HH:mm")),
        )
    }
    val availableAccounts = accounts.filter { !it.isArchived || it.id == charge?.accountId }
    val availableCategories = categories.filter { !it.isArchived || it.id == charge?.categoryId }
    var accountId by remember(charge?.id) {
        mutableStateOf(charge?.accountId ?: availableAccounts.firstOrNull { !it.isArchived }?.id.orEmpty())
    }
    var categoryId by remember(charge?.id) {
        mutableStateOf(charge?.categoryId ?: availableCategories.firstOrNull { !it.isArchived }?.id.orEmpty())
    }
    var frequency by remember(charge?.id) { mutableStateOf(charge?.frequency ?: RecurrenceFrequency.MONTHLY) }
    var formError by remember(charge?.id) { mutableStateOf<String?>(null) }
    var isSaving by remember(charge?.id) { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (charge == null) "添加定时扣款" else "编辑定时扣款") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("金额（$currency）") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                RecurringChoiceField(
                    label = "扣款账户",
                    value = availableAccounts.firstOrNull { it.id == accountId }?.let {
                        it.name + if (it.isArchived) "（已归档）" else ""
                    }.orEmpty(),
                    choices = availableAccounts.map { it.id to (it.name + if (it.isArchived) "（已归档）" else "") },
                    onChoose = { accountId = it },
                )
                RecurringChoiceField(
                    label = "支出分类",
                    value = availableCategories.firstOrNull { it.id == categoryId }?.let {
                        it.name + if (it.isArchived) "（已归档）" else ""
                    }.orEmpty(),
                    choices = availableCategories.map { it.id to (it.name + if (it.isArchived) "（已归档）" else "") },
                    onChoose = { categoryId = it },
                )
                RecurringChoiceField(
                    label = "重复周期",
                    value = frequencyLabel(frequency),
                    choices = RecurrenceFrequency.entries.map { it.name to frequencyLabel(it) },
                    onChoose = { frequency = RecurrenceFrequency.valueOf(it) },
                )
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("开始日期（yyyy-MM-dd）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = timeText,
                    onValueChange = { timeText = it },
                    label = { Text("本地时间（HH:mm）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("时区：${zone.id}。开始日期早于当前日期时，已到期周期会补记。", style = MaterialTheme.typography.bodySmall)
                if (formError != null) {
                    Text(formError.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(enabled = !isSaving, onClick = {
                val amount = Money.parseToMinor(amountText)
                val start = runCatching { LocalDate.parse(dateText.trim()) }.getOrNull()
                val time = runCatching { LocalTime.parse(timeText.trim()) }.getOrNull()
                when {
                    name.isBlank() -> formError = "请填写名称"
                    amount == null || amount <= 0 -> formError = "请输入大于 0 的有效金额"
                    accountId.isBlank() -> formError = "请选择账户"
                    categoryId.isBlank() -> formError = "请选择支出分类"
                    start == null -> formError = "日期格式应为 yyyy-MM-dd"
                    time == null -> formError = "时间格式应为 HH:mm"
                    else -> {
                        formError = null
                        isSaving = true
                        onSave(
                            charge?.id,
                            RecurringChargeDraft(
                                ledgerId = ledgerId,
                                name = name,
                                amount = amount,
                                accountId = accountId,
                                categoryId = categoryId,
                                frequency = frequency,
                                startDate = start,
                                minuteOfDay = time.hour * 60 + time.minute,
                                zoneId = zone,
                            ),
                        ) { result ->
                            isSaving = false
                            if (result.isFailure) {
                                formError = result.exceptionOrNull()?.message ?: "保存失败"
                            }
                        }
                    }
                }
            }) { Text(if (isSaving) "保存中…" else "保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun RecurringChoiceField(
    label: String,
    value: String,
    choices: List<Pair<String, String>>,
    onChoose: (String) -> Unit,
) {
    var expanded by remember(label) { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        androidx.compose.foundation.layout.Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(value.ifBlank { "请选择" })
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                choices.forEach { (key, title) ->
                    DropdownMenuItem(
                        text = { Text(title) },
                        onClick = {
                            onChoose(key)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

private fun frequencyLabel(frequency: RecurrenceFrequency): String = when (frequency) {
    RecurrenceFrequency.DAILY -> "每天"
    RecurrenceFrequency.WEEKLY -> "每周"
    RecurrenceFrequency.MONTHLY -> "每月"
    RecurrenceFrequency.YEARLY -> "每年"
}

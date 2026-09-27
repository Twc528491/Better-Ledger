package com.personal.ledger.ui.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.personal.ledger.data.AppContainer
import com.personal.ledger.domain.calc.BalanceCalculator
import com.personal.ledger.domain.model.AccountType
import com.personal.ledger.domain.model.TransactionType
import com.personal.ledger.navigation.Routes
import com.personal.ledger.ui.common.AccountBadge
import com.personal.ledger.ui.common.EmptyState
import com.personal.ledger.ui.common.LedgerCard
import com.personal.ledger.ui.common.LedgerGroupItem
import com.personal.ledger.ui.common.LedgerSectionHeader
import com.personal.ledger.ui.common.MoneyText
import com.personal.ledger.ui.common.accountTypeLabel
import com.personal.ledger.ui.common.amountColor
import com.personal.ledger.ui.common.cardColor
import com.personal.ledger.ui.common.expenseAmountColor
import com.personal.ledger.ui.common.signedMoney
import com.personal.ledger.ui.theme.LedgerNumerals
import com.personal.ledger.ui.theme.LedgerSpacing
import com.personal.ledger.ui.common.rememberHapticConfirm
import com.personal.ledger.ui.common.rememberHapticLight
import com.personal.ledger.ui.common.rememberHapticNavigation
import com.personal.ledger.ui.common.rememberHapticReject
import com.personal.ledger.util.Money
import com.personal.ledger.util.TimeUtils
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(container: AppContainer, navController: NavHostController, accountId: String) {
    val scope = rememberCoroutineScope()
    val account by container.accountRepository.observeByLedger(ledgerOf(container))
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val txList by container.transactionRepository.observeByLedger(ledgerOf(container))
        .collectAsStateWithLifecycle(initialValue = emptyList())

    var showEdit by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val hapticNavigation = rememberHapticNavigation()
    val hapticConfirm = rememberHapticConfirm()
    val hapticReject = rememberHapticReject()

    val acc = account.firstOrNull { it.id == accountId }
    val txs = txList.filter { it.accountId == accountId || it.targetAccountId == accountId }
        .sortedByDescending { it.transactionTime }
    val balance = acc?.let { a ->
        val deltas = txList.mapNotNull {
            BalanceCalculator.deltaFor(it.type, it.amount, it.accountId, it.targetAccountId, a.id)
        }
        BalanceCalculator.compute(a.initialBalance, deltas)
    } ?: 0L

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(acc?.name ?: "账户详情") },
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
                        showEdit = true
                    }) {
                        Icon(Icons.Filled.Edit, contentDescription = "编辑")
                    }
                },
            )
        },
    ) { padding ->
        val currency = acc?.currency ?: "CNY"
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(
                start = LedgerSpacing.md,
                top = LedgerSpacing.xs,
                end = LedgerSpacing.md,
                bottom = LedgerSpacing.xl,
            ),
        ) {
            item {
                LedgerCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AccountBadge(type = acc?.type ?: AccountType.OTHER, sizeDp = 40)
                        Column(modifier = Modifier.padding(start = LedgerSpacing.sm)) {
                            Text(
                                "当前余额",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            MoneyText(
                                text = signedMoney(balance, currency, explicitPlus = false),
                                color = if (balance < 0) expenseAmountColor() else MaterialTheme.colorScheme.onSurface,
                                prominent = true,
                            )
                        }
                    }
                    Text(
                        "类型：${accountTypeLabel(acc?.type ?: AccountType.OTHER)} · 初始余额：${Money.formatWithCurrency(acc?.initialBalance ?: 0L, currency)}",
                        style = LedgerNumerals.of(MaterialTheme.typography.bodySmall),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                LedgerSectionHeader(
                    title = "交易记录",
                    modifier = Modifier.padding(start = LedgerSpacing.xxs, top = LedgerSpacing.lg, bottom = LedgerSpacing.xxs),
                )
            }
            if (txs.isEmpty()) {
                item {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = cardColor(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        EmptyState(
                            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                            title = "这个账户还没有交易",
                            subtitle = "记账或转账后会显示在这里",
                        )
                    }
                }
            }
            val count = txs.size
            itemsIndexed(txs, key = { _, tx -> tx.id }) { index, tx ->
                val outgoingTransfer = tx.type == TransactionType.TRANSFER && tx.accountId == accountId
                LedgerGroupItem(index = index, count = count) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                hapticNavigation()
                                navController.navigate(Routes.add(tx.id))
                            }
                            .heightIn(min = 56.dp)
                            .padding(horizontal = LedgerSpacing.md, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = LedgerSpacing.sm)) {
                            Text(
                                tx.note ?: typeLabelOf(tx.type),
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                TimeUtils.formatMonthDay(tx.transactionTime),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        MoneyText(
                            text = when (tx.type) {
                                TransactionType.TRANSFER -> signedMoney(if (outgoingTransfer) -tx.amount else tx.amount, currency)
                                TransactionType.EXPENSE -> signedMoney(-tx.amount, currency)
                                TransactionType.INCOME -> signedMoney(tx.amount, currency)
                            },
                            color = amountColor(tx.type),
                        )
                    }
                }
            }
        }
    }

    if (showEdit && acc != null) {
        EditAccountDialog(
            account = acc,
            onConfirm = { name, type, initialMinor, initialTime, archived ->
                scope.launch {
                    container.accountRepository.updateAccount(
                        acc, name, type, initialMinor, initialTime, archived,
                    ).onSuccess {
                        hapticConfirm()
                        showEdit = false
                        error = null
                    }.onFailure {
                        hapticReject()
                        error = it.message
                    }
                }
            },
            onDismiss = { showEdit = false; error = null },
            error = error,
        )
    }
}

@Composable
private fun ledgerOf(container: AppContainer): String {
    val ledger by container.ledgerRepository.currentLedger.collectAsStateWithLifecycle(initialValue = null)
    return ledger?.id ?: ""
}

private fun typeLabelOf(t: TransactionType) = when (t) {
    TransactionType.EXPENSE -> "支出"
    TransactionType.INCOME -> "收入"
    TransactionType.TRANSFER -> "转账"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditAccountDialog(
    account: com.personal.ledger.domain.model.Account,
    onConfirm: (name: String, type: AccountType, initialMinor: Long, initialTime: Long, archived: Boolean) -> Unit,
    onDismiss: () -> Unit,
    error: String?,
) {
    var name by remember { mutableStateOf(account.name) }
    var type by remember { mutableStateOf(account.type) }
    var initialText by remember { mutableStateOf(Money.formatMinor(account.initialBalance)) }
    var initialTime by remember { mutableStateOf(account.initialBalanceTime) }
    var showDatePicker by remember { mutableStateOf(false) }
    var archived by remember { mutableStateOf(account.isArchived) }
    var nameError by remember { mutableStateOf<String?>(null) }
    val hapticLight = rememberHapticLight()
    val hapticNavigation = rememberHapticNavigation()
    val hapticConfirm = rememberHapticConfirm()
    val hapticReject = rememberHapticReject()

        AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑账户") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it; nameError = null }, label = { Text("名称") }, singleLine = true)
                nameError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = initialText, onValueChange = { initialText = it }, label = { Text("初始余额") }, singleLine = true)
                Spacer(Modifier.height(12.dp))
                Text(
                    "类型",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                AccountTypeGrid(
                    selected = type,
                    onSelect = { t ->
                        hapticLight()
                        type = t
                    },
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("初始时间", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = {
                        hapticNavigation()
                        showDatePicker = true
                    }) {
                        Text(TimeUtils.formatMonthDay(initialTime))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(if (archived) "已归档" else "归档账户", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = {
                        hapticLight()
                        archived = !archived
                    }) {
                        Text(if (archived) "恢复" else "归档")
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isBlank()) {
                    hapticReject()
                    nameError = "名称不能为空"
                    return@Button
                }
                val minor = Money.parseToMinor(initialText, allowNegative = true) ?: run {
                    hapticReject()
                    return@Button
                }
                onConfirm(name.trim(), type, minor, initialTime, archived)
            }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialTime)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val date = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                        val current = Instant.ofEpochMilli(initialTime).atZone(ZoneId.systemDefault())
                        initialTime = date.atTime(current.toLocalTime())
                            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    }
                    hapticConfirm()
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
}

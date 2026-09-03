package com.personal.ledger.ui.account

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import com.personal.ledger.domain.model.AccountType
import com.personal.ledger.domain.model.AccountWithBalance
import com.personal.ledger.navigation.Routes
import com.personal.ledger.ui.common.AccountBadge
import com.personal.ledger.ui.common.AccountVisuals
import com.personal.ledger.ui.common.EmptyState
import com.personal.ledger.ui.common.accountTypeLabel
import com.personal.ledger.ui.common.balanceColor
import com.personal.ledger.ui.common.highlightCardColor
import com.personal.ledger.ui.common.highlightCardContentColor
import com.personal.ledger.util.Money
import com.personal.ledger.viewmodel.AccountsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(container: AppContainer, navController: NavHostController) {
    val vm: AccountsViewModel = viewModel {
        AccountsViewModel(container.ledgerRepository, container.accountRepository)
    }
    val state by vm.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    val hapticLight = com.personal.ledger.ui.common.rememberHapticLight()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("资产") },
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.SETTINGS) }) {
                        Icon(Icons.Filled.Settings, contentDescription = "设置")
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "新建账户")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                // 净资产主卡片
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = highlightCardColor(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("净资产", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            Money.formatWithSymbol(state.netWorth),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = highlightCardContentColor(),
                        )
                    }
                }
            }

            item {
                Text(
                    "资产",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                )
            }
            if (state.assets.isEmpty()) {
                item { EmptyState(emoji = "👛", title = "暂无资产账户", subtitle = "点右上角 + 新建") }
            } else {
                items(state.assets, key = { it.account.id }) { awb ->
                    AccountRow(
                        awb,
                        onClick = {
                            hapticLight()
                            navController.navigate(Routes.accountDetail(awb.account.id))
                        },
                    )
                }
            }

            item {
                Text(
                    "负债",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                )
            }
            if (state.liabilities.isEmpty()) {
                item { EmptyState(emoji = "💸", title = "无负债账户", subtitle = "信用卡 / 贷款类账户会显示在这里") }
            } else {
                items(state.liabilities, key = { it.account.id }) { awb ->
                    AccountRow(
                        awb,
                        onClick = {
                            hapticLight()
                            navController.navigate(Routes.accountDetail(awb.account.id))
                        },
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddAccountDialog(
            onConfirm = { name, type, initialMinor ->
                vm.addAccount(name, type, initialMinor) { result ->
                    result.onSuccess { showAddDialog = false }
                }
            },
            onDismiss = { showAddDialog = false },
        )
    }
}

@Composable
private fun AccountRow(awb: AccountWithBalance, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AccountBadge(type = awb.account.type)
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(awb.account.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(typeLabel(awb.account.type), style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(
                Money.formatWithSymbol(awb.balance),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = balanceColor(awb.balance),
            )
        }
    }
}

private fun typeLabel(type: AccountType): String = when (type) {
    AccountType.CASH -> "现金"
    AccountType.BANK -> "银行卡"
    AccountType.ALIPAY -> "支付宝"
    AccountType.WECHAT -> "微信"
    AccountType.CREDIT_CARD -> "信用卡"
    AccountType.SAVINGS -> "储蓄"
    AccountType.INVESTMENT -> "投资"
    AccountType.LOAN -> "贷款"
    AccountType.OTHER -> "其他"
}

@Composable
private fun AddAccountDialog(
    onConfirm: (name: String, type: AccountType, initialMinor: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var initialText by remember { mutableStateOf("0") }
    var type by remember { mutableStateOf(AccountType.CASH) }
    val hapticLight = com.personal.ledger.ui.common.rememberHapticLight()
    val hapticConfirm = com.personal.ledger.ui.common.rememberHapticConfirm()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建账户") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("账户名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = initialText,
                    onValueChange = { initialText = it },
                    label = { Text("初始余额（负数表示负债）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Text("类型", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                // 类型 9 宫格卡片
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AccountType.entries.chunked(3).forEach { rowTypes ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            rowTypes.forEach { t ->
                                AccountTypeOptionCard(
                                    type = t,
                                    selected = type == t,
                                    onClick = {
                                        hapticLight()
                                        type = t
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                hapticConfirm()
                val minor = Money.parseToMinor(initialText, allowNegative = true) ?: 0L
                if (name.isNotBlank()) onConfirm(name.trim(), type, minor)
            }) { Text("创建") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

/** 账户类型选择卡片（emoji + 名称，选中高亮）。 */
@Composable
private fun AccountTypeOptionCard(
    type: AccountType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(AccountVisuals.emojiFor(type), fontSize = MaterialTheme.typography.titleLarge.fontSize)
            Text(
                accountTypeLabel(type),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
            )
        }
    }
}

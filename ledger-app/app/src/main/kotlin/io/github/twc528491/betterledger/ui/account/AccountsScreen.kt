package io.github.twc528491.betterledger.ui.account

import io.github.twc528491.betterledger.ui.common.SettingsActionButton
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import io.github.twc528491.betterledger.data.AppContainer
import io.github.twc528491.betterledger.domain.model.AccountType
import io.github.twc528491.betterledger.domain.model.AccountWithBalance
import io.github.twc528491.betterledger.navigation.Routes
import io.github.twc528491.betterledger.ui.common.AccountBadge
import io.github.twc528491.betterledger.ui.common.HeroFigure
import io.github.twc528491.betterledger.ui.common.HeroTone
import io.github.twc528491.betterledger.ui.common.LedgerGroupItem
import io.github.twc528491.betterledger.ui.common.LedgerHeroCard
import io.github.twc528491.betterledger.ui.common.LedgerSectionHeader
import io.github.twc528491.betterledger.ui.common.MoneyText
import io.github.twc528491.betterledger.ui.common.accountTypeLabel
import io.github.twc528491.betterledger.ui.common.balanceColor
import io.github.twc528491.betterledger.ui.common.cardColor
import io.github.twc528491.betterledger.ui.common.rememberHapticConfirm
import io.github.twc528491.betterledger.ui.common.rememberHapticLight
import io.github.twc528491.betterledger.ui.common.rememberHapticNavigation
import io.github.twc528491.betterledger.ui.common.rememberHapticReject
import io.github.twc528491.betterledger.ui.common.signedMoney
import io.github.twc528491.betterledger.ui.theme.LedgerSpacing
import androidx.compose.ui.res.stringResource
import io.github.twc528491.betterledger.R
import io.github.twc528491.betterledger.util.Money
import io.github.twc528491.betterledger.viewmodel.AccountsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(container: AppContainer, navController: NavHostController) {
    val vm: AccountsViewModel = viewModel {
        AccountsViewModel(container.ledgerRepository, container.accountRepository)
    }
    val state by vm.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    val hapticLight = rememberHapticLight()
    val hapticNavigation = rememberHapticNavigation()
    val hapticConfirm = rememberHapticConfirm()
    val hapticReject = rememberHapticReject()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.account_title)) },
                actions = {
                    IconButton(onClick = {
                        hapticNavigation()
                        showAddDialog = true
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.account_new))
                    }
                    SettingsActionButton {
                        hapticNavigation()
                        navController.navigate(Routes.SETTINGS)
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = LedgerSpacing.md,
                top = LedgerSpacing.xs,
                end = LedgerSpacing.md,
                bottom = LedgerSpacing.xl,
            ),
        ) {
            item {
                // 净资产主卡片：净资产为主数字，资产 / 负债合计为次级信息（内容色 + 显式正负号）。
                // 账户全局共享：汇总全部账本；只计当前账本本位币的账户（V1 不做汇率）。
                val assetTotal = state.assets.sumOf { it.balance }
                val liabilityTotal = state.liabilities.sumOf { it.balance }
                LedgerHeroCard {
                    HeroFigure(
                        label = if (state.otherCurrencies.isEmpty()) {
                            stringResource(R.string.account_net_worth)
                        } else {
                            stringResource(R.string.account_net_worth_in, state.currency)
                        },
                        value = signedMoney(state.netWorth, state.currency, explicitPlus = false),
                        large = true,
                    )
                    Spacer(Modifier.height(LedgerSpacing.md))
                    Row(horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.sm)) {
                        HeroFigure(
                            label = stringResource(R.string.account_assets),
                            value = signedMoney(assetTotal, state.currency, explicitPlus = false),
                            tone = HeroTone.INCOME,
                            modifier = Modifier.weight(1f),
                        )
                        HeroFigure(
                            label = stringResource(R.string.account_liabilities),
                            value = signedMoney(liabilityTotal, state.currency, explicitPlus = false),
                            tone = HeroTone.NEGATIVE,
                            divider = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            item { AccountSectionHeader(stringResource(R.string.account_assets)) }
            if (state.assets.isEmpty()) {
                item {
                    SectionEmptyCard(
                        title = stringResource(R.string.account_no_assets),
                        subtitle = stringResource(R.string.account_no_assets_hint),
                    )
                }
            } else {
                val count = state.assets.size
                itemsIndexed(state.assets, key = { _, awb -> awb.account.id }) { index, awb ->
                    LedgerGroupItem(index = index, count = count) {
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

            item { AccountSectionHeader(stringResource(R.string.account_liabilities)) }
            if (state.liabilities.isEmpty()) {
                item {
                    SectionEmptyCard(
                        title = stringResource(R.string.account_no_liabilities),
                        subtitle = stringResource(R.string.account_no_liabilities_hint),
                    )
                }
            } else {
                val count = state.liabilities.size
                itemsIndexed(state.liabilities, key = { _, awb -> awb.account.id }) { index, awb ->
                    LedgerGroupItem(index = index, count = count) {
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

            // 其他币种账户：单独列出（显示币种），不计入净资产
            if (state.otherCurrencies.isNotEmpty()) {
                item { AccountSectionHeader(stringResource(R.string.account_other_currencies)) }
                val count = state.otherCurrencies.size
                itemsIndexed(state.otherCurrencies, key = { _, awb -> "currency_" + awb.account.id }) { index, awb ->
                    LedgerGroupItem(index = index, count = count) {
                        AccountRow(
                            awb,
                            showCurrency = true,
                            onClick = {
                                hapticLight()
                                navController.navigate(Routes.accountDetail(awb.account.id))
                            },
                        )
                    }
                }
            }

            // 已归档账户（全局归档，所有账本都不再提供）：点击进入详情，可恢复或彻底删除
            if (state.archived.isNotEmpty()) {
                item { AccountSectionHeader(stringResource(R.string.account_archived)) }
                val count = state.archived.size
                itemsIndexed(state.archived, key = { _, awb -> "archived_" + awb.account.id }) { index, awb ->
                    LedgerGroupItem(index = index, count = count) {
                        AccountRow(
                            awb,
                            showCurrency = awb.account.currency != state.currency,
                            onClick = {
                                hapticLight()
                                navController.navigate(Routes.accountDetail(awb.account.id))
                            },
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddAccountDialog(
            onConfirm = { name, type, initialMinor ->
                vm.addAccount(name, type, initialMinor) { result ->
                    result.onSuccess {
                        hapticConfirm()
                        showAddDialog = false
                    }.onFailure { hapticReject() }
                }
            },
            onDismiss = { showAddDialog = false },
        )
    }
}

@Composable
private fun AccountSectionHeader(title: String) {
    LedgerSectionHeader(
        title = title,
        modifier = Modifier.padding(
            start = LedgerSpacing.xxs,
            top = LedgerSpacing.lg,
            bottom = LedgerSpacing.xxs,
        ),
    )
}

@Composable
private fun SectionEmptyCard(title: String, subtitle: String) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = cardColor(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = LedgerSpacing.md, vertical = 14.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AccountRow(awb: AccountWithBalance, onClick: () -> Unit, showCurrency: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 64.dp)
            .padding(horizontal = LedgerSpacing.md, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AccountBadge(type = awb.account.type, sizeDp = 40)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = LedgerSpacing.sm),
        ) {
            Text(
                awb.account.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (showCurrency) "${accountTypeLabel(awb.account.type)} · ${awb.account.currency}"
                else accountTypeLabel(awb.account.type),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        MoneyText(
            text = signedMoney(awb.balance, awb.account.currency, explicitPlus = false),
            color = balanceColor(awb.balance),
        )
    }
}

@Composable
private fun AddAccountDialog(
    onConfirm: (name: String, type: AccountType, initialMinor: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var initialText by remember { mutableStateOf("0") }
    var type by remember { mutableStateOf(AccountType.CASH) }
    val hapticLight = rememberHapticLight()
    val hapticReject = rememberHapticReject()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.account_new)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.account_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = initialText,
                    onValueChange = { initialText = it },
                    label = { Text(stringResource(R.string.account_initial_balance)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.account_type),
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
            }
        },
        confirmButton = {
            Button(onClick = {
                val minor = Money.parseToMinor(initialText, allowNegative = true)
                if (name.isNotBlank() && minor != null) {
                    onConfirm(name.trim(), type, minor)
                } else {
                    hapticReject()
                }
            }) { Text(stringResource(R.string.account_create)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/** 账户类型 3 列网格（新建 / 编辑账户共用）。 */
@Composable
internal fun AccountTypeGrid(
    selected: AccountType,
    onSelect: (AccountType) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AccountType.entries.chunked(3).forEach { rowTypes ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                rowTypes.forEach { t ->
                    AccountTypeOptionCard(
                        type = t,
                        selected = selected == t,
                        onClick = { onSelect(t) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** 账户类型选择块（emoji + 名称；选中为浅品牌色底，不加粗边框）。 */
@Composable
private fun AccountTypeOptionCard(
    type: AccountType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        selected = selected,
        shape = MaterialTheme.shapes.small,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AccountBadge(type = type, sizeDp = 32, modifier = Modifier.padding(bottom = 4.dp))
            Text(
                accountTypeLabel(type),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

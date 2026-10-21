package io.github.twc528491.betterledger.ui.account

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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Merge
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import io.github.twc528491.betterledger.data.AppContainer
import io.github.twc528491.betterledger.data.repository.AccountMergePlan
import io.github.twc528491.betterledger.data.repository.AccountPurgeImpact
import io.github.twc528491.betterledger.domain.calc.AccountRules
import io.github.twc528491.betterledger.domain.calc.BalanceCalculator
import io.github.twc528491.betterledger.domain.model.Account
import io.github.twc528491.betterledger.domain.model.AccountType
import io.github.twc528491.betterledger.domain.model.TransactionType
import io.github.twc528491.betterledger.navigation.Routes
import io.github.twc528491.betterledger.ui.common.AccountBadge
import io.github.twc528491.betterledger.ui.common.EmptyState
import io.github.twc528491.betterledger.ui.common.LedgerCard
import io.github.twc528491.betterledger.ui.common.LedgerGroup
import io.github.twc528491.betterledger.ui.common.LedgerGroupItem
import io.github.twc528491.betterledger.ui.common.LedgerIconTile
import io.github.twc528491.betterledger.ui.common.LedgerListRow
import io.github.twc528491.betterledger.ui.common.LedgerSectionHeader
import io.github.twc528491.betterledger.ui.common.MoneyText
import io.github.twc528491.betterledger.ui.common.accountTypeLabel
import io.github.twc528491.betterledger.ui.common.amountColor
import io.github.twc528491.betterledger.ui.common.cardColor
import io.github.twc528491.betterledger.ui.common.expenseAmountColor
import io.github.twc528491.betterledger.ui.common.signedMoney
import io.github.twc528491.betterledger.ui.theme.LedgerNumerals
import io.github.twc528491.betterledger.ui.theme.LedgerSpacing
import io.github.twc528491.betterledger.ui.common.rememberHapticConfirm
import io.github.twc528491.betterledger.ui.common.rememberHapticLight
import io.github.twc528491.betterledger.ui.common.rememberHapticNavigation
import io.github.twc528491.betterledger.ui.common.rememberHapticReject
import io.github.twc528491.betterledger.ui.settings.IrreversibleActionDialog
import io.github.twc528491.betterledger.ui.settings.IrreversibleActionTexts
import io.github.twc528491.betterledger.ui.settings.PurgeDialog
import io.github.twc528491.betterledger.ui.transaction.EntryPickerOption
import io.github.twc528491.betterledger.ui.transaction.EntryPickerSheet
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.twc528491.betterledger.R
import io.github.twc528491.betterledger.util.DateLabels
import io.github.twc528491.betterledger.util.Money
import io.github.twc528491.betterledger.util.userMessage
import io.github.twc528491.betterledger.util.TimeUtils
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

/**
 * 账户详情。账户全局共享：余额与交易列表汇总全部账本，
 * 其他账本的账单在副标题中标出账本名，可用筛选切换“仅当前账本 / 全部账本”。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(container: AppContainer, navController: NavHostController, accountId: String) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val acc by remember(accountId) { container.accountRepository.observeAccount(accountId) }
        .collectAsStateWithLifecycle(initialValue = null)
    val allAccounts by remember { container.accountRepository.observeAll() }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val txList by remember(accountId) { container.accountRepository.observeAccountTransactions(accountId) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val ledgers by container.ledgerRepository.ledgers.collectAsStateWithLifecycle(initialValue = emptyList())
    val currentLedger by container.ledgerRepository.currentLedger.collectAsStateWithLifecycle(initialValue = null)

    var showEdit by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showMergePicker by remember { mutableStateOf(false) }
    // 合并 / 彻底删除会删除当前账户：对话框使用打开时的账户快照，
    // 避免账户流先变为 null 导致对话框（及其协程）提前退出而未能返回。
    var mergeRequest by remember { mutableStateOf<Pair<Account, String>?>(null) }
    var purgeTarget by remember { mutableStateOf<Account?>(null) }
    var onlyCurrentLedger by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val hapticLight = rememberHapticLight()
    val hapticNavigation = rememberHapticNavigation()
    val hapticConfirm = rememberHapticConfirm()
    val hapticReject = rememberHapticReject()

    val currentLedgerId = currentLedger?.id
    val ledgerNameOf = remember(ledgers) { ledgers.associate { it.id to it.name } }
    // 余额始终汇总全部账本；筛选只影响列表。
    val balance = acc?.let { a ->
        val deltas = txList.mapNotNull {
            BalanceCalculator.deltaFor(it.type, it.amount, it.accountId, it.targetAccountId, a.id)
        }
        BalanceCalculator.compute(a.initialBalance, deltas)
    } ?: 0L
    val txs = if (onlyCurrentLedger) txList.filter { it.ledgerId == currentLedgerId } else txList

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(acc?.name ?: stringResource(R.string.account_details), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = {
                        hapticNavigation()
                        navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        hapticNavigation()
                        showEdit = true
                    }) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_edit))
                    }
                    IconButton(onClick = {
                        hapticLight()
                        showMenu = true
                    }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.action_more))
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.account_merge)) },
                            leadingIcon = { Icon(Icons.Outlined.Merge, contentDescription = null) },
                            enabled = acc != null,
                            onClick = {
                                hapticNavigation()
                                showMenu = false
                                showMergePicker = true
                            },
                        )
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
                                stringResource(R.string.account_current_balance),
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
                        stringResource(
                            R.string.account_detail_summary,
                            accountTypeLabel(acc?.type ?: AccountType.OTHER),
                            Money.formatWithCurrency(acc?.initialBalance ?: 0L, currency),
                        ),
                        style = LedgerNumerals.of(MaterialTheme.typography.bodySmall),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (acc?.isArchived == true) {
                item {
                    LedgerGroup(
                        title = stringResource(R.string.account_archived),
                        modifier = Modifier.padding(top = LedgerSpacing.lg),
                    ) {
                        LedgerListRow(
                            title = stringResource(R.string.account_restore),
                            subtitle = stringResource(R.string.account_restore_hint),
                            leading = { LedgerIconTile(Icons.Outlined.Restore) },
                            onClick = {
                                scope.launch {
                                    container.accountRepository.setArchived(accountId, false)
                                        .onSuccess { hapticConfirm() }
                                        .onFailure { hapticReject() }
                                }
                            },
                        )
                        LedgerListRow(
                            title = stringResource(R.string.account_purge),
                            titleColor = MaterialTheme.colorScheme.error,
                            subtitle = stringResource(R.string.account_purge_hint),
                            leading = {
                                LedgerIconTile(
                                    Icons.Outlined.DeleteForever,
                                    tint = MaterialTheme.colorScheme.error,
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                )
                            },
                            onClick = {
                                hapticConfirm()
                                purgeTarget = acc
                            },
                        )
                    }
                }
            }
            item {
                LedgerSectionHeader(
                    title = stringResource(R.string.account_transactions),
                    modifier = Modifier.padding(start = LedgerSpacing.xxs, top = LedgerSpacing.lg, bottom = LedgerSpacing.xxs),
                )
            }
            item {
                // 账户跨账本共享：列表默认显示全部账本，可切换为仅当前账本。
                Row(
                    horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.xs),
                    modifier = Modifier.padding(bottom = LedgerSpacing.xs),
                ) {
                    FilterChip(
                        selected = !onlyCurrentLedger,
                        onClick = {
                            hapticLight()
                            onlyCurrentLedger = false
                        },
                        label = { Text(stringResource(R.string.account_filter_all_ledgers)) },
                    )
                    FilterChip(
                        selected = onlyCurrentLedger,
                        onClick = {
                            hapticLight()
                            onlyCurrentLedger = true
                        },
                        label = { Text(stringResource(R.string.account_filter_current_ledger)) },
                    )
                }
            }
            if (txs.isEmpty()) {
                item {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = cardColor(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (onlyCurrentLedger && txList.isNotEmpty()) {
                            EmptyState(
                                icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                                title = stringResource(R.string.account_no_transactions_in_ledger),
                                subtitle = stringResource(R.string.account_no_transactions_in_ledger_hint),
                            )
                        } else {
                            EmptyState(
                                icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                                title = stringResource(R.string.account_no_transactions),
                                subtitle = stringResource(R.string.account_no_transactions_hint),
                            )
                        }
                    }
                }
            }
            val count = txs.size
            itemsIndexed(txs, key = { _, tx -> tx.id }) { index, tx ->
                val outgoingTransfer = tx.type == TransactionType.TRANSFER && tx.accountId == accountId
                val dateText = DateLabels.monthDay(TimeUtils.localDateOf(tx.transactionTime), locale)
                // 非当前账本的账单标出账本名；点击可直接编辑（按账单所属账本加载分类等）。
                val subtitle = if (tx.ledgerId != currentLedgerId) {
                    ledgerNameOf[tx.ledgerId]?.let { "$dateText · $it" } ?: dateText
                } else {
                    dateText
                }
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
                                subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
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

    val account = acc
    if (showMergePicker && account != null) {
        val candidates = remember(account, allAccounts) { AccountRules.mergeCandidates(account, allAccounts) }
        EntryPickerSheet(
            title = stringResource(R.string.account_merge),
            subtitle = stringResource(R.string.account_merge_pick_subtitle, account.name),
            hint = if (candidates.isEmpty()) stringResource(R.string.account_merge_no_candidates) else null,
            options = candidates.map { target ->
                EntryPickerOption(
                    id = target.id,
                    title = target.name,
                    detail = "${accountTypeLabel(target.type)} · ${target.currency}",
                    leading = { AccountBadge(target.type, sizeDp = 36) },
                )
            },
            onPick = { id ->
                showMergePicker = false
                mergeRequest = account to id
            },
            onDismiss = { showMergePicker = false },
        )
    }

    mergeRequest?.let { (source, targetId) ->
        AccountMergeDialog(
            container = container,
            source = source,
            targetId = targetId,
            onDismiss = { mergeRequest = null },
            onMerged = {
                mergeRequest = null
                // 源账户已删除：返回并打开合并后的目标账户。
                navController.popBackStack()
                navController.navigate(Routes.accountDetail(targetId))
            },
        )
    }

    purgeTarget?.let { target ->
        var impact by remember(target.id) { mutableStateOf<AccountPurgeImpact?>(null) }
        LaunchedEffect(target.id) { impact = container.purgeRepository.accountImpact(target.id) }
        val i = impact
        PurgeDialog(
            container = container,
            title = stringResource(R.string.account_purge),
            impact = if (i == null) {
                stringResource(R.string.account_purge_calculating)
            } else {
                val notes = buildList {
                    add(
                        stringResource(
                            R.string.account_purge_impact,
                            target.name,
                            pluralStringResource(R.plurals.account_purge_tx_count, i.transactionCount, i.transactionCount, i.transferCount),
                            pluralStringResource(R.plurals.account_purge_recurring_count, i.recurringCount, i.recurringCount),
                        ),
                    )
                    if (i.ledgerCount > 1) {
                        add(pluralStringResource(R.plurals.account_purge_ledgers_note, i.ledgerCount, i.ledgerCount))
                    }
                    if (i.transferCount > 0) add(stringResource(R.string.account_purge_transfer_note))
                }
                notes.reduce { joined, note -> stringResource(R.string.account_purge_impact_with_note, joined, note) }
            },
            confirmEnabled = i != null,
            onDismiss = { purgeTarget = null },
            onPurged = {
                purgeTarget = null
                navController.popBackStack()
            },
            purge = { container.purgeRepository.purgeAccount(target.id) },
        )
    }

    if (showEdit && account != null) {
        EditAccountDialog(
            account = account,
            onConfirm = { name, type, initialMinor, initialTime, archived ->
                scope.launch {
                    container.accountRepository.updateAccount(
                        account, name, type, initialMinor, initialTime, archived,
                    ).onSuccess {
                        hapticConfirm()
                        showEdit = false
                        error = null
                    }.onFailure {
                        hapticReject()
                        error = it.userMessage(context, R.string.account_update_failed)
                    }
                }
            },
            onDismiss = { showEdit = false; error = null },
            error = error,
        )
    }
}

/**
 * 合并确认：先计算影响（账单 / 定时扣款数、将删除的互转笔数、合并后的初始余额与时间），
 * 再以不可撤销确认框执行，默认勾选“合并前先立即备份”。
 */
@Composable
private fun AccountMergeDialog(
    container: AppContainer,
    source: Account,
    targetId: String,
    onDismiss: () -> Unit,
    onMerged: () -> Unit,
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    var plan by remember(source.id, targetId) { mutableStateOf<AccountMergePlan?>(null) }
    var planError by remember(source.id, targetId) { mutableStateOf<String?>(null) }
    LaunchedEffect(source.id, targetId) {
        runCatching { container.accountMergeRepository.plan(source.id, targetId) }
            .onSuccess { plan = it }
            .onFailure { planError = it.userMessage(context, R.string.account_merge_failed) }
    }
    val p = plan
    IrreversibleActionDialog(
        container = container,
        title = stringResource(R.string.account_merge_title),
        impact = when {
            planError != null -> planError!!
            p == null -> stringResource(R.string.account_purge_calculating)
            else -> stringResource(
                R.string.account_merge_impact,
                p.source.name,
                p.target.name,
                pluralStringResource(R.plurals.account_merge_tx_count, p.movedTransactions, p.movedTransactions),
                pluralStringResource(R.plurals.account_merge_recurring_count, p.recurringCharges, p.recurringCharges),
                Money.formatWithCurrency(p.mergedInitialBalance, p.target.currency),
                DateLabels.fullDate(TimeUtils.localDateOf(p.mergedInitialTime), locale),
            )
        },
        texts = IrreversibleActionTexts.MERGE_ACCOUNT,
        confirmEnabled = p != null,
        onDismiss = onDismiss,
        onDone = onMerged,
        action = { container.accountMergeRepository.merge(source.id, targetId).map { } },
        extra = {
            if (p != null && p.selfTransfers > 0) {
                Text(
                    pluralStringResource(R.plurals.account_merge_self_transfers, p.selfTransfers, p.selfTransfers),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
    )
}

@Composable
private fun typeLabelOf(t: TransactionType) = stringResource(
    when (t) {
        TransactionType.EXPENSE -> R.string.type_expense
        TransactionType.INCOME -> R.string.type_income
        TransactionType.TRANSFER -> R.string.type_transfer
    },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditAccountDialog(
    account: io.github.twc528491.betterledger.domain.model.Account,
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
    val nameRequiredMessage = stringResource(R.string.account_name_required)

        AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.account_edit)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it; nameError = null }, label = { Text(stringResource(R.string.account_name_short)) }, singleLine = true)
                nameError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = initialText, onValueChange = { initialText = it }, label = { Text(stringResource(R.string.account_opening_balance)) }, singleLine = true)
                Spacer(Modifier.height(12.dp))
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
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.account_opening_date), style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = {
                        hapticNavigation()
                        showDatePicker = true
                    }) {
                        Text(DateLabels.monthDay(TimeUtils.localDateOf(initialTime), LocalConfiguration.current.locales[0]))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(if (archived) R.string.account_archived else R.string.account_archive), style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = {
                        hapticLight()
                        archived = !archived
                    }) {
                        Text(stringResource(if (archived) R.string.action_restore else R.string.action_archive))
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isBlank()) {
                    hapticReject()
                    nameError = nameRequiredMessage
                    return@Button
                }
                val minor = Money.parseToMinor(initialText, allowNegative = true) ?: run {
                    hapticReject()
                    return@Button
                }
                onConfirm(name.trim(), type, minor, initialTime, archived)
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
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
                }) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

package io.github.twc528491.betterledger.ui.settings

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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import io.github.twc528491.betterledger.R
import io.github.twc528491.betterledger.data.AppContainer
import io.github.twc528491.betterledger.data.repository.RecurringChargeDraft
import io.github.twc528491.betterledger.domain.model.Account
import io.github.twc528491.betterledger.domain.model.Category
import io.github.twc528491.betterledger.domain.model.RecurrenceFrequency
import io.github.twc528491.betterledger.domain.model.RecurringCharge
import io.github.twc528491.betterledger.ui.common.CategoryBadge
import io.github.twc528491.betterledger.ui.common.EmptyState
import io.github.twc528491.betterledger.ui.common.MoneyText
import io.github.twc528491.betterledger.ui.common.cardColor
import io.github.twc528491.betterledger.ui.common.insetColor
import io.github.twc528491.betterledger.ui.common.rememberHapticDelete
import io.github.twc528491.betterledger.ui.theme.LedgerNumerals
import io.github.twc528491.betterledger.ui.theme.LedgerSpacing
import io.github.twc528491.betterledger.util.DateLabels
import io.github.twc528491.betterledger.util.Money
import io.github.twc528491.betterledger.util.userMessage
import io.github.twc528491.betterledger.viewmodel.RecurringChargesViewModel
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
    val context = LocalContext.current
    val appContext = context.applicationContext
    var editorVisible by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<RecurringCharge?>(null) }
    var deleting by remember { mutableStateOf<RecurringCharge?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    val hapticDelete = rememberHapticDelete()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.set_recurring_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
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
                text = { Text(stringResource(R.string.set_recurring_add_fab)) },
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
                            Text(stringResource(R.string.set_recurring_info), style = MaterialTheme.typography.bodyMedium)
                            Text(
                                stringResource(R.string.set_recurring_info_detail),
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
                        stringResource(R.string.set_recurring_no_ledger),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else if (state.charges.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.EventRepeat,
                        title = stringResource(R.string.set_recurring_empty),
                        subtitle = stringResource(R.string.set_recurring_empty_hint),
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
                                notice = result.exceptionOrNull()?.userMessage(context)
                                    ?: context.getString(
                                        if (charge.isPaused) R.string.set_recurring_resumed else R.string.set_recurring_paused_notice,
                                    )
                                if (result.isSuccess && charge.isPaused) {
                                    io.github.twc528491.betterledger.worker.RecurringChargeScheduler.enqueueCatchUp(
                                        appContext,
                                    )
                                }
                            }
                        },
                        onRetry = {
                            vm.retry(charge.id) { result ->
                                notice = result.exceptionOrNull()?.userMessage(context)
                                    ?: context.getString(R.string.set_recurring_retry_done)
                                if (result.isSuccess) {
                                    io.github.twc528491.betterledger.worker.RecurringChargeScheduler.enqueueCatchUp(
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
                        notice = context.getString(R.string.set_recurring_saved)
                        io.github.twc528491.betterledger.worker.RecurringChargeScheduler.enqueueCatchUp(
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
            title = { Text(stringResource(R.string.set_recurring_delete_title)) },
            text = { Text(stringResource(R.string.set_recurring_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    hapticDelete()
                    vm.delete(charge.id) { result ->
                        notice = result.exceptionOrNull()?.userMessage(context)
                            ?: context.getString(R.string.set_recurring_deleted)
                        if (result.isSuccess) deleting = null
                    }
                }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.action_cancel)) }
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
                        stringResource(
                            R.string.set_recurring_summary,
                            frequencyLabel(charge.frequency),
                            account?.name ?: stringResource(R.string.set_recurring_account_unavailable),
                            category?.name ?: stringResource(R.string.set_recurring_category_unavailable),
                        ),
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
                        stringResource(R.string.set_recurring_generate_failed, recurringErrorText(charge)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    TextButton(onClick = onRetry) { Text(stringResource(R.string.set_recurring_retry)) }
                }
                charge.isPaused -> Text(
                    stringResource(R.string.set_recurring_paused),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> {
                    val locale = LocalConfiguration.current.locales[0]
                    val nextAt = Instant.ofEpochMilli(charge.nextDueAt).atZone(charge.zoneId)
                    Text(
                        stringResource(
                            R.string.set_recurring_next,
                            DateLabels.fullDate(nextAt.toLocalDate(), locale),
                            DateTimeFormatter.ofPattern("HH:mm").format(nextAt),
                            charge.zoneId.id,
                        ),
                        style = LedgerNumerals.of(MaterialTheme.typography.bodySmall),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.xxs),
            ) {
                TextButton(onClick = onEdit) { Text(stringResource(R.string.action_edit)) }
                TextButton(onClick = onPauseToggle) {
                    Text(stringResource(if (charge.isPaused) R.string.set_recurring_resume else R.string.set_recurring_pause))
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDelete) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
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
    val context = LocalContext.current
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
        title = {
            Text(stringResource(if (charge == null) R.string.set_recurring_add_title else R.string.set_recurring_edit_title))
        },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val archivedSuffix = stringResource(R.string.label_archived_suffix)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.set_recurring_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(stringResource(R.string.set_recurring_amount, currency)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                RecurringChoiceField(
                    label = stringResource(R.string.set_recurring_account),
                    value = availableAccounts.firstOrNull { it.id == accountId }?.let {
                        it.name + if (it.isArchived) archivedSuffix else ""
                    }.orEmpty(),
                    choices = availableAccounts.map { it.id to (it.name + if (it.isArchived) archivedSuffix else "") },
                    onChoose = { accountId = it },
                )
                RecurringChoiceField(
                    label = stringResource(R.string.set_recurring_category),
                    value = availableCategories.firstOrNull { it.id == categoryId }?.let {
                        it.name + if (it.isArchived) archivedSuffix else ""
                    }.orEmpty(),
                    choices = availableCategories.map { it.id to (it.name + if (it.isArchived) archivedSuffix else "") },
                    onChoose = { categoryId = it },
                )
                RecurringChoiceField(
                    label = stringResource(R.string.set_recurring_frequency),
                    value = frequencyLabel(frequency),
                    choices = RecurrenceFrequency.entries.map { it.name to frequencyLabel(it) },
                    onChoose = { frequency = RecurrenceFrequency.valueOf(it) },
                )
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text(stringResource(R.string.set_recurring_start_date)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = timeText,
                    onValueChange = { timeText = it },
                    label = { Text(stringResource(R.string.set_recurring_time)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(stringResource(R.string.set_recurring_zone_note, zone.id), style = MaterialTheme.typography.bodySmall)
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
                    name.isBlank() -> formError = context.getString(R.string.set_recurring_error_name)
                    amount == null || amount <= 0 -> formError = context.getString(R.string.set_recurring_error_amount)
                    accountId.isBlank() -> formError = context.getString(R.string.set_recurring_error_account)
                    categoryId.isBlank() -> formError = context.getString(R.string.set_recurring_error_category)
                    start == null -> formError = context.getString(R.string.set_recurring_error_date)
                    time == null -> formError = context.getString(R.string.set_recurring_error_time)
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
                                formError = result.exceptionOrNull()?.userMessage(context, R.string.set_save_failed)
                                    ?: context.getString(R.string.set_save_failed)
                            }
                        }
                    }
                }
            }) { Text(stringResource(if (isSaving) R.string.set_saving else R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
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
                Text(
                    value.ifBlank { stringResource(R.string.set_please_choose) },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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

@Composable
private fun frequencyLabel(frequency: RecurrenceFrequency): String = stringResource(
    when (frequency) {
        RecurrenceFrequency.DAILY -> R.string.set_freq_daily
        RecurrenceFrequency.WEEKLY -> R.string.set_freq_weekly
        RecurrenceFrequency.MONTHLY -> R.string.set_freq_monthly
        RecurrenceFrequency.YEARLY -> R.string.set_freq_yearly
    },
)

/**
 * 定时扣款最近一次生成失败的原因。
 * 新数据为错误码，按当前语言解析；旧版保存的中文原文原样显示。
 */
@Composable
private fun recurringErrorText(charge: RecurringCharge): String =
    charge.lastError?.let { io.github.twc528491.betterledger.data.storedMessageText(it) }.orEmpty()

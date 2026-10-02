package com.personal.ledger.ui.transaction

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.personal.ledger.data.AppContainer
import com.personal.ledger.domain.model.Transaction
import com.personal.ledger.domain.model.TransactionType
import com.personal.ledger.ui.common.EmptyState
import com.personal.ledger.ui.common.LedgerGroupItem
import com.personal.ledger.ui.common.MoneyText
import com.personal.ledger.ui.common.MultiSelection
import com.personal.ledger.ui.common.SelectionCheckCircle
import com.personal.ledger.ui.common.SelectionTopBar
import com.personal.ledger.ui.common.amountColor
import com.personal.ledger.ui.common.rememberHapticConfirm
import com.personal.ledger.ui.common.rememberHapticDelete
import com.personal.ledger.ui.common.rememberHapticLight
import com.personal.ledger.ui.common.rememberHapticLongPress
import com.personal.ledger.ui.common.rememberHapticNavigation
import com.personal.ledger.ui.common.rememberHapticReject
import com.personal.ledger.ui.common.rememberMultiSelection
import com.personal.ledger.ui.common.selectionHighlightColor
import com.personal.ledger.ui.theme.LedgerSpacing
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.personal.ledger.R
import com.personal.ledger.util.DateLabels
import com.personal.ledger.util.Money
import com.personal.ledger.util.TimeUtils
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** 回收站里待确认的不可撤销操作。 */
private sealed interface RecyclePurgeRequest {
    data class Selected(val ids: List<String>) : RecyclePurgeRequest
    data class All(val ledgerId: String, val count: Int) : RecyclePurgeRequest
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    kotlinx.coroutines.ExperimentalCoroutinesApi::class,
)
@Composable
fun RecycleBinScreen(container: AppContainer, navController: NavHostController) {
    val scope = rememberCoroutineScope()
    val resources = LocalContext.current.resources
    val hapticNavigation = rememberHapticNavigation()
    val hapticConfirm = rememberHapticConfirm()
    val hapticReject = rememberHapticReject()
    val hapticLight = rememberHapticLight()
    val hapticLongPress = rememberHapticLongPress()
    val hapticDelete = rememberHapticDelete()
    val repository = container.transactionRepository
    val ledgerIdFlow = remember(container) {
        container.ledgerRepository.currentLedger.map { it?.id.orEmpty() }
    }
    val ledgerId by ledgerIdFlow.collectAsStateWithLifecycle(initialValue = "")
    val items by remember(ledgerIdFlow) {
        ledgerIdFlow.flatMapLatest { id ->
            if (id.isEmpty()) flowOf(emptyList()) else repository.observeRecycleBin(id)
        }
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    var selection by rememberMultiSelection()
    var purgeRequest by remember { mutableStateOf<RecyclePurgeRequest?>(null) }
    var busy by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val visibleIds = remember(items) { items.map { it.id } }
    LaunchedEffect(visibleIds) { selection = selection.retain(visibleIds) }
    BackHandler(enabled = selection.active) { selection = MultiSelection.None }

    fun notify(message: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    // 批量操作统一入口：防重复提交，成功后退出多选并提示结果。
    fun runBatch(action: suspend () -> Result<Int>, success: (Int) -> String, failure: String) {
        if (busy) return
        busy = true
        scope.launch {
            action()
                .onSuccess { n ->
                    hapticConfirm()
                    selection = MultiSelection.None
                    notify(success(n))
                }
                .onFailure {
                    hapticReject()
                    notify(failure)
                }
            busy = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (selection.active) {
                SelectionTopBar(
                    count = selection.count,
                    allSelected = selection.allSelected(visibleIds),
                    onClose = { selection = MultiSelection.None },
                    onToggleAll = {
                        hapticLight()
                        selection = selection.toggleAll(visibleIds)
                    },
                ) {
                    IconButton(
                        onClick = {
                            val ids = selection.selectedIn(visibleIds)
                            runBatch(
                                action = { repository.restoreAll(ids) },
                                success = { n -> resources.getQuantityString(R.plurals.tx_recycle_restored, n, n) },
                                failure = resources.getString(R.string.tx_recycle_restore_failed),
                            )
                        },
                        enabled = selection.count > 0 && !busy,
                    ) {
                        Icon(Icons.Filled.Restore, contentDescription = stringResource(R.string.tx_recycle_restore_selected))
                    }
                    IconButton(
                        onClick = { purgeRequest = RecyclePurgeRequest.Selected(selection.selectedIn(visibleIds)) },
                        enabled = selection.count > 0 && !busy,
                    ) {
                        Icon(Icons.Filled.DeleteForever, contentDescription = stringResource(R.string.tx_recycle_purge_selected))
                    }
                }
            } else {
                TopAppBar(
                    title = { Text(stringResource(R.string.tx_recycle_title)) },
                    navigationIcon = {
                        IconButton(onClick = {
                            hapticNavigation()
                            navController.popBackStack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    },
                    actions = {
                        if (items.isNotEmpty() && ledgerId.isNotEmpty()) {
                            IconButton(
                                onClick = { purgeRequest = RecyclePurgeRequest.All(ledgerId, items.size) },
                                enabled = !busy,
                            ) {
                                Icon(Icons.Filled.DeleteSweep, contentDescription = stringResource(R.string.tx_recycle_empty_bin))
                            }
                        }
                    },
                )
            }
        },
    ) { padding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.TopCenter,
            ) {
                EmptyState(
                    icon = Icons.Outlined.DeleteOutline,
                    title = stringResource(R.string.tx_recycle_empty_state),
                    modifier = Modifier.padding(top = LedgerSpacing.xxl),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(
                    start = LedgerSpacing.md,
                    top = LedgerSpacing.xs,
                    end = LedgerSpacing.md,
                    bottom = LedgerSpacing.xl,
                ),
            ) {
                item(key = "hint", contentType = "hint") {
                    Text(
                        stringResource(if (selection.active) R.string.tx_recycle_hint_selecting else R.string.tx_recycle_hint),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = LedgerSpacing.xxs, bottom = LedgerSpacing.xs),
                    )
                }
                val count = items.size
                itemsIndexed(items, key = { _, tx -> tx.id }) { index, tx ->
                    LedgerGroupItem(index = index, count = count) {
                        RecycleBinRow(
                            tx = tx,
                            selectionActive = selection.active,
                            selected = tx.id in selection,
                            onClick = {
                                if (selection.active) {
                                    hapticLight()
                                    selection = selection.toggle(tx.id)
                                }
                            },
                            onLongClick = {
                                if (!selection.active) hapticLongPress() else hapticLight()
                                selection = selection.longPress(tx.id)
                            },
                            onRestore = {
                                scope.launch {
                                    repository.restore(tx.id)
                                        .onSuccess { hapticConfirm() }
                                        .onFailure { hapticReject() }
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    purgeRequest?.let { request ->
        val n = when (request) {
            is RecyclePurgeRequest.Selected -> request.ids.size
            is RecyclePurgeRequest.All -> request.count
        }
        AlertDialog(
            onDismissRequest = { purgeRequest = null },
            title = {
                Text(
                    when (request) {
                        is RecyclePurgeRequest.Selected -> pluralStringResource(R.plurals.tx_recycle_purge_title, n, n)
                        is RecyclePurgeRequest.All -> stringResource(R.string.tx_recycle_empty_title)
                    },
                )
            },
            text = {
                Text(
                    when (request) {
                        is RecyclePurgeRequest.Selected -> stringResource(R.string.tx_recycle_purge_message)
                        is RecyclePurgeRequest.All -> pluralStringResource(R.plurals.tx_recycle_empty_message, n, n)
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    hapticDelete()
                    purgeRequest = null
                    when (request) {
                        is RecyclePurgeRequest.Selected -> runBatch(
                            action = { repository.purgeAll(request.ids) },
                            success = { deleted -> resources.getQuantityString(R.plurals.tx_recycle_purged, deleted, deleted) },
                            failure = resources.getString(R.string.tx_delete_failed),
                        )
                        is RecyclePurgeRequest.All -> runBatch(
                            action = { repository.emptyRecycleBin(request.ledgerId) },
                            success = { deleted -> resources.getQuantityString(R.plurals.tx_recycle_emptied, deleted, deleted) },
                            failure = resources.getString(R.string.tx_recycle_empty_failed),
                        )
                    }
                }) {
                    Text(
                        stringResource(if (request is RecyclePurgeRequest.All) R.string.tx_recycle_empty_action else R.string.tx_recycle_purge_action),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { purgeRequest = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecycleBinRow(
    tx: Transaction,
    selectionActive: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRestore: () -> Unit,
) {
    val rowBackground by animateColorAsState(
        if (selected) selectionHighlightColor() else Color.Transparent,
        animationSpec = tween(150),
        label = "recycle-selected-bg",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBackground)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                onClickLabel = if (selectionActive) stringResource(if (selected) R.string.tx_deselect else R.string.tx_select) else null,
                onLongClickLabel = if (selectionActive) null else stringResource(R.string.tx_multi_select),
                hapticFeedbackEnabled = false,
            )
            .then(if (selectionActive) Modifier.semantics { this.selected = selected } else Modifier)
            .heightIn(min = 56.dp)
            .padding(start = LedgerSpacing.md, end = LedgerSpacing.xxs, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(
            visible = selectionActive,
            enter = fadeIn(tween(150)) + expandHorizontally(tween(150)),
            exit = fadeOut(tween(120)) + shrinkHorizontally(tween(120)),
        ) {
            SelectionCheckCircle(
                selected = selected,
                modifier = Modifier.padding(end = LedgerSpacing.sm),
            )
        }
        Column(modifier = Modifier.weight(1f).padding(end = LedgerSpacing.sm)) {
            Text(
                tx.note ?: stringResource(R.string.tx_no_note),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                DateLabels.monthDay(TimeUtils.localDateOf(tx.transactionTime), LocalConfiguration.current.locales[0]),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        MoneyText(
            text = when (tx.type) {
                TransactionType.EXPENSE -> Money.formatSignedMinor(-tx.amount)
                TransactionType.INCOME -> Money.formatSignedMinor(tx.amount, explicitPlus = true)
                TransactionType.TRANSFER -> Money.formatMinor(tx.amount)
            },
            color = amountColor(tx.type),
            modifier = Modifier.padding(end = if (selectionActive) LedgerSpacing.sm else 0.dp),
        )
        // 多选时隐藏单条恢复按钮，避免与批量操作混淆。
        if (!selectionActive) {
            IconButton(onClick = onRestore) {
                Icon(
                    Icons.Filled.Restore,
                    contentDescription = stringResource(R.string.action_restore),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

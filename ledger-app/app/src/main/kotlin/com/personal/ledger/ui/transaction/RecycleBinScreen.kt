package com.personal.ledger.ui.transaction

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
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.personal.ledger.data.AppContainer
import com.personal.ledger.domain.model.TransactionType
import com.personal.ledger.ui.common.EmptyState
import com.personal.ledger.ui.common.LedgerGroupItem
import com.personal.ledger.ui.common.MoneyText
import com.personal.ledger.ui.common.amountColor
import com.personal.ledger.ui.theme.LedgerSpacing
import com.personal.ledger.ui.common.rememberHapticConfirm
import com.personal.ledger.ui.common.rememberHapticNavigation
import com.personal.ledger.ui.common.rememberHapticReject
import com.personal.ledger.util.Money
import com.personal.ledger.util.TimeUtils
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Composable
fun RecycleBinScreen(container: AppContainer, navController: NavHostController) {
    val scope = rememberCoroutineScope()
    val hapticNavigation = rememberHapticNavigation()
    val hapticConfirm = rememberHapticConfirm()
    val hapticReject = rememberHapticReject()
    val ledgerId = container.ledgerRepository.currentLedger
        .map { it?.id.orEmpty() }
    val items by ledgerId
        .flatMapLatest { id ->
            if (id.isEmpty()) flowOf(emptyList()) else container.transactionRepository.observeRecycleBin(id)
        }
        .collectAsStateWithLifecycle(initialValue = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("回收站") },
                navigationIcon = {
                    IconButton(onClick = {
                        hapticNavigation()
                        navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.TopCenter,
            ) {
                EmptyState(
                    icon = Icons.Outlined.DeleteOutline,
                    title = "回收站为空",
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
                val count = items.size
                itemsIndexed(items, key = { _, tx -> tx.id }) { index, tx ->
                    LedgerGroupItem(index = index, count = count) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .padding(start = LedgerSpacing.md, end = LedgerSpacing.xxs, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = LedgerSpacing.sm)) {
                                Text(
                                    tx.note ?: "无备注",
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
                                    TransactionType.EXPENSE -> Money.formatSignedMinor(-tx.amount)
                                    TransactionType.INCOME -> Money.formatSignedMinor(tx.amount, explicitPlus = true)
                                    TransactionType.TRANSFER -> Money.formatMinor(tx.amount)
                                },
                                color = amountColor(tx.type),
                            )
                            IconButton(onClick = {
                                scope.launch {
                                    container.transactionRepository.restore(tx.id)
                                        .onSuccess { hapticConfirm() }
                                        .onFailure { hapticReject() }
                                }
                            }) {
                                Icon(
                                    Icons.Filled.Restore,
                                    contentDescription = "恢复",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

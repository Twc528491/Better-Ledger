package com.personal.ledger.ui.transaction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.personal.ledger.data.AppContainer
import com.personal.ledger.domain.model.TransactionType
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
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        if (items.isEmpty()) {
            Text(
                "回收站为空",
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(items, key = { it.id }) { tx ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                tx.note ?: "无备注",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                TimeUtils.formatMonthDay(tx.transactionTime),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                when (tx.type) {
                                    TransactionType.EXPENSE -> Money.formatSignedMinor(-tx.amount)
                                    TransactionType.INCOME -> Money.formatSignedMinor(tx.amount, explicitPlus = true)
                                    TransactionType.TRANSFER -> Money.formatMinor(tx.amount)
                                },
                                style = MaterialTheme.typography.titleMedium,
                            )
                            IconButton(onClick = {
                                scope.launch { container.transactionRepository.restore(tx.id) }
                            }) {
                                Icon(Icons.Filled.Restore, contentDescription = "恢复")
                            }
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

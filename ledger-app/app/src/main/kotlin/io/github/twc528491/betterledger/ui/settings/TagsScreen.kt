package io.github.twc528491.betterledger.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import io.github.twc528491.betterledger.R
import io.github.twc528491.betterledger.data.AppContainer
import io.github.twc528491.betterledger.ui.common.EmptyState
import io.github.twc528491.betterledger.domain.model.Tag
import io.github.twc528491.betterledger.ui.common.LedgerGroupItem
import io.github.twc528491.betterledger.ui.common.LedgerGroupLabel
import io.github.twc528491.betterledger.ui.common.LedgerIconTile
import io.github.twc528491.betterledger.ui.common.LedgerListRow
import io.github.twc528491.betterledger.ui.theme.LedgerSpacing
import io.github.twc528491.betterledger.ui.common.rememberHapticConfirm
import io.github.twc528491.betterledger.ui.common.rememberHapticNavigation
import io.github.twc528491.betterledger.ui.common.rememberHapticReject
import io.github.twc528491.betterledger.util.userMessage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsScreen(container: AppContainer, navController: NavHostController) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val currentLedger by container.ledgerRepository.currentLedger.collectAsStateWithLifecycle(initialValue = null)
    val tags by container.tagRepository.observeByLedger(
        currentLedger?.id ?: ""
    ).collectAsStateWithLifecycle(initialValue = emptyList())
    var showAdd by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var purgeTarget by remember { mutableStateOf<Tag?>(null) }
    val hapticNavigation = rememberHapticNavigation()
    val hapticConfirm = rememberHapticConfirm()
    val hapticReject = rememberHapticReject()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.set_tags_title)) },
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
                        showAdd = true
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.set_tag_new))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(
                start = LedgerSpacing.md,
                top = LedgerSpacing.xs,
                end = LedgerSpacing.md,
                bottom = LedgerSpacing.xl,
            ),
        ) {
            if (tags.isEmpty()) {
                item(key = "tags_empty") {
                    EmptyState(
                        icon = Icons.Outlined.Sell,
                        title = stringResource(R.string.set_tag_empty),
                        subtitle = stringResource(R.string.set_tag_empty_hint),
                    )
                }
            }
            val active = tags.filterNot { it.isArchived }
            val archived = tags.filter { it.isArchived }
            itemsIndexed(active, key = { _, tag -> tag.id }) { index, tag ->
                LedgerGroupItem(index = index, count = active.size) {
                    LedgerListRow(
                        title = tag.name,
                        leading = { LedgerIconTile(Icons.Outlined.Sell) },
                        trailing = {
                            Text(
                                stringResource(R.string.action_archive),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        },
                        onClick = {
                            scope.launch {
                                container.tagRepository.setArchived(tag.id, true)
                                    .onSuccess { hapticConfirm() }
                                    .onFailure { hapticReject() }
                            }
                        },
                    )
                }
            }
            if (archived.isNotEmpty()) {
                item(key = "tags_archived_label") {
                    LedgerGroupLabel(stringResource(R.string.set_tag_archived_section), modifier = Modifier.padding(top = LedgerSpacing.md))
                }
                itemsIndexed(archived, key = { _, tag -> tag.id }) { index, tag ->
                    LedgerGroupItem(index = index, count = archived.size) {
                        LedgerListRow(
                            title = tag.name,
                            titleColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            leading = {
                                LedgerIconTile(Icons.Outlined.Sell, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            trailing = {
                                Row {
                                    TextButton(onClick = {
                                        scope.launch {
                                            container.tagRepository.setArchived(tag.id, false)
                                                .onSuccess { hapticConfirm() }
                                                .onFailure { hapticReject() }
                                        }
                                    }) { Text(stringResource(R.string.action_restore)) }
                                    TextButton(onClick = {
                                        hapticConfirm()
                                        purgeTarget = tag
                                    }) { Text(stringResource(R.string.set_purge_short), color = MaterialTheme.colorScheme.error) }
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    purgeTarget?.let { tag ->
        var usage by remember(tag.id) { mutableStateOf<Int?>(null) }
        LaunchedEffect(tag.id) { usage = container.purgeRepository.tagUsage(tag.id) }
        PurgeDialog(
            container = container,
            title = stringResource(R.string.set_tag_purge_title),
            impact = usage?.let {
                pluralStringResource(R.plurals.set_tag_purge_impact, it, tag.name, it)
            } ?: stringResource(R.string.set_purge_counting),
            confirmEnabled = usage != null,
            onDismiss = { purgeTarget = null },
            onPurged = { purgeTarget = null },
            purge = { container.purgeRepository.purgeTag(tag.id) },
        )
    }

    if (showAdd) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text(stringResource(R.string.set_tag_new)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.set_tag_name_label)) },
                        singleLine = true,
                    )
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val ledgerId = currentLedger?.id ?: return@TextButton
                    scope.launch {
                        container.tagRepository.addTag(ledgerId, name)
                            .onSuccess {
                                hapticConfirm()
                                showAdd = false
                                error = null
                            }
                            .onFailure {
                                hapticReject()
                                error = it.userMessage(context, R.string.set_create_failed)
                            }
                    }
                }) { Text(stringResource(R.string.set_create)) }
            },
            dismissButton = {
                TextButton(onClick = { showAdd = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

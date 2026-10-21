package io.github.twc528491.betterledger.ui.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import io.github.twc528491.betterledger.R
import io.github.twc528491.betterledger.domain.model.Transaction

/** 长按账单后的操作层，并在真正删除前保留第二次确认。 */
@Composable
fun TransactionDeleteDialogs(
    transaction: Transaction?,
    onDismiss: () -> Unit,
    onDelete: (Transaction, onFinished: () -> Unit) -> Unit,
) {
    if (transaction == null) return
    var awaitingConfirmation by remember(transaction.id) { mutableStateOf(false) }
    val hapticConfirm = rememberHapticConfirm()
    val hapticDelete = rememberHapticDelete()

    if (!awaitingConfirmation) {
        AlertDialog(
            onDismissRequest = {
                onDismiss()
            },
            title = { Text(stringResource(R.string.core_tx_actions_title)) },
            text = { Text(stringResource(R.string.core_tx_actions_message)) },
            confirmButton = {
                TextButton(onClick = {
                    hapticConfirm()
                    awaitingConfirmation = true
                }) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onDismiss()
                }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    } else {
        AlertDialog(
            onDismissRequest = {
                onDismiss()
            },
            title = { Text(stringResource(R.string.action_confirm_delete)) },
            text = { Text(stringResource(R.string.core_tx_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    hapticDelete()
                    onDelete(transaction, onDismiss)
                }) {
                    Text(stringResource(R.string.action_confirm_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    awaitingConfirmation = false
                }) { Text(stringResource(R.string.action_back)) }
            },
        )
    }
}

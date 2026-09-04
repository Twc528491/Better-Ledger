package com.personal.ledger.ui.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.personal.ledger.domain.model.Transaction

/** 长按账单后的操作层，并在真正删除前保留第二次确认。 */
@Composable
fun TransactionDeleteDialogs(
    transaction: Transaction?,
    onDismiss: () -> Unit,
    onDelete: (Transaction, onFinished: () -> Unit) -> Unit,
) {
    if (transaction == null) return
    var awaitingConfirmation by remember(transaction.id) { mutableStateOf(false) }
    val hapticNavigation = rememberHapticNavigation()
    val hapticConfirm = rememberHapticConfirm()
    val hapticStrong = rememberHapticStrong()

    if (!awaitingConfirmation) {
        AlertDialog(
            onDismissRequest = {
                hapticNavigation()
                onDismiss()
            },
            title = { Text("账单操作") },
            text = { Text("要对这笔账单做什么？") },
            confirmButton = {
                TextButton(onClick = {
                    hapticConfirm()
                    awaitingConfirmation = true
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    hapticNavigation()
                    onDismiss()
                }) { Text("取消") }
            },
        )
    } else {
        AlertDialog(
            onDismissRequest = {
                hapticNavigation()
                onDismiss()
            },
            title = { Text("确认删除") },
            text = { Text("删除后账单会进入回收站，仍可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    hapticStrong()
                    onDelete(transaction, onDismiss)
                }) {
                    Text("确认删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    hapticNavigation()
                    awaitingConfirmation = false
                }) { Text("返回") }
            },
        )
    }
}

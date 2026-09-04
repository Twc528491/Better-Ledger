package com.personal.ledger.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.personal.ledger.ui.theme.LedgerShapes
import com.personal.ledger.ui.theme.LedgerSpacing

/** 全应用统一的内容容器；扁平、低层级，不额外制造强阴影。 */
@Composable
fun LedgerCard(
    modifier: Modifier = Modifier,
    containerColor: Color = cardColor(),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = containerColor,
        shape = LedgerShapes.large,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(LedgerSpacing.md),
            verticalArrangement = Arrangement.spacedBy(LedgerSpacing.sm),
            content = content,
        )
    }
}

@Composable
fun LedgerSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun MoneyText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    prominent: Boolean = false,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = if (prominent) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
}

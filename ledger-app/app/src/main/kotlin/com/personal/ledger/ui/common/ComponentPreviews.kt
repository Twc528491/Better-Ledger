package com.personal.ledger.ui.common

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.personal.ledger.ui.theme.LedgerTheme

@Preview(name = "通用卡片 - 浅色", showBackground = true)
@Composable
private fun LedgerCardPreview() {
    LedgerTheme {
        LedgerCard {
            LedgerSectionHeader(title = "最近交易", actionLabel = "查看全部", onAction = {})
            Text("午餐 · 招商银行")
            MoneyText(text = "¥-25.00")
        }
    }
}

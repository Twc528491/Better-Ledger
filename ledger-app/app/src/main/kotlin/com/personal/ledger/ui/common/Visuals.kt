package com.personal.ledger.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.ledger.domain.model.AccountType
import com.personal.ledger.domain.model.CategoryType
import com.personal.ledger.domain.model.TransactionType
import kotlin.math.abs

/**
 * UI 视觉语义（配色 / 图标 / 分类 emoji）。
 * 收入 / 支出 / 负债除颜色外始终配合 + / - 号（UI.md 第 9 节）。
 */

object LedgerColors {
    val Expense = Color(0xFFE53935)
    val ExpenseContainer = Color(0xFFFFEBEE)
    val Income = Color(0xFF43A047)
    val IncomeContainer = Color(0xFFE8F5E9)
    val Transfer = Color(0xFF1E88E5)
    val TransferContainer = Color(0xFFE3F2FD)
    val Warning = Color(0xFFFB8C00)
    val Danger = Color(0xFFD32F2F)
}

/** 金额颜色：支出红 / 收入绿 / 转账蓝。 */
fun amountColor(type: TransactionType, fallback: Color): Color = when (type) {
    TransactionType.EXPENSE -> LedgerColors.Expense
    TransactionType.INCOME -> LedgerColors.Income
    TransactionType.TRANSFER -> LedgerColors.Transfer
}

/** 余额颜色：正 = 资产（绿），负 = 负债（红）。 */
fun balanceColor(balance: Long, fallback: Color): Color =
    if (balance >= 0) LedgerColors.Income else LedgerColors.Expense

/** 预算进度颜色：<80% 绿，80~100% 橙，超支红；未设置用默认色。 */
fun budgetColor(permille: Int?, fallback: Color): Color = when {
    permille == null -> fallback
    permille < 800 -> LedgerColors.Income
    permille < 1000 -> LedgerColors.Warning
    else -> LedgerColors.Expense
}

/** 分类 emoji（种子分类带专属 emoji；自定义分类按类型回退）。 */
object CategoryVisuals {
    private val emojiByKey = mapOf(
        "餐饮" to "🍜", "交通" to "🚌", "购物" to "🛒", "居住" to "🏠",
        "娱乐" to "🎮", "医疗" to "🏥", "其他" to "📦",
        "工资" to "💰", "其他收入" to "🎁",
    )

    fun emojiFor(name: String, type: CategoryType): String =
        emojiByKey[name] ?: if (type == CategoryType.INCOME) "💵" else "🏷️"

    /** 按名称稳定取色（同一分类永远同色）。 */
    fun colorFor(name: String): Color {
        val hue = abs(name.hashCode()) % 360
        return Color.hsv(hue.toFloat(), 0.45f, 0.92f)
    }
}

/** 账户类型视觉：emoji + 主题色。 */
object AccountVisuals {
    fun emojiFor(type: AccountType): String = when (type) {
        AccountType.CASH -> "💵"
        AccountType.BANK -> "🏦"
        AccountType.ALIPAY -> "🔵"
        AccountType.WECHAT -> "💚"
        AccountType.CREDIT_CARD -> "💳"
        AccountType.SAVINGS -> "🐷"
        AccountType.INVESTMENT -> "📈"
        AccountType.LOAN -> "🏛️"
        AccountType.OTHER -> "👛"
    }

    fun colorFor(type: AccountType): Color = when (type) {
        AccountType.CASH -> Color(0xFF43A047)
        AccountType.BANK -> Color(0xFF1E88E5)
        AccountType.ALIPAY -> Color(0xFF1677FF)
        AccountType.WECHAT -> Color(0xFF07C160)
        AccountType.CREDIT_CARD -> Color(0xFF8E24AA)
        AccountType.SAVINGS -> Color(0xFFFB8C00)
        AccountType.INVESTMENT -> Color(0xFF00897B)
        AccountType.LOAN -> Color(0xFFE53935)
        AccountType.OTHER -> Color(0xFF757575)
    }
}

/** 带色底圆标的 emoji 图标。 */
@Composable
fun EmojiBadge(
    emoji: String,
    background: Color,
    modifier: Modifier = Modifier,
    sizeDp: Int = 36,
) {
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .background(background, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, fontSize = (sizeDp * 0.55f).sp)
    }
}

/** 分类圆标（emoji + 稳定分类色）。 */
@Composable
fun CategoryBadge(name: String, type: CategoryType, modifier: Modifier = Modifier) {
    EmojiBadge(
        emoji = CategoryVisuals.emojiFor(name, type),
        background = CategoryVisuals.colorFor(name),
        modifier = modifier,
    )
}

/** 账户圆标。 */
@Composable
fun AccountBadge(type: AccountType, modifier: Modifier = Modifier) {
    EmojiBadge(
        emoji = AccountVisuals.emojiFor(type),
        background = AccountVisuals.colorFor(type),
        modifier = modifier,
    )
}

package com.personal.ledger.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.ledger.domain.model.AccountType
import com.personal.ledger.domain.model.CategoryType
import com.personal.ledger.domain.model.TransactionType
import kotlin.math.abs

/**
 * UI 视觉语义（配色 / 图标 / 分类 emoji），浅色与深色主题自适应。
 * 收入 / 支出 / 负债除颜色外始终配合 + / - 号（UI.md 第 9 节）。
 */

/** 支出语义色（浅/深）。 */
@Composable fun expenseAmountColor(): Color =
    if (isSystemInDarkTheme()) Color(0xFFFF8A80) else Color(0xFFE53935)

/** 支出容器色（浅/深）。 */
@Composable fun expenseContainerColor(): Color =
    if (isSystemInDarkTheme()) Color(0xFF3A2020) else Color(0xFFFFEBEE)

/** 收入语义色。 */
@Composable fun incomeAmountColor(): Color =
    if (isSystemInDarkTheme()) Color(0xFF81C784) else Color(0xFF43A047)

/** 收入容器色。 */
@Composable fun incomeContainerColor(): Color =
    if (isSystemInDarkTheme()) Color(0xFF1B3520) else Color(0xFFE8F5E9)

/** 转账语义色。 */
@Composable fun transferAmountColor(): Color =
    if (isSystemInDarkTheme()) Color(0xFF90CAF9) else Color(0xFF1E88E5)

/** 转账容器色。 */
@Composable fun transferContainerColor(): Color =
    if (isSystemInDarkTheme()) Color(0xFF1A2B3D) else Color(0xFFE3F2FD)

/** 警示（预算 80~100%）。 */
@Composable fun warningColor(): Color =
    if (isSystemInDarkTheme()) Color(0xFFFFB74D) else Color(0xFFFB8C00)

/** 金额颜色：支出红 / 收入绿 / 转账蓝。 */
@Composable
fun amountColor(type: TransactionType): Color = when (type) {
    TransactionType.EXPENSE -> expenseAmountColor()
    TransactionType.INCOME -> incomeAmountColor()
    TransactionType.TRANSFER -> transferAmountColor()
}

/** 余额颜色：正 = 资产（绿），负 = 负债（红）。 */
@Composable
fun balanceColor(balance: Long): Color =
    if (balance >= 0) incomeAmountColor() else expenseAmountColor()

/** 预算进度颜色：<80% 绿，80~100% 橙，超支红；未设置用主色。 */
@Composable
fun budgetColor(permille: Int?, fallback: Color): Color = when {
    permille == null -> fallback
    permille < 800 -> incomeAmountColor()
    permille < 1000 -> warningColor()
    else -> expenseAmountColor()
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
}

/** 按名称稳定取色（同一分类永远同色；深色模式降低亮度保证对比）。 */
@Composable
fun categoryColor(name: String): Color {
    val hue = abs(name.hashCode()) % 360
    return if (isSystemInDarkTheme()) {
        Color.hsv(hue.toFloat(), 0.30f, 0.38f)
    } else {
        Color.hsv(hue.toFloat(), 0.42f, 0.90f)
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
}

/** 账户类型主题色（深色模式用亮色调保证可见）。 */
@Composable
fun accountColor(type: AccountType): Color {
    val dark = isSystemInDarkTheme()
    return when (type) {
        AccountType.CASH -> if (dark) Color(0xFF81C784) else Color(0xFF43A047)
        AccountType.BANK -> if (dark) Color(0xFF90CAF9) else Color(0xFF1E88E5)
        AccountType.ALIPAY -> if (dark) Color(0xFF6FA8FF) else Color(0xFF1677FF)
        AccountType.WECHAT -> if (dark) Color(0xFF66E09A) else Color(0xFF07C160)
        AccountType.CREDIT_CARD -> if (dark) Color(0xFFCE93D8) else Color(0xFF8E24AA)
        AccountType.SAVINGS -> if (dark) Color(0xFFFFB74D) else Color(0xFFFB8C00)
        AccountType.INVESTMENT -> if (dark) Color(0xFF4DB6AC) else Color(0xFF00897B)
        AccountType.LOAN -> if (dark) Color(0xFFFF8A80) else Color(0xFFE53935)
        AccountType.OTHER -> if (dark) Color(0xFFBDBDBD) else Color(0xFF757575)
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
fun CategoryBadge(name: String, type: CategoryType, modifier: Modifier = Modifier, sizeDp: Int = 36) {
    EmojiBadge(
        emoji = CategoryVisuals.emojiFor(name, type),
        background = categoryColor(name),
        modifier = modifier,
        sizeDp = sizeDp,
    )
}

/** 账户圆标。 */
@Composable
fun AccountBadge(type: AccountType, modifier: Modifier = Modifier, sizeDp: Int = 36) {
    EmojiBadge(
        emoji = AccountVisuals.emojiFor(type),
        background = accountColor(type),
        modifier = modifier,
        sizeDp = sizeDp,
    )
}

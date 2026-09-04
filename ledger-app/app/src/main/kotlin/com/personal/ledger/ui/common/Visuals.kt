package com.personal.ledger.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
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
import com.personal.ledger.ui.theme.LocalLedgerDarkTheme

/**
 * UI 视觉语义（配色 / 图标 / 分类 emoji），按主题分支：
 * - 浅色模式：沿用旧版扁平样式——卡片用默认 surface、金额/余额用默认文字色，保证辨识度；
 * - 深色模式：使用丰富配色（卡片深色容器 + 亮色金额）。
 * 收入 / 支出 / 负债除颜色外始终配合 + / - 号（UI.md 第 9 节）。
 */

/** 卡片容器色：深色 = 深色容器；浅色 = 默认 surface（旧版扁平）。 */
@Composable fun cardColor(): Color =
    if (LocalLedgerDarkTheme.current) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surface

/** 主卡片（本月支出 / 净资产）背景色。 */
@Composable fun highlightCardColor(): Color =
    if (LocalLedgerDarkTheme.current) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface

/** 主卡片内容色。 */
@Composable fun highlightCardContentColor(): Color =
    if (LocalLedgerDarkTheme.current) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

/** 支出语义色（仅深色模式使用彩色；浅色回退默认文字色）。 */
@Composable fun expenseAmountColor(): Color =
    if (LocalLedgerDarkTheme.current) Color(0xFFFF8A80) else MaterialTheme.colorScheme.onSurface

/** 支出容器色。 */
@Composable fun expenseContainerColor(): Color =
    if (LocalLedgerDarkTheme.current) Color(0xFF3A2020) else MaterialTheme.colorScheme.surface

/** 收入语义色。 */
@Composable fun incomeAmountColor(): Color =
    if (LocalLedgerDarkTheme.current) Color(0xFF81C784) else MaterialTheme.colorScheme.onSurface

/** 收入容器色。 */
@Composable fun incomeContainerColor(): Color =
    if (LocalLedgerDarkTheme.current) Color(0xFF1B3520) else MaterialTheme.colorScheme.surface

/** 转账语义色。 */
@Composable fun transferAmountColor(): Color =
    if (LocalLedgerDarkTheme.current) Color(0xFF90CAF9) else MaterialTheme.colorScheme.onSurface

/** 转账容器色。 */
@Composable fun transferContainerColor(): Color =
    if (LocalLedgerDarkTheme.current) Color(0xFF1A2B3D) else MaterialTheme.colorScheme.surface

/** 警示（预算 80~100%）。 */
@Composable fun warningColor(): Color =
    if (LocalLedgerDarkTheme.current) Color(0xFFFFB74D) else MaterialTheme.colorScheme.onSurface

/** 金额颜色：深色 = 支出红 / 收入绿 / 转账蓝；浅色 = 默认文字色（旧版样式）。 */
@Composable
fun amountColor(type: TransactionType): Color = when (type) {
    TransactionType.EXPENSE -> expenseAmountColor()
    TransactionType.INCOME -> incomeAmountColor()
    TransactionType.TRANSFER -> transferAmountColor()
}

/** 余额颜色：深色 = 正绿负红；浅色 = 默认文字色。 */
@Composable
fun balanceColor(balance: Long): Color =
    if (LocalLedgerDarkTheme.current) {
        if (balance >= 0) Color(0xFF81C784) else Color(0xFFFF8A80)
    } else {
        MaterialTheme.colorScheme.onSurface
    }

/** 预算进度颜色：深色 = <80% 绿 / 80~100% 橙 / 超支红；浅色 = 主色（旧版样式）。 */
@Composable
fun budgetColor(permille: Int?, fallback: Color): Color {
    if (!LocalLedgerDarkTheme.current) return fallback
    return when {
        permille == null -> fallback
        permille < 800 -> Color(0xFF81C784)
        permille < 1000 -> Color(0xFFFFB74D)
        else -> Color(0xFFFF8A80)
    }
}

/** 分类 emoji（种子分类带专属 emoji；自定义分类按类型回退）。 */
object CategoryVisuals {
    private val emojiByKey = mapOf(
        "餐饮" to "🍜", "交通" to "🚌", "购物" to "🛒", "居住" to "🏠",
        "娱乐" to "🎮", "医疗" to "🏥", "其他" to "📦",
        "工资" to "💰", "其他收入" to "🎁",
    )

    private val expenseFallbacks = listOf("🏷️", "🧾", "📦", "🪙", "🛍️")
    private val incomeFallbacks = listOf("💵", "💰", "🎁", "🪙")

    fun emojiFor(name: String, type: CategoryType, visualKey: String = name): String {
        emojiByKey[name]?.let { return it }
        val fallbacks = if (type == CategoryType.INCOME) incomeFallbacks else expenseFallbacks
        return fallbacks[Math.floorMod(visualKey.hashCode(), fallbacks.size)]
    }
}

/** 按稳定标识取色；传分类 ID 后，重命名不会改变颜色。 */
@Composable
fun categoryColor(visualKey: String): Color {
    val hue = Math.floorMod(visualKey.hashCode(), 360)
    return if (LocalLedgerDarkTheme.current) {
        Color.hsv(hue.toFloat(), 0.30f, 0.38f)
    } else {
        Color.hsv(hue.toFloat(), 0.30f, 0.94f)
    }
}

/** 账户类型视觉：emoji。 */
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

/** 账户类型中文标签（各界面共用）。 */
fun accountTypeLabel(type: AccountType): String = when (type) {
    AccountType.CASH -> "现金"
    AccountType.BANK -> "银行卡"
    AccountType.ALIPAY -> "支付宝"
    AccountType.WECHAT -> "微信"
    AccountType.CREDIT_CARD -> "信用卡"
    AccountType.SAVINGS -> "储蓄"
    AccountType.INVESTMENT -> "投资"
    AccountType.LOAN -> "贷款"
    AccountType.OTHER -> "其他"
}

/** 账户类型主题色（浅色模式用柔和浅底，深色模式用亮色调）。 */
@Composable
fun accountColor(type: AccountType): Color {
    if (!LocalLedgerDarkTheme.current) {
        return MaterialTheme.colorScheme.surfaceVariant
    }
    return when (type) {
        AccountType.CASH -> Color(0xFF81C784)
        AccountType.BANK -> Color(0xFF90CAF9)
        AccountType.ALIPAY -> Color(0xFF6FA8FF)
        AccountType.WECHAT -> Color(0xFF66E09A)
        AccountType.CREDIT_CARD -> Color(0xFFCE93D8)
        AccountType.SAVINGS -> Color(0xFFFFB74D)
        AccountType.INVESTMENT -> Color(0xFF4DB6AC)
        AccountType.LOAN -> Color(0xFFFF8A80)
        AccountType.OTHER -> Color(0xFFBDBDBD)
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
fun CategoryBadge(
    name: String,
    type: CategoryType,
    modifier: Modifier = Modifier,
    sizeDp: Int = 36,
    visualKey: String = name,
) {
    EmojiBadge(
        emoji = CategoryVisuals.emojiFor(name, type, visualKey),
        background = categoryColor(visualKey),
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

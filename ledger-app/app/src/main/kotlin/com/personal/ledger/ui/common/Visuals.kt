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
 * - 浅色模式：使用低饱和浅色容器 + 深色文字，避免所有卡片都退化为白色；
 * - 深色模式：使用同一色相的深色容器 + 亮色文字。
 * 收入 / 支出 / 负债除颜色外始终配合 + / - 号（UI.md 第 9 节）。
 */

// 浅色容器刻意沿用深色语义色相，但降低饱和度和亮度，保证文字对比度。
private val LightCardColor = Color(0xFFF0F6EE)
private val LightHighlightColor = Color(0xFFCBE8CE)
private val LightExpenseContainer = Color(0xFFFFEDEA)
private val LightIncomeContainer = Color(0xFFE5F4E7)
private val LightTransferContainer = Color(0xFFE6F0FC)
private val LightExpenseAmount = Color(0xFFB3261E)
private val LightIncomeAmount = Color(0xFF216E2D)
private val LightTransferAmount = Color(0xFF185A97)
private val LightWarningAmount = Color(0xFF8A4B08)

/** 卡片容器色：浅色使用柔和浅绿底，深色使用主题深色容器。 */
@Composable fun cardColor(): Color =
    if (LocalLedgerDarkTheme.current) MaterialTheme.colorScheme.surfaceContainerLow else LightCardColor

/** 主卡片（本月支出 / 净资产）背景色。 */
@Composable fun highlightCardColor(): Color =
    if (LocalLedgerDarkTheme.current) MaterialTheme.colorScheme.primaryContainer else LightHighlightColor

/** 主卡片内容色。 */
@Composable fun highlightCardContentColor(): Color =
    if (LocalLedgerDarkTheme.current) MaterialTheme.colorScheme.onPrimaryContainer else Color(0xFF103B16)

/** 支出语义色。 */
@Composable fun expenseAmountColor(): Color =
    if (LocalLedgerDarkTheme.current) Color(0xFFFF8A80) else LightExpenseAmount

/** 支出容器色。 */
@Composable fun expenseContainerColor(): Color =
    if (LocalLedgerDarkTheme.current) Color(0xFF3A2020) else LightExpenseContainer

/** 收入语义色。 */
@Composable fun incomeAmountColor(): Color =
    if (LocalLedgerDarkTheme.current) Color(0xFF81C784) else LightIncomeAmount

/** 收入容器色。 */
@Composable fun incomeContainerColor(): Color =
    if (LocalLedgerDarkTheme.current) Color(0xFF1B3520) else LightIncomeContainer

/** 转账语义色。 */
@Composable fun transferAmountColor(): Color =
    if (LocalLedgerDarkTheme.current) Color(0xFF90CAF9) else LightTransferAmount

/** 转账容器色。 */
@Composable fun transferContainerColor(): Color =
    if (LocalLedgerDarkTheme.current) Color(0xFF1A2B3D) else LightTransferContainer

/** 警示（预算 80~100%）。 */
@Composable fun warningColor(): Color =
    if (LocalLedgerDarkTheme.current) Color(0xFFFFB74D) else LightWarningAmount

/** 金额颜色：深色 = 支出红 / 收入绿 / 转账蓝；浅色 = 默认文字色（旧版样式）。 */
@Composable
fun amountColor(type: TransactionType): Color = when (type) {
    TransactionType.EXPENSE -> expenseAmountColor()
    TransactionType.INCOME -> incomeAmountColor()
    TransactionType.TRANSFER -> transferAmountColor()
}

/** 余额颜色：正数使用收入色，负数使用支出色。 */
@Composable
fun balanceColor(balance: Long): Color =
    if (balance >= 0) incomeAmountColor() else expenseAmountColor()

/** 预算进度颜色：<80% 绿 / 80~100% 橙 / 超支红。 */
@Composable
fun budgetColor(permille: Int?, fallback: Color): Color {
    return when {
        permille == null -> fallback
        permille < 800 -> incomeAmountColor()
        permille < 1000 -> warningColor()
        else -> expenseAmountColor()
    }
}

/** 分类 emoji（种子分类与自定义分类均使用可预测的名称 / 类型映射）。 */
object CategoryVisuals {
    private val emojiByKey = mapOf(
        "餐饮" to "🍜", "交通" to "🚌", "购物" to "🛒", "居住" to "🏠",
        "娱乐" to "🎮", "医疗" to "🏥", "其他" to "📦",
        "工资" to "💰", "其他收入" to "🎁",
    )

    private val keywordMappings = listOf(
        listOf("餐", "饭", "食", "外卖", "咖啡", "早餐", "午餐", "晚餐") to "🍜",
        listOf("交通", "公交", "地铁", "打车", "出租", "加油", "停车", "出行") to "🚌",
        listOf("购物", "买", "超市", "商场", "衣", "服饰") to "🛒",
        listOf("住", "房", "租", "居家", "家居", "水电") to "🏠",
        listOf("娱乐", "游戏", "电影", "音乐", "旅行", "旅游") to "🎮",
        listOf("医", "药", "医院", "健康", "保健") to "🏥",
        listOf("工资", "薪", "奖金", "收入", "报销", "兼职") to "💰",
        listOf("礼", "红包", "赠", "利息", "股息") to "🎁",
    )

    @Suppress("UNUSED_PARAMETER")
    fun emojiFor(name: String, type: CategoryType, visualKey: String = name): String {
        val normalized = name.trim()
        emojiByKey[normalized]?.let { return it }
        keywordMappings.firstOrNull { (keywords, _) ->
            keywords.any { normalized.contains(it) }
        }?.second?.let { return it }
        // visualKey 仅保留在 API 中以兼容旧调用；图标不再按随机/ID 哈希变化。
        return if (type == CategoryType.INCOME) "💵" else "📦"
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

/** 分类列表的低强度底色；颜色与分类圆标同色相，便于快速扫读。 */
@Composable
fun categoryContainerColor(visualKey: String): Color {
    return categoryColor(visualKey).copy(alpha = if (LocalLedgerDarkTheme.current) 0.72f else 0.26f)
}

/** 图表使用更高对比度的分类色，和列表中的分类色保持同一色相。 */
@Composable
fun categoryChartColor(visualKey: String): Color {
    val hue = Math.floorMod(visualKey.hashCode(), 360)
    return if (LocalLedgerDarkTheme.current) {
        Color.hsv(hue.toFloat(), 0.62f, 0.88f)
    } else {
        Color.hsv(hue.toFloat(), 0.68f, 0.70f)
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
        return when (type) {
            AccountType.CASH -> Color(0xFFDDF2DF)
            AccountType.BANK -> Color(0xFFDCEBFA)
            AccountType.ALIPAY -> Color(0xFFDCE8FF)
            AccountType.WECHAT -> Color(0xFFDDF4E8)
            AccountType.CREDIT_CARD -> Color(0xFFF0E0F3)
            AccountType.SAVINGS -> Color(0xFFFFEDD0)
            AccountType.INVESTMENT -> Color(0xFFD8F0EC)
            AccountType.LOAN -> Color(0xFFFFE4E1)
            AccountType.OTHER -> Color(0xFFE8EAED)
        }
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

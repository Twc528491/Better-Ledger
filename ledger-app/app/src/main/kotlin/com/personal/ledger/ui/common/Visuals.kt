package com.personal.ledger.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.twotone.BakeryDining
import androidx.compose.material.icons.twotone.DirectionsBus
import androidx.compose.material.icons.twotone.Eco
import androidx.compose.material.icons.twotone.Fastfood
import androidx.compose.material.icons.twotone.Home
import androidx.compose.material.icons.twotone.HomeRepairService
import androidx.compose.material.icons.twotone.LocalCafe
import androidx.compose.material.icons.twotone.LocalGroceryStore
import androidx.compose.material.icons.twotone.LocalHospital
import androidx.compose.material.icons.twotone.LocalShipping
import androidx.compose.material.icons.twotone.Paid
import androidx.compose.material.icons.twotone.SmokingRooms
import androidx.compose.material.icons.twotone.Subscriptions
import androidx.compose.material.icons.twotone.VolunteerActivism
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.ledger.domain.model.AccountType
import com.personal.ledger.domain.model.CategoryType
import com.personal.ledger.domain.model.TransactionType
import com.personal.ledger.ui.theme.LocalLedgerDarkTheme

/**
 * UI 视觉语义（配色 / 分类与账户图标），按主题分支。
 * 表面保持中性，语义色只落在金额、分类圆标和少量状态容器上。
 * 收入 / 支出 / 负债除颜色外始终配合 + / - 号（UI.md 第 9 节）。
 */

private val LightExpenseContainer = Color(0xFFFCEBE8)
private val LightIncomeContainer = Color(0xFFE3F2E6)
private val LightTransferContainer = Color(0xFFE5EDFA)
private val LightExpenseAmount = Color(0xFFC0392B)
private val LightIncomeAmount = Color(0xFF1E7B3A)
private val LightTransferAmount = Color(0xFF2F5FA8)
private val LightWarningAmount = Color(0xFF9A5B00)

/** 卡片容器色：浅色为纯白卡片落在暖灰底上，深色为略亮一级的表面。 */
@Composable fun cardColor(): Color = MaterialTheme.colorScheme.surfaceContainerLow

/** 卡片内的次级分区 / 输入格底色。 */
@Composable fun insetColor(): Color = MaterialTheme.colorScheme.surfaceContainer

/** 主卡片（本月支出 / 净资产）背景色：品牌深青绿。 */
@Composable fun highlightCardColor(): Color =
    if (LocalLedgerDarkTheme.current) Color(0xFF1D4A41) else MaterialTheme.colorScheme.primary

/** 主卡片内容色。 */
@Composable fun highlightCardContentColor(): Color =
    if (LocalLedgerDarkTheme.current) Color(0xFFE3F5EF) else Color.White

/** 支出语义色。 */
@Composable fun expenseAmountColor(): Color =
    if (LocalLedgerDarkTheme.current) Color(0xFFFF9A8E) else LightExpenseAmount

/** 支出容器色。 */
@Composable fun expenseContainerColor(): Color =
    if (LocalLedgerDarkTheme.current) Color(0xFF3A2320) else LightExpenseContainer

/** 收入语义色。 */
@Composable fun incomeAmountColor(): Color =
    if (LocalLedgerDarkTheme.current) Color(0xFF86D39A) else LightIncomeAmount

/** 收入容器色。 */
@Composable fun incomeContainerColor(): Color =
    if (LocalLedgerDarkTheme.current) Color(0xFF1C3323) else LightIncomeContainer

/** 转账语义色。 */
@Composable fun transferAmountColor(): Color =
    if (LocalLedgerDarkTheme.current) Color(0xFF9DBDF5) else LightTransferAmount

/** 转账容器色。 */
@Composable fun transferContainerColor(): Color =
    if (LocalLedgerDarkTheme.current) Color(0xFF1D2A3D) else LightTransferContainer

/** 警示（预算 80~100%）。 */
@Composable fun warningColor(): Color =
    if (LocalLedgerDarkTheme.current) Color(0xFFFFC069) else LightWarningAmount

/** 金额颜色：浅色和深色均按支出红 / 收入绿 / 转账蓝区分。 */
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

    private val iconKeyByName = mapOf(
        "餐饮" to "food",
        "交通" to "transit",
        "购物" to "shopping",
        "居住" to "home",
        "娱乐" to "movie",
        "医疗" to "medical",
        "其他" to "other",
        "工资" to "salary",
        "其他收入" to "other_income",
        "日用" to "daily",
        "服饰" to "clothing",
        "通讯" to "phone",
        "教育" to "education",
        "运动" to "fitness",
        "旅行" to "travel",
        "宠物" to "pets",
        "人情" to "gift",
        "汽车" to "car",
        "数码" to "devices",
        "育儿" to "childcare",
        "办公" to "office",
        "水电" to "electric",
        "美容" to "spa",
        "保险" to "insurance",
        "维修" to "repair",
        "税费" to "tax",
        "订阅" to "subscription",
        "生鲜" to "fresh",
        "零食" to "snack",
        "咖啡" to "coffee",
        "家政" to "housekeeping",
        "快递" to "delivery",
        "捐赠" to "donation",
        "烟酒" to "tobacco",
        "奖金" to "bonus",
        "兼职" to "work",
        "经营" to "business",
        "理财" to "invest",
        "报销" to "reimburse",
        "礼金" to "present",
        "退款" to "refund",
        "租金" to "rent",
        "分红" to "dividend",
        "利息" to "interest",
        "补贴" to "subsidy",
        "奖学金" to "scholarship",
        "稿费" to "royalty",
        "佣金" to "commission",
        "二手出售" to "secondhand",
    )

    private val iconKeywords = listOf(
        listOf("订阅", "会员", "月费") to "subscription",
        listOf("生鲜", "蔬菜", "水果") to "fresh",
        listOf("零食", "甜品", "蛋糕") to "snack",
        listOf("咖啡", "奶茶", "茶饮") to "coffee",
        listOf("家政", "保洁", "钟点工") to "housekeeping",
        listOf("快递", "邮寄", "包裹") to "delivery",
        listOf("捐赠", "慈善", "公益") to "donation",
        listOf("烟酒", "香烟", "酒水") to "tobacco",
        listOf("餐", "饭", "食", "外卖", "早餐", "午餐", "晚餐") to "food",
        listOf("交通", "公交", "地铁", "打车", "出租", "加油", "停车", "出行") to "transit",
        listOf("购物", "买", "超市", "商场") to "shopping",
        listOf("租房", "住房", "家居", "居家", "水电") to "home",
        listOf("娱乐", "游戏", "电影", "音乐") to "movie",
        listOf("医", "药", "医院", "健康", "保健") to "medical",
        listOf("工资", "薪", "奖金", "报销") to "salary",
        listOf("礼", "红包", "赠", "人情") to "gift",
    )

    /** 稳定、纯字符串映射，供图标显示和映射测试共用。 */
    fun iconKeyFor(name: String, type: CategoryType): String {
        val normalized = name.trim()
        iconKeyByName[normalized]?.let { return it }
        iconKeywords.firstOrNull { (keywords, _) ->
            keywords.any { normalized.contains(it) }
        }?.second?.let { return it }
        return if (type == CategoryType.INCOME) "other_income" else "other"
    }

    fun iconFor(name: String, type: CategoryType): ImageVector =
        when (iconKeyFor(name, type)) {
            "food" -> Icons.TwoTone.Fastfood
            "transit" -> Icons.TwoTone.DirectionsBus
            "shopping" -> Icons.TwoTone.LocalGroceryStore
            "home" -> Icons.TwoTone.Home
            "movie" -> Icons.Filled.Movie
            "medical" -> Icons.TwoTone.LocalHospital
            "other" -> Icons.Filled.Inventory2
            "salary" -> Icons.TwoTone.Paid
            "other_income" -> Icons.Filled.AccountBalanceWallet
            "daily" -> Icons.Filled.CleaningServices
            "clothing" -> Icons.Filled.Checkroom
            "phone" -> Icons.Filled.Smartphone
            "education" -> Icons.Filled.School
            "fitness" -> Icons.Filled.FitnessCenter
            "travel" -> Icons.Filled.Flight
            "pets" -> Icons.Filled.Pets
            "gift" -> Icons.Filled.CardGiftcard
            "car" -> Icons.Filled.DirectionsCar
            "devices" -> Icons.Filled.Devices
            "childcare" -> Icons.Filled.ChildCare
            "office" -> Icons.Filled.BusinessCenter
            "electric" -> Icons.Filled.ElectricalServices
            "spa" -> Icons.Filled.Spa
            "insurance" -> Icons.Filled.VerifiedUser
            "repair" -> Icons.Filled.Build
            "tax" -> Icons.Filled.RequestQuote
            "subscription" -> Icons.TwoTone.Subscriptions
            "fresh" -> Icons.TwoTone.Eco
            "snack" -> Icons.TwoTone.BakeryDining
            "coffee" -> Icons.TwoTone.LocalCafe
            "housekeeping" -> Icons.TwoTone.HomeRepairService
            "delivery" -> Icons.TwoTone.LocalShipping
            "donation" -> Icons.TwoTone.VolunteerActivism
            "tobacco" -> Icons.TwoTone.SmokingRooms
            "bonus" -> Icons.Filled.EmojiEvents
            "work" -> Icons.Filled.Work
            "business" -> Icons.Filled.Storefront
            "invest" -> Icons.AutoMirrored.Filled.TrendingUp
            "reimburse" -> Icons.AutoMirrored.Filled.ReceiptLong
            "present" -> Icons.Filled.Redeem
            "refund" -> Icons.Filled.CurrencyExchange
            "rent" -> Icons.TwoTone.Home
            "dividend" -> Icons.AutoMirrored.Filled.TrendingUp
            "interest" -> Icons.TwoTone.Paid
            "subsidy" -> Icons.Filled.RequestQuote
            "scholarship" -> Icons.Filled.School
            "royalty" -> Icons.AutoMirrored.Filled.ReceiptLong
            "commission" -> Icons.Filled.BusinessCenter
            "secondhand" -> Icons.Filled.CurrencyExchange
            else -> Icons.Filled.Inventory2
        }

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

/**
 * 分类色相表：挑选彼此区分明显、且在浅 / 深色下都不发灰发脏的色相。
 * 按稳定标识（分类 ID）哈希取色，重命名不会改变颜色。
 */
private val CategoryHues = floatArrayOf(4f, 22f, 38f, 88f, 142f, 168f, 190f, 208f, 226f, 256f, 284f, 318f, 340f)

private fun categoryHue(visualKey: String): Float =
    CategoryHues[Math.floorMod(visualKey.hashCode(), CategoryHues.size)]

/** 分类圆标底色：同色相的浅色调（深色模式为低亮度色调）。 */
@Composable
fun categoryColor(visualKey: String): Color {
    val hue = categoryHue(visualKey)
    return if (LocalLedgerDarkTheme.current) {
        Color.hsv(hue, 0.34f, 0.30f)
    } else {
        Color.hsv(hue, 0.12f, 0.97f)
    }
}

/** 分类圆标图标色：与底色同色相、更高饱和，保证识别度。 */
@Composable
fun categoryIconColor(visualKey: String): Color {
    val hue = categoryHue(visualKey)
    return if (LocalLedgerDarkTheme.current) {
        Color.hsv(hue, 0.40f, 0.95f)
    } else {
        Color.hsv(hue, 0.70f, 0.60f)
    }
}

/** 分类列表的低强度底色；颜色与分类圆标同色相，便于快速扫读。 */
@Composable
fun categoryContainerColor(visualKey: String): Color = categoryColor(visualKey)

/** 图表使用更高对比度的分类色，和列表中的分类色保持同一色相。 */
@Composable
fun categoryChartColor(visualKey: String): Color {
    val hue = categoryHue(visualKey)
    return if (LocalLedgerDarkTheme.current) {
        Color.hsv(hue, 0.55f, 0.88f)
    } else {
        Color.hsv(hue, 0.62f, 0.74f)
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
        AccountType.CASH -> Color(0xFF24402B)
        AccountType.BANK -> Color(0xFF1F3550)
        AccountType.ALIPAY -> Color(0xFF1E3358)
        AccountType.WECHAT -> Color(0xFF1D3F2E)
        AccountType.CREDIT_CARD -> Color(0xFF3B2842)
        AccountType.SAVINGS -> Color(0xFF45341A)
        AccountType.INVESTMENT -> Color(0xFF173C38)
        AccountType.LOAN -> Color(0xFF45241F)
        AccountType.OTHER -> Color(0xFF2E3230)
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

/** 分类圆标：浅色调圆底 + 同色相 Material 图标，扁平、无阴影。 */
@Composable
fun CategoryBadge(
    name: String,
    type: CategoryType,
    modifier: Modifier = Modifier,
    sizeDp: Int = 36,
    visualKey: String = name,
) {
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .background(categoryColor(visualKey), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = CategoryVisuals.iconFor(name, type),
            contentDescription = name + "分类图标",
            tint = categoryIconColor(visualKey),
            modifier = Modifier.size((sizeDp * 0.56f).coerceAtLeast(16f).dp),
        )
    }
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

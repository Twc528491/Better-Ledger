package io.github.twc528491.betterledger.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.RequestQuote
import androidx.compose.material.icons.rounded.Savings
import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.twc528491.betterledger.R
import io.github.twc528491.betterledger.domain.model.AccountType
import io.github.twc528491.betterledger.domain.model.CategoryType
import io.github.twc528491.betterledger.domain.model.TransactionType
import io.github.twc528491.betterledger.ui.theme.LocalLedgerDarkTheme

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

    /** 英文预置分类名（values-en/strings_core.xml 中的 seed_cat_*，小写）→ 图标 key。 */
    private val iconKeyByEnglishName = mapOf(
        "food" to "food",
        "transport" to "transit",
        "shopping" to "shopping",
        "housing" to "home",
        "entertainment" to "movie",
        "medical" to "medical",
        "other" to "other",
        "salary" to "salary",
        "other income" to "other_income",
        "household" to "daily",
        "clothing" to "clothing",
        "phone" to "phone",
        "education" to "education",
        "fitness" to "fitness",
        "travel" to "travel",
        "pets" to "pets",
        "gifts" to "gift",
        "car" to "car",
        "electronics" to "devices",
        "childcare" to "childcare",
        "office" to "office",
        "utilities" to "electric",
        "beauty" to "spa",
        "insurance" to "insurance",
        "repairs" to "repair",
        "taxes" to "tax",
        "subscriptions" to "subscription",
        "groceries" to "fresh",
        "snacks" to "snack",
        "coffee" to "coffee",
        "housekeeping" to "housekeeping",
        "shipping" to "delivery",
        "donations" to "donation",
        "tobacco & alcohol" to "tobacco",
        "bonus" to "bonus",
        "side job" to "work",
        "business" to "business",
        "investments" to "invest",
        "reimbursement" to "reimburse",
        "gift money" to "present",
        "refunds" to "refund",
        "rental income" to "rent",
        "dividends" to "dividend",
        "interest" to "interest",
        "allowances" to "subsidy",
        "scholarships" to "scholarship",
        "royalties" to "royalty",
        "commissions" to "commission",
        "resale" to "secondhand",
    )

    /**
     * 英文关键词（小写词干）：名称中任一单词以词干开头即命中，如 "grocer" 命中 "Groceries"；
     * 三个字母以内的词干只匹配整词（或加 s 的复数）。
     * 顺序即优先级：具体的在前、宽泛的在后，与中文关键词表保持一致。
     */
    private val englishIconKeywords = listOf(
        listOf("subscription", "membership", "streaming", "netflix", "spotify") to "subscription",
        listOf("grocer", "produce", "vegetable", "fruit", "fresh") to "fresh",
        listOf("snack", "dessert", "cake", "candy", "sweets", "bakery") to "snack",
        listOf("coffee", "cafe", "latte", "tea", "boba") to "coffee",
        listOf("housekeep", "cleaning", "cleaner", "maid") to "housekeeping",
        listOf("shipping", "delivery", "postage", "parcel", "courier") to "delivery",
        listOf("donat", "charity", "charities") to "donation",
        listOf("tobacco", "cigar", "alcohol", "liquor", "wine", "beer") to "tobacco",
        listOf("food", "dining", "dine", "meal", "restaurant", "breakfast", "lunch", "dinner", "takeout", "takeaway", "eat") to "food",
        listOf("taxi", "uber", "lyft", "cab") to "taxi",
        listOf("fuel", "gas", "petrol") to "fuel",
        listOf("parking") to "parking",
        listOf("transport", "transit", "bus", "subway", "metro", "train", "commut", "fare") to "transit",
        listOf("travel", "trip", "flight", "hotel", "vacation", "holiday") to "travel",
        listOf("car", "auto", "vehicle") to "car",
        listOf("shopping", "shop", "supermarket", "mall", "store") to "shopping",
        listOf("cloth", "apparel", "fashion", "shoe") to "clothing",
        listOf("rent", "mortgage", "housing", "home", "furniture", "utilit") to "home",
        listOf("electric", "water", "power") to "electric",
        listOf("phone", "mobile", "internet", "wifi", "broadband") to "phone",
        listOf("electronic", "gadget", "computer", "laptop", "device") to "devices",
        listOf("entertainment", "game", "gaming", "movie", "cinema", "film", "music", "concert") to "movie",
        listOf("medical", "medicine", "health", "hospital", "doctor", "pharmac", "dental", "clinic") to "medical",
        listOf("educat", "tuition", "school", "course", "class", "book") to "education",
        listOf("gym", "fitness", "sport", "workout") to "fitness",
        listOf("pet", "vet") to "pets",
        listOf("child", "baby", "kid") to "childcare",
        listOf("beauty", "salon", "cosmetic", "spa", "makeup") to "spa",
        listOf("haircut", "barber", "hair") to "haircut",
        listOf("insurance") to "insurance",
        listOf("repair", "maintenance") to "repair",
        listOf("tax", "taxes", "fee") to "tax",
        listOf("office", "stationery") to "office",
        listOf("refund", "cashback") to "refund",
        listOf("dividend", "invest", "stock", "fund", "crypto") to "invest",
        listOf("interest") to "interest",
        listOf("salary", "wage", "payroll", "paycheck", "income", "bonus", "reimburse") to "salary",
        listOf("gift", "present", "red packet") to "gift",
    )

    private val wordSeparator = Regex("[^\\p{L}\\p{N}&]+")

    private fun englishIconKey(name: String): String? {
        val lower = name.lowercase(java.util.Locale.ROOT)
        iconKeyByEnglishName[lower]?.let { return it }
        val words = lower.split(wordSeparator).filter { it.isNotEmpty() }
        if (words.isEmpty()) return null
        return englishIconKeywords.firstOrNull { (stems, _) ->
            stems.any { stem ->
                when {
                    ' ' in stem -> lower.contains(stem)
                    // 短词干只做整词（含复数）匹配，避免 "car" 命中 "card"
                    stem.length <= 3 -> words.any { it == stem || it == stem + "s" }
                    else -> words.any { it.startsWith(stem) }
                }
            }
        }?.second
    }

    /** 稳定、纯字符串映射，供图标显示和映射测试共用；中文与英文名称（不区分大小写）均可匹配。 */
    fun iconKeyFor(name: String, type: CategoryType): String {
        val normalized = name.trim()
        iconKeyByName[normalized]?.let { return it }
        iconKeywords.firstOrNull { (keywords, _) ->
            keywords.any { normalized.contains(it) }
        }?.second?.let { return it }
        englishIconKey(normalized)?.let { return it }
        return if (type == CategoryType.INCOME) "other_income" else "other"
    }

    /** 用户在图标库中选择的 [iconKey] 优先；否则按名称 / 关键词映射。 */
    fun iconFor(name: String, type: CategoryType, iconKey: String? = null): ImageVector =
        (CategoryIconLibrary.find(iconKey) ?: CategoryIconLibrary.find(iconKeyFor(name, type)))?.icon
            ?: Icons.Rounded.Inventory2

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

/** 账户类型视觉：Material Rounded 图标 + 固定色相，与分类圆标同一风格。 */
object AccountVisuals {
    fun iconFor(type: AccountType): ImageVector = when (type) {
        AccountType.CASH -> Icons.Rounded.Payments
        AccountType.BANK -> Icons.Rounded.AccountBalance
        AccountType.ALIPAY -> Icons.Rounded.AccountBalanceWallet
        AccountType.WECHAT -> Icons.AutoMirrored.Rounded.Chat
        AccountType.CREDIT_CARD -> Icons.Rounded.CreditCard
        AccountType.SAVINGS -> Icons.Rounded.Savings
        AccountType.INVESTMENT -> Icons.AutoMirrored.Rounded.ShowChart
        AccountType.LOAN -> Icons.Rounded.RequestQuote
        AccountType.OTHER -> Icons.Rounded.MoreHoriz
    }

    /** 固定色相；null 表示中性灰（“其他”）。 */
    fun hueFor(type: AccountType): Float? = when (type) {
        AccountType.CASH -> 142f
        AccountType.BANK -> 226f
        AccountType.ALIPAY -> 204f
        AccountType.WECHAT -> 160f
        AccountType.CREDIT_CARD -> 284f
        AccountType.SAVINGS -> 30f
        AccountType.INVESTMENT -> 340f
        AccountType.LOAN -> 4f
        AccountType.OTHER -> null
    }
}

/** 账户类型显示名资源（各界面共用）。 */
@get:StringRes
val AccountType.labelRes: Int
    get() = when (this) {
        AccountType.CASH -> R.string.core_account_type_cash
        AccountType.BANK -> R.string.core_account_type_bank
        AccountType.ALIPAY -> R.string.core_account_type_alipay
        AccountType.WECHAT -> R.string.core_account_type_wechat
        AccountType.CREDIT_CARD -> R.string.core_account_type_credit_card
        AccountType.SAVINGS -> R.string.core_account_type_savings
        AccountType.INVESTMENT -> R.string.core_account_type_investment
        AccountType.LOAN -> R.string.core_account_type_loan
        AccountType.OTHER -> R.string.core_account_type_other
    }

/** 账户类型显示名（当前语言）。 */
@Composable
@ReadOnlyComposable
fun accountTypeLabel(type: AccountType): String = stringResource(type.labelRes)

/** 交易类型显示名资源：支出 / 收入 / 转账。 */
@get:StringRes
val TransactionType.labelRes: Int
    get() = when (this) {
        TransactionType.EXPENSE -> R.string.type_expense
        TransactionType.INCOME -> R.string.type_income
        TransactionType.TRANSFER -> R.string.type_transfer
    }

/** 交易类型显示名（当前语言）。 */
@Composable
@ReadOnlyComposable
fun transactionTypeLabel(type: TransactionType): String = stringResource(type.labelRes)

/** 分类类型显示名资源：支出 / 收入。 */
@get:StringRes
val CategoryType.labelRes: Int
    get() = when (this) {
        CategoryType.EXPENSE -> R.string.core_category_type_expense
        CategoryType.INCOME -> R.string.core_category_type_income
    }

/** 分类类型显示名（当前语言）。 */
@Composable
@ReadOnlyComposable
fun categoryTypeLabel(type: CategoryType): String = stringResource(type.labelRes)

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

/**
 * 扁平色调圆标：浅色调圆底 + 同色相高饱和图标，分类与账户共用。
 * [hue] 为 null 时使用中性灰。
 */
@Composable
fun TonalIconBadge(
    icon: ImageVector,
    hue: Float?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    sizeDp: Int = 36,
) {
    val dark = LocalLedgerDarkTheme.current
    val background = when {
        hue == null -> MaterialTheme.colorScheme.surfaceContainerHigh
        dark -> Color.hsv(hue, 0.34f, 0.30f)
        else -> Color.hsv(hue, 0.12f, 0.97f)
    }
    val tint = when {
        hue == null -> MaterialTheme.colorScheme.onSurfaceVariant
        dark -> Color.hsv(hue, 0.40f, 0.95f)
        else -> Color.hsv(hue, 0.70f, 0.60f)
    }
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .background(background, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size((sizeDp * 0.56f).coerceAtLeast(16f).dp),
        )
    }
}

/** 分类圆标；[iconKey] 为用户在图标库中选择的图标，缺省按名称映射。 */
@Composable
fun CategoryBadge(
    name: String,
    type: CategoryType,
    modifier: Modifier = Modifier,
    sizeDp: Int = 36,
    visualKey: String = name,
    iconKey: String? = null,
) {
    TonalIconBadge(
        icon = CategoryVisuals.iconFor(name, type, iconKey),
        hue = categoryHue(visualKey),
        contentDescription = stringResource(R.string.core_cd_category_icon, name),
        modifier = modifier,
        sizeDp = sizeDp,
    )
}

/** 账户圆标。 */
@Composable
fun AccountBadge(type: AccountType, modifier: Modifier = Modifier, sizeDp: Int = 36) {
    TonalIconBadge(
        icon = AccountVisuals.iconFor(type),
        hue = AccountVisuals.hueFor(type),
        contentDescription = stringResource(R.string.core_cd_account_icon, accountTypeLabel(type)),
        modifier = modifier,
        sizeDp = sizeDp,
    )
}

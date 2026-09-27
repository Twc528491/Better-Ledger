package com.personal.ledger.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.BakeryDining
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.BusinessCenter
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.ElectricalServices
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.HomeRepairService
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.Icecream
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocalBar
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.LocalGroceryStore
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.LocalLaundryService
import androidx.compose.material.icons.rounded.LocalParking
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.LocalTaxi
import androidx.compose.material.icons.rounded.Luggage
import androidx.compose.material.icons.rounded.LunchDining
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.RamenDining
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.RequestQuote
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.SmokingRooms
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.vector.ImageVector

/** 图标库中的一项：稳定字符串 key（存入数据库）、中文名（无障碍 / 搜索）、矢量图形。 */
data class LibraryIcon(val key: String, val label: String, val icon: ImageVector)

/**
 * 分类图标库。统一使用 Material Rounded 风格，保证与分类 / 账户圆标一致。
 * key 一经发布不得更改或复用（已写入用户数据）；新增图标只能追加新 key。
 */
object CategoryIconLibrary {
    val groups: List<Pair<String, List<LibraryIcon>>> = listOf(
        "餐饮" to listOf(
            LibraryIcon("food", "餐饮", Icons.Rounded.Fastfood),
            LibraryIcon("restaurant", "正餐", Icons.Rounded.Restaurant),
            LibraryIcon("noodles", "面食", Icons.Rounded.RamenDining),
            LibraryIcon("burger", "快餐", Icons.Rounded.LunchDining),
            LibraryIcon("coffee", "咖啡", Icons.Rounded.LocalCafe),
            LibraryIcon("snack", "零食", Icons.Rounded.BakeryDining),
            LibraryIcon("dessert", "甜品", Icons.Rounded.Icecream),
            LibraryIcon("drinks", "酒水", Icons.Rounded.LocalBar),
            LibraryIcon("fresh", "生鲜", Icons.Rounded.Eco),
        ),
        "出行" to listOf(
            LibraryIcon("transit", "公交", Icons.Rounded.DirectionsBus),
            LibraryIcon("train", "火车", Icons.Rounded.Train),
            LibraryIcon("taxi", "打车", Icons.Rounded.LocalTaxi),
            LibraryIcon("car", "汽车", Icons.Rounded.DirectionsCar),
            LibraryIcon("fuel", "加油", Icons.Rounded.LocalGasStation),
            LibraryIcon("parking", "停车", Icons.Rounded.LocalParking),
            LibraryIcon("travel", "旅行", Icons.Rounded.Flight),
            LibraryIcon("hotel", "住宿", Icons.Rounded.Hotel),
            LibraryIcon("luggage", "行李", Icons.Rounded.Luggage),
        ),
        "居家" to listOf(
            LibraryIcon("home", "居住", Icons.Rounded.Home),
            LibraryIcon("electric", "水电", Icons.Rounded.ElectricalServices),
            LibraryIcon("water", "水费", Icons.Rounded.WaterDrop),
            LibraryIcon("internet", "网络", Icons.Rounded.Wifi),
            LibraryIcon("daily", "日用", Icons.Rounded.CleaningServices),
            LibraryIcon("laundry", "洗衣", Icons.Rounded.LocalLaundryService),
            LibraryIcon("housekeeping", "家政", Icons.Rounded.HomeRepairService),
            LibraryIcon("repair", "维修", Icons.Rounded.Build),
            LibraryIcon("delivery", "快递", Icons.Rounded.LocalShipping),
        ),
        "购物" to listOf(
            LibraryIcon("shopping", "购物", Icons.Rounded.LocalGroceryStore),
            LibraryIcon("bag", "百货", Icons.Rounded.ShoppingBag),
            LibraryIcon("clothing", "服饰", Icons.Rounded.Checkroom),
            LibraryIcon("jewelry", "饰品", Icons.Rounded.Diamond),
            LibraryIcon("phone", "通讯", Icons.Rounded.Smartphone),
            LibraryIcon("devices", "数码", Icons.Rounded.Devices),
            LibraryIcon("tobacco", "烟酒", Icons.Rounded.SmokingRooms),
        ),
        "娱乐与成长" to listOf(
            LibraryIcon("movie", "娱乐", Icons.Rounded.Movie),
            LibraryIcon("game", "游戏", Icons.Rounded.SportsEsports),
            LibraryIcon("music", "音乐", Icons.Rounded.MusicNote),
            LibraryIcon("subscription", "订阅", Icons.Rounded.Subscriptions),
            LibraryIcon("outdoor", "户外", Icons.Rounded.Park),
            LibraryIcon("party", "聚会", Icons.Rounded.Celebration),
            LibraryIcon("education", "教育", Icons.Rounded.School),
            LibraryIcon("book", "书籍", Icons.AutoMirrored.Rounded.MenuBook),
            LibraryIcon("fitness", "运动", Icons.Rounded.FitnessCenter),
        ),
        "健康与家庭" to listOf(
            LibraryIcon("medical", "医疗", Icons.Rounded.LocalHospital),
            LibraryIcon("medicine", "药品", Icons.Rounded.Medication),
            LibraryIcon("spa", "美容", Icons.Rounded.Spa),
            LibraryIcon("haircut", "理发", Icons.Rounded.ContentCut),
            LibraryIcon("childcare", "育儿", Icons.Rounded.ChildCare),
            LibraryIcon("pets", "宠物", Icons.Rounded.Pets),
            LibraryIcon("insurance", "保险", Icons.Rounded.VerifiedUser),
            LibraryIcon("gift", "人情", Icons.Rounded.CardGiftcard),
            LibraryIcon("donation", "捐赠", Icons.Rounded.VolunteerActivism),
        ),
        "收入与理财" to listOf(
            LibraryIcon("salary", "工资", Icons.Rounded.Paid),
            LibraryIcon("bonus", "奖金", Icons.Rounded.EmojiEvents),
            LibraryIcon("work", "兼职", Icons.Rounded.Work),
            LibraryIcon("business", "经营", Icons.Rounded.Storefront),
            LibraryIcon("office", "办公", Icons.Rounded.BusinessCenter),
            LibraryIcon("invest", "理财", Icons.AutoMirrored.Rounded.TrendingUp),
            LibraryIcon("savings", "储蓄", Icons.Rounded.Savings),
            LibraryIcon("reimburse", "报销", Icons.AutoMirrored.Rounded.ReceiptLong),
            LibraryIcon("tax", "税费", Icons.Rounded.RequestQuote),
            LibraryIcon("refund", "退款", Icons.Rounded.CurrencyExchange),
            LibraryIcon("present", "礼金", Icons.Rounded.Redeem),
            LibraryIcon("other_income", "其他收入", Icons.Rounded.AccountBalanceWallet),
            LibraryIcon("other", "其他", Icons.Rounded.Inventory2),
        ),
    )

    val all: List<LibraryIcon> = groups.flatMap { it.second }

    private val byKey: Map<String, LibraryIcon> = all.associateBy { it.key }

    /** 名称映射使用的历史 key 与库中图形相同的别名，不在库中重复展示。 */
    private val aliases = mapOf(
        "rent" to "home",
        "dividend" to "invest",
        "interest" to "salary",
        "subsidy" to "tax",
        "scholarship" to "education",
        "royalty" to "reimburse",
        "commission" to "office",
        "secondhand" to "refund",
    )

    fun find(key: String?): LibraryIcon? = key?.let { byKey[it] ?: byKey[aliases[it]] }

    fun contains(key: String): Boolean = byKey.containsKey(key)
}

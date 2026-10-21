package io.github.twc528491.betterledger.ui.common

import androidx.annotation.StringRes
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import io.github.twc528491.betterledger.R

/** 图标库中的一项：稳定字符串 key（存入数据库）、显示名资源（无障碍 / 搜索）、矢量图形。 */
data class LibraryIcon(val key: String, @StringRes val labelRes: Int, val icon: ImageVector) {
    /** 当前语言的显示名。 */
    val label: String
        @Composable @ReadOnlyComposable get() = stringResource(labelRes)
}

/**
 * 分类图标库。统一使用 Material Rounded 风格，保证与分类 / 账户圆标一致。
 * key 一经发布不得更改或复用（已写入用户数据）；新增图标只能追加新 key。
 */
object CategoryIconLibrary {
    /** 分组：分组标题资源 → 图标。 */
    val groups: List<Pair<Int, List<LibraryIcon>>> = listOf(
        R.string.icon_group_food to listOf(
            LibraryIcon("food", R.string.icon_food, Icons.Rounded.Fastfood),
            LibraryIcon("restaurant", R.string.icon_restaurant, Icons.Rounded.Restaurant),
            LibraryIcon("noodles", R.string.icon_noodles, Icons.Rounded.RamenDining),
            LibraryIcon("burger", R.string.icon_burger, Icons.Rounded.LunchDining),
            LibraryIcon("coffee", R.string.icon_coffee, Icons.Rounded.LocalCafe),
            LibraryIcon("snack", R.string.icon_snack, Icons.Rounded.BakeryDining),
            LibraryIcon("dessert", R.string.icon_dessert, Icons.Rounded.Icecream),
            LibraryIcon("drinks", R.string.icon_drinks, Icons.Rounded.LocalBar),
            LibraryIcon("fresh", R.string.icon_fresh, Icons.Rounded.Eco),
        ),
        R.string.icon_group_transport to listOf(
            LibraryIcon("transit", R.string.icon_transit, Icons.Rounded.DirectionsBus),
            LibraryIcon("train", R.string.icon_train, Icons.Rounded.Train),
            LibraryIcon("taxi", R.string.icon_taxi, Icons.Rounded.LocalTaxi),
            LibraryIcon("car", R.string.icon_car, Icons.Rounded.DirectionsCar),
            LibraryIcon("fuel", R.string.icon_fuel, Icons.Rounded.LocalGasStation),
            LibraryIcon("parking", R.string.icon_parking, Icons.Rounded.LocalParking),
            LibraryIcon("travel", R.string.icon_travel, Icons.Rounded.Flight),
            LibraryIcon("hotel", R.string.icon_hotel, Icons.Rounded.Hotel),
            LibraryIcon("luggage", R.string.icon_luggage, Icons.Rounded.Luggage),
        ),
        R.string.icon_group_home to listOf(
            LibraryIcon("home", R.string.icon_home, Icons.Rounded.Home),
            LibraryIcon("electric", R.string.icon_electric, Icons.Rounded.ElectricalServices),
            LibraryIcon("water", R.string.icon_water, Icons.Rounded.WaterDrop),
            LibraryIcon("internet", R.string.icon_internet, Icons.Rounded.Wifi),
            LibraryIcon("daily", R.string.icon_daily, Icons.Rounded.CleaningServices),
            LibraryIcon("laundry", R.string.icon_laundry, Icons.Rounded.LocalLaundryService),
            LibraryIcon("housekeeping", R.string.icon_housekeeping, Icons.Rounded.HomeRepairService),
            LibraryIcon("repair", R.string.icon_repair, Icons.Rounded.Build),
            LibraryIcon("delivery", R.string.icon_delivery, Icons.Rounded.LocalShipping),
        ),
        R.string.icon_group_shopping to listOf(
            LibraryIcon("shopping", R.string.icon_shopping, Icons.Rounded.LocalGroceryStore),
            LibraryIcon("bag", R.string.icon_bag, Icons.Rounded.ShoppingBag),
            LibraryIcon("clothing", R.string.icon_clothing, Icons.Rounded.Checkroom),
            LibraryIcon("jewelry", R.string.icon_jewelry, Icons.Rounded.Diamond),
            LibraryIcon("phone", R.string.icon_phone, Icons.Rounded.Smartphone),
            LibraryIcon("devices", R.string.icon_devices, Icons.Rounded.Devices),
            LibraryIcon("tobacco", R.string.icon_tobacco, Icons.Rounded.SmokingRooms),
        ),
        R.string.icon_group_leisure to listOf(
            LibraryIcon("movie", R.string.icon_movie, Icons.Rounded.Movie),
            LibraryIcon("game", R.string.icon_game, Icons.Rounded.SportsEsports),
            LibraryIcon("music", R.string.icon_music, Icons.Rounded.MusicNote),
            LibraryIcon("subscription", R.string.icon_subscription, Icons.Rounded.Subscriptions),
            LibraryIcon("outdoor", R.string.icon_outdoor, Icons.Rounded.Park),
            LibraryIcon("party", R.string.icon_party, Icons.Rounded.Celebration),
            LibraryIcon("education", R.string.icon_education, Icons.Rounded.School),
            LibraryIcon("book", R.string.icon_book, Icons.AutoMirrored.Rounded.MenuBook),
            LibraryIcon("fitness", R.string.icon_fitness, Icons.Rounded.FitnessCenter),
        ),
        R.string.icon_group_health to listOf(
            LibraryIcon("medical", R.string.icon_medical, Icons.Rounded.LocalHospital),
            LibraryIcon("medicine", R.string.icon_medicine, Icons.Rounded.Medication),
            LibraryIcon("spa", R.string.icon_spa, Icons.Rounded.Spa),
            LibraryIcon("haircut", R.string.icon_haircut, Icons.Rounded.ContentCut),
            LibraryIcon("childcare", R.string.icon_childcare, Icons.Rounded.ChildCare),
            LibraryIcon("pets", R.string.icon_pets, Icons.Rounded.Pets),
            LibraryIcon("insurance", R.string.icon_insurance, Icons.Rounded.VerifiedUser),
            LibraryIcon("gift", R.string.icon_gift, Icons.Rounded.CardGiftcard),
            LibraryIcon("donation", R.string.icon_donation, Icons.Rounded.VolunteerActivism),
        ),
        R.string.icon_group_income to listOf(
            LibraryIcon("salary", R.string.icon_salary, Icons.Rounded.Paid),
            LibraryIcon("bonus", R.string.icon_bonus, Icons.Rounded.EmojiEvents),
            LibraryIcon("work", R.string.icon_work, Icons.Rounded.Work),
            LibraryIcon("business", R.string.icon_business, Icons.Rounded.Storefront),
            LibraryIcon("office", R.string.icon_office, Icons.Rounded.BusinessCenter),
            LibraryIcon("invest", R.string.icon_invest, Icons.AutoMirrored.Rounded.TrendingUp),
            LibraryIcon("savings", R.string.icon_savings, Icons.Rounded.Savings),
            LibraryIcon("reimburse", R.string.icon_reimburse, Icons.AutoMirrored.Rounded.ReceiptLong),
            LibraryIcon("tax", R.string.icon_tax, Icons.Rounded.RequestQuote),
            LibraryIcon("refund", R.string.icon_refund, Icons.Rounded.CurrencyExchange),
            LibraryIcon("present", R.string.icon_present, Icons.Rounded.Redeem),
            LibraryIcon("other_income", R.string.icon_other_income, Icons.Rounded.AccountBalanceWallet),
            LibraryIcon("other", R.string.icon_other, Icons.Rounded.Inventory2),
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

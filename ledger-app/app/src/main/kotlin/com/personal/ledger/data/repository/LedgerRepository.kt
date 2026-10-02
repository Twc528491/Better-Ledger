package com.personal.ledger.data.repository

import androidx.annotation.StringRes
import com.personal.ledger.R
import com.personal.ledger.data.TextResolver
import com.personal.ledger.data.local.dao.AccountDao
import com.personal.ledger.data.local.dao.CategoryDao
import com.personal.ledger.data.local.dao.LedgerDao
import com.personal.ledger.data.local.entity.AccountEntity
import com.personal.ledger.data.local.entity.CategoryEntity
import com.personal.ledger.data.local.entity.LedgerEntity
import com.personal.ledger.data.settings.SettingsRepository
import com.personal.ledger.domain.model.AccountType
import com.personal.ledger.domain.model.CategoryType
import com.personal.ledger.domain.model.Ledger
import com.personal.ledger.util.TimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * 新账本预置的默认分类；既有账本只通过用户主动触发的补充操作添加新增项。
 *
 * 列表中的中文名是**稳定种子键**：用于生成可重复的分类 ID（[categoryId]），不随语言变化。
 * 写入数据库的显示名在创建时按当前应用语言取自 [nameRes]（之后即为用户数据，不再翻译）。
 */
object DefaultSeed {
    /** 未注入 [TextResolver]（测试）时使用的默认账本名 / 现金账户名，与中文资源一致。 */
    const val LEDGER_NAME_KEY = "个人账本"
    const val CASH_ACCOUNT_KEY = "现金"

    val SUPPLEMENT_EXPENSE_CATEGORIES = listOf(
        "日用", "服饰", "通讯", "教育", "运动", "旅行", "宠物", "人情", "汽车", "数码",
        "育儿", "办公", "水电", "美容", "保险", "维修", "税费",
        "订阅", "生鲜", "零食", "咖啡", "家政", "快递", "捐赠", "烟酒",
    )
    val SUPPLEMENT_INCOME_CATEGORIES = listOf(
        "奖金", "兼职", "经营", "理财", "报销", "礼金", "退款",
        "租金", "分红", "利息", "补贴", "奖学金", "稿费", "佣金", "二手出售",
    )
    val EXPENSE_CATEGORIES = listOf("餐饮", "交通", "购物", "居住", "娱乐", "医疗") +
        SUPPLEMENT_EXPENSE_CATEGORIES + "其他"
    val INCOME_CATEGORIES = listOf("工资") + SUPPLEMENT_INCOME_CATEGORIES + "其他收入"

    /** 种子键 → 显示名资源。 */
    private val NAME_RES: Map<String, Int> = mapOf(
        "餐饮" to R.string.seed_cat_food,
        "交通" to R.string.seed_cat_transit,
        "购物" to R.string.seed_cat_shopping,
        "居住" to R.string.seed_cat_home,
        "娱乐" to R.string.seed_cat_movie,
        "医疗" to R.string.seed_cat_medical,
        "日用" to R.string.seed_cat_daily,
        "服饰" to R.string.seed_cat_clothing,
        "通讯" to R.string.seed_cat_phone,
        "教育" to R.string.seed_cat_education,
        "运动" to R.string.seed_cat_fitness,
        "旅行" to R.string.seed_cat_travel,
        "宠物" to R.string.seed_cat_pets,
        "人情" to R.string.seed_cat_gift,
        "汽车" to R.string.seed_cat_car,
        "数码" to R.string.seed_cat_devices,
        "育儿" to R.string.seed_cat_childcare,
        "办公" to R.string.seed_cat_office,
        "水电" to R.string.seed_cat_electric,
        "美容" to R.string.seed_cat_spa,
        "保险" to R.string.seed_cat_insurance,
        "维修" to R.string.seed_cat_repair,
        "税费" to R.string.seed_cat_tax,
        "订阅" to R.string.seed_cat_subscription,
        "生鲜" to R.string.seed_cat_fresh,
        "零食" to R.string.seed_cat_snack,
        "咖啡" to R.string.seed_cat_coffee,
        "家政" to R.string.seed_cat_housekeeping,
        "快递" to R.string.seed_cat_delivery,
        "捐赠" to R.string.seed_cat_donation,
        "烟酒" to R.string.seed_cat_tobacco,
        "其他" to R.string.seed_cat_other,
        "工资" to R.string.seed_cat_salary,
        "奖金" to R.string.seed_cat_bonus,
        "兼职" to R.string.seed_cat_work,
        "经营" to R.string.seed_cat_business,
        "理财" to R.string.seed_cat_invest,
        "报销" to R.string.seed_cat_reimburse,
        "礼金" to R.string.seed_cat_present,
        "退款" to R.string.seed_cat_refund,
        "租金" to R.string.seed_cat_rent,
        "分红" to R.string.seed_cat_dividend,
        "利息" to R.string.seed_cat_interest,
        "补贴" to R.string.seed_cat_subsidy,
        "奖学金" to R.string.seed_cat_scholarship,
        "稿费" to R.string.seed_cat_royalty,
        "佣金" to R.string.seed_cat_commission,
        "二手出售" to R.string.seed_cat_secondhand,
        "其他收入" to R.string.seed_cat_other_income,
    )

    /** 种子键对应的显示名资源；未知键返回 null。 */
    @StringRes
    fun nameRes(key: String): Int? = NAME_RES[key]

    /** 按当前语言生成显示名；无 [text] 时返回种子键本身（中文）。 */
    fun displayName(key: String, text: TextResolver?): String =
        nameRes(key)?.let { res -> text?.get(res) } ?: key

    /** 为预置分类生成可重复的账本内 ID，方便识别改名或已归档的旧选项。[name] 为种子键。 */
    fun categoryId(ledgerId: String, type: CategoryType, name: String): String =
        UUID.nameUUIDFromBytes(
            ("default-category\u0000" + ledgerId + "\u0000" + type.name + "\u0000" + name)
                .toByteArray(Charsets.UTF_8),
        ).toString()
}

class LedgerRepository(
    private val ledgerDao: LedgerDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val settings: SettingsRepository,
    /** 预置数据的显示语言；为 null（测试）时使用中文种子键。 */
    private val text: TextResolver? = null,
) {

    /** 进程内防重入（首次启动初始化只应执行一次）。 */
    private val ensureStarted = java.util.concurrent.atomic.AtomicBoolean(false)

    val ledgers: Flow<List<Ledger>> = ledgerDao.observeAll().map { list -> list.map { it.toDomain() } }

    /** 当前账本（严格按当前账本隔离；账本管理为全局页面）。
     *  当前账本已归档时回退到第一个未归档账本。 */
    val currentLedger: Flow<Ledger?> = combine(
        settings.currentLedgerId,
        ledgerDao.observeAll(),
    ) { id, all ->
        all.firstOrNull { it.id == id && !it.isArchived }?.toDomain()
            ?: all.firstOrNull { !it.isArchived }?.toDomain()
    }

    suspend fun setCurrentLedger(id: String) = settings.setCurrentLedgerId(id)

    /**
     * 首次启动：无任何账本时创建默认账本（CNY），并预置默认分类和一个现金账户。
     * 幂等：已存在账本时不做任何事；进程内防重入。
     */
    suspend fun ensureDefaultLedger() {
        if (!ensureStarted.compareAndSet(false, true)) return
        val existing = ledgerDao.getAll()
        if (existing.isNotEmpty()) return

        val now = TimeUtils.now()
        val ledger = LedgerEntity(
            id = UUID.randomUUID().toString(),
            name = text?.get(R.string.seed_ledger_name) ?: DefaultSeed.LEDGER_NAME_KEY,
            baseCurrency = "CNY",
            createdAt = now,
            updatedAt = now,
            isArchived = false,
        )
        ledgerDao.insert(ledger)
        seedLedgerData(ledger)

        settings.setCurrentLedgerId(ledger.id)
    }

    suspend fun createLedger(name: String, baseCurrency: String = "CNY"): Ledger {
        val now = TimeUtils.now()
        val ledger = LedgerEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            baseCurrency = baseCurrency,
            createdAt = now,
            updatedAt = now,
            isArchived = false,
        )
        ledgerDao.insert(ledger)
        // 决策 7：新账本预置少量默认分类（全部可编辑、可归档）+ 一个现金账户，保证开箱即记。
        seedLedgerData(ledger)
        return ledger.toDomain()
    }

    /** 为新账本写入默认分类与现金账户。 */
    private suspend fun seedLedgerData(ledger: LedgerEntity) {
        val now = TimeUtils.now()
        DefaultSeed.EXPENSE_CATEGORIES.forEachIndexed { i, key ->
            categoryDao.insert(
                CategoryEntity(
                    id = DefaultSeed.categoryId(ledger.id, CategoryType.EXPENSE, key),
                    ledgerId = ledger.id,
                    name = DefaultSeed.displayName(key, text),
                    type = CategoryType.EXPENSE,
                    parentId = null,
                    sortOrder = i,
                    isArchived = false,
                )
            )
        }
        DefaultSeed.INCOME_CATEGORIES.forEachIndexed { i, key ->
            categoryDao.insert(
                CategoryEntity(
                    id = DefaultSeed.categoryId(ledger.id, CategoryType.INCOME, key),
                    ledgerId = ledger.id,
                    name = DefaultSeed.displayName(key, text),
                    type = CategoryType.INCOME,
                    parentId = null,
                    sortOrder = i,
                    isArchived = false,
                )
            )
        }
        accountDao.insert(
            AccountEntity(
                id = UUID.randomUUID().toString(),
                ledgerId = ledger.id,
                name = text?.get(R.string.seed_account_cash) ?: DefaultSeed.CASH_ACCOUNT_KEY,
                type = AccountType.CASH,
                currency = ledger.baseCurrency,
                initialBalance = 0L,
                initialBalanceTime = now,
                createdAt = now,
                updatedAt = now,
                isArchived = false,
            )
        )
    }

    suspend fun setLedgerArchived(id: String, archived: Boolean) {
        val ledger = ledgerDao.getById(id) ?: return
        ledgerDao.update(ledger.copy(isArchived = archived, updatedAt = TimeUtils.now()))
    }

    /** 恢复后当前账本 ID 失效时，自动选择第一个有效账本。 */
    suspend fun fallbackCurrentLedgerIfInvalid() {
        val currentId = settings.currentLedgerId.first()
        if (currentId == null) return
        val all = ledgerDao.getAll()
        val valid = all.any { it.id == currentId && !it.isArchived }
        if (!valid) {
            settings.setCurrentLedgerId(all.firstOrNull { !it.isArchived }?.id)
        }
    }
}

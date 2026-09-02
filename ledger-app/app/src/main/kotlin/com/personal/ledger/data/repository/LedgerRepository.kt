package com.personal.ledger.data.repository

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

/** 新账本预置的默认分类（决策 7：少量、全部可编辑可归档；清单实现前单独确定，当前为初版提案）。 */
object DefaultSeed {
    val EXPENSE_CATEGORIES = listOf("餐饮", "交通", "购物", "居住", "娱乐", "医疗", "其他")
    val INCOME_CATEGORIES = listOf("工资", "其他收入")
}

class LedgerRepository(
    private val ledgerDao: LedgerDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val settings: SettingsRepository,
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
            name = "个人账本",
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
        DefaultSeed.EXPENSE_CATEGORIES.forEachIndexed { i, name ->
            categoryDao.insert(
                CategoryEntity(
                    id = UUID.randomUUID().toString(),
                    ledgerId = ledger.id,
                    name = name,
                    type = CategoryType.EXPENSE,
                    parentId = null,
                    sortOrder = i,
                    isArchived = false,
                )
            )
        }
        DefaultSeed.INCOME_CATEGORIES.forEachIndexed { i, name ->
            categoryDao.insert(
                CategoryEntity(
                    id = UUID.randomUUID().toString(),
                    ledgerId = ledger.id,
                    name = name,
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
                name = "现金",
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

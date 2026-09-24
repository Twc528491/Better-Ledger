package com.personal.ledger.data.repository

import androidx.room.withTransaction
import com.personal.ledger.data.local.database.AppDatabase
import com.personal.ledger.data.local.entity.CategoryEntity
import com.personal.ledger.data.local.entity.MerchantCategoryEntity
import com.personal.ledger.data.local.entity.MerchantEntity
import com.personal.ledger.domain.model.Merchant
import com.personal.ledger.util.TimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class MerchantRepository(private val database: AppDatabase) {
    private val merchantDao = database.merchantDao()
    private val categoryDao = database.categoryDao()
    private val linkDao = database.merchantCategoryDao()

    fun observeByLedger(ledgerId: String): Flow<List<Merchant>> =
        merchantDao.observeByLedger(ledgerId).map { list -> list.map { it.toDomain() } }

    suspend fun getByLedger(ledgerId: String): List<Merchant> =
        merchantDao.getByLedger(ledgerId).map { it.toDomain() }

    /** Both first- and second-level IDs resolve to the same top-level library. */
    fun observeByCategory(ledgerId: String, categoryId: String): Flow<List<Merchant>> =
        merchantDao.observeByCategory(ledgerId, categoryId).map { list -> list.map { it.toDomain() } }

    suspend fun getByCategory(ledgerId: String, categoryId: String): List<Merchant> =
        merchantDao.getByCategory(ledgerId, categoryId).map { it.toDomain() }

    fun observeLinksByLedger(ledgerId: String): Flow<List<MerchantCategoryEntity>> =
        linkDao.observeByLedger(ledgerId)

    fun observeCategoryIdsByMerchant(merchantId: String): Flow<Set<String>> =
        linkDao.observeByMerchant(merchantId).map { links -> links.map { it.categoryId }.toSet() }

    suspend fun getCategoryIdsByMerchant(merchantId: String): Set<String> =
        linkDao.getByMerchant(merchantId).map { it.categoryId }.toSet()

    /** 商家允许同名（UI 避免误重复创建）。 */
    suspend fun addMerchant(
        ledgerId: String,
        name: String,
        categoryIds: Set<String> = emptySet(),
    ): Result<Merchant> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return Result.failure(IllegalArgumentException("名称不能为空"))
        return try {
            val entity = database.withTransaction {
                val roots = categoryIds.map { activeRoot(it, ledgerId) }.toSet()
                val created = MerchantEntity(
                    id = UUID.randomUUID().toString(),
                    ledgerId = ledgerId,
                    name = trimmed,
                    createdAt = TimeUtils.now(),
                    isArchived = false,
                )
                merchantDao.insert(created)
                roots.forEach { linkDao.insert(MerchantCategoryEntity(created.id, it)) }
                created
            }
            Result.success(entity.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Reusing a merchant in another category keeps the single shared merchant record. */
    suspend fun assignToCategory(merchantId: String, categoryId: String): Result<Unit> = try {
        database.withTransaction {
            val merchant = merchantDao.getById(merchantId)
                ?: throw NoSuchElementException("商家不存在")
            if (merchant.isArchived) throw IllegalArgumentException("商家已删除")
            val rootId = activeRoot(categoryId, merchant.ledgerId)
            linkDao.insert(MerchantCategoryEntity(merchantId, rootId))
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** Replaces selectable links while preserving archived historical categories. */
    suspend fun replaceCategories(merchantId: String, categoryIds: Set<String>): Result<Unit> = try {
        database.withTransaction {
            val merchant = merchantDao.getById(merchantId)
                ?: throw NoSuchElementException("商家不存在")
            if (merchant.isArchived) throw IllegalArgumentException("商家已删除")
            val selected = categoryIds.map { activeRoot(it, merchant.ledgerId) }.toSet()
            val archived = linkDao.getByMerchant(merchantId).mapNotNull { link ->
                categoryDao.getById(link.categoryId)?.takeIf { it.isArchived }?.id
            }.toSet()
            linkDao.deleteByMerchant(merchantId)
            (selected + archived).forEach { linkDao.insert(MerchantCategoryEntity(merchantId, it)) }
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private suspend fun activeRoot(categoryId: String, ledgerId: String): String {
        val selected = categoryDao.getById(categoryId)
            ?: throw IllegalArgumentException("分类不存在")
        if (selected.ledgerId != ledgerId || selected.isArchived) {
            throw IllegalArgumentException("分类与账本不匹配或已删除")
        }
        val root: CategoryEntity = selected.parentId?.let {
            categoryDao.getById(it) ?: throw IllegalArgumentException("一级分类不存在")
        } ?: selected
        if (root.ledgerId != ledgerId || root.parentId != null ||
            root.type != selected.type || root.isArchived
        ) {
            throw IllegalArgumentException("一级分类与账本不匹配或已删除")
        }
        return root.id
    }

    suspend fun setArchived(id: String, archived: Boolean): Result<Unit> {
        val entity = merchantDao.getById(id)
            ?: return Result.failure(NoSuchElementException("商家不存在"))
        return try {
            merchantDao.update(entity.copy(isArchived = archived))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 重命名商家；保留 id，历史交易仍能解析到新名称。 */
    suspend fun rename(id: String, newName: String): Result<Unit> {
        val entity = merchantDao.getById(id)
            ?: return Result.failure(NoSuchElementException("商家不存在"))
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return Result.failure(IllegalArgumentException("名称不能为空"))
        return try {
            merchantDao.update(entity.copy(name = trimmed))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 删除商家采用软归档，避免破坏历史交易中的 merchantId 引用。 */
    suspend fun delete(id: String): Result<Unit> = setArchived(id, archived = true)
}

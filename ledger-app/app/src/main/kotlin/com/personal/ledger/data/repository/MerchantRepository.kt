package com.personal.ledger.data.repository

import androidx.room.withTransaction
import com.personal.ledger.R
import com.personal.ledger.data.local.database.AppDatabase
import com.personal.ledger.data.local.entity.CategoryEntity
import com.personal.ledger.data.local.entity.MerchantCategoryEntity
import com.personal.ledger.data.local.entity.MerchantEntity
import com.personal.ledger.domain.model.Merchant
import com.personal.ledger.util.TimeUtils
import com.personal.ledger.util.UserFacingException
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

    /**
     * 新建商家。数据模型允许同名，但同账本内已存在未删除的同名商家（忽略大小写与空白）时，
     * 直接复用该商家并补充分类关联，避免在不同分类下误建出重复商家。
     */
    suspend fun addMerchant(
        ledgerId: String,
        name: String,
        categoryIds: Set<String> = emptySet(),
    ): Result<Merchant> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return Result.failure(UserFacingException(R.string.err_name_empty))
        return try {
            val entity = database.withTransaction {
                val roots = categoryIds.map { activeRoot(it, ledgerId) }.toSet()
                val key = nameKey(trimmed)
                val target = merchantDao.getByLedger(ledgerId)
                    .firstOrNull { !it.isArchived && nameKey(it.name) == key }
                    ?: MerchantEntity(
                        id = UUID.randomUUID().toString(),
                        ledgerId = ledgerId,
                        name = trimmed,
                        createdAt = TimeUtils.now(),
                        isArchived = false,
                    ).also { merchantDao.insert(it) }
                roots.forEach { linkDao.insert(MerchantCategoryEntity(target.id, it)) }
                target
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
                ?: throw UserFacingException(R.string.err_merchant_not_found)
            if (merchant.isArchived) throw UserFacingException(R.string.err_merchant_deleted)
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
                ?: throw UserFacingException(R.string.err_merchant_not_found)
            if (merchant.isArchived) throw UserFacingException(R.string.err_merchant_deleted)
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
            ?: throw UserFacingException(R.string.err_category_not_found)
        if (selected.ledgerId != ledgerId || selected.isArchived) {
            throw UserFacingException(R.string.err_category_ledger_mismatch)
        }
        val root: CategoryEntity = selected.parentId?.let {
            categoryDao.getById(it) ?: throw UserFacingException(R.string.err_root_category_not_found)
        } ?: selected
        if (root.ledgerId != ledgerId || root.parentId != null ||
            root.type != selected.type || root.isArchived
        ) {
            throw UserFacingException(R.string.err_root_category_ledger_mismatch)
        }
        return root.id
    }

    suspend fun setArchived(id: String, archived: Boolean): Result<Unit> {
        val entity = merchantDao.getById(id)
            ?: return Result.failure(UserFacingException(R.string.err_merchant_not_found))
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
            ?: return Result.failure(UserFacingException(R.string.err_merchant_not_found))
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return Result.failure(UserFacingException(R.string.err_name_empty))
        return try {
            merchantDao.update(entity.copy(name = trimmed))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 删除商家采用软归档，避免破坏历史交易中的 merchantId 引用。 */
    suspend fun delete(id: String): Result<Unit> = setArchived(id, archived = true)

    companion object {
        /** 商家名称比较键：去首尾空白、合并中间空白、忽略大小写。 */
        fun nameKey(name: String): String = name.trim().replace(Regex("\\s+"), " ").lowercase()
    }
}

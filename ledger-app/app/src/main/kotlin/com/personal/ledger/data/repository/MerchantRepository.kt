package com.personal.ledger.data.repository

import com.personal.ledger.data.local.dao.MerchantDao
import com.personal.ledger.data.local.entity.MerchantEntity
import com.personal.ledger.domain.model.Merchant
import com.personal.ledger.util.TimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class MerchantRepository(private val merchantDao: MerchantDao) {

    fun observeByLedger(ledgerId: String): Flow<List<Merchant>> =
        merchantDao.observeByLedger(ledgerId).map { list -> list.map { it.toDomain() } }

    suspend fun getByLedger(ledgerId: String): List<Merchant> =
        merchantDao.getByLedger(ledgerId).map { it.toDomain() }

    /** 商家允许同名（UI 避免误重复创建）。 */
    suspend fun addMerchant(ledgerId: String, name: String): Result<Merchant> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return Result.failure(IllegalArgumentException("名称不能为空"))
        return try {
            val entity = MerchantEntity(
                id = UUID.randomUUID().toString(),
                ledgerId = ledgerId,
                name = trimmed,
                createdAt = TimeUtils.now(),
                isArchived = false,
            )
            merchantDao.insert(entity)
            Result.success(entity.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
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

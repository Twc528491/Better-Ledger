package com.personal.ledger.data.repository

import com.personal.ledger.data.local.dao.TagDao
import com.personal.ledger.data.local.entity.TagEntity
import com.personal.ledger.domain.model.Tag
import com.personal.ledger.util.TimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class TagRepository(private val tagDao: TagDao) {

    fun observeByLedger(ledgerId: String): Flow<List<Tag>> =
        tagDao.observeByLedger(ledgerId).map { list -> list.map { it.toDomain() } }

    suspend fun getByLedger(ledgerId: String): List<Tag> =
        tagDao.getByLedger(ledgerId).map { it.toDomain() }

    /** 同账本内名称唯一。 */
    suspend fun addTag(ledgerId: String, name: String): Result<Tag> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return Result.failure(IllegalArgumentException("名称不能为空"))
        if (tagDao.countSameName(ledgerId, trimmed) > 0) {
            return Result.failure(IllegalArgumentException("已存在同名标签"))
        }
        return try {
            val entity = TagEntity(
                id = UUID.randomUUID().toString(),
                ledgerId = ledgerId,
                name = trimmed,
                createdAt = TimeUtils.now(),
                isArchived = false,
            )
            tagDao.insert(entity)
            Result.success(entity.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setArchived(id: String, archived: Boolean): Result<Unit> {
        val all = tagDao.getAll()
        val entity = all.firstOrNull { it.id == id }
            ?: return Result.failure(NoSuchElementException("标签不存在"))
        return try {
            tagDao.update(entity.copy(isArchived = archived))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

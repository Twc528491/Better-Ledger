package io.github.twc528491.betterledger.data.repository

import io.github.twc528491.betterledger.R
import io.github.twc528491.betterledger.data.local.dao.TagDao
import io.github.twc528491.betterledger.data.local.entity.TagEntity
import io.github.twc528491.betterledger.domain.model.Tag
import io.github.twc528491.betterledger.util.TimeUtils
import io.github.twc528491.betterledger.util.UserFacingException
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
        if (trimmed.isEmpty()) return Result.failure(UserFacingException(R.string.err_name_empty))
        if (tagDao.countSameName(ledgerId, trimmed) > 0) {
            return Result.failure(UserFacingException(R.string.err_tag_duplicate))
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
            ?: return Result.failure(UserFacingException(R.string.err_tag_not_found))
        return try {
            tagDao.update(entity.copy(isArchived = archived))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

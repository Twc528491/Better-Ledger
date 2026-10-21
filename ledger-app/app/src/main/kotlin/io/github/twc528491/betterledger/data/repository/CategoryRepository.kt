package io.github.twc528491.betterledger.data.repository

import androidx.room.withTransaction
import io.github.twc528491.betterledger.R
import io.github.twc528491.betterledger.data.TextResolver
import io.github.twc528491.betterledger.data.local.database.AppDatabase
import io.github.twc528491.betterledger.data.local.entity.CategoryEntity
import io.github.twc528491.betterledger.domain.model.Category
import io.github.twc528491.betterledger.domain.model.CategoryType
import io.github.twc528491.betterledger.util.TimeUtils
import io.github.twc528491.betterledger.util.UserFacingException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * 分类图标 key 的数据层校验。数据层不依赖 UI 图标库：只保证 key 为 null 或简短非空字符串；
 * 未知 key 由 UI 回退为按名称匹配的图标。
 */
object CategoryIconKeys {
    const val MAX_LENGTH = 64

    fun isValid(key: String?): Boolean =
        key == null || (key.isNotBlank() && key == key.trim() && key.length <= MAX_LENGTH)

    /** null 保持 null（自动图标）；去除首尾空白后校验，非法时抛出 [IllegalArgumentException]。 */
    fun normalize(key: String?): String? {
        if (key == null) return null
        val trimmed = key.trim()
        if (!isValid(trimmed)) throw UserFacingException(R.string.err_invalid_category_icon)
        return trimmed
    }
}

class CategoryRepository(
    private val database: AppDatabase,
    /** 补充分类的显示语言；为 null（测试）时使用中文种子键。 */
    private val text: TextResolver? = null,
) {

    private val categoryDao = database.categoryDao()

    fun observeByLedger(ledgerId: String): Flow<List<Category>> =
        categoryDao.observeByLedger(ledgerId).map { list -> list.map { it.toDomain() } }

    suspend fun getByLedger(ledgerId: String): List<Category> =
        categoryDao.getByLedger(ledgerId).map { it.toDomain() }

    suspend fun getById(id: String): Category? = categoryDao.getById(id)?.toDomain()

    /**
     * 给旧账本补充常用分类。只加旧版种子清单之外的选项；
     * 会跳过已有的同名（包括已归档）分类和已存在的稳定种子 ID。
     */
    suspend fun supplementCommonCategories(ledgerId: String): Result<Int> {
        return try {
            val insertedCount = database.withTransaction {
                val existing = categoryDao.getByLedger(ledgerId)
                var expenseSort = existing
                    .filter { it.type == CategoryType.EXPENSE && it.parentId == null }
                    .maxOfOrNull { it.sortOrder }?.plus(1) ?: 0
                var incomeSort = existing
                    .filter { it.type == CategoryType.INCOME && it.parentId == null }
                    .maxOfOrNull { it.sortOrder }?.plus(1) ?: 0
                var inserted = 0

                val additions = DefaultSeed.SUPPLEMENT_EXPENSE_CATEGORIES.map {
                    Triple(CategoryType.EXPENSE, it, expenseSort++)
                } + DefaultSeed.SUPPLEMENT_INCOME_CATEGORIES.map {
                    Triple(CategoryType.INCOME, it, incomeSort++)
                }

                additions.forEach { (type, key, sortOrder) ->
                    // ID 由稳定的中文种子键生成，与语言无关；同名判断同时比较种子键与当前语言显示名。
                    val id = DefaultSeed.categoryId(ledgerId, type, key)
                    val name = DefaultSeed.displayName(key, text)
                    val alreadyPresent = existing.any {
                        it.id == id ||
                            (it.type == type && it.parentId == null && (it.name == name || it.name == key))
                    }
                    if (!alreadyPresent) {
                        categoryDao.insert(
                            CategoryEntity(
                                id = id,
                                ledgerId = ledgerId,
                                name = name,
                                type = type,
                                parentId = null,
                                sortOrder = sortOrder,
                                isArchived = false,
                            ),
                        )
                        inserted += 1
                    }
                }
                inserted
            }
            Result.success(insertedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 新建分类。约束（DATA_MODEL.md）：
     * - 最多两级；二级分类 parentId 必须指向同账本一级分类，且父子 type 一致；
     * - 同账本同类型一级分类名称唯一，同一父分类下二级分类名称唯一；
     * - 唯一性校验必须在事务内进行（parentId 可空，不能仅依赖 SQLite UNIQUE）。
     * [iconKey] 为 null 时按名称自动匹配图标。
     */
    suspend fun addCategory(
        ledgerId: String,
        name: String,
        type: CategoryType,
        parentId: String? = null,
        iconKey: String? = null,
    ): Result<Category> {
        return try {
            val category = database.withTransaction {
                val normalizedIconKey = CategoryIconKeys.normalize(iconKey)
                val trimmed = name.trim()
                if (trimmed.isEmpty()) throw UserFacingException(R.string.err_name_empty)
                val parent = parentId?.let { categoryDao.getById(it) }
                if (parentId != null) {
                    if (parent == null) throw UserFacingException(R.string.err_parent_category_not_found)
                    if (parent.ledgerId != ledgerId) throw UserFacingException(R.string.err_parent_category_not_in_ledger)
                    if (parent.parentId != null) throw UserFacingException(R.string.err_category_max_depth)
                    if (parent.type != type) throw UserFacingException(R.string.err_parent_child_type)
                }
                val count = categoryDao.countSameName(ledgerId, type, parentId, trimmed)
                if (count > 0) throw UserFacingException(R.string.err_category_duplicate)
                val maxSort = categoryDao.getByLedgerAndType(ledgerId, type)
                    .filter { it.parentId == parentId }
                    .maxOfOrNull { it.sortOrder } ?: -1
                val entity = CategoryEntity(
                    id = UUID.randomUUID().toString(),
                    ledgerId = ledgerId,
                    name = trimmed,
                    type = type,
                    parentId = parentId,
                    sortOrder = maxSort + 1,
                    isArchived = false,
                    iconKey = normalizedIconKey,
                )
                categoryDao.insert(entity)
                entity.toDomain()
            }
            Result.success(category)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 重命名（保持层级与类型不变）。 */
    suspend fun rename(id: String, newName: String): Result<Unit> {
        return try {
            database.withTransaction {
                val entity = categoryDao.getById(id) ?: throw UserFacingException(R.string.err_category_not_found)
                val trimmed = newName.trim()
                if (trimmed.isEmpty()) throw UserFacingException(R.string.err_name_empty)
                val count = categoryDao.countSameName(
                    entity.ledgerId,
                    entity.type,
                    entity.parentId,
                    trimmed,
                    excludeId = entity.id,
                )
                if (count > 0) throw UserFacingException(R.string.err_category_duplicate)
                categoryDao.update(entity.copy(name = trimmed))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 更换分类图标；null 恢复为按名称自动匹配。仅修改图标，不影响名称、层级与排序。 */
    suspend fun setIconKey(id: String, iconKey: String?): Result<Unit> {
        return try {
            val normalized = CategoryIconKeys.normalize(iconKey)
            database.withTransaction {
                val entity = categoryDao.getById(id) ?: throw UserFacingException(R.string.err_category_not_found)
                if (entity.iconKey != normalized) {
                    categoryDao.update(entity.copy(iconKey = normalized))
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 归档 / 恢复归档。 */
    suspend fun setArchived(id: String, archived: Boolean): Result<Unit> {
        val entity = categoryDao.getById(id) ?: return Result.failure(UserFacingException(R.string.err_category_not_found))
        return try {
            categoryDao.update(entity.copy(isArchived = archived))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 删除分类采用软归档，保留交易与预算中的 categoryId。
     * 删除一级分类时同步归档其二级分类，避免留下看不见父级的活动子分类。
     */
    suspend fun delete(id: String): Result<Unit> {
        return try {
            database.withTransaction {
                val entity = categoryDao.getById(id) ?: throw UserFacingException(R.string.err_category_not_found)
                val all = categoryDao.getByLedger(entity.ledgerId)
                if (entity.parentId == null) {
                    all.filter { it.parentId == entity.id && !it.isArchived }
                        .forEach { categoryDao.update(it.copy(isArchived = true)) }
                }
                if (!entity.isArchived) {
                    categoryDao.update(entity.copy(isArchived = true))
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 在同一层级内移动分类；交换排序值可保持父子层级和其他分类稳定。 */
    suspend fun move(id: String, direction: Int): Result<Unit> {
        if (direction !in setOf(-1, 1)) return Result.failure(UserFacingException(R.string.err_invalid_move_direction))
        return try {
            database.withTransaction {
                val entity = categoryDao.getById(id) ?: throw UserFacingException(R.string.err_category_not_found)
                val siblings = categoryDao.getByLedgerAndType(entity.ledgerId, entity.type)
                    .filter { it.parentId == entity.parentId && it.isArchived == entity.isArchived }
                    .sortedBy { it.sortOrder }
                val index = siblings.indexOfFirst { it.id == id }
                val targetIndex = index + direction
                if (index < 0 || targetIndex !in siblings.indices) return@withTransaction
                val target = siblings[targetIndex]
                categoryDao.update(entity.copy(sortOrder = target.sortOrder))
                categoryDao.update(target.copy(sortOrder = entity.sortOrder))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 构建 分类id → 父分类id 映射（供预算分类树汇总使用）。 */
    suspend fun parentMap(ledgerId: String): Map<String, String?> =
        categoryDao.getByLedger(ledgerId).associate { it.id to it.parentId }
}

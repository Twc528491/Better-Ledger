package io.github.twc528491.betterledger.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.twc528491.betterledger.data.local.entity.CategoryEntity
import io.github.twc528491.betterledger.domain.model.CategoryType
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM category WHERE ledgerId = :ledgerId ORDER BY isArchived ASC, sortOrder ASC")
    fun observeByLedger(ledgerId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM category WHERE ledgerId = :ledgerId ORDER BY isArchived ASC, sortOrder ASC")
    suspend fun getByLedger(ledgerId: String): List<CategoryEntity>

    @Query(
        "SELECT * FROM category WHERE ledgerId = :ledgerId AND type = :type " +
            "ORDER BY isArchived ASC, sortOrder ASC"
    )
    suspend fun getByLedgerAndType(ledgerId: String, type: CategoryType): List<CategoryEntity>

    @Query("SELECT * FROM category WHERE id = :id")
    suspend fun getById(id: String): CategoryEntity?

    @Query("SELECT * FROM category")
    suspend fun getAll(): List<CategoryEntity>

    @Query("DELETE FROM category")
    suspend fun deleteAll()

    /**
     * 业务唯一性：同账本、同类型的一级分类名称唯一；同一父分类下二级分类名称唯一。
     * 因 parentId 可为空，不能依赖 SQLite 复合 UNIQUE，由 Repository 在事务内先查后插。
     * excludeId 用于重命名时排除自身。
     */
    @Query(
        "SELECT COUNT(*) FROM category WHERE ledgerId = :ledgerId AND type = :type " +
            "AND (parentId IS NULL AND :parentId IS NULL OR parentId = :parentId) AND name = :name " +
            "AND (:excludeId IS NULL OR id != :excludeId)"
    )
    suspend fun countSameName(
        ledgerId: String,
        type: CategoryType,
        parentId: String?,
        name: String,
        excludeId: String? = null,
    ): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(category: CategoryEntity)

    @Update
    suspend fun update(category: CategoryEntity)
}

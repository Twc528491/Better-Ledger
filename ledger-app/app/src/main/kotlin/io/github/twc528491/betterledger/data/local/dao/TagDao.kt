package io.github.twc528491.betterledger.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.twc528491.betterledger.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Query("SELECT * FROM tag WHERE ledgerId = :ledgerId ORDER BY createdAt ASC")
    fun observeByLedger(ledgerId: String): Flow<List<TagEntity>>

    @Query("SELECT * FROM tag WHERE ledgerId = :ledgerId ORDER BY createdAt ASC")
    suspend fun getByLedger(ledgerId: String): List<TagEntity>

    @Query("SELECT * FROM tag WHERE id = :id")
    suspend fun getById(id: String): TagEntity?

    @Query("SELECT * FROM tag")
    suspend fun getAll(): List<TagEntity>

    @Query("DELETE FROM tag")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM tag WHERE ledgerId = :ledgerId AND name = :name")
    suspend fun countSameName(ledgerId: String, name: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(tag: TagEntity)

    @Update
    suspend fun update(tag: TagEntity)
}

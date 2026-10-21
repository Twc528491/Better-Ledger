package io.github.twc528491.betterledger.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.twc528491.betterledger.data.local.entity.RecurringChargeEntity
import io.github.twc528491.betterledger.data.local.entity.RecurringOccurrenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringChargeDao {
    @Query("SELECT * FROM recurring_charge WHERE ledgerId = :ledgerId ORDER BY isPaused ASC, nextDueAt ASC, name COLLATE NOCASE ASC")
    fun observeByLedger(ledgerId: String): Flow<List<RecurringChargeEntity>>

    @Query("SELECT * FROM recurring_charge WHERE id = :id")
    suspend fun getById(id: String): RecurringChargeEntity?

    @Query("SELECT * FROM recurring_charge ORDER BY id")
    suspend fun getAllCharges(): List<RecurringChargeEntity>

    @Query("SELECT * FROM recurring_charge WHERE isPaused = 0 AND lastError IS NULL AND nextDueAt <= :nowMs ORDER BY nextDueAt LIMIT :limit")
    suspend fun getDue(nowMs: Long, limit: Int): List<RecurringChargeEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM recurring_charge WHERE isPaused = 0 AND lastError IS NULL AND nextDueAt <= :nowMs)")
    suspend fun hasDue(nowMs: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: RecurringChargeEntity)

    @Update
    suspend fun update(entity: RecurringChargeEntity)

    @Query("DELETE FROM recurring_charge WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM recurring_occurrence WHERE ruleId = :ruleId AND dueAt = :dueAt LIMIT 1")
    suspend fun getOccurrence(ruleId: String, dueAt: Long): RecurringOccurrenceEntity?

    @Query("SELECT * FROM recurring_occurrence ORDER BY ruleId, dueAt")
    suspend fun getAllOccurrences(): List<RecurringOccurrenceEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertOccurrence(entity: RecurringOccurrenceEntity)

    @Query("DELETE FROM recurring_occurrence")
    suspend fun deleteAllOccurrences()

    @Query("DELETE FROM recurring_charge")
    suspend fun deleteAllCharges()
}

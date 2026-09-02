package com.personal.ledger.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.personal.ledger.data.local.entity.TransactionTagEntity

@Dao
interface TransactionTagDao {

    @Query("SELECT tagId FROM transaction_tag WHERE transactionId = :transactionId")
    suspend fun getTagIds(transactionId: String): List<String>

    @Query(
        "SELECT tt.* FROM transaction_tag tt INNER JOIN `transaction` t ON t.id = tt.transactionId " +
            "WHERE t.ledgerId = :ledgerId"
    )
    fun observeByLedger(ledgerId: String): kotlinx.coroutines.flow.Flow<List<TransactionTagEntity>>

    @Query(
        "SELECT tt.* FROM transaction_tag tt INNER JOIN `transaction` t ON t.id = tt.transactionId " +
            "WHERE t.ledgerId = :ledgerId"
    )
    suspend fun getByLedger(ledgerId: String): List<TransactionTagEntity>

    @Query("SELECT * FROM transaction_tag")
    suspend fun getAll(): List<TransactionTagEntity>

    @Query("DELETE FROM transaction_tag")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(links: List<TransactionTagEntity>)

    @Query("DELETE FROM transaction_tag WHERE transactionId = :transactionId")
    suspend fun deleteByTransaction(transactionId: String)
}

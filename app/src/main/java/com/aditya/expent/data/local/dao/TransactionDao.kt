package com.aditya.expent.data.local.dao

import androidx.room.*
import com.aditya.expent.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 OR isDeleted IS NULL ORDER BY transactionDate DESC")
    fun getTransactions(): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions
        WHERE (isDeleted = 0 OR isDeleted IS NULL)
        AND transactionDate >= :from AND transactionDate <= :to
        ORDER BY transactionDate DESC
    """)
    fun getTransactions(from: String, to: String): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions
        WHERE (isDeleted = 0 OR isDeleted IS NULL)
        AND type = :type
        ORDER BY transactionDate DESC
    """)
    fun getTransactionsByType(type: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransaction(id: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transactions: List<TransactionEntity>)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions")
    suspend fun clear()
}

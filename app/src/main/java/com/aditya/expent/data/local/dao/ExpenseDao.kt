package com.aditya.expent.data.local.dao

import androidx.room.*
import com.aditya.expent.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses WHERE isDeleted = 0 OR isDeleted IS NULL ORDER BY date")
    fun getExpenses(): Flow<List<ExpenseEntity>>


    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpense(id: String): ExpenseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: ExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expenses: List<ExpenseEntity>)

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Delete
    suspend fun delete(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM expenses")
    suspend fun clear()

    // Used when a local-* category is swapped for its server row
    @Query("UPDATE expenses SET categoryId = :newId WHERE categoryId = :oldId")
    suspend fun updateCategoryId(oldId: String, newId: String)

    @Query("DELETE FROM expenses WHERE syncStatus = 'SYNCED' AND id NOT LIKE 'local-%'")
    suspend fun clearSynced()

    @Transaction
    suspend fun replaceAll(expenses: List<ExpenseEntity>) {
        clearSynced()
        insert(expenses)
    }
}

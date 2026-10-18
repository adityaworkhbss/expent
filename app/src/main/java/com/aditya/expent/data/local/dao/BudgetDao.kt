package com.aditya.expent.data.local.dao

import androidx.room.*
import com.aditya.expent.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budgets WHERE isDeleted = 0 OR isDeleted IS NULL ORDER BY date")
    fun getBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getBudget(id: String): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: BudgetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budgets: List<BudgetEntity>)

    @Update
    suspend fun update(budget: BudgetEntity)

    @Delete
    suspend fun delete(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM budgets")
    suspend fun clear()

    // Used when a local-* category is swapped for its server row
    @Query("UPDATE budgets SET categoryId = :newId WHERE categoryId = :oldId")
    suspend fun updateCategoryId(oldId: String, newId: String)

    @Query("DELETE FROM budgets WHERE syncStatus = 'SYNCED' AND id NOT LIKE 'local-%'")
    suspend fun clearSynced()

    @Transaction
    suspend fun replaceAll(budgets: List<BudgetEntity>) {
        clearSynced()
        insert(budgets)
    }
}

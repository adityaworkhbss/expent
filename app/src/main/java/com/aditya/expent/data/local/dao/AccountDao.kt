package com.aditya.expent.data.local.dao

import androidx.room.*
import com.aditya.expent.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Query("SELECT * FROM accounts WHERE isDeleted = 0 OR isDeleted IS NULL ORDER BY name")
    fun getAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccount(id: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(accounts: List<AccountEntity>)

    @Update
    suspend fun update(account: AccountEntity)

    @Delete
    suspend fun delete(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM accounts")
    suspend fun clear()

    @Query("DELETE FROM accounts WHERE syncStatus = 'SYNCED' AND id NOT LIKE 'local-%'")
    suspend fun clearSynced()

    @Transaction
    suspend fun replaceAll(accounts: List<AccountEntity>) {
        clearSynced()
        insert(accounts)
    }
}

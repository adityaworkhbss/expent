package com.aditya.expent.data.local.dao

import androidx.room.*
import com.aditya.expent.data.local.entity.PendingSyncEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingSyncDao {

    @Query("SELECT * FROM pending_sync ORDER BY createdAt ASC")
    fun getPendingSyncs(): Flow<List<PendingSyncEntity>>

    @Query("SELECT * FROM pending_sync ORDER BY createdAt ASC")
    suspend fun getAllPendingSyncs(): List<PendingSyncEntity>

    @Query("SELECT * FROM pending_sync WHERE id = :id")
    suspend fun getPendingSync(id: Long): PendingSyncEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pendingSync: PendingSyncEntity): Long

    @Update
    suspend fun update(pendingSync: PendingSyncEntity)

    @Delete
    suspend fun delete(pendingSync: PendingSyncEntity)

    @Query("DELETE FROM pending_sync WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM pending_sync")
    suspend fun clear()
}

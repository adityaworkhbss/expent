package com.aditya.expent.data.local.dao

import androidx.room.*
import com.aditya.expent.data.local.entity.CustomizationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomizationDao {

    @Query("SELECT * FROM customizations LIMIT 1")
    fun getCustomization(): Flow<CustomizationEntity?>

    @Query("SELECT * FROM customizations WHERE userId = :userId")
    suspend fun getCustomization(userId: String): CustomizationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(customization: CustomizationEntity)

    @Delete
    suspend fun delete(customization: CustomizationEntity)

    @Query("DELETE FROM customizations")
    suspend fun clear()

    // One row per signed-in user: a second row (e.g. local id "" vs server id) would make
    // getCustomization() return a stale value
    @androidx.room.Transaction
    suspend fun replace(customization: CustomizationEntity) {
        clear()
        insert(customization)
    }
}

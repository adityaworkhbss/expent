package com.aditya.expent.data.repository

import android.util.Log
import com.aditya.expent.data.local.dao.CustomizationDao
import com.aditya.expent.data.local.dao.PendingSyncDao
import com.aditya.expent.data.local.entity.PendingSyncEntity
import com.aditya.expent.data.mapper.toDto
import com.aditya.expent.data.mapper.toEntity
import com.aditya.expent.data.remote.ApiService
import com.aditya.expent.data.remote.dto.UserCustomizationResponseDto
import com.aditya.expent.domain.repository.CustomizationRepository
import com.aditya.expent.utils.AppLogger
import com.aditya.expent.utils.SessionManager
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CustomizationRepositoryImpl @Inject constructor(
    private val api: ApiService,
    private val sessionManager: SessionManager,
    private val customizationDao: CustomizationDao,
    private val pendingSyncDao: PendingSyncDao,
    private val gson: Gson
) : CustomizationRepository {

    override fun getCustomization(): Flow<UserCustomizationResponseDto> {
        return customizationDao.getCustomization().map { entity ->
            entity?.toDto() ?: sessionManager.getCustomization() ?: UserCustomizationResponseDto(
                id = "",
                userId = "",
                aiTransaction = false,
                reminder = false
            )
        }
    }

    override suspend fun updateCustomization(aiTransaction: Boolean, reminder: Boolean) {
        val current = sessionManager.getCustomization()
        val userId = current?.userId ?: ""
        val id = current?.id ?: ""

        val updated = UserCustomizationResponseDto(
            id = id,
            userId = userId,
            aiTransaction = aiTransaction,
            reminder = reminder
        )

        // Save locally first
        sessionManager.saveCustomization(updated)
        val entity = updated.toEntity()
        customizationDao.insert(entity)
        AppLogger.room("CREATE", "customization", entity, "Saved customization locally")

        // Enqueue sync queue
        enqueueSync("customization", "UPDATE", gson.toJson(updated))
    }

    override suspend fun refreshCustomization() {
        try {
            val response = api.getUserCustomization()
            val data = response.data ?: return
            sessionManager.saveCustomization(data)
            val entity = data.toEntity()
            customizationDao.insert(entity)
            AppLogger.room("REPLACE", "customization", entity, "Refreshed customization from API")
        } catch (e: Exception) {
            AppLogger.apiError("GET", "customization", e.message, throwable = e)
        }
    }

    private suspend fun enqueueSync(entityType: String, operation: String, payload: String) {
        val pendingEntity = PendingSyncEntity(
            entityType = entityType,
            entityId = "",
            operation = operation,
            payload = payload,
            createdAt = System.currentTimeMillis()
        )
        pendingSyncDao.insert(pendingEntity)
        AppLogger.room("CREATE", "pending_sync", "Type=$entityType, Op=$operation", "Enqueued")
    }
}

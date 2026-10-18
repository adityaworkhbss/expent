package com.aditya.expent.data.repository

import android.util.Log
import com.aditya.expent.data.local.dao.CustomizationDao
import com.aditya.expent.data.local.dao.PendingSyncDao
import com.aditya.expent.data.local.entity.PendingSyncEntity
import com.aditya.expent.data.mapper.toDto
import com.aditya.expent.data.mapper.toEntity
import com.aditya.expent.data.remote.ApiService
import com.aditya.expent.data.remote.dto.UserCustomizationResponseDto
import com.aditya.expent.data.sync.SyncScheduler
import com.aditya.expent.domain.repository.CustomizationRepository
import com.aditya.expent.utils.AppLogger
import com.aditya.expent.utils.SessionManager
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CustomizationRepositoryImpl @Inject constructor(
    private val api: ApiService,
    private val sessionManager: SessionManager,
    private val customizationDao: CustomizationDao,
    private val pendingSyncDao: PendingSyncDao,
    private val syncScheduler: SyncScheduler,
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
        val current = currentLocal()
        val updated = UserCustomizationResponseDto(
            id = current?.id.orEmpty(),
            userId = current?.userId?.takeIf { it.isNotBlank() } ?: sessionManager.getUser()?.id.orEmpty(),
            currency = current?.currency,
            theme = current?.theme,
            aiTransaction = aiTransaction,
            reminder = reminder
        )

        // Save locally first
        saveLocal(updated)
        AppLogger.room("REPLACE", "customization", updated, "Saved customization locally")

        enqueueSync("customization", "UPDATE", gson.toJson(updated))
        syncScheduler.enqueueCustomizationSync()
    }

    override suspend fun refreshCustomization() {
        try {
            val response = api.getUserCustomization()
            val data = response.data ?: return
            // The backend doesn't store aiTransaction / reminder yet, so keep the user's local choice
            val local = currentLocal()
            val merged = if (local != null) {
                data.copy(aiTransaction = local.aiTransaction, reminder = local.reminder)
            } else {
                data
            }
            saveLocal(merged)
            AppLogger.room("REPLACE", "customization", merged, "Refreshed customization from API")
        } catch (e: Exception) {
            AppLogger.apiError("GET", "customization", e.message, throwable = e)
        }
    }

    private suspend fun currentLocal(): UserCustomizationResponseDto? =
        customizationDao.getCustomization().first()?.toDto() ?: sessionManager.getCustomization()

    private suspend fun saveLocal(customization: UserCustomizationResponseDto) {
        sessionManager.saveCustomization(customization)
        customizationDao.replace(customization.toEntity())
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

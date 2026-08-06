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
        customizationDao.insert(updated.toEntity())

        // Enqueue sync queue
        enqueueSync("customization", "UPDATE", gson.toJson(updated))
        // Trigger sync scheduler (we can create a stub/trigger for scheduler if we want)
    }

    override suspend fun refreshCustomization() {
        try {
            Log.d("rest re", "refreshCustomization: Requesting from API")
            val response = api.getUserCustomization()
            sessionManager.saveCustomization(response)
            customizationDao.insert(response.toEntity())
            Log.d("rest re", "refreshCustomization: API Response: $response")
        } catch (e: Exception) {
            Log.e("rest re", "refreshCustomization: Error", e)
        }
    }

    private suspend fun enqueueSync(entityType: String, operation: String, payload: String) {
        pendingSyncDao.insert(
            PendingSyncEntity(
                entityType = entityType,
                entityId = "",
                operation = operation,
                payload = payload,
                createdAt = System.currentTimeMillis()
            )
        )
    }
}

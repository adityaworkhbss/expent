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
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class CustomizationRepositoryImpl @Inject constructor(
    private val api : ApiService,
    private val sessionManager: SessionManager,
    private val customizationDao: CustomizationDao,
    private val pendingSyncDao: PendingSyncDao,
    private val gson: Gson
) : CustomizationRepository {
    override suspend fun getCustomization(): Result<UserCustomizationResponseDto> {
        return try {
            val cached = sessionManager.getCustomization()
            if (cached != null) {
                Log.d("rest re", "getCustomization: Returning cached: $cached")
                return Result.success(cached)
            }
            val local = customizationDao.getCustomization().first()
            if (local != null) {
                val dto = local.toDto()
                sessionManager.saveCustomization(dto)
                Log.d("rest re", "getCustomization: Returning Room cached: $dto")
                return Result.success(dto)
            }
            Log.d("rest re", "getCustomization: Requesting from API")
            val response = api.getUserCustomization()
            sessionManager.saveCustomization(response)
            customizationDao.insert(response.toEntity())
            Log.d("rest re", "getCustomization: API Response: $response")
            Result.success(response)
        } catch (e: Exception) {
            Log.e("rest re", "getCustomization: Error", e)
            val local = customizationDao.getCustomization().first()
            if (local != null) {
                Result.success(local.toDto())
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun updateCustomization(
        aiTransaction: Boolean,
        reminder: Boolean
    ): Result<Unit> {
        return try {
            val current = sessionManager.getCustomization()
            val userId = current?.userId ?: ""
            val id = current?.id ?: ""
            val updated = UserCustomizationResponseDto(
                id = id,
                userId = userId,
                aiTransaction = aiTransaction,
                reminder = reminder
            )
            Log.d("rest re", "updateCustomization: Saving $updated to session manager")

            sessionManager.saveCustomization(updated)
            customizationDao.insert(updated.toEntity())
            val response = api.updateUserCustomization(updated)
            sessionManager.saveCustomization(response)
            customizationDao.insert(response.toEntity())
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CustomizationRepo", "updateCustomization: Error", e)
            val current = sessionManager.getCustomization()
            val updated = UserCustomizationResponseDto(
                id = current?.id ?: "",
                userId = current?.userId ?: sessionManager.getUser()?.id.orEmpty(),
                aiTransaction = aiTransaction,
                reminder = reminder
            )
            sessionManager.saveCustomization(updated)
            customizationDao.insert(updated.toEntity())
            enqueueSync("customization", "UPDATE", gson.toJson(updated))
            Result.failure(e)
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

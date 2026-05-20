package com.aditya.expent.data.repository

import android.util.Log
import com.aditya.expent.data.remote.ApiService
import com.aditya.expent.data.remote.dto.UserCustomizationResponseDto
import com.aditya.expent.domain.repository.CustomizationRepository
import com.aditya.expent.utils.SessionManager
import javax.inject.Inject

class CustomizationRepositoryImpl @Inject constructor(
    private val api : ApiService,
    private val sessionManager: SessionManager
) : CustomizationRepository {
    override suspend fun getCustomization(): Result<UserCustomizationResponseDto> {
        return try {
            val cached = sessionManager.getCustomization()
            if (cached != null) {
                Log.d("rest re", "getCustomization: Returning cached: $cached")
                return Result.success(cached)
            }
            Log.d("rest re", "getCustomization: Requesting from API")
            val response = api.getUserCustomization()
            sessionManager.saveCustomization(response)
            Log.d("rest re", "getCustomization: API Response: $response")
            Result.success(response)
        } catch (e: Exception) {
            Log.e("rest re", "getCustomization: Error", e)
            Result.failure(e)
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
            api.updateUserCustomization(updated)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CustomizationRepo", "updateCustomization: Error", e)
            Result.failure(e)
        }
    }
}
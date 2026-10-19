package com.aditya.expent.data.repository

import com.aditya.expent.data.remote.ApiService
import com.aditya.expent.data.remote.dto.RegisterDeviceTokenRequestDto
import com.aditya.expent.data.remote.dto.RemoveDeviceTokenRequestDto
import com.aditya.expent.domain.repository.DeviceTokenRepository
import com.aditya.expent.utils.AppLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceTokenRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : DeviceTokenRepository {

    override suspend fun registerDeviceToken(userId: String, token: String): Result<Unit> {
        return runCatching {
            AppLogger.debug(TAG, "Registering device token for userId=$userId")
            val response = apiService.registerDeviceToken(
                RegisterDeviceTokenRequestDto(token = token, userId = userId)
            )
            if (response.success) {
                AppLogger.debug(TAG, "Device token registered successfully: ${response.message}")
            } else {
                val errorMsg = response.error ?: response.message ?: "Failed to register device token"
                AppLogger.warn(TAG, "Backend returned error registering token: $errorMsg")
                throw Exception(errorMsg)
            }
        }.onFailure { e ->
            AppLogger.error(TAG, "Exception while registering device token", e)
        }
    }

    override suspend fun removeDeviceToken(token: String): Result<Unit> {
        return runCatching {
            AppLogger.debug(TAG, "Removing device token from backend")
            val response = apiService.removeDeviceToken(
                RemoveDeviceTokenRequestDto(token = token)
            )
            if (response.success) {
                AppLogger.debug(TAG, "Device token removed successfully")
            } else {
                val errorMsg = response.error ?: response.message ?: "Failed to remove device token"
                AppLogger.warn(TAG, "Backend returned error removing token: $errorMsg")
                throw Exception(errorMsg)
            }
        }.onFailure { e ->
            AppLogger.error(TAG, "Exception while removing device token", e)
        }
    }

    companion object {
        private const val TAG = "DeviceTokenRepo"
    }
}

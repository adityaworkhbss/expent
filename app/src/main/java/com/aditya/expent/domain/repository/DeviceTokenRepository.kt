package com.aditya.expent.domain.repository

interface DeviceTokenRepository {
    suspend fun registerDeviceToken(userId: String, token: String): Result<Unit>
    suspend fun removeDeviceToken(token: String): Result<Unit>
}

package com.aditya.expent.domain.usecase

import com.aditya.expent.domain.repository.DeviceTokenRepository
import javax.inject.Inject

class RegisterDeviceTokenUseCase @Inject constructor(
    private val repository: DeviceTokenRepository
) {
    suspend operator fun invoke(userId: String, token: String): Result<Unit> {
        return repository.registerDeviceToken(userId, token)
    }
}

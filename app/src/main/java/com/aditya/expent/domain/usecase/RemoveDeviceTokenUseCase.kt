package com.aditya.expent.domain.usecase

import com.aditya.expent.domain.repository.DeviceTokenRepository
import javax.inject.Inject

class RemoveDeviceTokenUseCase @Inject constructor(
    private val repository: DeviceTokenRepository
) {
    suspend operator fun invoke(token: String): Result<Unit> {
        return repository.removeDeviceToken(token)
    }
}

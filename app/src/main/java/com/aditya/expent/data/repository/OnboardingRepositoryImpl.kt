package com.aditya.expent.data.repository

import com.aditya.expent.data.remote.ApiService
import com.aditya.expent.data.remote.dto.OnboardingStepRequestDto
import com.aditya.expent.domain.repository.OnboardingRepository
import javax.inject.Inject

class OnboardingRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : OnboardingRepository {
    override suspend fun updateOnboardingCount(count: Int): Result<Unit> {
        return try {
            apiService.updateOnboardingCount(OnboardingStepRequestDto(count))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

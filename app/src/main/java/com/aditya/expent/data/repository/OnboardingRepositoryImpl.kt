package com.aditya.expent.data.repository

import com.aditya.expent.data.remote.ApiService
import com.aditya.expent.data.remote.dto.OnboardingStepRequestDto
import com.aditya.expent.domain.repository.OnboardingRepository
import javax.inject.Inject

class OnboardingRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : OnboardingRepository {
    override suspend fun updateOnboardingCount(count: Int) {
        try {
            apiService.updateOnboardingCount(OnboardingStepRequestDto(count))
        } catch (e: Exception) {
            // Offline fallback
        }
    }
}

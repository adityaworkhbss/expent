package com.aditya.expent.domain.usecase

import com.aditya.expent.domain.repository.OnboardingRepository
import javax.inject.Inject

class UpdateOnboardingCountUseCase @Inject constructor(
    private val repository: OnboardingRepository
) {
    suspend operator fun invoke(count: Int): Result<Unit> {
        return repository.updateOnboardingCount(count)
    }
}
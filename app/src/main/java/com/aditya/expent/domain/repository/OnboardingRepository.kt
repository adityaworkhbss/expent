package com.aditya.expent.domain.repository

import com.aditya.expent.presentation.onboard.OnboardState

interface OnboardingRepository {
    suspend fun submitOnboardingData(state: OnboardState): Result<Unit>
}

package com.aditya.expent.domain.repository

interface OnboardingRepository {
    suspend fun updateOnboardingCount(count: Int)
}

package com.aditya.expent.domain.repository

import com.aditya.expent.data.remote.dto.UserCustomizationResponseDto

interface CustomizationRepository {
    suspend fun getCustomization(): Result<UserCustomizationResponseDto>
    suspend fun updateCustomization(aiTransaction: Boolean, reminder: Boolean): Result<Unit>
}
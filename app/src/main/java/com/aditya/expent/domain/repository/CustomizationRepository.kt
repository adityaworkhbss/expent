package com.aditya.expent.domain.repository

import com.aditya.expent.data.remote.dto.UserCustomizationResponseDto
import kotlinx.coroutines.flow.Flow

interface CustomizationRepository {
    fun getCustomization(): Flow<UserCustomizationResponseDto>
    suspend fun updateCustomization(aiTransaction: Boolean, reminder: Boolean)
    suspend fun refreshCustomization()
}
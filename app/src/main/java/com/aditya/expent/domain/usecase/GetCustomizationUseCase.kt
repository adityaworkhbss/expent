package com.aditya.expent.domain.usecase

import com.aditya.expent.data.remote.dto.UserCustomizationResponseDto
import com.aditya.expent.domain.repository.CustomizationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCustomizationUseCase @Inject constructor(
    private val customizationRepository: CustomizationRepository
) {
    operator fun invoke(): Flow<UserCustomizationResponseDto> =
        customizationRepository.getCustomization()
}

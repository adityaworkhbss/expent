package com.aditya.expent.domain.usecase

import com.aditya.expent.domain.repository.CustomizationRepository
import javax.inject.Inject

class GetCustomizationUseCase @Inject constructor(
    private val customizationRepository: CustomizationRepository
) {
    suspend operator fun invoke() = customizationRepository.getCustomization()
}
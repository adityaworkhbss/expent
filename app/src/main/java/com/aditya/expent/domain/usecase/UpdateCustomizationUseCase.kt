package com.aditya.expent.domain.usecase

import com.aditya.expent.domain.repository.CustomizationRepository
import javax.inject.Inject

class UpdateCustomizationUseCase @Inject constructor(
    private val customizationRepository: CustomizationRepository
) {
    suspend operator fun invoke(aiTransaction: Boolean, reminder: Boolean) =
        customizationRepository.updateCustomization(aiTransaction, reminder)
}

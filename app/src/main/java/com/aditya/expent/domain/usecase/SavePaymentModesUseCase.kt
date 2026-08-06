package com.aditya.expent.domain.usecase

import com.aditya.expent.domain.model.OnboardPaymentMode
import com.aditya.expent.domain.repository.PaymentModeRepository
import javax.inject.Inject

class SavePaymentModesUseCase @Inject constructor(
    private val repository: PaymentModeRepository
) {
    suspend operator fun invoke(paymentModes: List<OnboardPaymentMode>) =
        repository.savePaymentModes(paymentModes)
}

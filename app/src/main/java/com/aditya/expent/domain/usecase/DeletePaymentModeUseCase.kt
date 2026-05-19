package com.aditya.expent.domain.usecase

import com.aditya.expent.domain.repository.PaymentModeRepository
import javax.inject.Inject

class DeletePaymentModeUseCase @Inject constructor(
    private val repository: PaymentModeRepository
) {
    suspend operator fun invoke(id: String) = repository.deleteAccounts(id)

}
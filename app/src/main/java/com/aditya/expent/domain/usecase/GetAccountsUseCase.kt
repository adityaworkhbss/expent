package com.aditya.expent.domain.usecase

import com.aditya.expent.domain.repository.PaymentModeRepository
import javax.inject.Inject

class GetAccountsUseCase @Inject constructor(
    private val repository: PaymentModeRepository
) {
    suspend operator fun invoke() = repository.getAccounts()
}

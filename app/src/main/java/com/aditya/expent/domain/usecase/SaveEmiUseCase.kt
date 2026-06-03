package com.aditya.expent.domain.usecase

import com.aditya.expent.domain.repository.ExpenseAndSubscriptionRepository
import javax.inject.Inject

class SaveEmiUseCase @Inject constructor(
    private val repository: ExpenseAndSubscriptionRepository
) {
    suspend operator fun invoke(
        type: String,
        name: String,
        amount: String,
        startDate: String,
        tenure: String? = null,
        monthsPaid: String? = null
    ): Result<Unit> {
        return repository.saveEmi(type, name, amount, startDate, tenure, monthsPaid)
    }
}

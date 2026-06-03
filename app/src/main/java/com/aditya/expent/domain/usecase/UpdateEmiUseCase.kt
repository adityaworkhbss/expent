package com.aditya.expent.domain.usecase

import com.aditya.expent.domain.repository.ExpenseAndSubscriptionRepository
import javax.inject.Inject

class UpdateEmiUseCase @Inject constructor(
    private val repository: ExpenseAndSubscriptionRepository
) {
    suspend operator fun invoke(
        id: String,
        type: String,
        name: String,
        amount: String,
        startDate: String,
        tenure: String? = null,
        monthsPaid: String? = null
    ): Result<Unit> {
        return repository.updateEmi(id, type, name, amount, startDate, tenure, monthsPaid)
    }
}

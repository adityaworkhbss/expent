package com.aditya.expent.domain.usecase

import com.aditya.expent.domain.repository.ExpenseAndSubscriptionRepository
import javax.inject.Inject

class UpdateEmiUseCase @Inject constructor(
    private val repository: ExpenseAndSubscriptionRepository
) {
    suspend operator fun invoke(
        id: String,
        name: String,
        amount: Double,
        date: Int,
        tenure: Int?,
        paid: Int
    ) = repository.updateEmi(id, name, amount, date, tenure, paid)
}

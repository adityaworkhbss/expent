package com.aditya.expent.domain.usecase

import com.aditya.expent.domain.repository.ExpenseAndSubscriptionRepository
import javax.inject.Inject

class SaveEmiUseCase @Inject constructor(
    private val repository: ExpenseAndSubscriptionRepository
) {
    suspend operator fun invoke(
        name: String,
        amount: Double,
        date: Int,
        tenure: Int?,
        paid: Int
    ) = repository.saveEmi(name, amount, date, tenure, paid)
}

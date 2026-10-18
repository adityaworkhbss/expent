package com.aditya.expent.domain.usecase

import com.aditya.expent.domain.repository.IncomeBudgetRepository
import javax.inject.Inject

class SaveBudgetUseCase @Inject constructor(
    private val repository: IncomeBudgetRepository
) {
    suspend operator fun invoke(
        categoryId: String?,
        amount: Double,
        date: Int
    ) = repository.saveBudget(categoryId, amount, date)
}

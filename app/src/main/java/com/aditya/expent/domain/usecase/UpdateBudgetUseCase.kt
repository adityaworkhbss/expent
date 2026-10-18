package com.aditya.expent.domain.usecase

import com.aditya.expent.domain.repository.IncomeBudgetRepository
import javax.inject.Inject

class UpdateBudgetUseCase @Inject constructor(
    private val repository: IncomeBudgetRepository
) {
    suspend operator fun invoke(
        id: String,
        categoryId: String?,
        amount: Double,
        date: Int
    ) = repository.updateBudget(id, categoryId, amount, date)
}

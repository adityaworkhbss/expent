package com.aditya.expent.domain.usecase

import com.aditya.expent.domain.repository.IncomeBudgetRepository
import javax.inject.Inject

class SaveBudgetUseCase @Inject constructor(
    private val repository: IncomeBudgetRepository
) {
    suspend operator fun invoke(
        categoryId: String?,
        periodType: String,
        amount: Double,
        startDate: String,
        endDate: String?
    ): Result<Unit> {
        return repository.saveBudget(categoryId, periodType, amount, startDate, endDate)
    }
}

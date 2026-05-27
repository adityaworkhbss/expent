package com.aditya.expent.domain.usecase

import com.aditya.expent.domain.repository.IncomeBudgetRepository
import javax.inject.Inject

class DeleteBudgetUseCase @Inject constructor(
    private val repository: IncomeBudgetRepository
) {
    suspend operator fun invoke(id: String): Result<Unit> {
        return repository.deleteBudget(id)
    }
}

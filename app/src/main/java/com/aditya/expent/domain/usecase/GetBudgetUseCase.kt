package com.aditya.expent.domain.usecase

import com.aditya.expent.data.remote.dto.BudgetResponseDto
import com.aditya.expent.domain.repository.IncomeBudgetRepository
import javax.inject.Inject

class GetBudgetUseCase @Inject constructor(
    private val repository: IncomeBudgetRepository
) {
    suspend operator fun invoke(): Result<List<BudgetResponseDto>> {
        return repository.getBudgets()
    }
}
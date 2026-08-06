package com.aditya.expent.domain.usecase

import com.aditya.expent.data.remote.dto.BudgetResponseDto
import com.aditya.expent.domain.repository.IncomeBudgetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBudgetUseCase @Inject constructor(
    private val repository: IncomeBudgetRepository
) {
    operator fun invoke(): Flow<List<BudgetResponseDto>> = repository.getBudgets()
}

package com.aditya.expent.domain.usecase

import com.aditya.expent.data.remote.dto.ExpenseIncomeResponseDto
import com.aditya.expent.domain.repository.ExpenseAndSubscriptionRepository
import javax.inject.Inject

class GetExpensesAndSubscriptionUseCase @Inject constructor(
    val repository: ExpenseAndSubscriptionRepository
) {

    suspend operator fun invoke() : Result<List<ExpenseIncomeResponseDto>>{
        return repository.getExpensesAndSubscription()
    }
}
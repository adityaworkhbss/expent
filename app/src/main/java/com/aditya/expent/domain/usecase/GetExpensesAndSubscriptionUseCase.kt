package com.aditya.expent.domain.usecase

import com.aditya.expent.domain.model.Expense
import com.aditya.expent.domain.repository.ExpenseAndSubscriptionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetExpensesAndSubscriptionUseCase @Inject constructor(
    val repository: ExpenseAndSubscriptionRepository
) {
    operator fun invoke(): Flow<List<Expense>> = repository.getExpensesAndSubscription()
}

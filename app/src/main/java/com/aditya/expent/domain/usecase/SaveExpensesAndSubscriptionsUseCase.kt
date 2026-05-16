package com.aditya.expent.domain.usecase

import com.aditya.expent.domain.repository.ExpenseAndSubscriptionRepository
import com.aditya.expent.presentation.onboard.RecurringExpense
import com.aditya.expent.presentation.onboard.Subscription
import javax.inject.Inject

class SaveExpensesAndSubscriptionsUseCase @Inject constructor(
    val repository: ExpenseAndSubscriptionRepository
) {
    suspend operator fun invoke(
        expenses: List<RecurringExpense>,
        subscriptions: List<Subscription>
    ): Result<Unit> {
        return repository.saveExpensesAndSubscriptions(expenses, subscriptions)
    }
}
package com.aditya.expent.domain.repository

import com.aditya.expent.domain.model.Expense
import com.aditya.expent.presentation.onboard.RecurringExpense
import com.aditya.expent.presentation.onboard.Subscription
import kotlinx.coroutines.flow.Flow

interface ExpenseAndSubscriptionRepository {

    fun getExpensesAndSubscription(): Flow<List<Expense>>

    suspend fun saveExpensesAndSubscriptions(
        expenses: List<RecurringExpense>,
        subscriptions: List<Subscription>
    )

    suspend fun deleteEmi(id: String)

    /** [date] = day of the month (1-31). [tenure] null = every month (subscription). */
    suspend fun updateEmi(
        id: String,
        name: String,
        amount: Double,
        date: Int,
        tenure: Int?,
        paid: Int
    )

    /** [date] = day of the month (1-31). [tenure] null = every month (subscription). */
    suspend fun saveEmi(
        name: String,
        amount: Double,
        date: Int,
        tenure: Int?,
        paid: Int
    )

    suspend fun refreshExpensesAndSubscriptions()
}

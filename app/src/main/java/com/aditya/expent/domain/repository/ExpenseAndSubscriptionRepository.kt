package com.aditya.expent.domain.repository

import com.aditya.expent.data.remote.dto.ExpenseIncomeResponseDto
import com.aditya.expent.presentation.onboard.RecurringExpense
import com.aditya.expent.presentation.onboard.Subscription
import kotlinx.coroutines.flow.Flow

interface ExpenseAndSubscriptionRepository {
    fun getExpensesAndSubscription(): Flow<List<ExpenseIncomeResponseDto>>

    suspend fun saveExpensesAndSubscriptions(
        expenses: List<RecurringExpense>,
        subscriptions: List<Subscription>
    )

    suspend fun deleteEmi(id: String)

    suspend fun updateEmi(
        id: String,
        type: String,
        name: String,
        amount: String,
        startDate: String,
        tenure: String? = null,
        monthsPaid: String? = null
    )

    suspend fun saveEmi(
        type: String,
        name: String,
        amount: String,
        startDate: String,
        tenure: String? = null,
        monthsPaid: String? = null
    )

    suspend fun refreshExpensesAndSubscriptions()
}
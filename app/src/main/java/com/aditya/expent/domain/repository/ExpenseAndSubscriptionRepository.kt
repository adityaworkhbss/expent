package com.aditya.expent.domain.repository

import com.aditya.expent.data.remote.dto.ExpenseIncomeResponseDto
import com.aditya.expent.presentation.onboard.RecurringExpense
import com.aditya.expent.presentation.onboard.Subscription

interface ExpenseAndSubscriptionRepository {
    suspend fun saveExpensesAndSubscriptions(
        expenses: List<RecurringExpense>,
        subscriptions: List<Subscription>
    ): Result<Unit>

    suspend fun getExpensesAndSubscription() : Result<List<ExpenseIncomeResponseDto>>

    suspend fun deleteEmi(id: String): Result<Unit>

    suspend fun updateEmi(
        id: String,
        type: String,
        name: String,
        amount: String,
        startDate: String,
        tenure: String? = null,
        monthsPaid: String? = null
    ): Result<Unit>

    suspend fun saveEmi(
        type: String,
        name: String,
        amount: String,
        startDate: String,
        tenure: String? = null,
        monthsPaid: String? = null
    ): Result<Unit>
}
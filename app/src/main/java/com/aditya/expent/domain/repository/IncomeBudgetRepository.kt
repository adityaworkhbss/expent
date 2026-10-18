package com.aditya.expent.domain.repository

import com.aditya.expent.domain.model.Budget
import com.aditya.expent.presentation.onboard.RecurringIncome
import kotlinx.coroutines.flow.Flow

interface IncomeBudgetRepository {

    fun getBudgets(): Flow<List<Budget>>

    suspend fun saveIncomeBudget(
        salary: RecurringIncome,
        additionalIncome: List<RecurringIncome>
    )

    /** [date] is the day of the month (1-31) the income arrives. */
    suspend fun saveBudget(categoryId: String?, amount: Double, date: Int)

    suspend fun deleteBudget(id: String)

    suspend fun updateBudget(id: String, categoryId: String?, amount: Double, date: Int)

    suspend fun refreshBudgets()
}

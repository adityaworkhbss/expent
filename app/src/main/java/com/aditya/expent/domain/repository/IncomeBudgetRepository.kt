package com.aditya.expent.domain.repository

import com.aditya.expent.data.remote.dto.BudgetRequestDto
import com.aditya.expent.data.remote.dto.BudgetResponseDto
import com.aditya.expent.presentation.onboard.RecurringIncome
import kotlinx.coroutines.flow.Flow

interface IncomeBudgetRepository {
    fun getBudgets(): Flow<List<BudgetResponseDto>>

    suspend fun saveIncomeBudget(
        salary: RecurringIncome,
        additionalIncome: List<RecurringIncome>
    )

    suspend fun saveBudget(
        categoryId: String?,
        periodType: String,
        amount: Double,
        startDate: String,
        endDate: String?
    )

    suspend fun deleteBudget(id: String)

    suspend fun updateBudget(
        id: String,
        categoryId: String?,
        periodType: String,
        amount: Double,
        startDate: String,
        endDate: String?
    )

    suspend fun refreshBudgets()
}
package com.aditya.expent.domain.repository

import com.aditya.expent.presentation.onboard.RecurringIncome

interface IncomeBudgetRepository {
    suspend fun saveIncomeBudget(
        salary : RecurringIncome,
        additionalIncome : List<RecurringIncome>
    ): Result<Unit>

    suspend fun saveBudget(
        categoryId: String?,
        periodType: String,
        amount: Double,
        startDate: String,
        endDate: String?
    ): Result<Unit>
}
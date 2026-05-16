package com.aditya.expent.data.repository

import com.aditya.expent.data.remote.ApiService
import com.aditya.expent.data.remote.dto.BudgetRequestDto
import com.aditya.expent.domain.repository.IncomeBudgetRepository
import com.aditya.expent.presentation.onboard.RecurringIncome
import javax.inject.Inject

class IncomeBudgetRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : IncomeBudgetRepository {

    override suspend fun saveIncomeBudget(
        salary : RecurringIncome,
        additionalIncome : List<RecurringIncome>
    ): Result<Unit> {
        return try {
            val categories = apiService.getCategories()
            val allIncomes = listOf(salary) + additionalIncome

            val requests = allIncomes.map { income ->
                // Try to find category ID by name if it's not already a UUID
                val categoryId = categories.find { it.name == income.categoryId }?.id ?: income.categoryId
                
                BudgetRequestDto(
                    categoryId = categoryId,
                    periodType = income.periodType,
                    limitAmount = income.amount.toDoubleOrNull() ?: 0.0,
                    startDate = income.startDate.ifBlank { 
                        java.time.OffsetDateTime.now().toString()
                    },
                    endDate = if (income.endDate.isBlank()) null else income.endDate
                )
            }

            apiService.saveBudgets(requests)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveBudget(
        categoryId: String?,
        periodType: String,
        amount: Double,
        startDate: String,
        endDate: String?
    ): Result<Unit> {
        return try {
            val categories = apiService.getCategories()
            val resolvedCategoryId = categories.find { it.name == categoryId }?.id ?: categoryId

            val request = BudgetRequestDto(
                categoryId = resolvedCategoryId,
                periodType = periodType,
                limitAmount = amount,
                startDate = startDate.ifBlank { java.time.OffsetDateTime.now().toString() },
                endDate = if (endDate.isNullOrBlank()) null else endDate
            )

            apiService.saveBudgets(listOf(request))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
package com.aditya.expent.data.repository

import android.util.Log
import com.aditya.expent.data.local.dao.BudgetDao
import com.aditya.expent.data.local.dao.PendingSyncDao
import com.aditya.expent.data.local.entity.PendingSyncEntity
import com.aditya.expent.data.mapper.toDto
import com.aditya.expent.data.mapper.toEntity
import com.aditya.expent.data.remote.ApiService
import com.aditya.expent.data.remote.dto.BudgetRequestDto
import com.aditya.expent.data.remote.dto.BudgetResponseDto
import com.aditya.expent.domain.repository.IncomeBudgetRepository
import com.aditya.expent.presentation.onboard.RecurringIncome
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class IncomeBudgetRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val budgetDao: BudgetDao,
    private val pendingSyncDao: PendingSyncDao,
    private val gson: Gson
) : IncomeBudgetRepository {

    override suspend fun saveIncomeBudget(
        salary: RecurringIncome,
        additionalIncome: List<RecurringIncome>
    ): Result<Unit> {
        return try {
            val categories = apiService.getCategories()
            val allIncomes = listOf(salary) + additionalIncome

            val requests = allIncomes.map { income ->
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
            refreshBudgetsCache()
            Result.success(Unit)
        } catch (e: Exception) {
            enqueueSync("budget", "CREATE", gson.toJson(mapOf("salary" to salary, "additional" to additionalIncome)))
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
            refreshBudgetsCache()
            Result.success(Unit)
        } catch (e: Exception) {
            enqueueSync(
                "budget",
                "CREATE",
                gson.toJson(
                    mapOf(
                        "categoryId" to categoryId,
                        "periodType" to periodType,
                        "amount" to amount,
                        "startDate" to startDate,
                        "endDate" to endDate
                    )
                )
            )
            Result.failure(e)
        }
    }

    override suspend fun getBudgets(): Result<List<BudgetResponseDto>> {
        return try {
            Log.d("rest re", "Get Budget Called")
            val response = apiService.getBudgets()
            Log.d("rest re", "Get Budget response : $response")
            budgetDao.insert(response.map { it.toEntity() })
            Result.success(response)
        } catch (e: Exception) {
            Log.d("rest re", "Error Get Budget Response : $e")
            val cached = budgetDao.getBudgets().first()
            if (cached.isNotEmpty()) {
                Result.success(cached.map { it.toDto() })
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun deleteBudget(id: String): Result<Unit> {
        return try {
            apiService.deleteBudget(id)
            budgetDao.getBudget(id)?.let { budgetDao.delete(it) }
            Result.success(Unit)
        } catch (e: Exception) {
            enqueueSync("budget", "DELETE", id)
            Result.failure(e)
        }
    }

    override suspend fun updateBudget(
        id: String,
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
                startDate = startDate,
                endDate = if (endDate.isNullOrBlank()) null else endDate
            )
            apiService.updateBudget(id, request)
            refreshBudgetsCache()
            Result.success(Unit)
        } catch (e: Exception) {
            enqueueSync("budget", "UPDATE", gson.toJson(mapOf("id" to id, "request" to categoryId)))
            Result.failure(e)
        }
    }

    private suspend fun refreshBudgetsCache() {
        runCatching {
            val response = apiService.getBudgets()
            budgetDao.insert(response.map { it.toEntity() })
        }
    }

    private suspend fun enqueueSync(entityType: String, operation: String, payload: String) {
        pendingSyncDao.insert(
            PendingSyncEntity(
                entityType = entityType,
                entityId = "",
                operation = operation,
                payload = payload,
                createdAt = System.currentTimeMillis()
            )
        )
    }
}

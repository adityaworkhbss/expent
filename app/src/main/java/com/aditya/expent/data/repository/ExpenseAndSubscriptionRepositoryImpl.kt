package com.aditya.expent.data.repository

import android.util.Log
import com.aditya.expent.data.local.dao.ExpenseDao
import com.aditya.expent.data.local.dao.PendingSyncDao
import com.aditya.expent.data.local.entity.ExpenseEntity
import com.aditya.expent.data.local.entity.PendingSyncEntity
import com.aditya.expent.data.mapper.toDto
import com.aditya.expent.data.mapper.toEntity
import com.aditya.expent.data.remote.ApiService
import com.aditya.expent.data.remote.dto.ExpenseIncomeRequestDto
import com.aditya.expent.data.remote.dto.ExpenseIncomeResponseDto
import com.aditya.expent.domain.repository.ExpenseAndSubscriptionRepository
import com.aditya.expent.presentation.onboard.RecurringExpense
import com.aditya.expent.presentation.onboard.Subscription
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

class ExpenseAndSubscriptionRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val expenseDao: ExpenseDao,
    private val pendingSyncDao: PendingSyncDao,
    private val gson: Gson
) : ExpenseAndSubscriptionRepository {

    override suspend fun saveExpensesAndSubscriptions(
        expenses: List<RecurringExpense>,
        subscriptions: List<Subscription>
    ): Result<Unit> {
        return try {
            val requests = expenses.map { expense ->
                ExpenseIncomeRequestDto(
                    type = "expense",
                    name = expense.name,
                    amount = expense.amount,
                    startDate = expense.startDate,
                    tenure = expense.totalMonths,
                    monthsPaid = expense.monthsPaid
                )
            } + subscriptions.map { subscription ->
                ExpenseIncomeRequestDto(
                    type = "subscription",
                    name = subscription.name,
                    amount = subscription.amount,
                    startDate = subscription.billingDate
                )
            }
            val response = apiService.saveExpensesAndSubscriptions(requests)
            expenseDao.insert(response.map { it.toEntity() })
            Result.success(Unit)
        } catch (e: Exception) {
            val requests = expenses.map { expense ->
                ExpenseIncomeRequestDto(
                    type = "expense",
                    name = expense.name,
                    amount = expense.amount,
                    startDate = expense.startDate,
                    tenure = expense.totalMonths,
                    monthsPaid = expense.monthsPaid
                )
            } + subscriptions.map { subscription ->
                ExpenseIncomeRequestDto(
                    type = "subscription",
                    name = subscription.name,
                    amount = subscription.amount,
                    startDate = subscription.billingDate
                )
            }
            enqueueSync("emi", "CREATE", gson.toJson(requests))
            Result.failure(e)
        }
    }

    override suspend fun getExpensesAndSubscription(): Result<List<ExpenseIncomeResponseDto>> {
        return try {
            Log.d("rest re", "Get ExpensesAndSubscription Req ")
            val result = apiService.getExpensesAndSubscriptions()
            Log.d("rest re", "Get ExpensesAndSubscription Res :: $result")
            expenseDao.insert(result.map { it.toEntity() })
            Result.success(result)
        } catch (e: Exception) {
            val cached = expenseDao.getExpenses().first()
            if (cached.isNotEmpty()) {
                Result.success(cached.map { it.toDto() })
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun deleteEmi(id: String): Result<Unit> {
        return try {
            apiService.deleteEmi(id)
            expenseDao.getExpense(id)?.let { expenseDao.delete(it) }
            Result.success(Unit)
        } catch (e: Exception) {
            enqueueSync("emi", "DELETE", id)
            Result.failure(e)
        }
    }

    override suspend fun updateEmi(
        id: String,
        type: String,
        name: String,
        amount: String,
        startDate: String,
        tenure: String?,
        monthsPaid: String?
    ): Result<Unit> {
        return try {
            val request = ExpenseIncomeRequestDto(
                type = type,
                name = name,
                amount = amount,
                startDate = startDate,
                tenure = tenure,
                monthsPaid = monthsPaid
            )
            val response = apiService.updateEmi(id, request)
            expenseDao.insert(response.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            val request = ExpenseIncomeRequestDto(
                type = type,
                name = name,
                amount = amount,
                startDate = startDate,
                tenure = tenure,
                monthsPaid = monthsPaid
            )
            expenseDao.getExpense(id)?.let { existing ->
                expenseDao.insert(existing.copy(
                    name = name,
                    principal = amount,
                    monthlyEmi = amount,
                    startDate = startDate,
                    tenure = tenure?.toIntOrNull() ?: existing.tenure,
                    monthsPaid = monthsPaid?.toIntOrNull() ?: existing.monthsPaid,
                    updatedAt = nowIso()
                ))
            }
            enqueueSync("emi", "UPDATE", gson.toJson(mapOf("id" to id, "request" to request)))
            Result.failure(e)
        }
    }

    override suspend fun saveEmi(
        type: String,
        name: String,
        amount: String,
        startDate: String,
        tenure: String?,
        monthsPaid: String?
    ): Result<Unit> {
        return try {
            val request = ExpenseIncomeRequestDto(
                type = type,
                name = name,
                amount = amount,
                startDate = startDate,
                tenure = tenure,
                monthsPaid = monthsPaid
            )
            val response = apiService.saveExpensesAndSubscriptions(listOf(request))
            expenseDao.insert(response.map { it.toEntity() })
            Result.success(Unit)
        } catch (e: Exception) {
            val request = ExpenseIncomeRequestDto(
                type = type,
                name = name,
                amount = amount,
                startDate = startDate,
                tenure = tenure,
                monthsPaid = monthsPaid
            )
            expenseDao.insert(request.toPendingEntity())
            enqueueSync("emi", "CREATE", gson.toJson(listOf(request)))
            Result.failure(e)
        }
    }

    private fun ExpenseIncomeRequestDto.toPendingEntity(): ExpenseEntity {
        val now = nowIso()
        return ExpenseEntity(
            id = "local-${UUID.randomUUID()}",
            userId = "",
            accountId = null,
            transactionId = null,
            name = name,
            principal = amount,
            tenure = tenure?.toIntOrNull() ?: 0,
            monthlyEmi = amount,
            startDate = startDate,
            endDate = null,
            nextDueDate = startDate,
            remainingBalance = amount,
            monthsPaid = monthsPaid?.toIntOrNull() ?: 0,
            active = true,
            createdAt = now,
            updatedAt = now
        )
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

    private fun nowIso(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date())
}

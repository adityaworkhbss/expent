package com.aditya.expent.data.repository

import android.util.Log
import com.aditya.expent.data.remote.ApiService
import com.aditya.expent.data.remote.dto.ExpenseIncomeRequestDto
import com.aditya.expent.data.remote.dto.ExpenseIncomeResponseDto
import com.aditya.expent.domain.repository.ExpenseAndSubscriptionRepository
import com.aditya.expent.presentation.onboard.RecurringExpense
import com.aditya.expent.presentation.onboard.Subscription
import javax.inject.Inject

class ExpenseAndSubscriptionRepositoryImpl @Inject constructor(
    private val apiService: ApiService
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
            apiService.saveExpensesAndSubscriptions(requests)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getExpensesAndSubscription(): Result<List<ExpenseIncomeResponseDto>> {
        return try {
            Log.d("rest re", "Get ExpensesAndSubscription Req ")
            val result = apiService.getExpensesAndSubscriptions()
            Log.d("rest re", "Get ExpensesAndSubscription Res :: $result")
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteEmi(id: String): Result<Unit> {
        return try {
            apiService.deleteEmi(id)
            Result.success(Unit)
        } catch (e: Exception) {
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
            apiService.updateEmi(id, request)
            Result.success(Unit)
        } catch (e: Exception) {
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
            apiService.saveExpensesAndSubscriptions(listOf(request))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
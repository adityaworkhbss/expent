package com.aditya.expent.data.repository

import android.util.Log
import com.aditya.expent.data.local.dao.ExpenseDao
import com.aditya.expent.data.local.dao.PendingSyncDao
import com.aditya.expent.data.local.entity.ExpenseEntity
import com.aditya.expent.data.local.entity.PendingSyncEntity
import com.aditya.expent.data.local.entity.SyncStatus
import com.aditya.expent.data.mapper.toDto
import com.aditya.expent.data.mapper.toEntity
import com.aditya.expent.data.remote.ApiService
import com.aditya.expent.data.remote.dto.ExpenseIncomeRequestDto
import com.aditya.expent.data.remote.dto.ExpenseIncomeResponseDto
import com.aditya.expent.data.sync.SyncScheduler
import com.aditya.expent.domain.repository.ExpenseAndSubscriptionRepository
import com.aditya.expent.presentation.onboard.RecurringExpense
import com.aditya.expent.presentation.onboard.Subscription
import com.aditya.expent.utils.SessionManager
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

class ExpenseAndSubscriptionRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val expenseDao: ExpenseDao,
    private val pendingSyncDao: PendingSyncDao,
    private val syncScheduler: SyncScheduler,
    private val sessionManager: SessionManager,
    private val gson: Gson
) : ExpenseAndSubscriptionRepository {

    override fun getExpensesAndSubscription(): Flow<List<ExpenseIncomeResponseDto>> {
        return expenseDao.getExpenses().map { entities ->
            entities.map { it.toDto() }
        }
    }

    override suspend fun saveExpensesAndSubscriptions(
        expenses: List<RecurringExpense>,
        subscriptions: List<Subscription>
    ) {
        val userId = sessionManager.getUser()?.id.orEmpty()
        val now = nowIso()

        val entities = expenses.map { expense ->
            ExpenseEntity(
                id = "local-${UUID.randomUUID()}",
                userId = userId,
                accountId = null,
                transactionId = null,
                name = expense.name,
                principal = expense.amount,
                tenure = expense.totalMonths.toIntOrNull() ?: 0,
                monthlyEmi = expense.amount,
                startDate = expense.startDate,
                endDate = null,
                nextDueDate = expense.startDate,
                remainingBalance = expense.amount,
                monthsPaid = expense.monthsPaid.toIntOrNull() ?: 0,
                active = true,
                createdAt = now,
                updatedAt = now,
                accountName = null,
                syncStatus = SyncStatus.PENDING_CREATE,
                isDeleted = false
            )
        } + subscriptions.map { subscription ->
            ExpenseEntity(
                id = "local-${UUID.randomUUID()}",
                userId = userId,
                accountId = null,
                transactionId = null,
                name = subscription.name,
                principal = subscription.amount,
                tenure = 0,
                monthlyEmi = subscription.amount,
                startDate = subscription.billingDate,
                endDate = null,
                nextDueDate = subscription.billingDate,
                remainingBalance = subscription.amount,
                monthsPaid = 0,
                active = true,
                createdAt = now,
                updatedAt = now,
                accountName = null,
                syncStatus = SyncStatus.PENDING_CREATE,
                isDeleted = false
            )
        }

        expenseDao.insert(entities)

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
        syncScheduler.enqueueExpenseSync()
    }

    override suspend fun deleteEmi(id: String) {
        val expense = expenseDao.getExpense(id) ?: return
        expenseDao.update(
            expense.copy(
                isDeleted = true,
                syncStatus = SyncStatus.PENDING_DELETE
            )
        )
        
        enqueueSync("emi", "DELETE", id)
        syncScheduler.enqueueExpenseSync()
    }

    override suspend fun updateEmi(
        id: String,
        type: String,
        name: String,
        amount: String,
        startDate: String,
        tenure: String?,
        monthsPaid: String?
    ) {
        val expense = expenseDao.getExpense(id) ?: return
        expenseDao.update(
            expense.copy(
                name = name,
                principal = amount,
                monthlyEmi = amount,
                startDate = startDate,
                tenure = tenure?.toIntOrNull() ?: expense.tenure,
                monthsPaid = monthsPaid?.toIntOrNull() ?: expense.monthsPaid,
                updatedAt = nowIso(),
                syncStatus = SyncStatus.PENDING_UPDATE
            )
        )

        val request = ExpenseIncomeRequestDto(
            type = type,
            name = name,
            amount = amount,
            startDate = startDate,
            tenure = tenure,
            monthsPaid = monthsPaid
        )
        enqueueSync("emi", "UPDATE", gson.toJson(mapOf("id" to id, "request" to request)))
        syncScheduler.enqueueExpenseSync()
    }

    override suspend fun saveEmi(
        type: String,
        name: String,
        amount: String,
        startDate: String,
        tenure: String?,
        monthsPaid: String?
    ) {
        val userId = sessionManager.getUser()?.id.orEmpty()
        val now = nowIso()

        val entity = ExpenseEntity(
            id = "local-${UUID.randomUUID()}",
            userId = userId,
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
            updatedAt = now,
            accountName = null,
            syncStatus = SyncStatus.PENDING_CREATE,
            isDeleted = false
        )
        expenseDao.insert(entity)

        val request = ExpenseIncomeRequestDto(
            type = type,
            name = name,
            amount = amount,
            startDate = startDate,
            tenure = tenure,
            monthsPaid = monthsPaid
        )
        enqueueSync("emi", "CREATE", gson.toJson(listOf(request)))
        syncScheduler.enqueueExpenseSync()
    }

    override suspend fun refreshExpensesAndSubscriptions() {
        try {
            Log.d("rest re", "refreshExpensesAndSubscriptions Req ")
            val result = apiService.getExpensesAndSubscriptions()
            Log.d("rest re", "refreshExpensesAndSubscriptions Res :: $result")
            expenseDao.replaceAll(result.map { it.toEntity() })
        } catch (e: Exception) {
            Log.e("rest re", "Error refreshExpensesAndSubscriptions: ${e.message}", e)
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

    private fun nowIso(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date())
}

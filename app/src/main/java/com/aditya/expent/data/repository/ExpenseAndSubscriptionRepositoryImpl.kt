package com.aditya.expent.data.repository

import com.aditya.expent.data.local.dao.ExpenseDao
import com.aditya.expent.data.local.dao.PendingSyncDao
import com.aditya.expent.data.local.entity.ExpenseEntity
import com.aditya.expent.data.local.entity.PendingSyncEntity
import com.aditya.expent.data.local.entity.SyncStatus
import com.aditya.expent.data.mapper.toDomain
import com.aditya.expent.data.mapper.toEntity
import com.aditya.expent.data.mapper.toRequest
import com.aditya.expent.data.remote.ApiService
import com.aditya.expent.data.sync.SyncScheduler
import com.aditya.expent.domain.model.Expense
import com.aditya.expent.domain.repository.ExpenseAndSubscriptionRepository
import com.aditya.expent.presentation.onboard.RecurringExpense
import com.aditya.expent.presentation.onboard.Subscription
import com.aditya.expent.utils.AppLogger
import com.aditya.expent.utils.DayOfMonth
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

    override fun getExpensesAndSubscription(): Flow<List<Expense>> {
        return expenseDao.getExpenses().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveExpensesAndSubscriptions(
        expenses: List<RecurringExpense>,
        subscriptions: List<Subscription>
    ) {
        val entities = expenses.map { expense ->
            newEntity(
                name = expense.name,
                amount = expense.amount.toDoubleOrNull() ?: 0.0,
                date = expense.date ?: DayOfMonth.today(),
                tenure = expense.totalMonths.toIntOrNull()?.takeIf { it > 0 },
                paid = expense.monthsPaid.toIntOrNull() ?: 0
            )
        } + subscriptions.map { subscription ->
            newEntity(
                name = subscription.name,
                amount = subscription.amount.toDoubleOrNull() ?: 0.0,
                date = subscription.date ?: DayOfMonth.today(),
                tenure = null,
                paid = 0
            )
        }
        if (entities.isEmpty()) return

        expenseDao.insert(entities)
        AppLogger.room("CREATE", "expenses", entities, "Inserted ${entities.size} expenses & subscriptions")

        val requests = entities.map { it.toRequest() }
        val localIds = entities.joinToString(",") { it.id }
        enqueueSync("emi", "CREATE", gson.toJson(requests), localIds)
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
        AppLogger.room("UPDATE", "expenses", "ID=$id, isDeleted=true", "Marked deleted")

        enqueueSync("emi", "DELETE", id, id)
        syncScheduler.enqueueExpenseSync()
    }

    override suspend fun updateEmi(
        id: String,
        name: String,
        amount: Double,
        date: Int,
        tenure: Int?,
        paid: Int
    ) {
        val expense = expenseDao.getExpense(id) ?: return
        val updated = expense.copy(
            name = name,
            amount = amount,
            date = date.coerceIn(DayOfMonth.MIN, DayOfMonth.MAX),
            tenure = tenure?.takeIf { it > 0 },
            paid = if (tenure == null) 0 else paid,
            updatedAt = nowIso(),
            syncStatus = SyncStatus.PENDING_UPDATE
        )
        expenseDao.update(updated)
        AppLogger.room("UPDATE", "expenses", updated, "Updated EMI/expense $id")

        enqueueSync("emi", "UPDATE", gson.toJson(mapOf("id" to id, "request" to updated.toRequest())), id)
        syncScheduler.enqueueExpenseSync()
    }

    override suspend fun saveEmi(
        name: String,
        amount: Double,
        date: Int,
        tenure: Int?,
        paid: Int
    ) {
        val entity = newEntity(name, amount, date, tenure?.takeIf { it > 0 }, paid)
        expenseDao.insert(entity)
        AppLogger.room("CREATE", "expenses", entity, "Inserted EMI/expense ${entity.id}")

        enqueueSync("emi", "CREATE", gson.toJson(listOf(entity.toRequest())), entity.id)
        syncScheduler.enqueueExpenseSync()
    }

    override suspend fun refreshExpensesAndSubscriptions() {
        try {
            val result = apiService.getExpensesAndSubscriptions()
            val entities = result.data.orEmpty().map { it.toEntity() }
            expenseDao.replaceAll(entities)
            AppLogger.room("REPLACE", "expenses", "Refreshed ${entities.size} items from API", "Success")
        } catch (e: Exception) {
            AppLogger.apiError("GET", "expenses", e.message, throwable = e)
        }
    }

    private suspend fun newEntity(
        name: String,
        amount: Double,
        date: Int,
        tenure: Int?,
        paid: Int
    ): ExpenseEntity {
        val now = nowIso()
        return ExpenseEntity(
            id = "local-${UUID.randomUUID()}",
            userId = sessionManager.getUser()?.id.orEmpty(),
            name = name,
            amount = amount,
            date = date.coerceIn(DayOfMonth.MIN, DayOfMonth.MAX),
            tenure = tenure,
            paid = if (tenure == null) 0 else paid,
            createdAt = now,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING_CREATE
        )
    }

    private suspend fun enqueueSync(entityType: String, operation: String, payload: String, entityId: String = "") {
        val pendingEntity = PendingSyncEntity(
            entityType = entityType,
            entityId = entityId,
            operation = operation,
            payload = payload,
            createdAt = System.currentTimeMillis()
        )
        pendingSyncDao.insert(pendingEntity)
        AppLogger.room("CREATE", "pending_sync", "Type=$entityType, ID=$entityId, Op=$operation", "Enqueued")
    }

    private fun nowIso(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
}

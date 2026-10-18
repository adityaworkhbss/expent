package com.aditya.expent.data.repository

import com.aditya.expent.data.local.dao.BudgetDao
import com.aditya.expent.data.local.dao.CategoryDao
import com.aditya.expent.data.local.dao.PendingSyncDao
import com.aditya.expent.data.local.entity.BudgetEntity
import com.aditya.expent.data.local.entity.PendingSyncEntity
import com.aditya.expent.data.local.entity.SyncStatus
import com.aditya.expent.data.mapper.toDomain
import com.aditya.expent.data.mapper.toEntity
import com.aditya.expent.data.mapper.toRequest
import com.aditya.expent.data.remote.ApiService
import com.aditya.expent.data.sync.SyncScheduler
import com.aditya.expent.domain.model.Budget
import com.aditya.expent.domain.repository.IncomeBudgetRepository
import com.aditya.expent.presentation.onboard.RecurringIncome
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

class IncomeBudgetRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val budgetDao: BudgetDao,
    private val categoryDao: CategoryDao,
    private val pendingSyncDao: PendingSyncDao,
    private val syncScheduler: SyncScheduler,
    private val sessionManager: SessionManager,
    private val gson: Gson
) : IncomeBudgetRepository {

    override fun getBudgets(): Flow<List<Budget>> {
        return budgetDao.getBudgets().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveIncomeBudget(
        salary: RecurringIncome,
        additionalIncome: List<RecurringIncome>
    ) {
        val entities = (listOf(salary) + additionalIncome).map { income ->
            newEntity(
                categoryId = income.categoryId,
                amount = income.amount.toDoubleOrNull() ?: 0.0,
                date = income.date ?: DayOfMonth.today()
            )
        }
        budgetDao.insert(entities)
        AppLogger.room("CREATE", "budgets", entities, "Inserted ${entities.size} income budgets")

        val requests = entities.map { it.toRequest() }
        val localIds = entities.joinToString(",") { it.id }
        enqueueSync("budget", "CREATE", gson.toJson(requests), localIds)
        syncScheduler.enqueueBudgetSync()
    }

    override suspend fun saveBudget(categoryId: String?, amount: Double, date: Int) {
        val entity = newEntity(categoryId, amount, date)
        budgetDao.insert(entity)
        AppLogger.room("CREATE", "budgets", entity, "Inserted budget ${entity.id}")

        enqueueSync("budget", "CREATE", gson.toJson(listOf(entity.toRequest())), entity.id)
        syncScheduler.enqueueBudgetSync()
    }

    override suspend fun deleteBudget(id: String) {
        val budget = budgetDao.getBudget(id) ?: return
        budgetDao.update(
            budget.copy(
                isDeleted = true,
                syncStatus = SyncStatus.PENDING_DELETE
            )
        )
        AppLogger.room("UPDATE", "budgets", "ID=$id, isDeleted=true", "Marked deleted")

        enqueueSync("budget", "DELETE", id, id)
        syncScheduler.enqueueBudgetSync()
    }

    override suspend fun updateBudget(id: String, categoryId: String?, amount: Double, date: Int) {
        val budget = budgetDao.getBudget(id) ?: return
        val updated = budget.copy(
            categoryId = categoryId,
            categoryName = categoryNameFor(categoryId) ?: budget.categoryName,
            amount = amount,
            date = date,
            updatedAt = nowIso(),
            syncStatus = SyncStatus.PENDING_UPDATE
        )
        budgetDao.update(updated)
        AppLogger.room("UPDATE", "budgets", updated, "Updated budget $id")

        enqueueSync(
            "budget",
            "UPDATE",
            gson.toJson(mapOf("id" to id, "request" to updated.toRequest())),
            id
        )
        syncScheduler.enqueueBudgetSync()
    }

    override suspend fun refreshBudgets() {
        try {
            val response = apiService.getBudgets()
            val entities = response.data.orEmpty().map { dto ->
                val entity = dto.toEntity()
                entity.copy(categoryName = entity.categoryName ?: categoryNameFor(entity.categoryId))
            }
            budgetDao.replaceAll(entities)
            AppLogger.room("REPLACE", "budgets", "Refreshed ${entities.size} items from API", "Success")
        } catch (e: Exception) {
            AppLogger.apiError("GET", "budgets", e.message, throwable = e)
        }
    }

    private suspend fun newEntity(categoryId: String?, amount: Double, date: Int): BudgetEntity {
        val now = nowIso()
        return BudgetEntity(
            id = "local-${UUID.randomUUID()}",
            userId = sessionManager.getUser()?.id.orEmpty(),
            categoryId = categoryId,
            amount = amount,
            date = date.coerceIn(DayOfMonth.MIN, DayOfMonth.MAX),
            categoryName = categoryNameFor(categoryId),
            createdAt = now,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING_CREATE
        )
    }

    // Income categories always come from Room (the ones the user saved), so the id is enough
    private suspend fun categoryNameFor(categoryId: String?): String? =
        categoryId?.takeIf { it.isNotBlank() }?.let { categoryDao.getCategory(it)?.name }

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

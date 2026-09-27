package com.aditya.expent.data.repository

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.aditya.expent.data.local.dao.BudgetDao
import com.aditya.expent.data.local.dao.PendingSyncDao
import com.aditya.expent.data.local.entity.BudgetEntity
import com.aditya.expent.data.local.entity.PendingSyncEntity
import com.aditya.expent.data.local.entity.SyncStatus
import com.aditya.expent.data.mapper.toDto
import com.aditya.expent.data.mapper.toEntity
import com.aditya.expent.data.remote.ApiService
import com.aditya.expent.data.remote.dto.BudgetRequestDto
import com.aditya.expent.data.remote.dto.BudgetResponseDto
import com.aditya.expent.data.sync.SyncScheduler
import com.aditya.expent.domain.repository.IncomeBudgetRepository
import com.aditya.expent.presentation.onboard.RecurringIncome
import com.aditya.expent.utils.AppLogger
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
    private val pendingSyncDao: PendingSyncDao,
    private val syncScheduler: SyncScheduler,
    private val sessionManager: SessionManager,
    private val gson: Gson
) : IncomeBudgetRepository {

    override fun getBudgets(): Flow<List<BudgetResponseDto>> {
        return budgetDao.getBudgets().map { entities ->
            entities.map { it.toDto() }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun saveIncomeBudget(
        salary: RecurringIncome,
        additionalIncome: List<RecurringIncome>
    ) {
        val userId = sessionManager.getUser()?.id.orEmpty()
        val allIncomes = listOf(salary) + additionalIncome

        val entities = allIncomes.map { income ->
            BudgetEntity(
                id = "local-${UUID.randomUUID()}",
                userId = userId,
                categoryId = income.categoryId,
                periodType = income.periodType,
                limitAmount = income.amount,
                startDate = income.startDate.ifBlank { nowIso() },
                endDate = if (income.endDate.isBlank()) null else income.endDate,
                categoryName = null,
                syncStatus = SyncStatus.PENDING_CREATE,
                isDeleted = false
            )
        }
        budgetDao.insert(entities)
        AppLogger.room("CREATE", "budgets", entities, "Inserted ${entities.size} income budgets")

        val requests = allIncomes.map { income ->
            BudgetRequestDto(
                categoryId = income.categoryId,
                periodType = income.periodType,
                limitAmount = income.amount.toDoubleOrNull() ?: 0.0,
                startDate = income.startDate.ifBlank { null },
                endDate = if (income.endDate.isBlank()) null else income.endDate
            )
        }
        enqueueSync("budget", "CREATE", gson.toJson(requests))
        syncScheduler.enqueueBudgetSync()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun saveBudget(
        categoryId: String?,
        periodType: String,
        amount: Double,
        startDate: String,
        endDate: String?
    ) {
        val userId = sessionManager.getUser()?.id.orEmpty()
        val entity = BudgetEntity(
            id = "local-${UUID.randomUUID()}",
            userId = userId,
            categoryId = categoryId,
            periodType = periodType,
            limitAmount = amount.toString(),
            startDate = startDate.ifBlank { nowIso() },
            endDate = if (endDate.isNullOrBlank()) null else endDate,
            categoryName = null,
            syncStatus = SyncStatus.PENDING_CREATE,
            isDeleted = false
        )
        budgetDao.insert(entity)
        AppLogger.room("CREATE", "budgets", entity, "Inserted budget ${entity.id}")

        val request = BudgetRequestDto(
            categoryId = categoryId,
            periodType = periodType,
            limitAmount = amount,
            startDate = startDate.ifBlank { null },
            endDate = if (endDate.isNullOrBlank()) null else endDate
        )

        enqueueSync("budget", "CREATE", gson.toJson(listOf(request)), entity.id)
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

    override suspend fun updateBudget(
        id: String,
        categoryId: String?,
        periodType: String,
        amount: Double,
        startDate: String,
        endDate: String?
    ) {
        val budget = budgetDao.getBudget(id) ?: return
        val updated = budget.copy(
            categoryId = categoryId,
            periodType = periodType,
            limitAmount = amount.toString(),
            startDate = startDate,
            endDate = endDate,
            syncStatus = SyncStatus.PENDING_UPDATE
        )
        budgetDao.update(updated)
        AppLogger.room("UPDATE", "budgets", updated, "Updated budget $id")

        enqueueSync(
            "budget",
            "UPDATE",
            gson.toJson(
                mapOf(
                    "id" to id,
                    "categoryId" to categoryId,
                    "periodType" to periodType,
                    "amount" to amount,
                    "startDate" to startDate,
                    "endDate" to endDate
                )
            ),
            id
        )
        syncScheduler.enqueueBudgetSync()
    }

    override suspend fun refreshBudgets() {
        try {
            val response = apiService.getBudgets()
            val entities = response.map { it.toEntity() }
            budgetDao.replaceAll(entities)
            AppLogger.room("REPLACE", "budgets", "Refreshed ${entities.size} items from API", "Success")
        } catch (e: Exception) {
            AppLogger.apiError("GET", "budgets", e.message, throwable = e)
        }
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
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date())
}

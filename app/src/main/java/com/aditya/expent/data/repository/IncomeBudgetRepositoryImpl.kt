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

        enqueueSync("budget", "CREATE", gson.toJson(mapOf("salary" to salary, "additional" to additionalIncome)))
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
        
        enqueueSync("budget", "DELETE", id)
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
        budgetDao.update(
            budget.copy(
                categoryId = categoryId,
                periodType = periodType,
                limitAmount = amount.toString(),
                startDate = startDate,
                endDate = endDate,
                syncStatus = SyncStatus.PENDING_UPDATE
            )
        )

        enqueueSync("budget", "UPDATE", gson.toJson(mapOf("id" to id, "categoryId" to categoryId, "periodType" to periodType, "amount" to amount, "startDate" to startDate, "endDate" to endDate)))
        syncScheduler.enqueueBudgetSync()
    }

    override suspend fun refreshBudgets() {
        try {
            Log.d("rest re", "refreshBudgets Called")
            val response = apiService.getBudgets()
            Log.d("rest re", "refreshBudgets response : $response")
            budgetDao.replaceAll(response.map { it.toEntity() })
        } catch (e: Exception) {
            Log.e("rest re", "Error refreshBudgets: ${e.message}", e)
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

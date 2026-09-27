package com.aditya.expent.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aditya.expent.data.local.dao.AccountDao
import com.aditya.expent.data.local.dao.BudgetDao
import com.aditya.expent.data.local.dao.CategoryDao
import com.aditya.expent.data.local.dao.CustomizationDao
import com.aditya.expent.data.local.dao.ExpenseDao
import com.aditya.expent.data.local.dao.PendingSyncDao
import com.aditya.expent.data.local.dao.TransactionDao
import com.aditya.expent.data.local.entity.PendingSyncEntity
import com.aditya.expent.data.local.entity.SyncStatus
import com.aditya.expent.data.mapper.toEntity
import com.aditya.expent.data.remote.ApiService
import com.aditya.expent.data.remote.dto.BudgetRequestDto
import com.aditya.expent.data.remote.dto.CategoryRequestDto
import com.aditya.expent.data.remote.dto.CreateTransactionRequestDto
import com.aditya.expent.data.remote.dto.ExpenseIncomeRequestDto
import com.aditya.expent.data.remote.dto.OnboardingStepRequestDto
import com.aditya.expent.data.remote.dto.PaymentModeRequestDto
import com.aditya.expent.data.remote.dto.UserCustomizationResponseDto
import com.aditya.expent.domain.repository.CategoryRepository
import com.aditya.expent.domain.repository.CustomizationRepository
import com.aditya.expent.domain.repository.ExpenseAndSubscriptionRepository
import com.aditya.expent.domain.repository.IncomeBudgetRepository
import com.aditya.expent.domain.repository.PaymentModeRepository
import com.aditya.expent.domain.repository.TransactionRepository
import com.aditya.expent.utils.SessionManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SyncWorkerEntryPoint {
        fun categoryRepository(): CategoryRepository
        fun paymentModeRepository(): PaymentModeRepository
        fun incomeBudgetRepository(): IncomeBudgetRepository
        fun expenseAndSubscriptionRepository(): ExpenseAndSubscriptionRepository
        fun transactionRepository(): TransactionRepository
        fun customizationRepository(): CustomizationRepository
        fun pendingSyncDao(): PendingSyncDao
        fun categoryDao(): CategoryDao
        fun accountDao(): AccountDao
        fun budgetDao(): BudgetDao
        fun expenseDao(): ExpenseDao
        fun transactionDao(): TransactionDao
        fun customizationDao(): CustomizationDao
        fun sessionManager(): SessionManager
        fun apiService(): ApiService
        fun gson(): Gson
    }

    override suspend fun doWork(): Result {
        Log.d("SyncWorker", "Starting background data sync...")
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                SyncWorkerEntryPoint::class.java
            )

            val sessionManager = entryPoint.sessionManager()
            if (sessionManager.getUser() == null) {
                Log.d("SyncWorker", "User not logged in, skipping background sync.")
                return Result.success()
            }

            val categoryRepo = entryPoint.categoryRepository()
            val paymentRepo = entryPoint.paymentModeRepository()
            val budgetRepo = entryPoint.incomeBudgetRepository()
            val expenseRepo = entryPoint.expenseAndSubscriptionRepository()
            val transactionRepo = entryPoint.transactionRepository()
            val customizationRepo = entryPoint.customizationRepository()
            val pendingSyncDao = entryPoint.pendingSyncDao()
            val apiService = entryPoint.apiService()
            val gson = entryPoint.gson()

            // 1. Process pending offline sync tasks
            val pendingItems = pendingSyncDao.getAllPendingSyncs()
            Log.d("SyncWorker", "Found ${pendingItems.size} pending offline sync items")

            var hasPendingFailures = false

            for (item in pendingItems) {
                try {
                    val success = processPendingItem(item, apiService, entryPoint, gson)
                    if (success) {
                        pendingSyncDao.delete(item)
                        Log.d("SyncWorker", "Successfully synced pending item ID: ${item.id}")
                    } else {
                        hasPendingFailures = true
                    }
                } catch (e: Exception) {
                    Log.e("SyncWorker", "Error processing pending sync item ${item.id}: ${e.message}", e)
                    val nextRetry = item.retryCount + 1
                    if (nextRetry >= 5) {
                        Log.w("SyncWorker", "Discarding pending sync item ${item.id} after 5 failed attempts")
                        pendingSyncDao.delete(item)
                    } else {
                        pendingSyncDao.update(item.copy(retryCount = nextRetry))
                        hasPendingFailures = true
                    }
                }
            }

            // 2. Fetch fresh data from remote API & persist into Room DB
            Log.d("SyncWorker", "Fetching remote data to update local Room database...")
            runCatching { categoryRepo.refreshCategories() }
            runCatching { paymentRepo.refreshAccounts() }
            runCatching { budgetRepo.refreshBudgets() }
            runCatching { expenseRepo.refreshExpensesAndSubscriptions() }
            runCatching { transactionRepo.refreshTransactions(1, 100) }
            runCatching { customizationRepo.refreshCustomization() }

            Log.d("SyncWorker", "Background data sync completed!")
            if (hasPendingFailures && runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            Log.e("SyncWorker", "SyncWorker failed: ${e.message}", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private suspend fun processPendingItem(
        item: PendingSyncEntity,
        apiService: ApiService,
        entryPoint: SyncWorkerEntryPoint,
        gson: Gson
    ): Boolean {
        val userId = entryPoint.sessionManager().getUser()?.id

        return when (item.entityType) {
            "category" -> {
                if (item.operation == "DELETE") {
                    apiService.deleteCategory(item.payload)
                    entryPoint.categoryDao().deleteById(item.payload)
                } else {
                    val type = object : TypeToken<List<CategoryRequestDto>>() {}.type
                    val list: List<CategoryRequestDto> = runCatching {
                        gson.fromJson<List<CategoryRequestDto>>(item.payload, type)
                    }.getOrElse {
                        listOf(gson.fromJson(item.payload, CategoryRequestDto::class.java))
                    }
                    val created = apiService.createCategories(list)
                    entryPoint.categoryDao().insert(created.map { it.toEntity(userId, SyncStatus.SYNCED) })
                }
                true
            }

            "account" -> {
                if (item.operation == "DELETE") {
                    apiService.deleteAccount(item.payload)
                    entryPoint.accountDao().deleteById(item.payload)
                } else {
                    val type = object : TypeToken<List<PaymentModeRequestDto>>() {}.type
                    val list: List<PaymentModeRequestDto> = runCatching {
                        gson.fromJson<List<PaymentModeRequestDto>>(item.payload, type)
                    }.getOrElse {
                        listOf(gson.fromJson(item.payload, PaymentModeRequestDto::class.java))
                    }
                    val created = apiService.savePaymentModes(list)
                    entryPoint.accountDao().insert(created.map { it.toEntity(userId, SyncStatus.SYNCED) })
                }
                true
            }

            "budget" -> {
                when (item.operation) {
                    "DELETE" -> {
                        if (!item.payload.startsWith("local-")) {
                            apiService.deleteBudget(item.payload)
                        }
                        entryPoint.budgetDao().deleteById(item.payload)
                    }
                    "UPDATE" -> {
                        val updateMap = gson.fromJson(item.payload, Map::class.java)
                        val id = updateMap["id"] as? String ?: item.entityId
                        val categoryId = updateMap["categoryId"] as? String
                        val periodType = updateMap["periodType"] as? String ?: "MONTHLY"
                        val amount = (updateMap["amount"] as? Number)?.toDouble()
                            ?: (updateMap["amount"] as? String)?.toDoubleOrNull()
                            ?: 0.0
                        val startDate = updateMap["startDate"] as? String
                        val endDate = updateMap["endDate"] as? String

                        val request = BudgetRequestDto(
                            categoryId = categoryId,
                            periodType = periodType,
                            limitAmount = amount,
                            startDate = startDate,
                            endDate = endDate
                        )

                        if (id.startsWith("local-")) {
                            val created = apiService.saveBudgets(listOf(request))
                            entryPoint.budgetDao().deleteById(id)
                            entryPoint.budgetDao().insert(created.map { it.toEntity(SyncStatus.SYNCED) })
                        } else {
                            val updated = apiService.updateBudget(id, request)
                            entryPoint.budgetDao().insert(updated.toEntity(SyncStatus.SYNCED))
                        }
                    }
                    else -> {
                        val type = object : TypeToken<List<BudgetRequestDto>>() {}.type
                        val list: List<BudgetRequestDto> = runCatching {
                            gson.fromJson<List<BudgetRequestDto>>(item.payload, type)
                        }.getOrElse {
                            listOf(gson.fromJson(item.payload, BudgetRequestDto::class.java))
                        }
                        val created = apiService.saveBudgets(list)
                        if (item.entityId.isNotBlank()) {
                            entryPoint.budgetDao().deleteById(item.entityId)
                        }
                        entryPoint.budgetDao().insert(created.map { it.toEntity(SyncStatus.SYNCED) })
                    }
                }
                true
            }

            "emi" -> {
                when (item.operation) {
                    "DELETE" -> {
                        if (!item.payload.startsWith("local-")) {
                            apiService.deleteEmi(item.payload)
                        }
                        entryPoint.expenseDao().deleteById(item.payload)
                    }
                    "UPDATE" -> {
                        val updateMap = gson.fromJson(item.payload, Map::class.java)
                        val id = updateMap["id"] as? String ?: item.entityId
                        val requestJson = gson.toJson(updateMap["request"])
                        val request = gson.fromJson(requestJson, ExpenseIncomeRequestDto::class.java)

                        if (id.startsWith("local-")) {
                            val created = apiService.saveExpensesAndSubscriptions(listOf(request))
                            entryPoint.expenseDao().deleteById(id)
                            entryPoint.expenseDao().insert(created.map { it.toEntity(SyncStatus.SYNCED) })
                        } else {
                            val updated = apiService.updateEmi(id, request)
                            entryPoint.expenseDao().insert(updated.toEntity(SyncStatus.SYNCED))
                        }
                    }
                    else -> {
                        val type = object : TypeToken<List<ExpenseIncomeRequestDto>>() {}.type
                        val list: List<ExpenseIncomeRequestDto> = runCatching {
                            gson.fromJson<List<ExpenseIncomeRequestDto>>(item.payload, type)
                        }.getOrElse {
                            listOf(gson.fromJson(item.payload, ExpenseIncomeRequestDto::class.java))
                        }
                        val created = apiService.saveExpensesAndSubscriptions(list)
                        if (item.entityId.isNotBlank()) {
                            entryPoint.expenseDao().deleteById(item.entityId)
                        }
                        entryPoint.expenseDao().insert(created.map { it.toEntity(SyncStatus.SYNCED) })
                    }
                }
                true
            }

            "transaction" -> {
                if (item.operation == "CREATE") {
                    val dto = gson.fromJson(item.payload, CreateTransactionRequestDto::class.java)
                    val created = apiService.addTransaction(dto)
                    if (item.entityId.isNotBlank()) {
                        entryPoint.transactionDao().deleteById(item.entityId)
                    }
                    entryPoint.transactionDao().insert(created.toEntity(SyncStatus.SYNCED))
                }
                true
            }

            "customization" -> {
                if (item.operation == "UPDATE") {
                    val dto = gson.fromJson(item.payload, UserCustomizationResponseDto::class.java)
                    val updated = apiService.updateUserCustomization(dto)
                    entryPoint.customizationDao().insert(updated.toEntity())
                    entryPoint.sessionManager().saveCustomization(updated)
                }
                true
            }

            "onboarding" -> {
                if (item.operation == "INCREMENT") {
                    val step = item.payload.toIntOrNull() ?: 1
                    apiService.updateOnboardingCount(OnboardingStepRequestDto(step))
                }
                true
            }

            else -> true
        }
    }
}

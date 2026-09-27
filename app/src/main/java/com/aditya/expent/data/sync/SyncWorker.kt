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
import com.aditya.expent.utils.AppLogger
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
        AppLogger.sync("START", "Starting background data sync...")
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                SyncWorkerEntryPoint::class.java
            )

            val sessionManager = entryPoint.sessionManager()
            if (sessionManager.getUser() == null) {
                AppLogger.sync("SKIP", "User not logged in, skipping background sync.")
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
            AppLogger.sync("QUEUE", "Found ${pendingItems.size} pending offline sync items in queue")

            var hasPendingFailures = false

            for (item in pendingItems) {
                try {
                    AppLogger.sync("PROCESS_ITEM", "Attempting sync: ID=${item.id}, Type=${item.entityType}, Op=${item.operation}, EntityId=${item.entityId}")
                    val success = processPendingItem(item, apiService, entryPoint, gson)
                    if (success) {
                        pendingSyncDao.delete(item)
                        AppLogger.room("DELETE", "pending_sync", payload = "ID: ${item.id}", result = "Task deleted after successful sync")
                        AppLogger.sync("ITEM_SUCCESS", "Successfully synced item ID=${item.id} (${item.entityType}:${item.operation})")
                    } else {
                        hasPendingFailures = true
                    }
                } catch (e: Exception) {
                    AppLogger.syncError("ITEM_FAILED", "Error processing pending sync item ${item.id} (${item.entityType}): ${e.message}", e)
                    val nextRetry = item.retryCount + 1
                    if (nextRetry >= 5) {
                        AppLogger.sync("ITEM_DISCARD", "Discarding pending sync item ${item.id} after 5 failed attempts")
                        pendingSyncDao.delete(item)
                        AppLogger.room("DELETE", "pending_sync", payload = "ID: ${item.id}", result = "Task discarded (max retries)")
                    } else {
                        pendingSyncDao.update(item.copy(retryCount = nextRetry))
                        AppLogger.room("UPDATE", "pending_sync", payload = "ID: ${item.id}, retryCount=$nextRetry", result = "Updated retry count")
                        hasPendingFailures = true
                    }
                }
            }

            // 2. Fetch fresh data from remote API & persist into Room DB if requested and not throttled
            val isFullRefreshRequested = inputData.getBoolean(SyncScheduler.KEY_FULL_REFRESH, false)
            val lastSyncTime = sessionManager.getLastSyncTime()
            val timeSinceLastSync = System.currentTimeMillis() - lastSyncTime
            val throttleWindowMs = 5 * 60 * 1000L // 5 minutes

            if (isFullRefreshRequested && (lastSyncTime == 0L || timeSinceLastSync > throttleWindowMs)) {
                AppLogger.sync("REFRESH_START", "Fetching remote data to refresh local Room database...")
                runCatching { categoryRepo.refreshCategories() }
                runCatching { paymentRepo.refreshAccounts() }
                runCatching { budgetRepo.refreshBudgets() }
                runCatching { expenseRepo.refreshExpensesAndSubscriptions() }
                runCatching { transactionRepo.refreshTransactions(1, 100) }
                runCatching { customizationRepo.refreshCustomization() }
                sessionManager.setLastSyncTime(System.currentTimeMillis())
            } else {
                AppLogger.sync(
                    "REFRESH_SKIPPED",
                    "Skipping full remote refresh (requested=$isFullRefreshRequested, lastSync=${timeSinceLastSync / 1000}s ago, throttle=${throttleWindowMs / 1000}s)"
                )
            }

            AppLogger.sync("COMPLETE", "Background data sync completed! HasFailures=$hasPendingFailures")
            if (hasPendingFailures && runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            AppLogger.syncError("FATAL", "SyncWorker failed: ${e.message}", e)
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
                    AppLogger.room("DELETE", "categories", payload = "ID: ${item.payload}", result = "Success")
                } else {
                    val type = object : TypeToken<List<CategoryRequestDto>>() {}.type
                    val list: List<CategoryRequestDto> = runCatching {
                        gson.fromJson<List<CategoryRequestDto>>(item.payload, type)
                    }.getOrElse {
                        listOf(gson.fromJson(item.payload, CategoryRequestDto::class.java))
                    }
                    val created = apiService.createCategories(list)
                    val entities = created.map { it.toEntity(userId, SyncStatus.SYNCED) }
                    entryPoint.categoryDao().insert(entities)
                    AppLogger.room("CREATE", "categories", payload = entities, result = "Inserted ${entities.size} categories")
                }
                true
            }

            "account" -> {
                if (item.operation == "DELETE") {
                    apiService.deleteAccount(item.payload)
                    entryPoint.accountDao().deleteById(item.payload)
                    AppLogger.room("DELETE", "accounts", payload = "ID: ${item.payload}", result = "Success")
                } else {
                    val type = object : TypeToken<List<PaymentModeRequestDto>>() {}.type
                    val list: List<PaymentModeRequestDto> = runCatching {
                        gson.fromJson<List<PaymentModeRequestDto>>(item.payload, type)
                    }.getOrElse {
                        listOf(gson.fromJson(item.payload, PaymentModeRequestDto::class.java))
                    }
                    val created = apiService.savePaymentModes(list)
                    val entities = created.map { it.toEntity(userId, SyncStatus.SYNCED) }
                    entryPoint.accountDao().insert(entities)
                    AppLogger.room("CREATE", "accounts", payload = entities, result = "Inserted ${entities.size} accounts")
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
                        AppLogger.room("DELETE", "budgets", payload = "ID: ${item.payload}", result = "Success")
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
                            val entities = created.map { it.toEntity(SyncStatus.SYNCED) }
                            entryPoint.budgetDao().insert(entities)
                            AppLogger.room("CREATE", "budgets", payload = entities, result = "Swapped local $id with server budget")
                        } else {
                            val updated = apiService.updateBudget(id, request)
                            val entity = updated.toEntity(SyncStatus.SYNCED)
                            entryPoint.budgetDao().insert(entity)
                            AppLogger.room("UPDATE", "budgets", payload = entity, result = "Updated budget $id")
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
                        val entities = created.map { it.toEntity(SyncStatus.SYNCED) }
                        entryPoint.budgetDao().insert(entities)
                        AppLogger.room("CREATE", "budgets", payload = entities, result = "Inserted ${entities.size} budgets")
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
                        AppLogger.room("DELETE", "expenses", payload = "ID: ${item.payload}", result = "Success")
                    }
                    "UPDATE" -> {
                        val updateMap = gson.fromJson(item.payload, Map::class.java)
                        val id = updateMap["id"] as? String ?: item.entityId
                        val requestJson = gson.toJson(updateMap["request"])
                        val request = gson.fromJson(requestJson, ExpenseIncomeRequestDto::class.java)

                        if (id.startsWith("local-")) {
                            val created = apiService.saveExpensesAndSubscriptions(listOf(request))
                            entryPoint.expenseDao().deleteById(id)
                            val entities = created.map { it.toEntity(SyncStatus.SYNCED) }
                            entryPoint.expenseDao().insert(entities)
                            AppLogger.room("CREATE", "expenses", payload = entities, result = "Swapped local $id with server EMI")
                        } else {
                            val updated = apiService.updateEmi(id, request)
                            val entity = updated.toEntity(SyncStatus.SYNCED)
                            entryPoint.expenseDao().insert(entity)
                            AppLogger.room("UPDATE", "expenses", payload = entity, result = "Updated EMI $id")
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
                        val entities = created.map { it.toEntity(SyncStatus.SYNCED) }
                        entryPoint.expenseDao().insert(entities)
                        AppLogger.room("CREATE", "expenses", payload = entities, result = "Inserted ${entities.size} expenses")
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
                        AppLogger.room("DELETE", "transactions", payload = "ID: ${item.entityId}", result = "Removed local temp transaction")
                    }
                    val entity = created.toEntity(SyncStatus.SYNCED)
                    entryPoint.transactionDao().insert(entity)
                    AppLogger.room("CREATE", "transactions", payload = entity, result = "Inserted server transaction ${entity.id}")
                }
                true
            }

            "customization" -> {
                if (item.operation == "UPDATE") {
                    val dto = gson.fromJson(item.payload, UserCustomizationResponseDto::class.java)
                    val updated = apiService.updateUserCustomization(dto)
                    val entity = updated.toEntity()
                    entryPoint.customizationDao().insert(entity)
                    entryPoint.sessionManager().saveCustomization(updated)
                    AppLogger.room("UPDATE", "customizations", payload = entity, result = "Success")
                }
                true
            }

            "onboarding" -> {
                if (item.operation == "INCREMENT") {
                    val step = item.payload.toIntOrNull() ?: 1
                    apiService.updateOnboardingCount(OnboardingStepRequestDto(step))
                    AppLogger.sync("ONBOARDING_INCREMENT", "Synced onboarding step increment to $step")
                }
                true
            }

            else -> true
        }
    }
}

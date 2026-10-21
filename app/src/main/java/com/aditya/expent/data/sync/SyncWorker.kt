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
import com.aditya.expent.data.mapper.toRequest
import com.aditya.expent.data.remote.ApiService
import com.aditya.expent.data.remote.dto.BudgetRequestDto
import com.aditya.expent.data.remote.dto.CategoryRequestDto
import com.aditya.expent.data.remote.dto.CreateTransactionRequestDto
import com.aditya.expent.data.remote.dto.ExpenseIncomeRequestDto
import com.aditya.expent.data.remote.dto.FeedbackRequestDto
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private val queueMutex = Mutex()
    }

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

            // 1. Process pending offline sync tasks (one worker at a time so items are never POSTed twice)
            var hasPendingFailures = false

            queueMutex.withLock {
                val pendingItems = pendingSyncDao.getAllPendingSyncs()
                AppLogger.sync("QUEUE", "Found ${pendingItems.size} pending offline sync items in queue")

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
            }

            // 2. Fetch fresh data from remote API & persist into Room DB if requested and not throttled.
            // While onboarding is in progress Room is the source of truth, so there is nothing to pull.
            val isFullRefreshRequested = inputData.getBoolean(SyncScheduler.KEY_FULL_REFRESH, false)
            val isOnboardingComplete = sessionManager.isOnboardingComplete()
            val lastSyncTime = sessionManager.getLastSyncTime()
            val timeSinceLastSync = System.currentTimeMillis() - lastSyncTime
            val throttleWindowMs = 5 * 60 * 1000L // 5 minutes

            if (isFullRefreshRequested && isOnboardingComplete && (lastSyncTime == 0L || timeSinceLastSync > throttleWindowMs)) {
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
                    "Skipping full remote refresh (requested=$isFullRefreshRequested, onboardingComplete=$isOnboardingComplete, lastSync=${timeSinceLastSync / 1000}s ago, throttle=${throttleWindowMs / 1000}s)"
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

    // The POST already landed on the server, so a failure in the local Room swap must not leave the
    // item queued: the next push (e.g. accounts) would send it again and duplicate it server-side.
    private inline fun afterRemoteSuccess(item: PendingSyncEntity, block: () -> Unit) {
        runCatching(block).onFailure {
            AppLogger.syncError("LOCAL_SWAP_FAILED", "Server accepted ${item.entityType}:${item.operation} (ID=${item.id}) but updating Room failed", it)
        }
    }

    private fun PendingSyncEntity.localIds(): List<String> =
        entityId.split(",").map { it.trim() }.filter { it.isNotBlank() }

    // UPDATE payloads are {"id": ..., "request": ...}; the row itself is re-read from Room
    private fun updateTargetId(item: PendingSyncEntity, gson: Gson): String =
        runCatching { gson.fromJson(item.payload, Map::class.java)["id"] as? String }.getOrNull()
            ?: item.entityId

    // Fallback for items queued before rows were re-read from Room
    private inline fun <reified T> parseList(payload: String, gson: Gson): List<T> =
        runCatching { gson.fromJson<List<T>>(payload, object : TypeToken<List<T>>() {}.type) }
            .getOrElse { listOf(gson.fromJson(payload, T::class.java)) }

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
                    afterRemoteSuccess(item) {
                        val localIds = item.localIds()
                        val localCategories = localIds.mapNotNull { entryPoint.categoryDao().getCategory(it) }
                        // Delete local pending entities before inserting server-returned ones
                        localIds.forEach { localId ->
                            entryPoint.categoryDao().deleteById(localId)
                            AppLogger.room("DELETE", "categories", payload = "LocalID: $localId", result = "Removed pending local entity")
                        }
                        val entities = created.data.orEmpty().map { it.toEntity(userId ?: it.userId, SyncStatus.SYNCED) }
                        entryPoint.categoryDao().insert(entities)
                        AppLogger.room("CREATE", "categories", payload = entities, result = "Inserted ${entities.size} categories")

                        // Budgets / expenses saved offline still point at the local-* id; move them to the server id
                        localCategories.forEach { local ->
                            val server = entities.firstOrNull {
                                it.name.equals(local.name, ignoreCase = true) && it.type.equals(local.type, ignoreCase = true)
                            } ?: return@forEach
                            entryPoint.budgetDao().updateCategoryId(local.id, server.id)
                            entryPoint.expenseDao().updateCategoryId(local.id, server.id)
                        }
                    }
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
                    afterRemoteSuccess(item) {
                        // Delete local pending entities before inserting server-returned ones
                        item.entityId.split(",").filter { it.isNotBlank() }.forEach { localId ->
                            entryPoint.accountDao().deleteById(localId)
                            AppLogger.room("DELETE", "accounts", payload = "LocalID: $localId", result = "Removed pending local entity")
                        }
                        val entities = created.data.orEmpty().map { it.toEntity(userId ?: it.userId, SyncStatus.SYNCED) }
                        entryPoint.accountDao().insert(entities)
                        AppLogger.room("CREATE", "accounts", payload = entities, result = "Inserted ${entities.size} accounts")
                    }
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
                        val id = updateTargetId(item, gson)
                        // A local-* row hasn't been created yet; its pending CREATE sends the latest row
                        if (!id.startsWith("local-")) {
                            val budget = entryPoint.budgetDao().getBudget(id)
                            if (budget != null && !budget.isDeleted) {
                                val updated = apiService.updateBudget(id, budget.toRequest())
                                afterRemoteSuccess(item) {
                                    val entity = updated.data?.toEntity(SyncStatus.SYNCED)
                                        ?.let { it.copy(categoryName = it.categoryName ?: budget.categoryName) }
                                    if (entity != null) {
                                        entryPoint.budgetDao().insert(entity)
                                        AppLogger.room("UPDATE", "budgets", payload = entity, result = "Updated budget $id")
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        // Send the rows as they are now (edits made offline, server category ids), skipping deleted ones
                        val localIds = item.localIds()
                        val budgets = localIds.mapNotNull { entryPoint.budgetDao().getBudget(it) }.filterNot { it.isDeleted }
                        val list = if (localIds.isEmpty()) {
                            parseList<BudgetRequestDto>(item.payload, gson)
                        } else {
                            budgets.map { it.toRequest() }
                        }
                        if (list.isNotEmpty()) {
                            val created = apiService.saveBudgets(list)
                            afterRemoteSuccess(item) {
                                localIds.forEach { localId ->
                                    entryPoint.budgetDao().deleteById(localId)
                                    AppLogger.room("DELETE", "budgets", payload = "LocalID: $localId", result = "Removed pending local entity")
                                }
                                val entities = created.data.orEmpty().mapIndexed { index, dto ->
                                    val entity = dto.toEntity(SyncStatus.SYNCED)
                                    entity.copy(categoryName = entity.categoryName ?: budgets.getOrNull(index)?.categoryName)
                                }
                                entryPoint.budgetDao().insert(entities)
                                AppLogger.room("CREATE", "budgets", payload = entities, result = "Inserted ${entities.size} budgets")
                            }
                        } else {
                            localIds.forEach { entryPoint.budgetDao().deleteById(it) }
                        }
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
                        val id = updateTargetId(item, gson)
                        // A local-* row hasn't been created yet; its pending CREATE sends the latest row
                        if (!id.startsWith("local-")) {
                            val expense = entryPoint.expenseDao().getExpense(id)
                            if (expense != null && !expense.isDeleted) {
                                val updated = apiService.updateEmi(id, expense.toRequest())
                                afterRemoteSuccess(item) {
                                    val entity = updated.data?.toEntity(SyncStatus.SYNCED)
                                    if (entity != null) {
                                        entryPoint.expenseDao().insert(entity)
                                        AppLogger.room("UPDATE", "expenses", payload = entity, result = "Updated EMI $id")
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        // Send the rows as they are now (edits made offline), skipping deleted ones
                        val localIds = item.localIds()
                        val list = if (localIds.isEmpty()) {
                            parseList<ExpenseIncomeRequestDto>(item.payload, gson)
                        } else {
                            localIds.mapNotNull { entryPoint.expenseDao().getExpense(it) }
                                .filterNot { it.isDeleted }
                                .map { it.toRequest() }
                        }
                        if (list.isNotEmpty()) {
                            val created = apiService.saveExpensesAndSubscriptions(list)
                            afterRemoteSuccess(item) {
                                localIds.forEach { localId ->
                                    entryPoint.expenseDao().deleteById(localId)
                                    AppLogger.room("DELETE", "expenses", payload = "LocalID: $localId", result = "Removed pending local entity")
                                }
                                val entities = created.data.orEmpty().map { it.toEntity(SyncStatus.SYNCED) }
                                entryPoint.expenseDao().insert(entities)
                                AppLogger.room("CREATE", "expenses", payload = entities, result = "Inserted ${entities.size} expenses")
                            }
                        } else {
                            localIds.forEach { entryPoint.expenseDao().deleteById(it) }
                        }
                    }
                }
                true
            }

            "transaction" -> {
                if (item.operation == "CREATE") {
                    val dto = gson.fromJson(item.payload, CreateTransactionRequestDto::class.java)
                    val created = apiService.addTransaction(dto)
                    afterRemoteSuccess(item) {
                        // The API doesn't return names; keep the display names from the local row
                        val local = item.entityId.takeIf { it.isNotBlank() }
                            ?.let { entryPoint.transactionDao().getTransaction(it) }
                        if (item.entityId.isNotBlank()) {
                            entryPoint.transactionDao().deleteById(item.entityId)
                            AppLogger.room("DELETE", "transactions", payload = "ID: ${item.entityId}", result = "Removed local temp transaction")
                        }
                        val entity = created.data?.toEntity(SyncStatus.SYNCED)
                            ?.let { it.copy(categoryName = local?.categoryName, accountName = local?.accountName) }
                        if (entity != null) {
                            entryPoint.transactionDao().insert(entity)
                            AppLogger.room("CREATE", "transactions", payload = entity, result = "Inserted server transaction ${entity.id}")
                        }
                    }
                }
                true
            }

            "customization" -> {
                if (item.operation == "UPDATE") {
                    val dto = gson.fromJson(item.payload, UserCustomizationResponseDto::class.java)
                    val updated = apiService.updateUserCustomization(dto)
                    val data = updated.data
                    if (data != null) {
                        afterRemoteSuccess(item) {
                            // The backend doesn't store aiTransaction / reminder yet; keep the latest local
                            // choice (the user may have toggled again since this item was queued)
                            val local = entryPoint.customizationDao().getCustomization().first()
                            val merged = data.copy(
                                aiTransaction = local?.aiTransaction ?: dto.aiTransaction,
                                reminder = local?.reminder ?: dto.reminder
                            )
                            val entity = merged.toEntity()
                            entryPoint.customizationDao().replace(entity)
                            entryPoint.sessionManager().saveCustomization(merged)
                            AppLogger.room("UPDATE", "customizations", payload = entity, result = "Success")
                        }
                    }
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

            // Best effort: feedback has no local row, a failed send just retries like any other item
            "feedback" -> {
                apiService.submitFeedback(gson.fromJson(item.payload, FeedbackRequestDto::class.java))
                AppLogger.sync("FEEDBACK", "Submitted feedback ID=${item.id}")
                true
            }

            else -> true
        }
    }
}

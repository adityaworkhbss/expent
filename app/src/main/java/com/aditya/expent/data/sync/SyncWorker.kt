package com.aditya.expent.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aditya.expent.data.local.dao.PendingSyncDao
import com.aditya.expent.data.local.entity.PendingSyncEntity
import com.aditya.expent.data.remote.ApiService
import com.aditya.expent.data.remote.dto.BudgetRequestDto
import com.aditya.expent.data.remote.dto.CategoryRequestDto
import com.aditya.expent.data.remote.dto.CreateTransactionRequestDto
import com.aditya.expent.data.remote.dto.ExpenseIncomeRequestDto
import com.aditya.expent.data.remote.dto.PaymentModeRequestDto
import com.aditya.expent.data.remote.dto.UserCustomizationResponseDto
import com.aditya.expent.domain.repository.CategoryRepository
import com.aditya.expent.domain.repository.CustomizationRepository
import com.aditya.expent.domain.repository.ExpenseAndSubscriptionRepository
import com.aditya.expent.domain.repository.IncomeBudgetRepository
import com.aditya.expent.domain.repository.PaymentModeRepository
import com.aditya.expent.domain.repository.TransactionRepository
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

            for (item in pendingItems) {
                try {
                    val success = processPendingItem(item, apiService, gson)
                    if (success) {
                        pendingSyncDao.delete(item)
                        Log.d("SyncWorker", "Successfully synced pending item ID: ${item.id}")
                    }
                } catch (e: Exception) {
                    Log.e("SyncWorker", "Error processing pending sync item ${item.id}: ${e.message}", e)
                }
            }

            // 2. Fetch fresh data from remote API & persist into Room DB
            Log.d("SyncWorker", "Fetching remote data to update local Room database...")
            categoryRepo.refreshCategories()
            paymentRepo.refreshAccounts()
            budgetRepo.refreshBudgets()
            expenseRepo.refreshExpensesAndSubscriptions()
            transactionRepo.refreshTransactions(1, 100)
            customizationRepo.refreshCustomization()

            Log.d("SyncWorker", "Background data sync completed successfully!")
            Result.success()
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
        gson: Gson
    ): Boolean {
        return when (item.entityType) {
            "category" -> {
                if (item.operation == "DELETE") {
                    apiService.deleteCategory(item.payload)
                } else {
                    val type = object : TypeToken<List<CategoryRequestDto>>() {}.type
                    val list: List<CategoryRequestDto> = runCatching {
                        gson.fromJson<List<CategoryRequestDto>>(item.payload, type)
                    }.getOrElse {
                        listOf(gson.fromJson(item.payload, CategoryRequestDto::class.java))
                    }
                    apiService.createCategories(list)
                }
                true
            }

            "account" -> {
                if (item.operation == "DELETE") {
                    apiService.deleteAccount(item.payload)
                } else {
                    val type = object : TypeToken<List<PaymentModeRequestDto>>() {}.type
                    val list: List<PaymentModeRequestDto> = runCatching {
                        gson.fromJson<List<PaymentModeRequestDto>>(item.payload, type)
                    }.getOrElse {
                        listOf(gson.fromJson(item.payload, PaymentModeRequestDto::class.java))
                    }
                    apiService.savePaymentModes(list)
                }
                true
            }

            "budget" -> {
                if (item.operation == "DELETE") {
                    apiService.deleteBudget(item.payload)
                } else {
                    val type = object : TypeToken<List<BudgetRequestDto>>() {}.type
                    val list: List<BudgetRequestDto> = runCatching {
                        gson.fromJson<List<BudgetRequestDto>>(item.payload, type)
                    }.getOrElse {
                        listOf(gson.fromJson(item.payload, BudgetRequestDto::class.java))
                    }
                    apiService.saveBudgets(list)
                }
                true
            }

            "emi" -> {
                if (item.operation == "DELETE") {
                    apiService.deleteEmi(item.payload)
                } else {
                    val type = object : TypeToken<List<ExpenseIncomeRequestDto>>() {}.type
                    val list: List<ExpenseIncomeRequestDto> = runCatching {
                        gson.fromJson<List<ExpenseIncomeRequestDto>>(item.payload, type)
                    }.getOrElse {
                        listOf(gson.fromJson(item.payload, ExpenseIncomeRequestDto::class.java))
                    }
                    apiService.saveExpensesAndSubscriptions(list)
                }
                true
            }

            "transaction" -> {
                if (item.operation == "CREATE") {
                    val dto = gson.fromJson(item.payload, CreateTransactionRequestDto::class.java)
                    apiService.addTransaction(dto)
                }
                true
            }

            "customization" -> {
                if (item.operation == "UPDATE") {
                    val dto = gson.fromJson(item.payload, UserCustomizationResponseDto::class.java)
                    apiService.updateUserCustomization(dto)
                }
                true
            }

            else -> true
        }
    }
}

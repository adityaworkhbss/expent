package com.aditya.expent.domain.usecase

import android.util.Log
import com.aditya.expent.domain.repository.CategoryRepository
import com.aditya.expent.domain.repository.CustomizationRepository
import com.aditya.expent.domain.repository.ExpenseAndSubscriptionRepository
import com.aditya.expent.domain.repository.IncomeBudgetRepository
import com.aditya.expent.domain.repository.PaymentModeRepository
import com.aditya.expent.domain.repository.TransactionRepository
import com.aditya.expent.utils.SessionManager
import com.aditya.expent.data.sync.SyncScheduler
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class FetchUserDataUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val paymentModeRepository: PaymentModeRepository,
    private val incomeBudgetRepository: IncomeBudgetRepository,
    private val expenseAndSubscriptionRepository: ExpenseAndSubscriptionRepository,
    private val transactionRepository: TransactionRepository,
    private val customizationRepository: CustomizationRepository,
    private val sessionManager: SessionManager,
    private val syncScheduler: SyncScheduler
) {
    suspend operator fun invoke() = coroutineScope {
        Log.d("FetchUserDataUseCase", "Starting post-login data fetch from remote API into Room DB...")

        // Cancel any in-flight sync workers to avoid concurrent writes that cause duplicates
        syncScheduler.cancelAllSync()

        // 1. Fetch base reference entities first (Categories, Accounts, Customization)
        val categoriesJob = async {
            runCatching { categoryRepository.refreshCategories() }
                .onFailure { Log.e("FetchUserDataUseCase", "Failed to fetch categories", it) }
        }
        val accountsJob = async {
            runCatching { paymentModeRepository.refreshAccounts() }
                .onFailure { Log.e("FetchUserDataUseCase", "Failed to fetch accounts", it) }
        }
        val customizationJob = async {
            runCatching { customizationRepository.refreshCustomization() }
                .onFailure { Log.e("FetchUserDataUseCase", "Failed to fetch customization", it) }
        }

        categoriesJob.await()
        accountsJob.await()
        customizationJob.await()

        // 2. Fetch dependent entities that reference categories and accounts (Budgets, Expenses, Transactions)
        val budgetsJob = async {
            runCatching { incomeBudgetRepository.refreshBudgets() }
                .onFailure { Log.e("FetchUserDataUseCase", "Failed to fetch budgets", it) }
        }
        val expensesJob = async {
            runCatching { expenseAndSubscriptionRepository.refreshExpensesAndSubscriptions() }
                .onFailure { Log.e("FetchUserDataUseCase", "Failed to fetch expenses/subscriptions", it) }
        }
        val transactionsJob = async {
            runCatching { transactionRepository.refreshTransactions(1, 100) }
                .onFailure { Log.e("FetchUserDataUseCase", "Failed to fetch transactions", it) }
        }

        budgetsJob.await()
        expensesJob.await()
        transactionsJob.await()

        sessionManager.setLastSyncTime(System.currentTimeMillis())
        Log.d("FetchUserDataUseCase", "Completed post-login data fetch successfully!")
    }
}

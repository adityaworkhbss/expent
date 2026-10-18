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
    suspend operator fun invoke(): Unit = coroutineScope {
        // onboardingCount says what the user has already pushed to the server:
        // 0 = nothing, 1 = categories, 2 = + accounts, 3 = + budgets, 4 = + EMIs (onboarding done)
        val onboardingStep = sessionManager.getOnboardingStep()
        Log.d("FetchUserDataUseCase", "Starting post-login data fetch for onboarding step $onboardingStep")

        if (onboardingStep <= 0) {
            Log.d("FetchUserDataUseCase", "Nothing on the server yet, skipping fetch")
            return@coroutineScope
        }

        // Cancel any in-flight sync workers to avoid concurrent writes that cause duplicates
        syncScheduler.cancelAllSync()

        // 1. Fetch base reference entities first (Categories, Accounts, Customization)
        val categoriesJob = async {
            runCatching { categoryRepository.refreshCategories() }
                .onFailure { Log.e("FetchUserDataUseCase", "Failed to fetch categories", it) }
        }
        val accountsJob = if (onboardingStep >= 2) async {
            runCatching { paymentModeRepository.refreshAccounts() }
                .onFailure { Log.e("FetchUserDataUseCase", "Failed to fetch accounts", it) }
        } else null
        val customizationJob = if (onboardingStep >= 4) async {
            runCatching { customizationRepository.refreshCustomization() }
                .onFailure { Log.e("FetchUserDataUseCase", "Failed to fetch customization", it) }
        } else null

        categoriesJob.await()
        accountsJob?.await()
        customizationJob?.await()

        // 2. Fetch dependent entities that reference categories and accounts (Budgets, Expenses, Transactions)
        val budgetsJob = if (onboardingStep >= 3) async {
            runCatching { incomeBudgetRepository.refreshBudgets() }
                .onFailure { Log.e("FetchUserDataUseCase", "Failed to fetch budgets", it) }
        } else null
        val expensesJob = if (onboardingStep >= 4) async {
            runCatching { expenseAndSubscriptionRepository.refreshExpensesAndSubscriptions() }
                .onFailure { Log.e("FetchUserDataUseCase", "Failed to fetch expenses/subscriptions", it) }
        } else null
        val transactionsJob = if (onboardingStep >= 4) async {
            runCatching { transactionRepository.refreshTransactions(1, 100) }
                .onFailure { Log.e("FetchUserDataUseCase", "Failed to fetch transactions", it) }
        } else null

        budgetsJob?.await()
        expensesJob?.await()
        transactionsJob?.await()

        sessionManager.setLastSyncTime(System.currentTimeMillis())
        Log.d("FetchUserDataUseCase", "Completed post-login data fetch successfully!")
    }
}

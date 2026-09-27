package com.aditya.expent.domain.usecase

import android.util.Log
import com.aditya.expent.domain.repository.CategoryRepository
import com.aditya.expent.domain.repository.CustomizationRepository
import com.aditya.expent.domain.repository.ExpenseAndSubscriptionRepository
import com.aditya.expent.domain.repository.IncomeBudgetRepository
import com.aditya.expent.domain.repository.PaymentModeRepository
import com.aditya.expent.domain.repository.TransactionRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class FetchUserDataUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val paymentModeRepository: PaymentModeRepository,
    private val incomeBudgetRepository: IncomeBudgetRepository,
    private val expenseAndSubscriptionRepository: ExpenseAndSubscriptionRepository,
    private val transactionRepository: TransactionRepository,
    private val customizationRepository: CustomizationRepository
) {
    suspend operator fun invoke() = coroutineScope {
        Log.d("FetchUserDataUseCase", "Starting post-login data fetch from remote API into Room DB...")
        
        val categoriesJob = async {
            runCatching { categoryRepository.refreshCategories() }
                .onFailure { Log.e("FetchUserDataUseCase", "Failed to fetch categories", it) }
        }
        val accountsJob = async {
            runCatching { paymentModeRepository.refreshAccounts() }
                .onFailure { Log.e("FetchUserDataUseCase", "Failed to fetch accounts", it) }
        }
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
        val customizationJob = async {
            runCatching { customizationRepository.refreshCustomization() }
                .onFailure { Log.e("FetchUserDataUseCase", "Failed to fetch customization", it) }
        }

        categoriesJob.await()
        accountsJob.await()
        budgetsJob.await()
        expensesJob.await()
        transactionsJob.await()
        customizationJob.await()

        Log.d("FetchUserDataUseCase", "Completed post-login data fetch successfully!")
    }
}

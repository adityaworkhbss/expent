package com.aditya.expent.presentation.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya.expent.domain.model.Transaction
import com.aditya.expent.domain.model.TransactionType
import com.aditya.expent.domain.usecase.GetTransactionUseCase
import com.aditya.expent.domain.usecase.GetCategoriesUseCase
import com.aditya.expent.domain.usecase.GetAccountsUseCase
import com.aditya.expent.domain.usecase.AddTransactionsUseCase
import com.aditya.expent.data.remote.dto.CategoryResponseDto
import com.aditya.expent.data.remote.dto.PaymentModeResponseDto
import com.aditya.expent.domain.usecase.GetCustomizationUseCase
import com.aditya.expent.domain.usecase.ParseTransactionUseCase
import com.aditya.expent.domain.usecase.UpdateCustomizationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class DashboardState(
    val totalBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val recentTransactions: List<Transaction> = emptyList(),
    val categories: List<CategoryResponseDto> = emptyList(),
    val accounts: List<PaymentModeResponseDto> = emptyList(),
    val userName: String = "User",
    val aiTransaction: Boolean = false,
    val reminder: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getTransactionUseCase: GetTransactionUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val addTransactionsUseCase: AddTransactionsUseCase,
    private val getCustomizationUseCase: GetCustomizationUseCase,
    private val parseTransactionUseCase: ParseTransactionUseCase,
    private val updateCustomizationUseCase: UpdateCustomizationUseCase
) : ViewModel() {
    
    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        loadTransactions()
        loadCategories()
        loadAccounts()
        loadCustomization()
    }

    fun loadCustomization(){
        Log.d("DashboardViewModel", "loadCustomization: Calling getCustomizationUseCase")
        viewModelScope.launch {
            getCustomizationUseCase().onSuccess { customization ->
                Log.d("DashboardViewModel", "loadCustomization SUCCESS: retrieved customization -> aiTransaction = ${customization.aiTransaction}, reminder = ${customization.reminder}")
                _state.value = _state.value.copy(
                    aiTransaction = customization.aiTransaction,
                    reminder = customization.reminder
                )
            }.onFailure {
                Log.e("DashboardViewModel", "loadCustomization FAILURE: unable to load customization from repository", it)
                // Keep default on error
            }
        }
    }

    fun loadTransactions() {
        viewModelScope.launch {
            val endDate = LocalDate.now()
            val startDate = endDate.minusDays(7)

            val result = getTransactionUseCase(
                startDate.toString(),
                endDate.toString()
            )

            result.onSuccess { paginatedResponse ->
                val transactions = paginatedResponse.data.map { dto ->
                    val typeEnum = if (dto.type.uppercase() == "INCOME") TransactionType.INCOME else TransactionType.EXPENSE
                    val rawAmount = dto.amount.toDoubleOrNull() ?: 0.0
                    val amount = if (typeEnum == TransactionType.EXPENSE) -Math.abs(rawAmount) else Math.abs(rawAmount)

                    Transaction(
                        id = dto.id,
                        title = dto.note ?: dto.merchant ?: "Transaction",
                        amount = amount,
                        date = dto.transactionDate,
                        category = dto.category?.name ?: "Other",
                        type = typeEnum,
                        accountId = dto.accountId,
                        categoryId = dto.categoryId,
                        paymentMethod = dto.paymentMethod ?: dto.account?.name
                    )
                }
                updateStateWithList(transactions)
            }.onFailure {
                updateStateWithList(emptyList())
            }
        }
    }

    fun loadCategories() {
        viewModelScope.launch {
            getCategoriesUseCase().onSuccess { categoryList ->
                _state.value = _state.value.copy(categories = categoryList)
            }.onFailure {
                // Keep default empty on error
            }
        }
    }

    fun loadAccounts() {
        viewModelScope.launch {
            getAccountsUseCase().onSuccess { accountList ->
                _state.value = _state.value.copy(accounts = accountList)
            }.onFailure {
                // Keep default empty on error
            }
        }
    }

    fun addTransaction(title: String, amount: Double, category: String, type: TransactionType, date: String, accountName: String, accountId: String) {
        viewModelScope.launch {
            val categoryId = _state.value.categories.firstOrNull { it.name.equals(category, ignoreCase = true) }?.id

            val newTransaction = Transaction(
                id = java.util.UUID.randomUUID().toString(),
                title = title,
                amount = if (type == TransactionType.EXPENSE) -Math.abs(amount) else Math.abs(amount),
                date = date,
                category = category,
                type = type,
                accountId = accountId,
                categoryId = categoryId,
                paymentMethod = accountName
            )
            
            // Optimistic local update
            val updatedList = listOf(newTransaction) + _state.value.recentTransactions
            updateStateWithList(updatedList)

            // Save to server
            addTransactionsUseCase(newTransaction)
            
            // Refresh list from server to get accurate data
            loadTransactions()
        }
    }

    fun addAiTransaction(rawText: String) {
        viewModelScope.launch {
            if (!_state.value.aiTransaction) {
                Log.d("DashboardViewModel", "AI transaction parsing is disabled. Enabling it via updateCustomizationUseCase.")
                updateCustomizationUseCase(aiTransaction = true, reminder = _state.value.reminder).onSuccess {
                    Log.d("DashboardViewModel", "Successfully enabled AI transactions on server.")
                    _state.value = _state.value.copy(aiTransaction = true)
                }.onFailure { error ->
                    Log.e("DashboardViewModel", "Failed to enable AI transactions on server", error)
                }
            }

            val result = parseTransactionUseCase(rawText)
            result.onSuccess { response ->
                if (response.success && !response.requiresUserInput && response.data != null) {
                    val data = response.data
                    val amount = data.amount
                    val type = if (data.transactionType.uppercase() == "INCOME") TransactionType.INCOME else TransactionType.EXPENSE
                    val categoryName = data.categoryName ?: "Others"
                    val categoryId = data.categoryId ?: _state.value.categories.firstOrNull { it.name.equals(categoryName, ignoreCase = true) }?.id ?: _state.value.categories.firstOrNull { it.name == "Others" }?.id
                    
                    val accountId = data.accountId ?: _state.value.accounts.firstOrNull()?.id ?: "0"
                    val accountName = data.accountName ?: data.paymentMethod ?: _state.value.accounts.firstOrNull { it.id == accountId }?.name ?: "Cash"

                    val newTransaction = Transaction(
                        id = java.util.UUID.randomUUID().toString(),
                        title = data.note ?: data.merchant ?: rawText,
                        amount = if (type == TransactionType.EXPENSE) -Math.abs(amount) else Math.abs(amount),
                        date = data.date,
                        category = categoryName,
                        type = type,
                        accountId = accountId,
                        categoryId = categoryId,
                        paymentMethod = accountName
                    )

                    // Save transaction to server
                    addTransactionsUseCase(newTransaction)

                    // Refresh transactions
                    loadTransactions()
                } else {
                    Log.e("DashboardViewModel", "AI parse failed or requires user input: $response")
                }
            }.onFailure { error ->
                Log.e("DashboardViewModel", "Failed to parse AI transaction", error)
            }
        }
    }

    fun deleteTransaction(id: String) {
        val updatedList = _state.value.recentTransactions.filter { it.id != id }
        updateStateWithList(updatedList)
    }

    private fun updateStateWithList(list: List<Transaction>) {
        var income = 0.0
        var expense = 0.0
        list.forEach {
            if (it.type == TransactionType.INCOME) {
                income += Math.abs(it.amount)
            } else {
                expense += Math.abs(it.amount)
            }
        }
        val balance = income - expense

        _state.value = _state.value.copy(
            recentTransactions = list,
            totalBalance = balance,
            totalIncome = income,
            totalExpense = expense
        )
    }
}

package com.aditya.expent.presentation.dashboard

import android.os.Build
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
import com.aditya.expent.data.sync.SyncScheduler
import com.aditya.expent.utils.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDate.now
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
    val reminder: Boolean = false,
    val greetingMessage: String = "Welcome",
    val isLoading: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getTransactionUseCase: GetTransactionUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val addTransactionsUseCase: AddTransactionsUseCase,
    private val getCustomizationUseCase: GetCustomizationUseCase,
    private val parseTransactionUseCase: ParseTransactionUseCase,
    private val updateCustomizationUseCase: UpdateCustomizationUseCase,
    private val sessionManager: SessionManager,
    private val syncScheduler: SyncScheduler
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        syncScheduler.scheduleInitialSync()
        loadTransactions()
        loadCategories()
        loadAccounts()
        loadCustomization()
        loadInfos()
    }

    fun loadInfos() {
        _state.value = _state.value.copy(userName = sessionManager.getUser()?.name ?: "User")
        val hour = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            now().atStartOfDay().hour
        } else {
            0
        }
        val greeting = when (hour) {
            in 5..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..21 -> "Good Evening"
            else -> "Welcome"
        }
        _state.value = _state.value.copy(greetingMessage = greeting)
    }

    fun loadCustomization() {
        Log.d("DashboardViewModel", "loadCustomization: Calling getCustomizationUseCase")
        viewModelScope.launch {
            getCustomizationUseCase()
                .catch { Log.e("DashboardViewModel", "loadCustomization FAILURE", it) }
                .collect { customization ->
                    Log.d("DashboardViewModel", "loadCustomization SUCCESS: aiTransaction=${customization.aiTransaction}")
                    _state.value = _state.value.copy(
                        aiTransaction = customization.aiTransaction,
                        reminder = customization.reminder
                    )
                }
        }
    }

    fun loadTransactions() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val endDate = now()
            val startDate = endDate.minusDays(7)

            getTransactionUseCase(startDate.toString(), endDate.toString())
                .catch {
                    updateStateWithList(emptyList())
                    _state.value = _state.value.copy(isLoading = false)
                }
                .collect { paginatedResponse ->
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
                    _state.value = _state.value.copy(isLoading = false)
                }
        }
    }

    fun loadCategories() {
        viewModelScope.launch {
            getCategoriesUseCase()
                .catch { /* Keep default empty on error */ }
                .collect { categoryList ->
                    val dtoList = categoryList.map { cat ->
                        CategoryResponseDto(
                            id = cat.id ?: "",
                            name = cat.name,
                            type = cat.type
                        )
                    }
                    _state.value = _state.value.copy(categories = dtoList)
                }
        }
    }

    fun loadAccounts() {
        viewModelScope.launch {
            getAccountsUseCase()
                .catch { /* Keep default empty on error */ }
                .collect { accountList ->
                    _state.value = _state.value.copy(accounts = accountList)
                }
        }
    }

    fun addTransaction(title: String, amount: Double, category: String, type: TransactionType, date: String, accountName: String, accountId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
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

            // Save to DB (offline-first)
            addTransactionsUseCase(newTransaction)

            _state.value = _state.value.copy(isLoading = false)
        }
    }

    fun addAiTransaction(rawText: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            if (!_state.value.aiTransaction) {
                Log.d("DashboardViewModel", "AI transaction parsing is disabled. Enabling it.")
                runCatching {
                    updateCustomizationUseCase(aiTransaction = true, reminder = _state.value.reminder)
                    _state.value = _state.value.copy(aiTransaction = true)
                }.onFailure { Log.e("DashboardViewModel", "Failed to enable AI transactions", it) }
            }

            runCatching {
                val response = parseTransactionUseCase(rawText)
                if (response.success && !response.requiresUserInput && response.data != null) {
                    val data = response.data
                    val amount = data.amount
                    val type = if (data.resolvedTransactionType.uppercase() == "INCOME") TransactionType.INCOME else TransactionType.EXPENSE
                    val categoryName = data.resolvedCategoryName
                    val categoryId = data.categoryId
                        ?: _state.value.categories.firstOrNull { it.name.equals(categoryName, ignoreCase = true) }?.id
                        ?: _state.value.categories.firstOrNull { it.name == "Others" }?.id

                    val accountId = data.accountId ?: _state.value.accounts.firstOrNull()?.id ?: "0"
                    val accountName = data.accountName ?: data.paymentMethod
                        ?: _state.value.accounts.firstOrNull { it.id == accountId }?.name ?: "Cash"

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

                    addTransactionsUseCase(newTransaction)
                    _state.value = _state.value.copy(isLoading = false)
                } else {
                    Log.e("DashboardViewModel", "AI parse failed or requires user input: $response")
                    _state.value = _state.value.copy(isLoading = false)
                }
            }.onFailure { error ->
                Log.e("DashboardViewModel", "Failed to parse AI transaction", error)
                _state.value = _state.value.copy(isLoading = false)
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

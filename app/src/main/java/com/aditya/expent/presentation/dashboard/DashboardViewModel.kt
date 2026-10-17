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
import com.aditya.expent.data.remote.dto.BudgetResponseDto
import com.aditya.expent.data.remote.dto.CategoryResponseDto
import com.aditya.expent.domain.usecase.GetBudgetUseCase
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
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDate.now
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

data class DashboardState(
    val totalBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val recentTransactions: List<Transaction> = emptyList(),
    val categories: List<CategoryResponseDto> = emptyList(),
    val accounts: List<PaymentModeResponseDto> = emptyList(),
    val incomeSources: List<BudgetResponseDto> = emptyList(),
    val userName: String = "User",
    val aiTransaction: Boolean = false,
    val reminder: Boolean = false,
    val greetingMessage: String = "Welcome",
    val isLoading: Boolean = false
) {
    // Income amount per category name, taken from the income sources set up in Cashflow
    val incomeAmountByCategory: Map<String, Double>
        get() = incomeSources
            .mapNotNull { source ->
                val name = source.category?.name?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                    ?: categories.firstOrNull { it.id == source.categoryId }?.name
                name?.let { it to source.limitAmount }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, amounts) -> amounts.sum() }
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getTransactionUseCase: GetTransactionUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val getBudgetUseCase: GetBudgetUseCase,
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
        loadTransactions()
        loadCategories()
        loadAccounts()
        loadIncomeSources()
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
                .catch {
                    /* Keep default empty on error */
                }
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

    fun loadIncomeSources() {
        viewModelScope.launch {
            getBudgetUseCase()
                .catch { /* Keep default empty on error */ }
                .collect { budgets ->
                    _state.value = _state.value.copy(incomeSources = budgets)
                }
        }
    }

    fun addTransaction(title: String, amount: Double, category: String, type: TransactionType, date: String, accountName: String, accountId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val categoryId = _state.value.categories.firstOrNull { it.name.equals(category, ignoreCase = true) }?.id

            val newTransaction = Transaction(
                id = "local-${java.util.UUID.randomUUID()}",
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
                    val type = if (data.resolvedTransactionType.equals("INCOME", ignoreCase = true)) TransactionType.INCOME else TransactionType.EXPENSE

                    // Match the parsed category against the user's categories of the same type
                    val typeCategories = _state.value.categories.filter { it.type.equals(type.name, ignoreCase = true) }
                    val matchedCategory = data.categoryId?.let { id -> _state.value.categories.firstOrNull { it.id == id } }
                        ?: typeCategories.firstOrNull { it.name.equals(data.resolvedCategoryName, ignoreCase = true) }
                        ?: typeCategories.firstOrNull { it.name.equals("Others", ignoreCase = true) }
                    val categoryName = matchedCategory?.name ?: data.resolvedCategoryName
                    val categoryId = matchedCategory?.id

                    // Income only goes to PAY_NOW accounts, same as the manual form
                    val eligibleAccounts = if (type == TransactionType.INCOME) {
                        _state.value.accounts.filter { it.type.equals("PAY_NOW", ignoreCase = true) }
                    } else {
                        _state.value.accounts
                    }
                    val account = data.accountId?.let { id -> _state.value.accounts.firstOrNull { it.id == id } }
                        ?: data.accountName?.let { name -> _state.value.accounts.firstOrNull { it.name.equals(name, ignoreCase = true) } }
                        ?: eligibleAccounts.firstOrNull()
                        ?: _state.value.accounts.firstOrNull()
                    if (account == null) {
                        Log.e("DashboardViewModel", "No account available to store AI transaction")
                        _state.value = _state.value.copy(isLoading = false)
                        return@runCatching
                    }

                    val newTransaction = Transaction(
                        id = "local-${java.util.UUID.randomUUID()}",
                        title = data.resolvedNote.ifBlank { rawText },
                        amount = if (type == TransactionType.EXPENSE) -Math.abs(amount) else Math.abs(amount),
                        date = resolveParsedDate(data.date),
                        category = categoryName,
                        type = type,
                        accountId = account.id,
                        categoryId = categoryId,
                        paymentMethod = data.paymentMethod ?: account.name
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

    // Parsed date may be blank, yyyy-MM-dd, ISO or dd/MM/yyyy; blank, unparseable or future dates become today
    private fun resolveParsedDate(raw: String?): String {
        val output = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val today = Date()
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) return output.format(today)

        val parsed = listOf("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd", "dd/MM/yyyy")
            .firstNotNullOfOrNull { pattern ->
                runCatching {
                    SimpleDateFormat(pattern, Locale.US).apply {
                        isLenient = false
                        if (pattern.endsWith("'Z'")) timeZone = TimeZone.getTimeZone("UTC")
                    }.parse(trimmed)
                }.getOrNull()
            }
        return output.format(if (parsed == null || parsed.after(today)) today else parsed)
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
            recentTransactions = list.distinctBy { it.id },
            totalBalance = balance,
            totalIncome = income,
            totalExpense = expense
        )
    }
}

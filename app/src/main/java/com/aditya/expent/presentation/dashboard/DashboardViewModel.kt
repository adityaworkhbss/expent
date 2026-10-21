package com.aditya.expent.presentation.dashboard

import android.os.Build
import android.util.Log
import com.aditya.expent.config.AppText
import com.aditya.expent.utils.AppLogger
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya.expent.domain.model.Transaction
import com.aditya.expent.domain.model.TransactionType
import com.aditya.expent.domain.usecase.GetTransactionUseCase
import com.aditya.expent.domain.usecase.GetCategoriesUseCase
import com.aditya.expent.domain.usecase.GetAccountsUseCase
import com.aditya.expent.domain.usecase.AddTransactionsUseCase
import com.aditya.expent.domain.model.Budget
import com.aditya.expent.domain.model.Expense
import com.aditya.expent.domain.model.Reminder
import com.aditya.expent.domain.usecase.DeleteEmiUseCase
import com.aditya.expent.domain.usecase.GetExpensesAndSubscriptionUseCase
import com.aditya.expent.domain.usecase.ReminderCalculator
import com.aditya.expent.domain.usecase.UpdateEmiUseCase
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
    val incomeSources: List<Budget> = emptyList(),
    val userName: String = AppText.get("dashboard.header.defaultUserName", "User"),
    val aiTransaction: Boolean = false,
    val reminder: Boolean = false,
    val greetingMessage: String = AppText.get("dashboard.header.greetings.default", "Welcome"),
    val isLoading: Boolean = false,
    val aiMessage: String? = null,
    val reminders: List<Reminder> = emptyList(),
    val completedEmiName: String? = null
) {
    val incomeAmountByCategory: Map<String, Double>
        get() = incomeSources
            .mapNotNull { source ->
                val name = source.categoryName?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                    ?: categories.firstOrNull { it.id == source.categoryId }?.name
                name?.let { it to source.amount }
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
    private val getExpensesAndSubscriptionUseCase: GetExpensesAndSubscriptionUseCase,
    private val updateEmiUseCase: UpdateEmiUseCase,
    private val deleteEmiUseCase: DeleteEmiUseCase,
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
        loadExpenses()
        loadCustomization()
        loadInfos()
    }

    fun loadInfos() {
        _state.value = _state.value.copy(userName = sessionManager.getUser()?.name ?: AppText.get("dashboard.header.defaultUserName", "User"))
        val hour = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            now().atStartOfDay().hour
        } else {
            0
        }
        val greeting = when (hour) {
            in 5..11 -> AppText.get("dashboard.header.greetings.morning", "Good Morning")
            in 12..16 -> AppText.get("dashboard.header.greetings.afternoon", "Good Afternoon")
            in 17..21 -> AppText.get("dashboard.header.greetings.evening", "Good Evening")
            else -> AppText.get("dashboard.header.greetings.default", "Welcome")
        }
        _state.value = _state.value.copy(greetingMessage = greeting)
    }

    fun loadCustomization() {
        Log.d("DashboardViewModel", "loadCustomization: Calling getCustomizationUseCase")
        viewModelScope.launch {
            getCustomizationUseCase()
                .catch { AppLogger.error("DashboardViewModel", "loadCustomization FAILURE", it) }
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
                            title = dto.category?.name ?: "Transaction",
                            amount = amount,
                            date = dto.date.orEmpty(),
                            category = dto.category?.name ?: "Other",
                            type = typeEnum,
                            accountId = dto.accountId,
                            categoryId = dto.categoryId,
                            accountName = dto.account?.name
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
                    refreshReminders()
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
                    refreshReminders()
                }
        }
    }

    // Latest recurring outgoings from Room; only used to build reminders
    private var expenses: List<Expense> = emptyList()

    private fun loadExpenses() {
        viewModelScope.launch {
            getExpensesAndSubscriptionUseCase()
                .catch { /* Keep default empty on error */ }
                .collect {
                    expenses = it
                    refreshReminders()
                }
        }
    }

    private fun refreshReminders() {
        val categoryNames = _state.value.categories.associate { it.id to it.name }
        val reminders = ReminderCalculator.upcoming(
            budgets = _state.value.incomeSources,
            expenses = expenses,
            categoryNames = categoryNames,
            isDone = sessionManager::isReminderDone
        )
        _state.value = _state.value.copy(reminders = reminders)
    }

    /**
     * Records the reminder as a transaction and hides it for this cycle.
     * For an EMI this also counts the month as paid; paying the last month removes the EMI.
     */
    fun completeReminder(reminder: Reminder, amount: Double, accountId: String, accountName: String) {
        addTransaction(
            title = reminder.category,
            amount = amount,
            category = reminder.category,
            type = reminder.type,
            date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()),
            accountName = accountName,
            accountId = accountId,
            categoryId = reminder.categoryId
        )
        sessionManager.markReminderDone(reminder.sourceId, reminder.cycle)

        val emi = expenses.firstOrNull { it.id == reminder.sourceId }
        if (reminder.kind == Reminder.Kind.EMI && emi?.tenure != null) {
            viewModelScope.launch {
                val newPaid = emi.paid + 1
                runCatching {
                    if (newPaid >= emi.tenure) {
                        deleteEmiUseCase(emi.id)
                        _state.value = _state.value.copy(completedEmiName = emi.name)
                    } else {
                        updateEmiUseCase(emi.id, emi.name, emi.amount, emi.date, emi.categoryId, emi.tenure, newPaid)
                    }
                }.onFailure { AppLogger.error("DashboardViewModel", "Failed to update EMI after payment", it) }
            }
        }
        refreshReminders()
    }

    fun clearCompletedEmi() {
        _state.value = _state.value.copy(completedEmiName = null)
    }

    fun addTransaction(title: String, amount: Double, category: String, type: TransactionType, date: String, accountName: String, accountId: String, categoryId: String? = null) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val resolvedCategoryId = categoryId
                ?: _state.value.categories.firstOrNull { it.name.equals(category, ignoreCase = true) }?.id

            val newTransaction = Transaction(
                id = "local-${java.util.UUID.randomUUID()}",
                title = title,
                amount = if (type == TransactionType.EXPENSE) -Math.abs(amount) else Math.abs(amount),
                date = date,
                category = category,
                type = type,
                accountId = accountId,
                categoryId = resolvedCategoryId,
                accountName = accountName
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
            // Never switch the user's AI setting back on from here; Profile owns it

            runCatching {
                val response = parseTransactionUseCase(rawText)
                val data = response.data
                if (!response.success || data == null) {
                    AppLogger.error("DashboardViewModel", "AI parse failed: $response")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        aiMessage = response.message ?: AppText.get("dashboard.addTransactionSheet.aiMessages.parseFailed", "Couldn't understand that transaction. Please try again.")
                    )
                    return@runCatching
                }
                if (response.requiresUserInput) {
                    val missing = response.missingRequiredFields
                        .joinToString(AppText.get("dashboard.addTransactionSheet.aiMessages.missingFieldsJoiner", " and "))
                        .ifBlank { AppText.get("dashboard.addTransactionSheet.aiMessages.missingFieldsFallback", "some details") }
                    _state.value = _state.value.copy(
                        isLoading = false,
                        aiMessage = AppText.get(
                            "dashboard.addTransactionSheet.aiMessages.missingFields",
                            "Couldn't find the {missing}. Try e.g. \"Spent 250 on Uber today\".",
                            "missing" to missing
                        )
                    )
                    return@runCatching
                }

                val type = if ((data.type ?: "EXPENSE").equals("INCOME", ignoreCase = true)) TransactionType.INCOME else TransactionType.EXPENSE

                // Backend returns the user's real category/account IDs; local lookups are only fallbacks
                val localCategory = data.categoryId?.let { id -> _state.value.categories.firstOrNull { it.id == id } }
                    ?: _state.value.categories.firstOrNull {
                        it.type.equals(type.name, ignoreCase = true) && it.name.equals(data.resolvedCategoryName, ignoreCase = true)
                    }
                val categoryId = data.categoryId ?: localCategory?.id
                val categoryName = data.categoryName ?: localCategory?.name ?: data.resolvedCategoryName

                // account_id is null when the backend couldn't match an account; fall back like the manual form
                // (income goes to PAY_NOW accounts)
                val fallbackAccount = if (type == TransactionType.INCOME) {
                    _state.value.accounts.firstOrNull { it.type.equals("PAY_NOW", ignoreCase = true) }
                } else {
                    null
                } ?: _state.value.accounts.firstOrNull()
                val accountId = data.accountId ?: fallbackAccount?.id
                if (accountId == null) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        aiMessage = AppText.get("dashboard.addTransactionSheet.aiMessages.noPaymentMode", "Add a payment mode in Profile before adding transactions.")
                    )
                    return@runCatching
                }
                val accountName = data.accountName
                    ?: _state.value.accounts.firstOrNull { it.id == accountId }?.name

                val newTransaction = Transaction(
                    id = "local-${java.util.UUID.randomUUID()}",
                    title = categoryName,
                    amount = if (type == TransactionType.EXPENSE) -Math.abs(data.amount) else Math.abs(data.amount),
                    date = resolveParsedDate(data.date),
                    category = categoryName,
                    type = type,
                    accountId = accountId,
                    categoryId = categoryId,
                    accountName = accountName
                )

                // Saved to Room first (PENDING_CREATE), then the sync worker posts it to /transactions
                addTransactionsUseCase(newTransaction)
                _state.value = _state.value.copy(isLoading = false, aiMessage = AppText.get("dashboard.addTransactionSheet.aiMessages.added", "Added: {title}", "title" to newTransaction.title))
            }.onFailure { error ->
                AppLogger.error("DashboardViewModel", "Failed to parse AI transaction", error)
                _state.value = _state.value.copy(
                    isLoading = false,
                    aiMessage = AppText.get("dashboard.addTransactionSheet.aiMessages.networkError", "Couldn't reach the server to read that. Check your connection and try again.")
                )
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

    fun clearAiMessage() {
        _state.value = _state.value.copy(aiMessage = null)
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

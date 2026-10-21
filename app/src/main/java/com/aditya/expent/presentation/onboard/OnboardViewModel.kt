package com.aditya.expent.presentation.onboard

import android.util.Log
import com.aditya.expent.config.AppText
import com.aditya.expent.utils.AppLogger
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya.expent.domain.model.OnboardCategory
import com.aditya.expent.domain.model.OnboardPaymentMode
import com.aditya.expent.data.sync.SyncScheduler
import com.aditya.expent.domain.usecase.GetCategoriesUseCase
import com.aditya.expent.domain.usecase.SaveCategoriesUseCase
import com.aditya.expent.domain.usecase.SaveExpensesAndSubscriptionsUseCase
import com.aditya.expent.domain.usecase.SaveIncomeBudgetUseCase
import com.aditya.expent.domain.usecase.SavePaymentModesUseCase
import com.aditya.expent.domain.usecase.UpdateOnboardingCountUseCase
import com.aditya.expent.utils.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class OnboardStep(val index: Int) {
    WELCOME(-1),
    CATEGORIES(1),
    PAYMENT_MODES(2),
    INCOMING(3),
    OUTGOING(4),
    FINISH(-1)
}

// Form models for recurring monthly items. `date` is the day of the month (1-31), null until picked.

data class RecurringExpense(
    val name: String,
    val amount: String,
    val totalMonths: String,
    val monthsPaid: String,
    val date: Int?,
    val id: String? = null,
    val categoryId: String? = null
)

data class Subscription(
    val name: String,
    val amount: String,
    val date: Int?,
    val id: String? = null,
    val categoryId: String? = null
)

data class RecurringIncome(
    val name: String = "",
    val amount: String = "",
    val categoryId: String? = null,
    val date: Int? = null
)

data class OnboardState(
    val currentStep: OnboardStep = OnboardStep.WELCOME,
    val selectedCategories: List<OnboardCategory> = emptyList(),
    val salary: RecurringIncome = RecurringIncome(name = "Salary"),
    val customIncomes: List<RecurringIncome> = emptyList(),
    val creditCardBill: String = "",
    val nextMonthPendingPayment: String = "",
    val recurringExpenses: List<RecurringExpense> = emptyList(),
    val subscriptions: List<Subscription> = emptyList(),
    val paymentModes: List<OnboardPaymentMode> = emptyList(),
    val availableCategories: List<OnboardCategory> = listOf(
        OnboardCategory("Food & Dining", "EXPENSE"),
        OnboardCategory("Transportation", "EXPENSE"),
        OnboardCategory("Shopping", "EXPENSE"),
        OnboardCategory("Entertainment", "EXPENSE"),
        OnboardCategory("Bills & Utilities", "EXPENSE"),
        OnboardCategory("Groceries", "EXPENSE"),
        OnboardCategory("Healthcare", "EXPENSE"),
        OnboardCategory("Education", "EXPENSE"),
        OnboardCategory("Travel", "EXPENSE"),
        OnboardCategory("Housing", "EXPENSE"),
        OnboardCategory("Salary", "INCOME"),
        OnboardCategory("Freelance", "INCOME"),
        OnboardCategory("Investments", "INCOME"),
        OnboardCategory("Gifts", "INCOME"),
        OnboardCategory("Business Profits", "INCOME")
    ),
    // Categories as stored in Room (what the user actually saved), unlike the suggestion list above
    val savedCategories: List<OnboardCategory> = emptyList(),
    val hasFetchedCategories: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class OnboardViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val saveCategoriesUseCase: SaveCategoriesUseCase,
    private val savePaymentModesUseCase: SavePaymentModesUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val saveIncomeBudgetUseCase: SaveIncomeBudgetUseCase,
    private val saveExpensesAndSubscriptionsUseCase: SaveExpensesAndSubscriptionsUseCase,
    private val updateOnboardingCountUseCase: UpdateOnboardingCountUseCase,
    private val syncScheduler: SyncScheduler
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardState())
    val state: StateFlow<OnboardState> = _state.asStateFlow()

    init {
        val savedStep = sessionManager.getOnboardingStep()
        Log.d("OnboardVM", "Saved onboarding step from session: $savedStep")
        val initialStep = when (savedStep) {
            0 -> OnboardStep.WELCOME
            1 -> OnboardStep.PAYMENT_MODES   // categories done
            2 -> OnboardStep.INCOMING        // payment modes done
            3 -> OnboardStep.OUTGOING        // incoming done
            4 -> OnboardStep.FINISH          // all done
            else -> OnboardStep.WELCOME
        }
        _state.update { it.copy(currentStep = initialStep) }
        // Observe Room only (no API call); login already fetched server categories when onboardingCount >= 1
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            getCategoriesUseCase()
                .catch { AppLogger.error("OnboardVM", "Failed to load categories", it) }
                .collect { fetchedCategories ->
                    _state.update { s ->
                        val currentAvailable = s.availableCategories
                        val merged = (fetchedCategories + currentAvailable)
                            .distinctBy { cat -> "${cat.name.lowercase()}-${cat.type.uppercase()}" }
                        s.copy(
                            availableCategories = merged,
                            savedCategories = fetchedCategories,
                            selectedCategories = if (s.selectedCategories.isEmpty() && fetchedCategories.isNotEmpty()) {
                                fetchedCategories
                            } else if (s.selectedCategories.isNotEmpty()) {
                                s.selectedCategories.map { sel ->
                                    fetchedCategories.find {
                                        it.name.equals(sel.name, ignoreCase = true) &&
                                        it.type.equals(sel.type, ignoreCase = true)
                                    } ?: sel
                                }
                            } else {
                                s.selectedCategories
                            },
                            isLoading = false,
                            hasFetchedCategories = true
                        )
                    }
                }
        }
    }

    private suspend fun saveCategories(): Boolean {
        _state.update { it.copy(isLoading = true, error = null) }
        return runCatching { saveCategoriesUseCase(state.value.selectedCategories) }
            .onSuccess {
                _state.update { it.copy(isLoading = false) }
                updateOnboardingStep()
            }
            .onFailure {
                _state.update { it.copy(isLoading = false, error = AppText.get("onboarding.errors.saveCategoriesFailed", "Failed to save categories")) }
            }
            .isSuccess
    }

    private suspend fun savePaymentModes(): Boolean {
        _state.update { it.copy(isLoading = true, error = null) }
        return runCatching { savePaymentModesUseCase(state.value.paymentModes) }
            .onSuccess {
                _state.update { it.copy(isLoading = false) }
                updateOnboardingStep()
            }
            .onFailure {
                _state.update { it.copy(isLoading = false, error = AppText.get("onboarding.errors.savePaymentMethodsFailed", "Failed to save payment methods")) }
            }
            .isSuccess
    }

    private suspend fun saveIncomeBudget(): Boolean {
        _state.update { it.copy(isLoading = true, error = null) }
        return runCatching {
            saveIncomeBudgetUseCase(
                salary = state.value.salary,
                customIncomes = state.value.customIncomes
            )
        }
            .onSuccess {
                _state.update { it.copy(isLoading = false) }
                updateOnboardingStep()
            }
            .onFailure {
                _state.update { it.copy(isLoading = false, error = AppText.get("onboarding.errors.saveIncomeFailed", "Failed to save income and budget")) }
            }
            .isSuccess
    }

    private suspend fun saveExpensesAndSubscriptions(): Boolean {
        _state.update { it.copy(isLoading = true, error = null) }
        return runCatching {
            saveExpensesAndSubscriptionsUseCase(
                expenses = state.value.recurringExpenses,
                subscriptions = state.value.subscriptions
            )
        }
            .onSuccess {
                _state.update { it.copy(isLoading = false) }
                updateOnboardingStep()
            }
            .onFailure {
                _state.update { it.copy(isLoading = false, error = AppText.get("onboarding.errors.saveExpensesFailed", "Failed to save expenses and subscriptions")) }
            }
            .isSuccess
    }

    fun nextStep() {
        viewModelScope.launch {
            val currentState = state.value

            val canProceed = when (currentState.currentStep) {
                OnboardStep.CATEGORIES -> saveCategories()
                OnboardStep.PAYMENT_MODES -> savePaymentModes()
                OnboardStep.INCOMING -> saveIncomeBudget()
                OnboardStep.OUTGOING -> saveExpensesAndSubscriptions()
                else -> true
            }

            if (canProceed) {
                val next = when (currentState.currentStep) {
                    OnboardStep.WELCOME -> {
                        val savedStep = sessionManager.getOnboardingStep()
                        when (savedStep) {
                            0 -> OnboardStep.CATEGORIES
                            1 -> OnboardStep.PAYMENT_MODES
                            2 -> OnboardStep.INCOMING
                            3 -> OnboardStep.OUTGOING
                            4 -> OnboardStep.FINISH
                            else -> OnboardStep.CATEGORIES
                        }
                    }
                    OnboardStep.CATEGORIES -> OnboardStep.PAYMENT_MODES
                    OnboardStep.PAYMENT_MODES -> OnboardStep.INCOMING
                    OnboardStep.INCOMING -> OnboardStep.OUTGOING
                    OnboardStep.OUTGOING -> OnboardStep.FINISH
                    OnboardStep.FINISH -> OnboardStep.FINISH
                }
                if (next == OnboardStep.FINISH) {
                    // Every step already pushed its data and Room holds it, so no full refresh here
                    syncScheduler.schedulePeriodicSync()
                }
                _state.update { it.copy(currentStep = next) }
            }
        }
    }

    suspend fun updateOnboardingStep() {
        val nextStep = sessionManager.getOnboardingStep() + 1
        // Persist locally first; the API call falls back to the pending sync queue when offline
        sessionManager.setOnboardingStep(nextStep)
        updateOnboardingCountUseCase(nextStep)
    }

    fun onCategoriesSelected(categories: List<OnboardCategory>) {
        _state.update { it.copy(selectedCategories = categories) }
    }

    fun onSalaryAmountChanged(amount: String) {
        _state.update { it.copy(salary = it.salary.copy(amount = amount)) }
    }

    fun onSalaryCategoryChanged(categoryId: String?) {
        _state.update { it.copy(salary = it.salary.copy(categoryId = categoryId)) }
    }

    fun onSalaryDateChanged(day: Int) {
        _state.update { it.copy(salary = it.salary.copy(date = day)) }
    }

    fun onCustomIncomesChanged(incomes: List<RecurringIncome>) {
        _state.update { it.copy(customIncomes = incomes) }
    }

    fun onCreditCardBillChanged(bill: String) {
        _state.update { it.copy(creditCardBill = bill) }
    }

    fun onNextMonthPendingPaymentChanged(amount: String) {
        _state.update { it.copy(nextMonthPendingPayment = amount) }
    }

    fun onRecurringExpensesChanged(expenses: List<RecurringExpense>) {
        _state.update { it.copy(recurringExpenses = expenses) }
    }

    fun onSubscriptionsChanged(subscriptions: List<Subscription>) {
        _state.update { it.copy(subscriptions = subscriptions) }
    }

    fun onPaymentModesChanged(modes: List<OnboardPaymentMode>) {
        _state.update { it.copy(paymentModes = modes) }
    }

}

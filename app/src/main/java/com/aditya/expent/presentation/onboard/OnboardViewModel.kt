package com.aditya.expent.presentation.onboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya.expent.domain.model.OnboardCategory
import com.aditya.expent.domain.model.OnboardPaymentMode
import com.aditya.expent.domain.usecase.GetCategoriesUseCase
import com.aditya.expent.domain.usecase.SaveCategoriesUseCase
import com.aditya.expent.domain.usecase.SaveBudgetUseCase
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

data class RecurringExpense(
    val name: String,
    val amount: String,
    val totalMonths: String,
    val monthsPaid: String,
    val startDate: String
)

data class Subscription(
    val name: String,
    val amount: String,
    val billingDate: String,
    val id: String? = null
)

data class RecurringIncome(
    val name: String = "",
    val amount: String = "",
    val categoryId: String? = null,
    val periodType: String = "MONTHLY",
    val startDate: String = "",
    val endDate: String = ""
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
    val budgetLimit: String = "",
    val budgetPeriod: String = "MONTHLY",
    val budgetCategoryId: String? = null,
    val budgetStartDate: String = "",
    val budgetEndDate: String = "",
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
    private val updateOnboardingCountUseCase: UpdateOnboardingCountUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardState())
    val state: StateFlow<OnboardState> = _state.asStateFlow()

    init {
        val savedStep = sessionManager.getOnboardingStep()
        Log.d("OnboardVM", "Saved onboarding step from session: $savedStep")
        if (savedStep > 0) {
            val initialStep = when (savedStep) {
                1 -> OnboardStep.PAYMENT_MODES
                2 -> OnboardStep.INCOMING
                3 -> OnboardStep.OUTGOING
                4 -> OnboardStep.FINISH
                else -> OnboardStep.WELCOME
            }
            _state.update { it.copy(currentStep = initialStep) }
            loadCategories()
        }
    }

    fun loadCategories() {
        if (state.value.hasFetchedCategories) return
        
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = getCategoriesUseCase()
            if (result.isSuccess) {
                val fetchedCategories = result.getOrNull()?.map { 
                    OnboardCategory(
                        id = it.id,
                        name = it.name,
                        type = it.type
                    )
                } ?: emptyList()
                
                _state.update { 
                    val currentAvailable = it.availableCategories
                    val merged = (currentAvailable + fetchedCategories).distinctBy { cat -> "${cat.name}-${cat.type}" }
                    it.copy(
                        availableCategories = merged, 
                        selectedCategories = fetchedCategories,
                        isLoading = false, 
                        hasFetchedCategories = true
                    ) 
                }
            } else {
                _state.update { it.copy(error = "Failed to load categories", isLoading = false) }
            }
        }
    }

    private suspend fun saveCategories(): Boolean {
        _state.update { it.copy(isLoading = true, error = null) }
        val result = saveCategoriesUseCase(state.value.selectedCategories)
        return if (result.isSuccess) {
            _state.update { it.copy(isLoading = false) }
            updateOnboardingStep()
            true
        } else {
            _state.update { it.copy(isLoading = false, error = "Failed to save categories") }
            false
        }
    }

    private suspend fun savePaymentModes(): Boolean {
        _state.update { it.copy(isLoading = true, error = null) }
        val result = savePaymentModesUseCase(state.value.paymentModes)
        return if (result.isSuccess) {
            _state.update { it.copy(isLoading = false) }
            updateOnboardingStep()
            true
        } else {
            _state.update { it.copy(isLoading = false, error = "Failed to save payment methods") }
            false
        }
    }

    private suspend fun saveIncomeBudget(): Boolean {
        _state.update { it.copy(isLoading = true, error = null) }

        val result = saveIncomeBudgetUseCase(
            salary = state.value.salary,
            customIncomes = state.value.customIncomes
        )

        return if (result.isSuccess) {
            _state.update { it.copy(isLoading = false) }
            updateOnboardingStep()
            true
        } else {
            _state.update { it.copy(isLoading = false, error = "Failed to save income and budget") }
            false
        }
    }

    private suspend fun saveExpensesAndSubscriptions() : Boolean {
        _state.update { it.copy(isLoading = true, error = null) }
        val result = saveExpensesAndSubscriptionsUseCase(
            expenses = state.value.recurringExpenses,
            subscriptions = state.value.subscriptions
        )

        return if (result.isSuccess)  {
            _state.update { it.copy(isLoading = false) }
            updateOnboardingStep()
            true
        } else {
            _state.update { it.copy(isLoading = false, error = "Failed to save expenses and subscriptions") }
            false
        }
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
                _state.update { it.copy(currentStep = next) }
            }
        }
    }

    suspend fun updateOnboardingStep() {
        val nextStep = sessionManager.getOnboardingStep() + 1
        updateOnboardingCountUseCase(nextStep)
        sessionManager.setOnboardingStep(nextStep)
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

    fun onSalaryPeriodChanged(period: String) {
        _state.update { it.copy(salary = it.salary.copy(periodType = period)) }
    }

    fun onSalaryStartDateChanged(date: String) {
        _state.update { it.copy(salary = it.salary.copy(startDate = date)) }
    }

    fun onSalaryEndDateChanged(date: String) {
        _state.update { it.copy(salary = it.salary.copy(endDate = date)) }
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

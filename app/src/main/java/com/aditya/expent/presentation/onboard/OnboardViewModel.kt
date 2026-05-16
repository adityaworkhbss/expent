package com.aditya.expent.presentation.onboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya.expent.domain.model.OnboardCategory
import com.aditya.expent.domain.model.OnboardPaymentMode
import com.aditya.expent.domain.usecase.GetCategoriesUseCase
import com.aditya.expent.domain.usecase.SaveCategoriesUseCase
import com.aditya.expent.domain.usecase.SaveBudgetUseCase
import com.aditya.expent.domain.usecase.SaveIncomeBudgetUseCase
import com.aditya.expent.domain.usecase.SavePaymentModesUseCase
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
    BUDGETING(4),
    OUTGOING(5),
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
    val billingDate: String
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
    val availableCategories: List<OnboardCategory> = emptyList(),
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
    private val saveBudgetUseCase: SaveBudgetUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardState())
    val state: StateFlow<OnboardState> = _state.asStateFlow()

    private fun fetchCategories() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = getCategoriesUseCase()
            if (result.isSuccess) {
                val categories = result.getOrNull()?.map { 
                    OnboardCategory(
                        id = it.id,
                        name = it.name,
                        type = it.type
                    )
                } ?: emptyList()
                _state.update { it.copy(selectedCategories = categories, isLoading = false) }
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

    private suspend fun saveBudget(): Boolean {
        _state.update { it.copy(isLoading = true, error = null) }
        val currentState = state.value
        val result = saveBudgetUseCase(
            categoryId = currentState.budgetCategoryId,
            periodType = currentState.budgetPeriod,
            amount = currentState.budgetLimit.toDoubleOrNull() ?: 0.0,
            startDate = currentState.budgetStartDate,
            endDate = if (currentState.budgetEndDate.isBlank()) null else currentState.budgetEndDate
        )
        return if (result.isSuccess) {
            _state.update { it.copy(isLoading = false) }
            updateOnboardingStep()
            true
        } else {
            _state.update { it.copy(isLoading = false, error = "Failed to save budget") }
            false
        }
    }

    fun nextStep() {
        viewModelScope.launch {
            val currentState = state.value

            if(currentState.currentStep != OnboardStep.CATEGORIES &&
                currentState.availableCategories.isEmpty()) {
                fetchCategories()
            }

            val canProceed = when (currentState.currentStep) {
                OnboardStep.CATEGORIES -> saveCategories()
                OnboardStep.PAYMENT_MODES -> savePaymentModes()
                OnboardStep.INCOMING -> saveIncomeBudget()
                OnboardStep.BUDGETING -> saveBudget()
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
                            3 -> OnboardStep.BUDGETING
                            4 -> OnboardStep.OUTGOING
                            else -> OnboardStep.CATEGORIES
                        }
                    }
                    OnboardStep.CATEGORIES -> OnboardStep.PAYMENT_MODES
                    OnboardStep.PAYMENT_MODES -> OnboardStep.INCOMING
                    OnboardStep.INCOMING -> OnboardStep.BUDGETING
                    OnboardStep.BUDGETING -> OnboardStep.OUTGOING
                    OnboardStep.OUTGOING -> OnboardStep.FINISH
                    OnboardStep.FINISH -> OnboardStep.FINISH
                }
                _state.update { it.copy(currentStep = next) }
            }
        }
    }

    fun updateOnboardingStep(){
        if (state.value.currentStep.index != -1) {
            sessionManager.setOnboardingStep(state.value.currentStep.index)
        }
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

    fun onBudgetLimitChanged(limit: String) {
        _state.update { it.copy(budgetLimit = limit) }
    }

    fun onBudgetPeriodChanged(period: String) {
        _state.update { it.copy(budgetPeriod = period) }
    }

    fun onBudgetCategoryChanged(categoryId: String?) {
        _state.update { it.copy(budgetCategoryId = categoryId) }
    }

    fun onBudgetStartDateChanged(date: String) {
        _state.update { it.copy(budgetStartDate = date) }
    }

    fun onBudgetEndDateChanged(date: String) {
        _state.update { it.copy(budgetEndDate = date) }
    }
}

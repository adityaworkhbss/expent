package com.aditya.expent.presentation.onboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya.expent.domain.model.OnboardCategory
import com.aditya.expent.domain.usecase.SaveCategoriesUseCase
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

data class PaymentMode(
    val name: String,
    val type: String
)

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

data class OnboardState(
    val currentStep: OnboardStep = OnboardStep.WELCOME,
    val selectedCategories: List<OnboardCategory> = emptyList(),
    val salary: String = "",
    val bankBalance: String = "",
    val customIncomes: List<Pair<String, String>> = emptyList(),
    val creditCardBill: String = "",
    val nextMonthPendingPayment: String = "",
    val recurringExpenses: List<RecurringExpense> = emptyList(),
    val subscriptions: List<Subscription> = emptyList(),
    val paymentModes: List<PaymentMode> = emptyList()
)

@HiltViewModel
class OnboardViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val saveCategoriesUseCase: SaveCategoriesUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardState())
    val state: StateFlow<OnboardState> = _state.asStateFlow()

    fun saveCategories() {
        viewModelScope.launch {
            saveCategoriesUseCase(state.value.selectedCategories)
        }
    }

    fun nextStep() {
        _state.update {
            // Save current step index before moving to next
            if (it.currentStep.index != -1) {
                sessionManager.setOnboardingStep(it.currentStep.index)
            }
            
            if (it.currentStep == OnboardStep.CATEGORIES) {
                saveCategories()
            }
            
            val next = when (it.currentStep) {
                OnboardStep.WELCOME -> {
                    val savedStep = sessionManager.getOnboardingStep()
                    when (savedStep) {
                        0 -> OnboardStep.CATEGORIES
                        1 -> OnboardStep.PAYMENT_MODES
                        2 -> OnboardStep.INCOMING
                        3 -> OnboardStep.OUTGOING
                        else -> OnboardStep.CATEGORIES
                    }
                }
                OnboardStep.CATEGORIES -> OnboardStep.PAYMENT_MODES
                OnboardStep.PAYMENT_MODES -> OnboardStep.INCOMING
                OnboardStep.INCOMING -> OnboardStep.OUTGOING
                OnboardStep.OUTGOING -> OnboardStep.FINISH
                OnboardStep.FINISH -> OnboardStep.FINISH
            }

            it.copy(currentStep = next)
        }
    }

    fun onCategoriesSelected(categories: List<OnboardCategory>) {
        _state.update { it.copy(selectedCategories = categories) }
    }

    fun onSalaryChanged(salary: String) {
        _state.update { it.copy(salary = salary) }
    }

    fun onBankBalanceChanged(balance: String) {
        _state.update { it.copy(bankBalance = balance) }
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

    fun onCustomIncomesChanged(incomes: List<Pair<String, String>>) {
        _state.update { it.copy(customIncomes = incomes) }
    }

    fun onPaymentModesChanged(modes: List<PaymentMode>) {
        _state.update { it.copy(paymentModes = modes) }
    }
}

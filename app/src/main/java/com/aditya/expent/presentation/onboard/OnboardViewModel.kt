package com.aditya.expent.presentation.onboard

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

enum class OnboardStep {
    WELCOME,
    CATEGORIES,
    PAYMENT_MODES,
    INCOMING,
    OUTGOING,
    FINISH
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
    val selectedCategories: List<String> = emptyList(),
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
class OnboardViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(OnboardState())
    val state: StateFlow<OnboardState> = _state.asStateFlow()

    fun nextStep() {
        _state.update { 
            val next = when (it.currentStep) {
                OnboardStep.WELCOME -> OnboardStep.CATEGORIES
                OnboardStep.CATEGORIES -> OnboardStep.PAYMENT_MODES
                OnboardStep.PAYMENT_MODES -> OnboardStep.INCOMING
                OnboardStep.INCOMING -> OnboardStep.OUTGOING
                OnboardStep.OUTGOING -> OnboardStep.FINISH
                OnboardStep.FINISH -> OnboardStep.FINISH
            }
            it.copy(currentStep = next)
        }
    }

    fun previousStep() {
        _state.update { 
            val prev = when (it.currentStep) {
                OnboardStep.WELCOME -> OnboardStep.WELCOME
                OnboardStep.CATEGORIES -> OnboardStep.WELCOME
                OnboardStep.PAYMENT_MODES -> OnboardStep.CATEGORIES
                OnboardStep.INCOMING -> OnboardStep.PAYMENT_MODES
                OnboardStep.OUTGOING -> OnboardStep.INCOMING
                OnboardStep.FINISH -> OnboardStep.OUTGOING
            }
            it.copy(currentStep = prev)
        }
    }

    fun onCategoriesSelected(categories: List<String>) {
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

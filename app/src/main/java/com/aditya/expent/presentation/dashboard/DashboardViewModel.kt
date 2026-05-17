package com.aditya.expent.presentation.dashboard

import androidx.lifecycle.ViewModel
import com.aditya.expent.domain.model.Transaction
import com.aditya.expent.domain.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class DashboardState(
    val totalBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val recentTransactions: List<Transaction> = emptyList(),
    val userName: String = "User"
)

@HiltViewModel
class DashboardViewModel @Inject constructor() : ViewModel() {
    
    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        loadMockData()
    }

    private fun loadMockData() {
        val mockTransactions = listOf(
            Transaction("1", "Salary Credited", 5000.0, "15/05/2026", "Job", TransactionType.INCOME),
            Transaction("2", "Weekly Grocery", -120.0, "14/05/2026", "Food", TransactionType.EXPENSE),
            Transaction("3", "Netflix Subscription", -15.0, "14/05/2026", "Subscription", TransactionType.EXPENSE),
            Transaction("4", "Freelance Work", 800.0, "10/05/2026", "Work", TransactionType.INCOME),
            Transaction("5", "Dinner at Restaurant", -45.50, "10/05/2026", "Food", TransactionType.EXPENSE)
        )
        updateStateWithList(mockTransactions)
    }

    fun addTransaction(title: String, amount: Double, category: String, type: TransactionType, date: String) {
        val newTransaction = Transaction(
            id = java.util.UUID.randomUUID().toString(),
            title = title,
            amount = if (type == TransactionType.EXPENSE) -Math.abs(amount) else Math.abs(amount),
            date = date,
            category = category,
            type = type
        )
        val updatedList = listOf(newTransaction) + _state.value.recentTransactions
        updateStateWithList(updatedList)
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

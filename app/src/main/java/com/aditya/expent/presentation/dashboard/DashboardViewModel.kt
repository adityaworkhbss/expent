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
        // Placeholder initial data
        loadMockData()
    }

    private fun loadMockData() {
        val mockTransactions = listOf(
            Transaction("1", "Salary", 5000.0, "15 May", "Job", TransactionType.INCOME),
            Transaction("2", "Grocery", -120.0, "14 May", "Food", TransactionType.EXPENSE),
            Transaction("3", "Netflix", -15.0, "12 May", "Subscription", TransactionType.EXPENSE),
            Transaction("4", "Freelance", 800.0, "10 May", "Work", TransactionType.INCOME)
        )
        
        _state.value = DashboardState(
            totalBalance = 5665.0,
            totalIncome = 5800.0,
            totalExpense = 135.0,
            recentTransactions = mockTransactions,
            userName = "Aditya"
        )
    }
}

package com.aditya.expent.presentation.cashflow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya.expent.data.remote.dto.BudgetResponseDto
import com.aditya.expent.domain.usecase.GetBudgetUseCase
import com.aditya.expent.domain.usecase.DeleteBudgetUseCase
import com.aditya.expent.domain.usecase.UpdateBudgetUseCase
import com.aditya.expent.domain.usecase.SaveBudgetUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BudgetUiState(
    val isLoading: Boolean = false,
    val budgets: List<BudgetResponseDto> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class CashflowViewModel @Inject constructor(
    private val getBudgetUseCase: GetBudgetUseCase,
    private val deleteBudgetUseCase: DeleteBudgetUseCase,
    private val updateBudgetUseCase: UpdateBudgetUseCase,
    private val saveBudgetUseCase: SaveBudgetUseCase
) : ViewModel() {

    private val _budgetState = MutableStateFlow(BudgetUiState())
    val budgetState: StateFlow<BudgetUiState> = _budgetState.asStateFlow()

    init {
        fetchBudgets()
//        fetch
    }

    fun fetchBudgets() {
        viewModelScope.launch {
            _budgetState.value = BudgetUiState(isLoading = true)
            getBudgetUseCase()
                .onSuccess { budgets ->
                    _budgetState.value = BudgetUiState(budgets = budgets)
                }
                .onFailure { error ->
                    _budgetState.value = BudgetUiState(error = error.message ?: "Unknown error")
                }
        }
    }

    fun deleteBudget(id: String) {
        viewModelScope.launch {
            _budgetState.value = _budgetState.value.copy(isLoading = true)
            deleteBudgetUseCase(id)
                .onSuccess {
                    fetchBudgets()
                }
                .onFailure { error ->
                    _budgetState.value = _budgetState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to delete budget"
                    )
                }
        }
    }

    fun updateBudget(
        id: String,
        categoryId: String?,
        periodType: String,
        amount: Double,
        startDate: String,
        endDate: String?
    ) {
        viewModelScope.launch {
            _budgetState.value = _budgetState.value.copy(isLoading = true)
            updateBudgetUseCase(id, categoryId, periodType, amount, startDate, endDate)
                .onSuccess {
                    fetchBudgets()
                }
                .onFailure { error ->
                    _budgetState.value = _budgetState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to update budget"
                    )
                }
        }
    }

    fun saveBudget(
        categoryId: String?,
        periodType: String,
        amount: Double,
        startDate: String,
        endDate: String?
    ) {
        viewModelScope.launch {
            _budgetState.value = _budgetState.value.copy(isLoading = true)
            saveBudgetUseCase(categoryId, periodType, amount, startDate, endDate)
                .onSuccess {
                    fetchBudgets()
                }
                .onFailure { error ->
                    _budgetState.value = _budgetState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to save budget"
                    )
                }
        }
    }
}
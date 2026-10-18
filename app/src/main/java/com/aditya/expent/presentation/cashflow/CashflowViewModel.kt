package com.aditya.expent.presentation.cashflow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya.expent.domain.model.Budget
import com.aditya.expent.data.remote.dto.CategoryResponseDto
import com.aditya.expent.domain.model.Expense
import com.aditya.expent.domain.usecase.GetBudgetUseCase
import com.aditya.expent.domain.usecase.DeleteBudgetUseCase
import com.aditya.expent.domain.usecase.GetCategoriesUseCase
import com.aditya.expent.domain.usecase.GetExpensesAndSubscriptionUseCase
import com.aditya.expent.domain.usecase.UpdateBudgetUseCase
import com.aditya.expent.domain.usecase.SaveBudgetUseCase
import com.aditya.expent.domain.usecase.SaveEmiUseCase
import com.aditya.expent.domain.usecase.UpdateEmiUseCase
import com.aditya.expent.domain.usecase.DeleteEmiUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BudgetUiState(
    val isLoading: Boolean = false,
    val budgets: List<Budget> = emptyList(),
    val expense: List<Expense> = emptyList(),
    val categories: List<CategoryResponseDto> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class CashflowViewModel @Inject constructor(
    private val getBudgetUseCase: GetBudgetUseCase,
    private val deleteBudgetUseCase: DeleteBudgetUseCase,
    private val updateBudgetUseCase: UpdateBudgetUseCase,
    private val saveBudgetUseCase: SaveBudgetUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getExpensesAndSubscriptionUseCase: GetExpensesAndSubscriptionUseCase,
    private val saveEmiUseCase: SaveEmiUseCase,
    private val updateEmiUseCase: UpdateEmiUseCase,
    private val deleteEmiUseCase: DeleteEmiUseCase
) : ViewModel() {

    private val _budgetState = MutableStateFlow(BudgetUiState())
    val budgetState: StateFlow<BudgetUiState> = _budgetState.asStateFlow()

    init {
        fetchBudgets()
        fetchCategories()
        fetchExpenseAndSubscription()
    }

    fun fetchExpenseAndSubscription() {
        viewModelScope.launch {
            _budgetState.value = _budgetState.value.copy(isLoading = true)
            getExpensesAndSubscriptionUseCase()
                .catch { error ->
                    _budgetState.value = _budgetState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Unknown error"
                    )
                }
                .collect { expense ->
                    _budgetState.value = _budgetState.value.copy(isLoading = false, expense = expense)
                }
        }
    }

    fun fetchCategories() {
        viewModelScope.launch {
            _budgetState.value = _budgetState.value.copy(isLoading = true)
            getCategoriesUseCase()
                .catch { error ->
                    _budgetState.value = _budgetState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Unknown error"
                    )
                }
                .collect { categoryList ->
                    val dtoList = categoryList.map { cat ->
                        CategoryResponseDto(
                            id = cat.id ?: "",
                            name = cat.name,
                            type = cat.type
                        )
                    }
                    _budgetState.value = _budgetState.value.copy(isLoading = false, categories = dtoList)
                }
        }
    }

    fun fetchBudgets() {
        viewModelScope.launch {
            _budgetState.value = _budgetState.value.copy(isLoading = true)
            getBudgetUseCase()
                .catch { error ->
                    _budgetState.value = _budgetState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Unknown error"
                    )
                }
                .collect { budgets ->
                    _budgetState.value = _budgetState.value.copy(isLoading = false, budgets = budgets)
                }
        }
    }

    fun deleteBudget(id: String) {
        viewModelScope.launch {
            _budgetState.value = _budgetState.value.copy(isLoading = true)
            runCatching { deleteBudgetUseCase(id) }
                .onSuccess {
                    _budgetState.value = _budgetState.value.copy(isLoading = false)
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
        amount: Double,
        date: Int
    ) {
        viewModelScope.launch {
            _budgetState.value = _budgetState.value.copy(isLoading = true)
            runCatching { updateBudgetUseCase(id, categoryId, amount, date) }
                .onSuccess {
                    _budgetState.value = _budgetState.value.copy(isLoading = false)
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
        amount: Double,
        date: Int
    ) {
        viewModelScope.launch {
            _budgetState.value = _budgetState.value.copy(isLoading = true)
            runCatching { saveBudgetUseCase(categoryId, amount, date) }
                .onSuccess {
                    _budgetState.value = _budgetState.value.copy(isLoading = false)
                }
                .onFailure { error ->
                    _budgetState.value = _budgetState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to save budget"
                    )
                }
        }
    }

    fun deleteEmi(id: String) {
        viewModelScope.launch {
            _budgetState.value = _budgetState.value.copy(isLoading = true)
            runCatching { deleteEmiUseCase(id) }
                .onSuccess {
                    _budgetState.value = _budgetState.value.copy(isLoading = false)
                }
                .onFailure { error ->
                    _budgetState.value = _budgetState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to delete EMI/Subscription"
                    )
                }
        }
    }

    /** [tenure] null = every month (subscription). */
    fun updateEmi(
        id: String,
        name: String,
        amount: Double,
        date: Int,
        tenure: Int?,
        paid: Int
    ) {
        viewModelScope.launch {
            _budgetState.value = _budgetState.value.copy(isLoading = true)
            runCatching { updateEmiUseCase(id, name, amount, date, tenure, paid) }
                .onSuccess {
                    _budgetState.value = _budgetState.value.copy(isLoading = false)
                }
                .onFailure { error ->
                    _budgetState.value = _budgetState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to update EMI/Subscription"
                    )
                }
        }
    }

    /** [tenure] null = every month (subscription). */
    fun saveEmi(
        name: String,
        amount: Double,
        date: Int,
        tenure: Int?,
        paid: Int
    ) {
        viewModelScope.launch {
            _budgetState.value = _budgetState.value.copy(isLoading = true)
            runCatching { saveEmiUseCase(name, amount, date, tenure, paid) }
                .onSuccess {
                    _budgetState.value = _budgetState.value.copy(isLoading = false)
                }
                .onFailure { error ->
                    _budgetState.value = _budgetState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to save EMI/Subscription"
                    )
                }
        }
    }
}
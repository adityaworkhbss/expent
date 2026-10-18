package com.aditya.expent.presentation.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya.expent.data.remote.dto.CategoryResponseDto
import com.aditya.expent.domain.model.Transaction
import com.aditya.expent.domain.model.TransactionType
import com.aditya.expent.domain.usecase.GetCategoriesUseCase
import com.aditya.expent.domain.usecase.GetTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionsState(
    val transactions: List<Transaction> = emptyList(),
    val category: List<CategoryResponseDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val pageSize: Int = 20,
    val isExpanded: Boolean = false
)

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getTransactionUseCase: GetTransactionUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(TransactionsState())
    val state: StateFlow<TransactionsState> = _state.asStateFlow()

    private var transactionsJob: Job? = null

    init {
        getTransactions()
        getCategories()
    }

    private fun getCategories() {
        _state.value = state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            getCategoriesUseCase()
                .catch { exception ->
                    _state.value = state.value.copy(
                        error = exception.message,
                        isLoading = false
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
                    _state.value = state.value.copy(
                        category = dtoList,
                        isLoading = false
                    )
                }
        }
    }

    // The Room flow re-emits on every table change, so keep a single collector and replace
    // the list on each emission instead of appending (appending duplicated item keys)
    private fun getTransactions() {
        _state.value = state.value.copy(isLoading = true, error = null)
        transactionsJob?.cancel()
        transactionsJob = viewModelScope.launch {
            getTransactionUseCase(
                page = 1,
                limit = state.value.pageSize
            )
                .catch { exception ->
                    _state.value = state.value.copy(
                        error = exception.message ?: "An error occurred",
                        isLoading = false
                    )
                }
                .collect { response ->
                    val transactions = response.data.map { dto ->
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
                    _state.value = state.value.copy(
                        transactions = transactions.distinctBy { it.id },
                        isLoading = false
                    )
                }
        }
    }

    fun onExpandClick() {
        if (state.value.transactions.size < state.value.pageSize) {
            _state.value = state.value.copy(
                error = "No more transactions to load",
                isExpanded = true
            )
            return
        }
        _state.value = state.value.copy(pageSize = state.value.pageSize + 20)
        getTransactions()
    }
}
package com.aditya.expent.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya.expent.data.remote.dto.CategoryResponseDto
import com.aditya.expent.data.remote.dto.PaymentModeResponseDto
import com.aditya.expent.domain.model.OnboardPaymentMode
import com.aditya.expent.domain.usecase.DeleteCategoriesUseCase
import com.aditya.expent.domain.usecase.GetAccountsUseCase
import com.aditya.expent.domain.usecase.GetCategoriesUseCase
import com.aditya.expent.domain.usecase.SavePaymentModesUseCase
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileState(
    val categories: List<CategoryResponseDto> = emptyList(),
    val accounts: List<PaymentModeResponseDto> = emptyList(),
    val userName: String = "User",
    val email: String = ""
)

class ProfileViewModel @Inject constructor(
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val deleteCategoriesUseCase: DeleteCategoriesUseCase,
    private val saveCategoriesUseCase: DeleteCategoriesUseCase,
    private val savePaymentModesUseCase: SavePaymentModesUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    init {
        loadCategories()
        loadAccounts()
    }

    fun loadCategories() {
        viewModelScope.launch {
            getCategoriesUseCase().onSuccess { categoryList ->
                _state.value = _state.value.copy(categories = categoryList)
            }.onFailure {
                // Keep default empty on error
            }
        }
    }

    fun loadAccounts() {
        viewModelScope.launch {
            getAccountsUseCase().onSuccess { accountList ->
                _state.value = _state.value.copy(accounts = accountList)
            }.onFailure {
                // Keep default empty on error
            }
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            deleteCategoriesUseCase(categoryId).onSuccess {
                loadCategories() // Refresh categories after deletion
            }.onFailure {
                // Handle error if needed
            }
        }
    }

    fun saveCategory(categoryId: String, name: String) {
        viewModelScope.launch {
            saveCategoriesUseCase(categoryId).onSuccess {
                loadCategories() // Refresh categories after saving
            }.onFailure {
                // Handle error if needed
            }
        }
    }

    fun savePaymentMode(paymentModes : List<OnboardPaymentMode>) {
        viewModelScope.launch {
            savePaymentModesUseCase(paymentModes).onSuccess {
                loadAccounts() // Refresh accounts after saving
            }.onFailure {
                // Handle error if needed
            }
        }
    }


}
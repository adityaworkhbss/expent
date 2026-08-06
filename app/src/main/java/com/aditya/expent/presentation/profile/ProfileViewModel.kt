package com.aditya.expent.presentation.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya.expent.data.remote.dto.CategoryResponseDto
import com.aditya.expent.data.remote.dto.PaymentModeResponseDto
import com.aditya.expent.domain.model.OnboardCategory
import com.aditya.expent.domain.model.OnboardPaymentMode
import com.aditya.expent.domain.usecase.DeleteCategoriesUseCase
import com.aditya.expent.domain.usecase.DeletePaymentModeUseCase
import com.aditya.expent.domain.usecase.GetAccountsUseCase
import com.aditya.expent.domain.usecase.GetCategoriesUseCase
import com.aditya.expent.domain.usecase.SaveCategoriesUseCase
import com.aditya.expent.domain.usecase.SavePaymentModesUseCase
import com.aditya.expent.domain.usecase.GetCustomizationUseCase
import com.aditya.expent.domain.usecase.UpdateCustomizationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class ProfileState(
    val categories: List<CategoryResponseDto> = emptyList(),
    val accounts: List<PaymentModeResponseDto> = emptyList(),
    val userName: String = "User",
    val email: String = "",
    val aiTransaction: Boolean = false,
    val reminder: Boolean = false,
    val isLoading: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val deleteCategoriesUseCase: DeleteCategoriesUseCase,
    private val saveCategoriesUseCase: SaveCategoriesUseCase,
    private val savePaymentModesUseCase: SavePaymentModesUseCase,
    private val deletePaymentModesUseCase: DeletePaymentModeUseCase,
    private val getCustomizationUseCase: GetCustomizationUseCase,
    private val updateCustomizationUseCase: UpdateCustomizationUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    init {
        loadCategories()
        loadAccounts()
        loadCustomization()
    }

    fun loadCategories() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            getCategoriesUseCase()
                .catch { _state.value = _state.value.copy(isLoading = false) }
                .collect { categoryList ->
                    val dtoList = categoryList.map { cat ->
                        CategoryResponseDto(
                            id = cat.id ?: "",
                            name = cat.name,
                            type = cat.type
                        )
                    }
                    _state.value = _state.value.copy(categories = dtoList, isLoading = false)
                }
        }
    }

    fun loadAccounts() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            getAccountsUseCase()
                .catch { _state.value = _state.value.copy(isLoading = false) }
                .collect { accountList ->
                    _state.value = _state.value.copy(accounts = accountList, isLoading = false)
                }
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            runCatching { deleteCategoriesUseCase(categoryId) }
                .onFailure { /* Handle error if needed */ }
        }
    }

    fun saveCategory(name: String, type: String) {
        viewModelScope.launch {
            runCatching { saveCategoriesUseCase(listOf(OnboardCategory(name = name, type = type))) }
                .onFailure { /* Handle error if needed */ }
        }
    }

    fun savePaymentMode(name: String, type: String) {
        viewModelScope.launch {
            runCatching { savePaymentModesUseCase(listOf(OnboardPaymentMode(name = name, type = type))) }
                .onFailure { /* Handle error if needed */ }
        }
    }

    fun deletePaymentMode(id: String) {
        viewModelScope.launch {
            runCatching { deletePaymentModesUseCase(id) }
                .onFailure { /* Handle error if needed */ }
        }
    }

    fun loadCustomization() {
        Log.d("ProfileViewModel", "loadCustomization: Calling getCustomizationUseCase")
        viewModelScope.launch {
            getCustomizationUseCase()
                .catch {
                    Log.e("ProfileViewModel", "loadCustomization FAILURE", it)
                }
                .collect { customization ->
                    Log.d("ProfileViewModel", "loadCustomization SUCCESS: aiTransaction=${customization.aiTransaction}, reminder=${customization.reminder}")
                    _state.value = _state.value.copy(
                        aiTransaction = customization.aiTransaction,
                        reminder = customization.reminder
                    )
                }
        }
    }

    fun updateCustomization(aiTransaction: Boolean, reminder: Boolean) {
        Log.d("ProfileViewModel", "updateCustomization: aiTransaction=$aiTransaction, reminder=$reminder")
        viewModelScope.launch {
            // Optimistic update
            _state.value = _state.value.copy(
                aiTransaction = aiTransaction,
                reminder = reminder
            )
            runCatching {
                updateCustomizationUseCase(aiTransaction, reminder)
                Log.d("ProfileViewModel", "updateCustomization SUCCESS")
            }.onFailure {
                Log.e("ProfileViewModel", "updateCustomization FAILURE: reverting", it)
                loadCustomization()
            }
        }
    }
}
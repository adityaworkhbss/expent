package com.aditya.expent.presentation.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya.expent.domain.usecase.LoginWithGoogleUseCase
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.aditya.expent.utils.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginWithGoogleUseCase: LoginWithGoogleUseCase,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun authenticateWithGoogle(context: Context, isSignUp: Boolean) {
        viewModelScope.launch {
            _authState.value = AuthState(isLoading = true)
            try {
                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setServerClientId("454618727771-orhjc0fe26oepqc1vepn7v2u1afrlpji.apps.googleusercontent.com")
                    .setFilterByAuthorizedAccounts(!isSignUp)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    context = context,
                    request = request
                )

                val credential = result.credential
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                
                Log.d("rest re", "GOOGLE ID TOKEN -> $idToken")

                val loginResult = loginWithGoogleUseCase(idToken)
                
                if (loginResult.isSuccess) {
                    val user = loginResult.getOrNull()
                    Log.d("rest re", "Successfully logged in: ${user?.name}")
                    user?.let { 
                        Log.d("AuthVM", "Saving user with onboarding step: ${it.onboardingStep}")
                        sessionManager.saveUser(it)
                        sessionManager.setOnboardingStep(it.onboardingStep)
                    }
                    _authState.value = AuthState(isSuccess = true)
                } else {
                    val errorMsg = loginResult.exceptionOrNull()?.message ?: "Backend verification failed"
                    Log.e("rest re", "BACKEND AUTH FAILED", loginResult.exceptionOrNull())
                    _authState.value = AuthState(error = errorMsg)
                }

            } catch (e: Exception) {
                Log.e("rest re", "GOOGLE AUTH FAILED", e)
                _authState.value = AuthState(error = e.localizedMessage ?: "Authentication failed")
            }
        }
    }
}
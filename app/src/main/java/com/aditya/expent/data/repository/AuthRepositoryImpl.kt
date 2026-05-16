package com.aditya.expent.data.repository

import android.util.Log
import com.aditya.expent.data.remote.ApiService
import com.aditya.expent.data.remote.dto.AuthRequestDto
import com.aditya.expent.domain.model.User
import com.aditya.expent.domain.repository.AuthRepository
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : AuthRepository {

    override suspend fun loginWithGoogle(idToken: String): Result<User> {
        val request = AuthRequestDto(idToken)
        Log.d("rest re", "Request loginWithGoogle: $request")
        return try {
            val response = apiService.verifyGoogleToken(request)
            Log.d("rest re", "Response loginWithGoogle: $response")
            Log.d("AuthRepo", "Extracted onboardingStep from response.user: ${response.user.onboardingStep}")
            val user = User(
                id = response.user.id,
                email = response.user.email,
                name = response.user.name,
                accessToken = response.accessToken,
                refreshToken = response.refreshToken,
                onboardingStep = response.user.onboardingStep
            )
            Result.success(user)
        } catch (e: Exception) {
            Log.e("rest re", "Error loginWithGoogle: ${e.message}", e)
            Result.failure(e)
        }
    }
}

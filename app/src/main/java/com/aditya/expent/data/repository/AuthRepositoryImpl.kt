package com.aditya.expent.data.repository

import android.util.Log
import com.aditya.expent.BuildConfig
import com.aditya.expent.data.remote.ApiService
import com.aditya.expent.data.remote.dto.AuthRequestDto
import com.aditya.expent.data.remote.dto.AuthTestRequestDto
import com.aditya.expent.domain.model.User
import com.aditya.expent.domain.repository.AuthRepository
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : AuthRepository {

    override suspend fun loginWithGoogle(idToken: String): Result<User> {
        val request = AuthRequestDto(idToken)
        Log.d("AuthRepo", "Request loginWithGoogle: $request")
        return try {
            val response = if (BuildConfig.USE_TEST_LOGIN) {

                val test_request = AuthTestRequestDto("test@example.com")
                Log.d("AuthRepo", "Test login mode enabled: calling testLogin endpoint")
                apiService.testLogin(test_request)
            } else {
                Log.d("AuthRepo", "Production mode: calling verifyGoogleToken endpoint")
                apiService.verifyGoogleToken(request)
            }

            Log.d("AuthRepo", "Response loginWithGoogle: $response")
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
            Log.e("AuthRepo", "Error loginWithGoogle: ${e.message}", e)
            Result.failure(e)
        }
    }
}

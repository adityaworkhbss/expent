package com.aditya.expent.data.repository

import com.aditya.expent.data.remote.ApiService
import com.aditya.expent.data.remote.dto.AuthRequestDto
import com.aditya.expent.domain.model.User
import com.aditya.expent.domain.repository.AuthRepository
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : AuthRepository {

    override suspend fun loginWithGoogle(idToken: String): Result<User> {
        return try {
            val response = apiService.verifyGoogleToken(AuthRequestDto(idToken))
            val user = User(
                id = response.user.id,
                email = response.user.email,
                name = response.user.name,
                token = response.token
            )
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

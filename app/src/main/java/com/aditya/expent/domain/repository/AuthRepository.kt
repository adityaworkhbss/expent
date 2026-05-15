package com.aditya.expent.domain.repository

import com.aditya.expent.domain.model.User

interface AuthRepository {
    suspend fun loginWithGoogle(idToken: String): Result<User>
}

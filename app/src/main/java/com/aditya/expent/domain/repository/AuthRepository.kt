package com.aditya.expent.domain.repository

import com.aditya.expent.domain.model.User

interface AuthRepository {
    suspend fun loginWithGoogle(idToken: String): Result<User>

    /** Deletes the account and all its data on the server, then wipes this device. Needs a connection. */
    suspend fun deleteAccount(): Result<Unit>
}

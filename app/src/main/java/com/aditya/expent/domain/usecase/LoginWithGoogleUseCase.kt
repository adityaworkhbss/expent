package com.aditya.expent.domain.usecase

import com.aditya.expent.domain.model.User
import com.aditya.expent.domain.repository.AuthRepository
import javax.inject.Inject

class LoginWithGoogleUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(idToken: String): Result<User> {
        return repository.loginWithGoogle(idToken)
    }
}

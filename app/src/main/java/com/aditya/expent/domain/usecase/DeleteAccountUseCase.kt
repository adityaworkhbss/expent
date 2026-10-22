package com.aditya.expent.domain.usecase

import com.aditya.expent.domain.repository.AuthRepository
import javax.inject.Inject

class DeleteAccountUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(): Result<Unit> = repository.deleteAccount()
}

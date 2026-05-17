package com.aditya.expent.domain.usecase

import com.aditya.expent.domain.repository.TransactionRepository
import javax.inject.Inject

class GetTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(from : String, to: String) = repository.getTransactions(from, to)
}
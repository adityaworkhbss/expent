package com.aditya.expent.domain.usecase

import com.aditya.expent.data.remote.dto.ParseTransactionResponseDto
import com.aditya.expent.domain.repository.TransactionRepository
import javax.inject.Inject

class ParseTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(
        text: String
    ): Result<ParseTransactionResponseDto> {
        return transactionRepository.parseTransaction(text)
    }
}

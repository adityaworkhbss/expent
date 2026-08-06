package com.aditya.expent.domain.usecase

import com.aditya.expent.domain.model.Transaction
import com.aditya.expent.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import com.aditya.expent.data.remote.dto.PaginatedTransactionsResponseDto
import javax.inject.Inject

class GetTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(from: String, to: String): Flow<PaginatedTransactionsResponseDto> =
        repository.getTransactions(from, to)
    operator fun invoke(page: Int, limit: Int): Flow<PaginatedTransactionsResponseDto> =
        repository.getTransactions(page, limit)
}

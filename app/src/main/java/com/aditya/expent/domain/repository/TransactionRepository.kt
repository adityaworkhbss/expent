package com.aditya.expent.domain.repository

import com.aditya.expent.data.remote.dto.PaginatedTransactionsResponseDto

interface TransactionRepository {
    suspend fun getTransactions(
        from: String,
        to: String
    ): Result<PaginatedTransactionsResponseDto>
}

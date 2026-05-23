package com.aditya.expent.domain.repository

import com.aditya.expent.data.remote.dto.PaginatedTransactionsResponseDto
import com.aditya.expent.data.remote.dto.ParseTransactionResponseDto
import com.aditya.expent.domain.model.Transaction

interface TransactionRepository {
    suspend fun getTransactions(
        from: String,
        to: String
    ): Result<PaginatedTransactionsResponseDto>

    suspend fun getTransactions(
        page: Int,
        limit: Int
    ): Result<PaginatedTransactionsResponseDto>

    suspend fun addTransaction(
        transaction: Transaction
    ): Result<Unit>

    suspend fun parseTransaction(
        text: String
    ): Result<ParseTransactionResponseDto>
}

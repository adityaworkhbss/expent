package com.aditya.expent.domain.repository

import com.aditya.expent.data.remote.dto.PaginatedTransactionsResponseDto
import com.aditya.expent.data.remote.dto.ParseTransactionResponseDto
import com.aditya.expent.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getTransactions(
        from: String,
        to: String
    ): Flow<PaginatedTransactionsResponseDto>

    fun getTransactions(
        page: Int,
        limit: Int
    ): Flow<PaginatedTransactionsResponseDto>

    suspend fun addTransaction(
        transaction: Transaction
    )

    suspend fun parseTransaction(
        text: String
    ): ParseTransactionResponseDto

    suspend fun refreshTransactions(from: String, to: String)
    suspend fun refreshTransactions(page: Int, limit: Int)
}

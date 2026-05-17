package com.aditya.expent.data.repository

import android.util.Log
import com.aditya.expent.data.remote.ApiService
import com.aditya.expent.data.remote.dto.PaginatedTransactionsResponseDto
import com.aditya.expent.domain.repository.TransactionRepository
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : TransactionRepository {

    override suspend fun getTransactions(
        from: String,
        to: String
    ) : Result <PaginatedTransactionsResponseDto>{
        return try {

            Log.d("rest re", "Request getTransactions: from=$from, to=$to")
            val response = apiService.getTransactions(from, to)
            Log.d("rest re", "Response getTransactions: $response")

            Result.success(response)
        } catch (e: Exception) {
            Log.d("rest re", "Error getTransactions: ${e.message}", e)
            Result.failure(e)
        }
    }
}
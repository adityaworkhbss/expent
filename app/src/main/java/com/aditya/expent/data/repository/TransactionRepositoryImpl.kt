package com.aditya.expent.data.repository

import android.util.Log
import com.aditya.expent.data.remote.ApiService
import com.aditya.expent.data.remote.dto.CreateTransactionRequestDto
import com.aditya.expent.data.remote.dto.PaginatedTransactionsResponseDto
import com.aditya.expent.domain.model.Transaction
import com.aditya.expent.domain.repository.TransactionRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import kotlin.math.abs

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

    override suspend fun addTransaction(transaction: Transaction): Result<Unit> {
        return try {
            val request = CreateTransactionRequestDto(
                type = transaction.type.name,
                amount = abs(transaction.amount),
                transactionDate = convertDateToIso(transaction.date),
                accountId = transaction.accountId ?: "",
                categoryId = transaction.categoryId,
                transferToAccountId = transaction.transferToAccountId,
                note = transaction.title,
                merchant = null,
                paymentMethod = null,
                tags = null,
                status = "CLEARED",
                isSalary = transaction.title.lowercase().contains("salary")
            )
            Log.d("rest re", "Request addTransaction: $request")
            val response = apiService.addTransaction(request)
            Log.d("rest re", "Response addTransaction: $response")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.d("rest re", "Error addTransaction: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun convertDateToIso(dateStr: String): String {
        try {
            if (dateStr.contains("-") && dateStr.contains("T")) {
                return dateStr
            }
            val parser = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = parser.parse(dateStr)
            if (date != null) {
                val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                isoFormatter.timeZone = TimeZone.getTimeZone("UTC")
                return isoFormatter.format(date)
            }
        } catch (e: Exception) {
            // ignore
        }
        try {
            val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            isoFormatter.timeZone = TimeZone.getTimeZone("UTC")
            return isoFormatter.format(Date())
        } catch (e: Exception) {
            return dateStr
        }
    }
}
package com.aditya.expent.data.repository

import android.util.Log
import com.aditya.expent.data.remote.ApiService
import com.aditya.expent.data.remote.dto.CreateTransactionRequestDto
import com.aditya.expent.data.remote.dto.PaginatedTransactionsResponseDto
import com.aditya.expent.data.remote.dto.ParseTransactionRequestDto
import com.aditya.expent.data.remote.dto.ParseTransactionResponseDto
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

    override suspend fun getTransactions(
        page: Int,
        limit: Int
    ) : Result <PaginatedTransactionsResponseDto>{
        return try {

            Log.d("rest re", "Request getTransactions: page=$page, limit=$limit")
            val response = apiService.getTransactions(page, limit)
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
                paymentMethod = transaction.paymentMethod,
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

    override suspend fun parseTransaction(text: String): Result<ParseTransactionResponseDto> {
        return try {
            Log.d("rest re", "Request parseTransaction: text=$text")
            val response = apiService.parseTransaction(ParseTransactionRequestDto(text))
            Log.d("rest re", "Response parseTransaction: $response")
            Result.success(response)
        } catch (e: Exception) {
            Log.e("rest re", "Error parseTransaction: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun convertDateToIso(dateStr: String): String {
        try {
            if (dateStr.contains("-") && dateStr.contains("T")) {
                return dateStr
            }
            var date: Date? = null
            try {
                val parser = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                date = parser.parse(dateStr)
            } catch (e: Exception) {
                // ignore
            }
            if (date == null) {
                try {
                    val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    date = parser.parse(dateStr)
                } catch (e: Exception) {
                    // ignore
                }
            }
            if (date != null) {
                val calendar = java.util.Calendar.getInstance()
                calendar.time = date
                // Set to 12:00:00 (noon) local time to prevent timezone offset shifts from flipping the calendar day
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 12)
                calendar.set(java.util.Calendar.MINUTE, 0)
                calendar.set(java.util.Calendar.SECOND, 0)
                calendar.set(java.util.Calendar.MILLISECOND, 0)

                val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                isoFormatter.timeZone = java.util.TimeZone.getTimeZone("UTC")
                return isoFormatter.format(calendar.time)
            }
        } catch (e: Exception) {
            // ignore
        }
        try {
            val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            isoFormatter.timeZone = java.util.TimeZone.getTimeZone("UTC")
            return isoFormatter.format(Date())
        } catch (e: Exception) {
            return dateStr
        }
    }
}
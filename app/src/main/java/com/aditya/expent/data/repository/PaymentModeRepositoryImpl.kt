package com.aditya.expent.data.repository

import android.util.Log
import com.aditya.expent.data.remote.ApiService
import com.aditya.expent.data.remote.dto.PaymentModeRequestDto
import com.aditya.expent.domain.model.OnboardPaymentMode
import com.aditya.expent.domain.repository.PaymentModeRepository
import com.aditya.expent.utils.SessionManager
import javax.inject.Inject

class PaymentModeRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) : PaymentModeRepository {

    override suspend fun savePaymentModes(
        paymentModes : List<OnboardPaymentMode>
    ): Result<Unit> {
        return try {
            val requests = paymentModes.map { mode ->
                PaymentModeRequestDto(
                    name = mode.name,
                    type = mode.type,
                    user_id = sessionManager.getUser()?.id
                )
            }
            Log.d("rest re", "Request savePaymentModes: $requests")
            val response = apiService.savePaymentModes(requests)
            Log.d("rest re", "Response savePaymentModes: $response")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("rest re", "Error savePaymentModes: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getAccounts(): Result<List<com.aditya.expent.data.remote.dto.PaymentModeResponseDto>> {
        return try {
            Log.d("rest re", "Request getAccounts")
            val response = apiService.getAccounts()
            Log.d("rest re", "Response getAccounts: $response")
            Result.success(response)
        } catch (e: Exception) {
            Log.e("rest re", "Error getAccounts: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteAccounts(id : String): Result<Unit> {
        return try {
            Log.d("rest re", "Request deleteAccounts")
            val response = apiService.deleteAccount(id)
            Log.d("rest re", "Response deleteAccounts: $response")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("rest re", "Error deleteAccounts: ${e.message}", e)
            Result.failure(e)
        }
    }
}
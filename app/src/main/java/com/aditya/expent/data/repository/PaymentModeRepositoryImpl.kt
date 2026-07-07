package com.aditya.expent.data.repository

import android.util.Log
import com.aditya.expent.data.local.dao.AccountDao
import com.aditya.expent.data.local.dao.PendingSyncDao
import com.aditya.expent.data.local.entity.PendingSyncEntity
import com.aditya.expent.data.mapper.toDto
import com.aditya.expent.data.mapper.toEntity
import com.aditya.expent.data.remote.ApiService
import com.aditya.expent.data.remote.dto.PaymentModeRequestDto
import com.aditya.expent.domain.model.OnboardPaymentMode
import com.aditya.expent.domain.repository.PaymentModeRepository
import com.aditya.expent.utils.SessionManager
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class PaymentModeRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val sessionManager: SessionManager,
    private val accountDao: AccountDao,
    private val pendingSyncDao: PendingSyncDao,
    private val gson: Gson
) : PaymentModeRepository {

    override suspend fun savePaymentModes(
        paymentModes: List<OnboardPaymentMode>
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
            val userId = sessionManager.getUser()?.id
            accountDao.insert(response.map { it.toEntity(userId) })
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("rest re", "Error savePaymentModes: ${e.message}", e)
            enqueueSync("account", "CREATE", gson.toJson(paymentModes))
            Result.failure(e)
        }
    }

    override suspend fun getAccounts(): Result<List<com.aditya.expent.data.remote.dto.PaymentModeResponseDto>> {
        return try {
            Log.d("rest re", "Request getAccounts")
            val response = apiService.getAccounts()
            Log.d("rest re", "Response getAccounts: $response")
            val userId = sessionManager.getUser()?.id
            accountDao.insert(response.map { it.toEntity(userId) })
            Result.success(response)
        } catch (e: Exception) {
            Log.e("rest re", "Error getAccounts: ${e.message}", e)
            val cached = accountDao.getAccounts().first()
            if (cached.isNotEmpty()) {
                Result.success(cached.map { it.toDto() })
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun deleteAccounts(id: String): Result<Unit> {
        return try {
            Log.d("rest re", "Request deleteAccounts")
            apiService.deleteAccount(id)
            Log.d("rest re", "Response deleteAccounts: Success")
            accountDao.getAccount(id)?.let { accountDao.delete(it) }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("rest re", "Error deleteAccounts: ${e.message}", e)
            enqueueSync("account", "DELETE", id)
            Result.failure(e)
        }
    }

    private suspend fun enqueueSync(entityType: String, operation: String, payload: String) {
        pendingSyncDao.insert(
            PendingSyncEntity(
                entityType = entityType,
                entityId = "",
                operation = operation,
                payload = payload,
                createdAt = System.currentTimeMillis()
            )
        )
    }
}

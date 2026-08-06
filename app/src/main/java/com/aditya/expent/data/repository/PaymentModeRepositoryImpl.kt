package com.aditya.expent.data.repository

import android.util.Log
import com.aditya.expent.data.local.dao.AccountDao
import com.aditya.expent.data.local.dao.PendingSyncDao
import com.aditya.expent.data.local.entity.PendingSyncEntity
import com.aditya.expent.data.local.entity.SyncStatus
import com.aditya.expent.data.mapper.toDto
import com.aditya.expent.data.mapper.toEntity
import com.aditya.expent.data.remote.ApiService
import com.aditya.expent.data.remote.dto.PaymentModeRequestDto
import com.aditya.expent.data.remote.dto.PaymentModeResponseDto
import com.aditya.expent.data.sync.SyncScheduler
import com.aditya.expent.domain.model.OnboardPaymentMode
import com.aditya.expent.domain.repository.PaymentModeRepository
import com.aditya.expent.utils.SessionManager
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PaymentModeRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val sessionManager: SessionManager,
    private val accountDao: AccountDao,
    private val pendingSyncDao: PendingSyncDao,
    private val syncScheduler: SyncScheduler,
    private val gson: Gson
) : PaymentModeRepository {

    override fun getAccounts(): Flow<List<PaymentModeResponseDto>> {
        return accountDao.getAccounts().map { entities ->
            entities.map { it.toDto() }
        }
    }

    override suspend fun savePaymentModes(paymentModes: List<OnboardPaymentMode>) {
        val userId = sessionManager.getUser()?.id
        val entities = paymentModes.map { mode ->
            mode.toEntity(
                userId = userId,
                syncStatus = SyncStatus.PENDING_CREATE
            )
        }
        accountDao.insert(entities)

        val requests = paymentModes.map { mode ->
            PaymentModeRequestDto(
                name = mode.name,
                type = mode.type,
                user_id = userId
            )
        }
        enqueueSync("account", "CREATE", gson.toJson(requests))
        syncScheduler.enqueueAccountSync()
    }

    override suspend fun deleteAccounts(id: String) {
        val account = accountDao.getAccount(id) ?: return
        accountDao.update(
            account.copy(
                isDeleted = true,
                syncStatus = SyncStatus.PENDING_DELETE
            )
        )
        
        enqueueSync("account", "DELETE", id)
        syncScheduler.enqueueAccountSync()
    }

    override suspend fun refreshAccounts() {
        try {
            Log.d("rest re", "Request refreshAccounts")
            val response = apiService.getAccounts()
            Log.d("rest re", "Response refreshAccounts: $response")
            val userId = sessionManager.getUser()?.id
            accountDao.replaceAll(response.map { it.toEntity(userId) })
        } catch (e: Exception) {
            Log.e("rest re", "Error refreshAccounts: ${e.message}", e)
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

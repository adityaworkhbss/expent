package com.aditya.expent.data.repository

import android.util.Log
import com.aditya.expent.data.local.dao.PendingSyncDao
import com.aditya.expent.data.local.dao.TransactionDao
import com.aditya.expent.data.local.entity.PendingSyncEntity
import com.aditya.expent.data.local.entity.TransactionEntity
import com.aditya.expent.data.local.entity.SyncStatus
import com.aditya.expent.data.mapper.toDto
import com.aditya.expent.data.mapper.toEntity
import com.aditya.expent.data.remote.ApiService
import com.aditya.expent.data.remote.dto.CreateTransactionRequestDto
import com.aditya.expent.data.remote.dto.MetaDto
import com.aditya.expent.data.remote.dto.PaginatedTransactionsResponseDto
import com.aditya.expent.data.remote.dto.ParseTransactionRequestDto
import com.aditya.expent.data.remote.dto.ParseTransactionResponseDto
import com.aditya.expent.data.sync.SyncScheduler
import com.aditya.expent.domain.model.Transaction
import com.aditya.expent.domain.repository.TransactionRepository
import com.aditya.expent.utils.AppLogger
import com.aditya.expent.utils.SessionManager
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlin.math.abs

class TransactionRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val transactionDao: TransactionDao,
    private val pendingSyncDao: PendingSyncDao,
    private val syncScheduler: SyncScheduler,
    private val sessionManager: SessionManager,
    private val gson: Gson
) : TransactionRepository {

    override fun getTransactions(
        from: String,
        to: String
    ): Flow<PaginatedTransactionsResponseDto> {
        return transactionDao.getTransactions(from, to).map { entities ->
            entities.toPaginatedResponse(page = 1, limit = entities.size.coerceAtLeast(1))
        }
    }

    override fun getTransactions(
        page: Int,
        limit: Int
    ): Flow<PaginatedTransactionsResponseDto> {
        return transactionDao.getTransactions().map { cached ->
            val safeLimit = limit.coerceAtLeast(1)
            val safePage = page.coerceAtLeast(1)
            val pageItems = cached.drop((safePage - 1) * safeLimit).take(safeLimit)
            pageItems.toPaginatedResponse(page = safePage, limit = safeLimit, total = cached.size)
        }
    }

    override suspend fun addTransaction(transaction: Transaction) {
        val userId = sessionManager.getUser()?.id.orEmpty()
        val localId = if (transaction.id.isBlank() || transaction.id.startsWith("local-")) {
            "local-${UUID.randomUUID()}"
        } else {
            transaction.id
        }

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

        val entity = transaction.toPendingEntity(request).copy(
            id = localId,
            userId = userId,
            syncStatus = SyncStatus.PENDING_CREATE
        )
        transactionDao.insert(entity)
        AppLogger.room("CREATE", "transactions", payload = entity, result = "Success (LocalID: $localId, Status: PENDING_CREATE)")
        
        enqueueSync("transaction", "CREATE", gson.toJson(request), localId)
        AppLogger.room("CREATE", "pending_sync", payload = "Type=transaction, ID=$localId", result = "Enqueued")
        syncScheduler.enqueueTransactionSync()
    }

    override suspend fun parseTransaction(text: String): ParseTransactionResponseDto {
        val response = apiService.parseTransaction(ParseTransactionRequestDto(text))
        return response
    }

    override suspend fun refreshTransactions(from: String, to: String) {
        try {
            val response = apiService.getTransactions(from, to)
            val entities = response.data?.data.orEmpty().map { it.toEntity() }
            transactionDao.replaceAll(entities)
            AppLogger.room("REPLACE", "transactions", payload = "Refreshed ${entities.size} items from API (from=$from, to=$to)", result = "ReplaceAll")
        } catch (e: Exception) {
            AppLogger.apiError("GET", "transactions?from=$from&to=$to", e.message, throwable = e)
        }
    }

    override suspend fun refreshTransactions(page: Int, limit: Int) {
        try {
            val response = apiService.getTransactions(page, limit)
            val entities = response.data?.data.orEmpty().map { it.toEntity() }
            transactionDao.replaceAll(entities)
            AppLogger.room("REPLACE", "transactions", payload = "Refreshed ${entities.size} items from API (page=$page, limit=$limit)", result = "ReplaceAll")
        } catch (e: Exception) {
            AppLogger.apiError("GET", "transactions?page=$page&limit=$limit", e.message, throwable = e)
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

    private fun List<TransactionEntity>.toPaginatedResponse(
        page: Int,
        limit: Int,
        total: Int = size
    ): PaginatedTransactionsResponseDto {
        val totalPages = if (total == 0) 0 else ((total + limit - 1) / limit)
        return PaginatedTransactionsResponseDto(
            data = map { it.toDto() },
            meta = MetaDto(
                total = total,
                page = page,
                limit = limit,
                totalPages = totalPages,
                hasNextPage = page < totalPages,
                hasPreviousPage = page > 1
            )
        )
    }

    private fun Transaction.toPendingEntity(request: CreateTransactionRequestDto): TransactionEntity {
        val now = nowIso()
        return TransactionEntity(
            id = if (id.isBlank() || id.startsWith("local-")) "local-${UUID.randomUUID()}" else id,
            userId = "",
            accountId = request.accountId,
            categoryId = request.categoryId,
            transferToAccountId = request.transferToAccountId,
            type = request.type,
            amount = request.amount.toString(),
            transactionDate = request.transactionDate ?: request.date ?: now,
            note = request.note,
            merchant = request.merchant,
            paymentMethod = request.paymentMethod,
            tags = request.tags,
            status = request.status,
            isSalary = request.isSalary,
            isDeleted = false,
            createdAt = now,
            updatedAt = now,
            categoryName = category,
            syncStatus = SyncStatus.PENDING_CREATE
        )
    }

    private suspend fun enqueueSync(entityType: String, operation: String, payload: String, entityId: String = "") {
        pendingSyncDao.insert(
            PendingSyncEntity(
                entityType = entityType,
                entityId = entityId,
                operation = operation,
                payload = payload,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    private fun nowIso(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        formatter.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return formatter.format(Date())
    }
}

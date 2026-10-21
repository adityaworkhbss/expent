package com.aditya.expent.data.repository

import com.aditya.expent.data.local.dao.PendingSyncDao
import com.aditya.expent.data.local.entity.PendingSyncEntity
import com.aditya.expent.data.remote.dto.FeedbackRequestDto
import com.aditya.expent.data.sync.SyncScheduler
import com.aditya.expent.domain.repository.FeedbackRepository
import com.aditya.expent.utils.AppLogger
import com.aditya.expent.utils.SessionManager
import com.google.gson.Gson
import javax.inject.Inject

// Feedback isn't stored locally: it only waits in pending_sync until the next push sync sends it
class FeedbackRepositoryImpl @Inject constructor(
    private val sessionManager: SessionManager,
    private val pendingSyncDao: PendingSyncDao,
    private val syncScheduler: SyncScheduler,
    private val gson: Gson
) : FeedbackRepository {

    override suspend fun submitFeedback(rating: Int, message: String) {
        val userId = sessionManager.getUser()?.id ?: return
        val request = FeedbackRequestDto(userId = userId, rating = rating, message = message.trim())
        pendingSyncDao.insert(
            PendingSyncEntity(
                entityType = "feedback",
                entityId = "",
                operation = "CREATE",
                payload = gson.toJson(request),
                createdAt = System.currentTimeMillis()
            )
        )
        AppLogger.room("CREATE", "pending_sync", "Type=feedback, Op=CREATE", "Enqueued")
        syncScheduler.enqueuePendingSync()
    }
}

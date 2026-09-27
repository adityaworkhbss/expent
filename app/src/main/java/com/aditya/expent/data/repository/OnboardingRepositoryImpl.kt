package com.aditya.expent.data.repository

import com.aditya.expent.data.local.dao.PendingSyncDao
import com.aditya.expent.data.local.entity.PendingSyncEntity
import com.aditya.expent.data.remote.ApiService
import com.aditya.expent.data.remote.dto.OnboardingStepRequestDto
import com.aditya.expent.data.sync.SyncScheduler
import com.aditya.expent.domain.repository.OnboardingRepository
import javax.inject.Inject

class OnboardingRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val pendingSyncDao: PendingSyncDao,
    private val syncScheduler: SyncScheduler
) : OnboardingRepository {
    override suspend fun updateOnboardingCount(count: Int) {
        try {
            apiService.updateOnboardingCount(OnboardingStepRequestDto(count))
        } catch (e: Exception) {
            pendingSyncDao.insert(
                PendingSyncEntity(
                    entityType = "onboarding",
                    entityId = "",
                    operation = "INCREMENT",
                    payload = count.toString(),
                    createdAt = System.currentTimeMillis()
                )
            )
            syncScheduler.enqueueOnboardingSync()
        }
    }
}

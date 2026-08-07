package com.aditya.expent.data.repository

import com.aditya.expent.data.local.dao.CategoryDao
import com.aditya.expent.data.local.dao.PendingSyncDao
import com.aditya.expent.data.local.entity.PendingSyncEntity
import com.aditya.expent.data.local.entity.SyncStatus
import com.aditya.expent.data.mapper.toEntity
import com.aditya.expent.data.mapper.toDomain
import com.aditya.expent.data.remote.ApiService
import com.aditya.expent.data.remote.dto.CategoryRequestDto
import com.aditya.expent.data.sync.SyncScheduler
import com.aditya.expent.domain.model.OnboardCategory
import com.aditya.expent.domain.repository.CategoryRepository
import com.aditya.expent.utils.SessionManager
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val sessionManager: SessionManager,
    private val categoryDao: CategoryDao,
    private val pendingSyncDao: PendingSyncDao,
    private val syncScheduler: SyncScheduler,
    private val gson: Gson
) : CategoryRepository {

    override fun getCategories(): Flow<List<OnboardCategory>> {
        return categoryDao.getCategories()
            .map { entities ->
                entities.map { it.toDomain() }
            }
    }

    override suspend fun addCategories(categories: List<OnboardCategory>) {
        val userId = sessionManager.getUser()?.id ?: return

        val entities = categories.map {
            it.toEntity(
                userId = userId,
                syncStatus = SyncStatus.PENDING_CREATE
            )
        }
        categoryDao.insert(entities)

        // Enqueue to PendingSyncDao so SyncWorker can push to API
        val requests = categories.map { cat ->
            CategoryRequestDto(
                name = cat.name,
                type = cat.type,
                user_id = userId
            )
        }
        pendingSyncDao.insert(
            PendingSyncEntity(
                entityType = "category",
                entityId = "",
                operation = "CREATE",
                payload = gson.toJson(requests),
                createdAt = System.currentTimeMillis()
            )
        )

        syncScheduler.enqueueCategorySync()
    }

    override suspend fun deleteCategory(categoryId: String) {
        val category = categoryDao.getCategory(categoryId) ?: return

        categoryDao.update(
            category.copy(
                isDeleted = true,
                syncStatus = SyncStatus.PENDING_DELETE
            )
        )

        pendingSyncDao.insert(
            PendingSyncEntity(
                entityType = "category",
                entityId = categoryId,
                operation = "DELETE",
                payload = categoryId,
                createdAt = System.currentTimeMillis()
            )
        )

        syncScheduler.enqueueCategorySync()
    }

    override suspend fun refreshCategories() {
        val userId = sessionManager.getUser()?.id ?: return

        val response = apiService.getCategories()

        categoryDao.replaceAll(
            response.map {
                it.toEntity(userId)
            }
        )
    }
}

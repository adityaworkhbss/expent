package com.aditya.expent.data.repository

import com.aditya.expent.data.local.dao.CategoryDao
import com.aditya.expent.data.local.entity.SyncStatus
import com.aditya.expent.data.mapper.toEntity
import com.aditya.expent.data.mapper.toDomain
import com.aditya.expent.data.remote.ApiService
import com.aditya.expent.data.sync.SyncScheduler
import com.aditya.expent.domain.model.OnboardCategory
import com.aditya.expent.domain.repository.CategoryRepository
import com.aditya.expent.utils.SessionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val sessionManager: SessionManager,
    private val categoryDao: CategoryDao,
    private val syncScheduler: SyncScheduler
) : CategoryRepository {

    override fun getCategories(): Flow<List<OnboardCategory>> {
        return categoryDao.getCategories()
            .map { entities ->
                entities.map { it.toDomain() }
            }
    }

    override suspend fun addCategories(categories: List<OnboardCategory>) {

        val userId = sessionManager.getUser()?.id ?: return

        categoryDao.insert(
            categories.map {
                it.toEntity(
                    userId = userId,
                    syncStatus = SyncStatus.PENDING_CREATE
                )
            }
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

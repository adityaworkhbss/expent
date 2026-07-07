package com.aditya.expent.data.repository

import android.util.Log
import com.aditya.expent.data.local.dao.CategoryDao
import com.aditya.expent.data.local.dao.PendingSyncDao
import com.aditya.expent.data.local.entity.PendingSyncEntity
import com.aditya.expent.data.mapper.toDto
import com.aditya.expent.data.mapper.toEntity
import com.aditya.expent.data.remote.ApiService
import com.aditya.expent.data.remote.dto.CategoryRequestDto
import com.aditya.expent.data.remote.dto.CategoryResponseDto
import com.aditya.expent.domain.model.OnboardCategory
import com.aditya.expent.domain.repository.CategoryRepository
import com.aditya.expent.utils.SessionManager
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val sessionManager: SessionManager,
    private val categoryDao: CategoryDao,
    private val pendingSyncDao: PendingSyncDao,
    private val gson: Gson
) : CategoryRepository {

    override suspend fun addCategories(categories: List<OnboardCategory>): Result<Unit> {
        return try {
            val requests = categories.map { category ->
                CategoryRequestDto(
                    name = category.name,
                    type = category.type,
                    user_id = sessionManager.getUser()?.id
                )
            }
            Log.d("rest re", "Request createCategories: $requests")
            val response = apiService.createCategories(requests)
            Log.d("rest re", "Response createCategories: $response")
            val userId = sessionManager.getUser()?.id
            categoryDao.insert(response.map { it.toEntity(userId) })
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("rest re", "Error addCategories: ${e.message}", e)
            enqueueSync("category", "CREATE", gson.toJson(categories))
            Result.failure(e)
        }
    }

    override suspend fun getCategories(): Result<List<CategoryResponseDto>> {
        return try {
            Log.d("rest re", "Request getCategories: No parameters")
            val response = apiService.getCategories()
            Log.d("rest re", "Response getCategories: $response")
            val userId = sessionManager.getUser()?.id
            categoryDao.insert(response.map { it.toEntity(userId) })
            Result.success(response)
        } catch (e: Exception) {
            Log.e("rest re", "Error getCategories: ${e.message}", e)
            val cached = categoryDao.getCategories().first()
            if (cached.isNotEmpty()) {
                Result.success(cached.map { it.toDto() })
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun deleteCategory(categoryId: String): Result<Unit> {
        return try {
            Log.d("rest re", "Request deleteCategory: categoryId=$categoryId")
            apiService.deleteCategory(categoryId)
            Log.d("rest re", "Response deleteCategory: Success")
            categoryDao.getCategory(categoryId)?.let { categoryDao.delete(it) }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("rest re", "Error deleteCategory: ${e.message}", e)
            enqueueSync("category", "DELETE", categoryId)
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

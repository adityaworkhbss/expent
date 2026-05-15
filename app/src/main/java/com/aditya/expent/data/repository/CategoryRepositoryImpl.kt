package com.aditya.expent.data.repository

import android.util.Log
import com.aditya.expent.data.remote.ApiService
import com.aditya.expent.data.remote.dto.CategoryRequestDto
import com.aditya.expent.domain.model.OnboardCategory
import com.aditya.expent.domain.repository.CategoryRepository
import com.aditya.expent.utils.SessionManager
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val sessionManager: SessionManager
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
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("rest re", "Error addCategories: ${e.message}", e)
            Result.failure(e)
        }
    }
}

package com.aditya.expent.domain.repository

import com.aditya.expent.domain.model.OnboardCategory
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getCategories(): Flow<List<OnboardCategory>>
    suspend fun addCategories(categories: List<OnboardCategory>)
    suspend fun deleteCategory(categoryId: String)
    suspend fun refreshCategories()
}

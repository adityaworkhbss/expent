package com.aditya.expent.domain.repository

import com.aditya.expent.domain.model.OnboardCategory

interface CategoryRepository {
    suspend fun addCategories(categories: List<OnboardCategory>): Result<Unit>
}

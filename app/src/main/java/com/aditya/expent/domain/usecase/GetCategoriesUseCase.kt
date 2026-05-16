package com.aditya.expent.domain.usecase

import com.aditya.expent.data.remote.dto.CategoryResponseDto
import com.aditya.expent.domain.repository.CategoryRepository
import jakarta.inject.Inject

class GetCategoriesUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke(): Result<List<CategoryResponseDto>> {
        return repository.getCategories()
    }
}
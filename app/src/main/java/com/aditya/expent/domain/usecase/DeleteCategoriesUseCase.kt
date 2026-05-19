package com.aditya.expent.domain.usecase

import com.aditya.expent.domain.repository.CategoryRepository
import javax.inject.Inject

class DeleteCategoriesUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke(categoryIds: String) = repository.deleteCategory(categoryIds)
}
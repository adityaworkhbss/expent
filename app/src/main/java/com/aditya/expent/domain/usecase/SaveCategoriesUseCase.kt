package com.aditya.expent.domain.usecase

import com.aditya.expent.domain.model.OnboardCategory
import com.aditya.expent.domain.repository.CategoryRepository
import javax.inject.Inject

class SaveCategoriesUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke(categories: List<OnboardCategory>): Result<Unit> {
        return repository.addCategories(categories)
    }
}

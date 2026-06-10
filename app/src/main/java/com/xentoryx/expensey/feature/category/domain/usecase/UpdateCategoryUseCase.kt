package com.xentoryx.expensey.feature.category.domain.usecase

import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.category.domain.model.Category
import com.xentoryx.expensey.feature.category.domain.repository.CategoryRepository

class UpdateCategoryUseCase(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke(
        id: String,
        name: String,
        icon: String? = null,
        color: String? = null
    ): Result<Category, DataError> {
        if (name.isBlank()) {
            return Result.Error(DataError.Api("Category name cannot be empty"))
        }
        return repository.updateCategory(id, name.trim(), icon, color)
    }
}

package com.xentoryx.expensey.feature.category.domain.usecase

import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.category.domain.model.Category
import com.xentoryx.expensey.feature.category.domain.repository.CategoryRepository

class CreateCategoryUseCase(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke(
        name: String,
        type: String,
        parentId: String? = null,
        icon: String? = null,
        color: String? = null
    ): Result<Category, DataError> {
        if (name.isBlank()) {
            return Result.Error(DataError.Api("Category name cannot be empty"))
        }
        return repository.createCategory(name.trim(), type, parentId, icon, color)
    }
}

package com.xentoryx.expensey.feature.category.domain.usecase

import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.category.domain.repository.CategoryRepository

class DeleteCategoryUseCase(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke(id: String): Result<Unit, DataError> {
        if (id.isBlank()) {
            return Result.Error(DataError.Api("Category ID cannot be empty"))
        }
        return repository.deleteCategory(id)
    }
}

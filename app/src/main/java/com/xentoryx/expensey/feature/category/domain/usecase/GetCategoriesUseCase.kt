package com.xentoryx.expensey.feature.category.domain.usecase

import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.category.domain.model.Category
import com.xentoryx.expensey.feature.category.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow

class GetCategoriesUseCase(
    private val repository: CategoryRepository
) {
    operator fun invoke(): Flow<List<Category>> {
        return repository.getCategoriesFlow()
    }

    suspend fun sync(): Result<Unit, DataError> {
        return repository.syncCategories()
    }
}

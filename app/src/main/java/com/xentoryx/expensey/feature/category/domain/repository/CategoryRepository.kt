package com.xentoryx.expensey.feature.category.domain.repository

import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.category.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getCategoriesFlow(): Flow<List<Category>>
    
    suspend fun createCategory(
        name: String,
        type: String,
        parentId: String?,
        icon: String?,
        color: String?
    ): Result<Category, DataError>

    suspend fun updateCategory(
        id: String,
        name: String,
        icon: String?,
        color: String?
    ): Result<Category, DataError>

    suspend fun deleteCategory(id: String): Result<Unit, DataError>
    
    suspend fun syncCategories(): Result<Unit, DataError>
}

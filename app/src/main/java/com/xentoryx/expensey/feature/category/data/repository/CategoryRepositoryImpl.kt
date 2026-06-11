package com.xentoryx.expensey.feature.category.data.repository

import com.xentoryx.expensey.core.data.networking.safeCall
import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.category.data.remote.api.CategoryApiService
import com.xentoryx.expensey.feature.category.data.remote.dto.CategoryResponseDto
import com.xentoryx.expensey.feature.category.data.remote.dto.CreateCategoryRequestDto
import com.xentoryx.expensey.feature.category.data.remote.dto.UpdateCategoryRequestDto
import com.xentoryx.expensey.feature.category.domain.model.Category
import com.xentoryx.expensey.feature.category.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class CategoryRepositoryImpl(
    private val apiService: CategoryApiService
) : CategoryRepository {

    private val _categoriesFlow = MutableStateFlow<List<Category>>(emptyList())

    override fun getCategoriesFlow(): Flow<List<Category>> = _categoriesFlow.asStateFlow()

    override suspend fun createCategory(
        name: String,
        type: String,
        parentId: String?,
        icon: String?,
        color: String?
    ): Result<Category, DataError> {
        val result = safeCall<CategoryResponseDto> {
            apiService.createCategory(
                CreateCategoryRequestDto(
                    name = name,
                    type = type,
                    parentId = parentId,
                    icon = icon,
                    color = color
                )
            )
        }
        return when (result) {
            is Result.Success -> {
                val category = result.data.toDomain()
                _categoriesFlow.value = _categoriesFlow.value + category
                Result.Success(category)
            }
            is Result.Error -> result
        }
    }

    override suspend fun updateCategory(
        id: String,
        name: String,
        icon: String?,
        color: String?
    ): Result<Category, DataError> {
        val result = safeCall<CategoryResponseDto> {
            apiService.updateCategory(id, UpdateCategoryRequestDto(name = name, icon = icon, color = color))
        }
        return when (result) {
            is Result.Success -> {
                val updated = result.data.toDomain()
                _categoriesFlow.value = _categoriesFlow.value.map { if (it.id == id) updated else it }
                Result.Success(updated)
            }
            is Result.Error -> result
        }
    }

    override suspend fun deleteCategory(id: String): Result<Unit, DataError> {
        val result = safeCall<Unit> { apiService.deleteCategory(id) }
        return when (result) {
            is Result.Success -> {
                _categoriesFlow.value = _categoriesFlow.value.filter { it.id != id }
                Result.Success(Unit)
            }
            is Result.Error -> result
        }
    }

    override suspend fun syncCategories(): Result<Unit, DataError> {
        val result = safeCall<List<CategoryResponseDto>> { apiService.getCategories() }
        return when (result) {
            is Result.Success -> {
                _categoriesFlow.value = result.data.map { it.toDomain() }
                Result.Success(Unit)
            }
            is Result.Error -> result
        }
    }
}

fun CategoryResponseDto.toDomain() = Category(
    id = id,
    name = name,
    type = type,
    parentId = parentId,
    icon = icon,
    color = color,
    isSystem = isSystem
)

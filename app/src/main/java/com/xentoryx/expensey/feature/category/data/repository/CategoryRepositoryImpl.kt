package com.xentoryx.expensey.feature.category.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.xentoryx.expensey.core.data.database.dao.CategoryDao
import com.xentoryx.expensey.core.data.database.entity.CategoryEntity
import com.xentoryx.expensey.core.data.database.entity.SyncStatus
import com.xentoryx.expensey.core.data.networking.safeCall
import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.core.sync.SyncCategoriesWorker
import com.xentoryx.expensey.feature.category.data.remote.api.CategoryApiService
import com.xentoryx.expensey.feature.category.data.remote.dto.CategoryResponseDto
import com.xentoryx.expensey.feature.category.domain.model.Category
import com.xentoryx.expensey.feature.category.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class CategoryRepositoryImpl(
    private val context: Context,
    private val categoryDao: CategoryDao,
    private val apiService: CategoryApiService
) : CategoryRepository {

    override fun getCategoriesFlow(): Flow<List<Category>> {
        return categoryDao.getCategoriesFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun createCategory(
        name: String,
        type: String,
        parentId: String?,
        icon: String?,
        color: String?
    ): Result<Category, DataError> {
        val id = UUID.randomUUID().toString()
        val entity = CategoryEntity(
            id = id,
            name = name,
            type = type,
            parentId = parentId,
            icon = icon,
            color = color,
            isSystem = false,
            syncStatus = SyncStatus.PENDING,
            isNewLocal = true,
            isDeleted = false
        )

        try {
            categoryDao.insertCategory(entity)
        } catch (e: Exception) {
            return Result.Error(DataError.Api("Failed to save category locally"))
        }

        triggerSync()
        return Result.Success(entity.toDomain())
    }

    override suspend fun updateCategory(
        id: String,
        name: String,
        icon: String?,
        color: String?
    ): Result<Category, DataError> {
        val existing = try {
            categoryDao.getCategoryById(id)
        } catch (e: Exception) {
            null
        } ?: return Result.Error(DataError.Api("Category not found"))

        val updated = existing.copy(
            name = name,
            icon = icon,
            color = color,
            syncStatus = SyncStatus.PENDING
        )

        try {
            categoryDao.insertCategory(updated)
        } catch (e: Exception) {
            return Result.Error(DataError.Api("Failed to update category locally"))
        }

        triggerSync()
        return Result.Success(updated.toDomain())
    }

    override suspend fun deleteCategory(id: String): Result<Unit, DataError> {
        try {
            categoryDao.markCategoryDeleted(id)
        } catch (e: Exception) {
            return Result.Error(DataError.Api("Failed to delete category locally"))
        }

        triggerSync()
        return Result.Success(Unit)
    }

    override suspend fun syncCategories(): Result<Unit, DataError> {
        val responseResult = safeCall<List<CategoryResponseDto>> {
            apiService.getCategories()
        }

        return when (responseResult) {
            is Result.Success -> {
                try {
                    val networkEntities = responseResult.data.map { dto ->
                        CategoryEntity(
                            id = dto.id,
                            name = dto.name,
                            type = dto.type,
                            parentId = dto.parentId,
                            icon = dto.icon,
                            color = dto.color,
                            isSystem = dto.isSystem,
                            syncStatus = SyncStatus.SYNCED,
                            isNewLocal = false,
                            isDeleted = false
                        )
                    }
                    categoryDao.replaceCategories(networkEntities)
                    Result.Success(Unit)
                } catch (e: Exception) {
                    Result.Error(DataError.Api("Failed to save synced categories locally"))
                }
            }
            is Result.Error -> {
                Result.Error(responseResult.error)
            }
        }
    }

    private fun triggerSync() {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncCategoriesWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(syncRequest)
        } catch (e: Exception) {
            // Ignore WorkManager setup failures
        }
    }
}

fun CategoryEntity.toDomain() = Category(
    id = id,
    name = name,
    type = type,
    parentId = parentId,
    icon = icon,
    color = color,
    isSystem = isSystem
)

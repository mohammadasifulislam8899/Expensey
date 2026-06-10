package com.xentoryx.expensey.core.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.xentoryx.expensey.core.data.database.dao.CategoryDao
import com.xentoryx.expensey.core.data.database.entity.CategoryEntity
import com.xentoryx.expensey.feature.category.data.remote.api.CategoryApiService
import com.xentoryx.expensey.feature.category.data.remote.dto.CategoryResponseDto
import com.xentoryx.expensey.feature.category.data.remote.dto.CreateCategoryRequestDto
import com.xentoryx.expensey.feature.category.data.remote.dto.UpdateCategoryRequestDto
import io.ktor.client.call.body
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SyncCategoriesWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val categoryDao: CategoryDao by inject()
    private val apiService: CategoryApiService by inject()

    override suspend fun doWork(): Result {
        var allSuccessful = true

        // 1. Process deletions
        val deletions = try {
            categoryDao.getUnsyncedDeletions()
        } catch (e: Exception) {
            return Result.failure()
        }

        for (category in deletions) {
            try {
                if (category.isNewLocal) {
                    categoryDao.deleteCategoryById(category.id)
                } else {
                    val response = apiService.deleteCategory(category.id)
                    if (response.status.value in 200..299 || response.status.value == 404) {
                        categoryDao.deleteCategoryById(category.id)
                    } else {
                        allSuccessful = false
                    }
                }
            } catch (e: Exception) {
                allSuccessful = false
            }
        }

        // 2. Process insertions & updates
        val unsynced = try {
            categoryDao.getUnsyncedCategories()
        } catch (e: Exception) {
            return Result.failure()
        }

        for (category in unsynced) {
            try {
                if (category.isNewLocal) {
                    val request = CreateCategoryRequestDto(
                        name = category.name,
                        type = category.type,
                        parentId = category.parentId,
                        icon = category.icon,
                        color = category.color
                    )
                    val response = apiService.createCategory(request)
                    if (response.status.value in 200..299) {
                        val responseDto = response.body<CategoryResponseDto>()
                        categoryDao.deleteCategoryById(category.id)
                        categoryDao.insertCategory(
                            CategoryEntity(
                                id = responseDto.id,
                                name = responseDto.name,
                                type = responseDto.type,
                                parentId = responseDto.parentId,
                                icon = responseDto.icon,
                                color = responseDto.color,
                                isSystem = responseDto.isSystem,
                                isSynced = true,
                                isNewLocal = false,
                                isDeleted = false
                            )
                        )
                    } else {
                        allSuccessful = false
                    }
                } else {
                    val request = UpdateCategoryRequestDto(
                        name = category.name,
                        icon = category.icon,
                        color = category.color
                    )
                    val response = apiService.updateCategory(category.id, request)
                    if (response.status.value in 200..299) {
                        val responseDto = response.body<CategoryResponseDto>()
                        categoryDao.insertCategory(
                            CategoryEntity(
                                id = responseDto.id,
                                name = responseDto.name,
                                type = responseDto.type,
                                parentId = responseDto.parentId,
                                icon = responseDto.icon,
                                color = responseDto.color,
                                isSystem = responseDto.isSystem,
                                isSynced = true,
                                isNewLocal = false,
                                isDeleted = false
                            )
                        )
                    } else {
                        allSuccessful = false
                    }
                }
            } catch (e: Exception) {
                allSuccessful = false
            }
        }

        return if (allSuccessful) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}

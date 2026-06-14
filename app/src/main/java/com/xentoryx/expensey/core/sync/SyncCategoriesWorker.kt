package com.xentoryx.expensey.core.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.xentoryx.expensey.core.data.database.dao.CategoryDao
import com.xentoryx.expensey.core.data.database.dao.TransactionDao
import com.xentoryx.expensey.core.data.database.dao.BudgetDao
import com.xentoryx.expensey.core.data.database.dao.RecurringTransactionDao
import com.xentoryx.expensey.core.data.database.entity.CategoryEntity
import com.xentoryx.expensey.feature.category.data.remote.api.CategoryApiService
import com.xentoryx.expensey.feature.category.data.remote.dto.CategoryResponseDto
import com.xentoryx.expensey.feature.category.data.remote.dto.CreateCategoryRequestDto
import com.xentoryx.expensey.feature.category.data.remote.dto.UpdateCategoryRequestDto
import com.xentoryx.expensey.core.data.database.entity.SyncStatus
import com.xentoryx.expensey.core.data.networking.tryToRefreshToken
import io.ktor.client.call.body
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SyncCategoriesWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val categoryDao: CategoryDao by inject()
    private val apiService: CategoryApiService by inject()
    private val transactionDao: TransactionDao by inject()
    private val budgetDao: BudgetDao by inject()
    private val recurringTransactionDao: RecurringTransactionDao by inject()

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
                    var response = apiService.deleteCategory(category.id)
                    if (response.status.value == 401) {
                        if (tryToRefreshToken()) {
                            response = apiService.deleteCategory(category.id)
                        }
                    }
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
                    var response = apiService.createCategory(request)
                    if (response.status.value == 401) {
                        if (tryToRefreshToken()) {
                            response = apiService.createCategory(request)
                        }
                    }
                    if (response.status.value in 200..299) {
                        val responseDto = response.body<CategoryResponseDto>()
                        val oldId = category.id
                        val newId = responseDto.id
                        
                        // Cascade ID updates to other tables
                        transactionDao.updateTransactionCategoryId(oldId, newId)
                        budgetDao.updateBudgetCategoryId(oldId, newId)
                        recurringTransactionDao.updateRecurringTransactionCategoryId(oldId, newId)

                        categoryDao.deleteCategoryById(oldId)
                        categoryDao.insertCategory(
                            CategoryEntity(
                                id = newId,
                                name = responseDto.name,
                                type = responseDto.type,
                                parentId = responseDto.parentId,
                                icon = responseDto.icon,
                                color = responseDto.color,
                                isSystem = responseDto.isSystem,
                                syncStatus = SyncStatus.SYNCED,
                                isNewLocal = false,
                                isDeleted = false
                            )
                        )
                    } else {
                        categoryDao.insertCategory(category.copy(syncStatus = SyncStatus.FAILED))
                        allSuccessful = false
                    }
                } else {
                    val request = UpdateCategoryRequestDto(
                        name = category.name,
                        icon = category.icon,
                        color = category.color
                    )
                    var response = apiService.updateCategory(category.id, request)
                    if (response.status.value == 401) {
                        if (tryToRefreshToken()) {
                            response = apiService.updateCategory(category.id, request)
                        }
                    }
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
                                syncStatus = SyncStatus.SYNCED,
                                isNewLocal = false,
                                isDeleted = false
                            )
                        )
                    } else {
                        categoryDao.insertCategory(category.copy(syncStatus = SyncStatus.FAILED))
                        allSuccessful = false
                    }
                }
            } catch (e: Exception) {
                try {
                    categoryDao.insertCategory(category.copy(syncStatus = SyncStatus.FAILED))
                } catch (_: Exception) {}
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

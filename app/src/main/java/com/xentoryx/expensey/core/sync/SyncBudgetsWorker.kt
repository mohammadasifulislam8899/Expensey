package com.xentoryx.expensey.core.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.xentoryx.expensey.core.data.database.dao.BudgetDao
import com.xentoryx.expensey.core.data.database.entity.BudgetEntity
import com.xentoryx.expensey.feature.budget.data.remote.api.BudgetApiService
import com.xentoryx.expensey.feature.budget.data.remote.dto.CreateBudgetRequestDto
import com.xentoryx.expensey.feature.budget.data.remote.dto.UpdateBudgetRequestDto
import com.xentoryx.expensey.feature.budget.data.remote.dto.BudgetResponseDto
import com.xentoryx.expensey.core.data.database.entity.SyncStatus
import com.xentoryx.expensey.core.data.networking.tryToRefreshToken
import io.ktor.client.call.body
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SyncBudgetsWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val budgetDao: BudgetDao by inject()
    private val apiService: BudgetApiService by inject()

    override suspend fun doWork(): Result {
        var allSuccessful = true

        // 1. Process deletions
        val deletions = try {
            budgetDao.getUnsyncedDeletions()
        } catch (e: Exception) {
            return Result.failure()
        }

        for (budget in deletions) {
            try {
                if (budget.isNewLocal) {
                    // Created and deleted offline, never reached server
                    budgetDao.deleteBudgetById(budget.id)
                } else {
                    var response = apiService.deleteBudget(budget.id)
                    if (response.status.value == 401) {
                        if (tryToRefreshToken()) {
                            response = apiService.deleteBudget(budget.id)
                        }
                    }
                    if (response.status.value in 200..299 || response.status.value == 404) {
                        budgetDao.deleteBudgetById(budget.id)
                    } else {
                        allSuccessful = false
                    }
                }
            } catch (e: Exception) {
                allSuccessful = false
            }
        }

        // 2. Process insertions and updates
        val unsynced = try {
            budgetDao.getUnsyncedBudgets()
        } catch (e: Exception) {
            return Result.failure()
        }

        for (budget in unsynced) {
            try {
                if (budget.isNewLocal) {
                    val request = CreateBudgetRequestDto(
                        categoryId = budget.categoryId,
                        amountLimit = budget.amountLimit,
                        period = budget.period,
                        startDate = budget.startDate.ifBlank { null },
                        endDate = budget.endDate.ifBlank { null }
                    )
                    var response = apiService.createBudget(request)
                    if (response.status.value == 401) {
                        if (tryToRefreshToken()) {
                            response = apiService.createBudget(request)
                        }
                    }
                    if (response.status.value in 200..299) {
                        val responseDto = response.body<BudgetResponseDto>()
                        budgetDao.deleteBudgetById(budget.id)
                        budgetDao.insertBudget(
                            BudgetEntity(
                                id = responseDto.id,
                                categoryId = responseDto.categoryId,
                                categoryName = responseDto.categoryName,
                                categoryIcon = responseDto.categoryIcon,
                                categoryColor = responseDto.categoryColor,
                                amountLimit = responseDto.amountLimit,
                                period = responseDto.period,
                                startDate = responseDto.startDate,
                                endDate = responseDto.endDate,
                                spent = responseDto.spent,
                                remaining = responseDto.remaining,
                                percentage = responseDto.percentage,
                                isExceeded = responseDto.isExceeded,
                                syncStatus = SyncStatus.SYNCED,
                                isNewLocal = false,
                                isDeleted = false
                            )
                        )
                    } else {
                        budgetDao.insertBudget(budget.copy(syncStatus = SyncStatus.FAILED))
                        allSuccessful = false
                    }
                } else {
                    val request = UpdateBudgetRequestDto(
                        amountLimit = budget.amountLimit,
                        period = budget.period,
                        startDate = budget.startDate.ifBlank { null },
                        endDate = budget.endDate.ifBlank { null }
                    )
                    var response = apiService.updateBudget(budget.id, request)
                    if (response.status.value == 401) {
                        if (tryToRefreshToken()) {
                            response = apiService.updateBudget(budget.id, request)
                        }
                    }
                    if (response.status.value in 200..299) {
                        val responseDto = response.body<BudgetResponseDto>()
                        budgetDao.insertBudget(
                            BudgetEntity(
                                id = responseDto.id,
                                categoryId = responseDto.categoryId,
                                categoryName = responseDto.categoryName,
                                categoryIcon = responseDto.categoryIcon,
                                categoryColor = responseDto.categoryColor,
                                amountLimit = responseDto.amountLimit,
                                period = responseDto.period,
                                startDate = responseDto.startDate,
                                endDate = responseDto.endDate,
                                spent = responseDto.spent,
                                remaining = responseDto.remaining,
                                percentage = responseDto.percentage,
                                isExceeded = responseDto.isExceeded,
                                syncStatus = SyncStatus.SYNCED,
                                isNewLocal = false,
                                isDeleted = false
                            )
                        )
                    } else {
                        budgetDao.insertBudget(budget.copy(syncStatus = SyncStatus.FAILED))
                        allSuccessful = false
                    }
                }
            } catch (e: Exception) {
                try {
                    budgetDao.insertBudget(budget.copy(syncStatus = SyncStatus.FAILED))
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

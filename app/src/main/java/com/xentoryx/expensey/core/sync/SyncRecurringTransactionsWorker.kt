package com.xentoryx.expensey.core.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.xentoryx.expensey.core.data.database.dao.RecurringTransactionDao
import com.xentoryx.expensey.core.data.database.entity.RecurringTransactionEntity
import com.xentoryx.expensey.feature.recurring_transaction.data.remote.api.RecurringApiService
import com.xentoryx.expensey.feature.recurring_transaction.data.remote.dto.CreateRecurringTransactionRequestDto
import com.xentoryx.expensey.feature.recurring_transaction.data.remote.dto.UpdateRecurringTransactionRequestDto
import com.xentoryx.expensey.feature.recurring_transaction.data.remote.dto.RecurringTransactionResponseDto
import io.ktor.client.call.body
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SyncRecurringTransactionsWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val recurringDao: RecurringTransactionDao by inject()
    private val apiService: RecurringApiService by inject()

    override suspend fun doWork(): Result {
        var allSuccessful = true

        // 1. Process deletions
        val deletions = try {
            recurringDao.getUnsyncedDeletions()
        } catch (e: Exception) {
            return Result.failure()
        }

        for (recurring in deletions) {
            try {
                if (recurring.isNewLocal) {
                    // Created and deleted offline, never reached server
                    recurringDao.deleteRecurringTransactionById(recurring.id)
                } else {
                    val response = apiService.deleteRecurringTransaction(recurring.id)
                    if (response.status.value in 200..299 || response.status.value == 404) {
                        recurringDao.deleteRecurringTransactionById(recurring.id)
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
            recurringDao.getUnsyncedRecurringTransactions()
        } catch (e: Exception) {
            return Result.failure()
        }

        for (recurring in unsynced) {
            try {
                if (recurring.isNewLocal) {
                    val request = CreateRecurringTransactionRequestDto(
                        accountId = recurring.accountId,
                        categoryId = recurring.categoryId,
                        amount = recurring.amount,
                        type = recurring.type,
                        frequency = recurring.frequency,
                        note = recurring.note?.ifBlank { null },
                        startDate = recurring.startDate.ifBlank { null },
                        endDate = recurring.endDate?.ifBlank { null }
                    )
                    val response = apiService.createRecurringTransaction(request)
                    if (response.status.value in 200..299) {
                        val responseDto = response.body<RecurringTransactionResponseDto>()
                        recurringDao.deleteRecurringTransactionById(recurring.id)
                        recurringDao.insertRecurringTransaction(
                            RecurringTransactionEntity(
                                id = responseDto.id,
                                accountId = responseDto.accountId,
                                categoryId = responseDto.categoryId,
                                amount = responseDto.amount,
                                type = responseDto.type,
                                frequency = responseDto.frequency,
                                note = responseDto.note,
                                startDate = responseDto.startDate,
                                endDate = responseDto.endDate,
                                nextRunDate = responseDto.nextRunDate,
                                isActive = responseDto.isActive,
                                createdAt = responseDto.createdAt,
                                isSynced = true,
                                isNewLocal = false,
                                isDeleted = false
                            )
                        )
                    } else {
                        allSuccessful = false
                    }
                } else {
                    val request = UpdateRecurringTransactionRequestDto(
                        accountId = recurring.accountId,
                        categoryId = recurring.categoryId,
                        amount = recurring.amount,
                        type = recurring.type,
                        frequency = recurring.frequency,
                        note = recurring.note?.ifBlank { null },
                        startDate = recurring.startDate.ifBlank { null },
                        endDate = recurring.endDate?.ifBlank { null }
                    )
                    val response = apiService.updateRecurringTransaction(recurring.id, request)
                    if (response.status.value in 200..299) {
                        val responseDto = response.body<RecurringTransactionResponseDto>()
                        recurringDao.insertRecurringTransaction(
                            RecurringTransactionEntity(
                                id = responseDto.id,
                                accountId = responseDto.accountId,
                                categoryId = responseDto.categoryId,
                                amount = responseDto.amount,
                                type = responseDto.type,
                                frequency = responseDto.frequency,
                                note = responseDto.note,
                                startDate = responseDto.startDate,
                                endDate = responseDto.endDate,
                                nextRunDate = responseDto.nextRunDate,
                                isActive = responseDto.isActive,
                                createdAt = responseDto.createdAt,
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

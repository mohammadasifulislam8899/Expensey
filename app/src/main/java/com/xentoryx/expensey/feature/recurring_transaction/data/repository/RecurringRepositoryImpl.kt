package com.xentoryx.expensey.feature.recurring_transaction.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.xentoryx.expensey.core.data.database.dao.RecurringTransactionDao
import com.xentoryx.expensey.core.data.database.entity.RecurringTransactionEntity
import com.xentoryx.expensey.core.data.database.entity.SyncStatus
import com.xentoryx.expensey.core.data.networking.safeCall
import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.core.sync.SyncRecurringTransactionsWorker
import com.xentoryx.expensey.feature.recurring_transaction.data.remote.api.RecurringApiService
import com.xentoryx.expensey.feature.recurring_transaction.data.remote.dto.RecurringTransactionResponseDto
import com.xentoryx.expensey.feature.recurring_transaction.domain.model.RecurringTransaction
import com.xentoryx.expensey.feature.recurring_transaction.domain.repository.RecurringRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class RecurringRepositoryImpl(
    private val context: Context,
    private val recurringDao: RecurringTransactionDao,
    private val apiService: RecurringApiService
) : RecurringRepository {

    override fun getRecurringTransactionsFlow(): Flow<List<RecurringTransaction>> {
        return recurringDao.getRecurringTransactionsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun createRecurringTransaction(
        accountId: String,
        categoryId: String,
        amount: Double,
        type: String,
        frequency: String,
        note: String?,
        startDate: String?,
        endDate: String?
    ): Result<RecurringTransaction, DataError> {
        val id = UUID.randomUUID().toString()
        val resolvedStartDate = startDate ?: LocalDate.now().toString()
        val entity = RecurringTransactionEntity(
            id = id,
            accountId = accountId,
            categoryId = categoryId,
            amount = amount,
            type = type,
            frequency = frequency,
            note = note,
            startDate = resolvedStartDate,
            endDate = endDate,
            nextRunDate = resolvedStartDate,
            isActive = true,
            createdAt = LocalDateTime.now().toString(),
            syncStatus = SyncStatus.PENDING,
            isNewLocal = true,
            isDeleted = false
        )

        try {
            recurringDao.insertRecurringTransaction(entity)
        } catch (e: Exception) {
            return Result.Error(DataError.Api("Failed to save recurring transaction locally"))
        }

        triggerSync()
        return Result.Success(entity.toDomain())
    }

    override suspend fun updateRecurringTransaction(
        id: String,
        accountId: String,
        categoryId: String,
        amount: Double,
        type: String,
        frequency: String,
        note: String?,
        startDate: String?,
        endDate: String?
    ): Result<RecurringTransaction, DataError> {
        val existing = try {
            recurringDao.getRecurringTransactionById(id)
        } catch (e: Exception) {
            null
        } ?: return Result.Error(DataError.Api("Recurring transaction not found"))

        val resolvedStartDate = startDate ?: existing.startDate
        val updated = existing.copy(
            accountId = accountId,
            categoryId = categoryId,
            amount = amount,
            type = type,
            frequency = frequency,
            note = note,
            startDate = resolvedStartDate,
            endDate = endDate,
            nextRunDate = resolvedStartDate, // reset/recompute on next execution or sync
            syncStatus = SyncStatus.PENDING
        )

        try {
            recurringDao.insertRecurringTransaction(updated)
        } catch (e: Exception) {
            return Result.Error(DataError.Api("Failed to update recurring transaction locally"))
        }

        triggerSync()
        return Result.Success(updated.toDomain())
    }

    override suspend fun deleteRecurringTransaction(id: String): Result<Unit, DataError> {
        try {
            recurringDao.markRecurringTransactionDeleted(id)
        } catch (e: Exception) {
            return Result.Error(DataError.Api("Failed to delete recurring transaction locally"))
        }

        triggerSync()
        return Result.Success(Unit)
    }

    override suspend fun toggleActive(id: String, isActive: Boolean): Result<RecurringTransaction, DataError> {
        val existing = try {
            recurringDao.getRecurringTransactionById(id)
        } catch (e: Exception) {
            null
        } ?: return Result.Error(DataError.Api("Recurring transaction not found"))

        val updated = existing.copy(
            isActive = isActive,
            syncStatus = SyncStatus.PENDING
        )

        try {
            recurringDao.insertRecurringTransaction(updated)
        } catch (e: Exception) {
            return Result.Error(DataError.Api("Failed to update active status locally"))
        }

        triggerSync()
        return Result.Success(updated.toDomain())
    }

    override suspend fun syncRecurringTransactions(): Result<Unit, DataError> {
        val responseResult = safeCall<List<RecurringTransactionResponseDto>> {
            apiService.getRecurringTransactions()
        }

        return when (responseResult) {
            is Result.Success -> {
                try {
                    val entities = responseResult.data.map { dto ->
                        RecurringTransactionEntity(
                            id = dto.id,
                            accountId = dto.accountId,
                            categoryId = dto.categoryId,
                            amount = dto.amount,
                            type = dto.type,
                            frequency = dto.frequency,
                            note = dto.note,
                            startDate = dto.startDate,
                            endDate = dto.endDate,
                            nextRunDate = dto.nextRunDate,
                            isActive = dto.isActive,
                            createdAt = dto.createdAt,
                            syncStatus = SyncStatus.SYNCED,
                            isNewLocal = false,
                            isDeleted = false
                        )
                    }
                    recurringDao.replaceRecurringTransactions(entities)
                    Result.Success(Unit)
                } catch (e: Exception) {
                    Result.Error(DataError.Api("Failed to save synced recurring transactions locally"))
                }
            }
            is Result.Error -> Result.Error(responseResult.error)
        }
    }

    private fun triggerSync() {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncRecurringTransactionsWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(syncRequest)
        } catch (e: Exception) {
            // Ignore WorkManager setup failures
        }
    }
}

fun RecurringTransactionEntity.toDomain() = RecurringTransaction(
    id = id,
    accountId = accountId,
    categoryId = categoryId,
    amount = amount,
    type = type,
    frequency = frequency,
    note = note,
    startDate = startDate,
    endDate = endDate,
    nextRunDate = nextRunDate,
    isActive = isActive,
    createdAt = createdAt,
    syncStatus = syncStatus
)

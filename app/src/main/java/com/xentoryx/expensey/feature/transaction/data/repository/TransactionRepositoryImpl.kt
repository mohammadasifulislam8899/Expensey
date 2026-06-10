package com.xentoryx.expensey.feature.transaction.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.xentoryx.expensey.core.data.database.dao.TransactionDao
import com.xentoryx.expensey.core.data.database.entity.TransactionEntity
import com.xentoryx.expensey.core.data.networking.safeCall
import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.core.storage.TokenManager
import com.xentoryx.expensey.core.sync.SyncWorker
import com.xentoryx.expensey.feature.dashboard.data.mapper.toDomain
import com.xentoryx.expensey.feature.dashboard.data.mapper.toEntity
import com.xentoryx.expensey.feature.dashboard.domain.model.Transaction
import com.xentoryx.expensey.feature.transaction.data.remote.api.TransactionApiService
import com.xentoryx.expensey.feature.transaction.data.remote.dto.TransactionListResponseDto
import com.xentoryx.expensey.feature.transaction.data.remote.dto.UpdateTransactionRequestDto
import com.xentoryx.expensey.feature.dashboard.data.remote.dto.TransactionResponseDto
import com.xentoryx.expensey.feature.transaction.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID

class TransactionRepositoryImpl(
    private val context: Context,
    private val transactionDao: TransactionDao,
    private val tokenManager: TokenManager,
    private val apiService: TransactionApiService
) : TransactionRepository {

    override suspend fun createTransaction(
        accountId: String,
        categoryId: String,
        transferToAccountId: String?,
        amount: Double,
        type: String,
        note: String?,
        transactionDate: String
    ): Result<Transaction, DataError> {
        val userId = tokenManager.getUserId() ?: return Result.Error(DataError.Api("Unauthorized"))
        val transactionId = UUID.randomUUID().toString()
        val now = Instant.now().toString()

        val entity = TransactionEntity(
            id = transactionId,
            userId = userId,
            accountId = accountId,
            categoryId = categoryId,
            transferToAccountId = transferToAccountId,
            amount = amount,
            type = type,
            note = note,
            transactionDate = transactionDate,
            createdAt = now,
            isSynced = false
        )

        try {
            transactionDao.insertTransaction(entity)
        } catch (e: Exception) {
            return Result.Error(DataError.Api("Failed to save transaction locally"))
        }

        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(syncRequest)
        } catch (e: Exception) {
            // Ignore WorkManager initialization errors
        }

        return Result.Success(entity.toDomain())
    }

    override fun getTransactionsFlow(): Flow<List<Transaction>> {
        return transactionDao.getRecentTransactionsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun syncTransactions(page: Int, limit: Int): Result<Unit, DataError> {
        val responseResult = safeCall<TransactionListResponseDto> {
            apiService.getTransactions(page, limit)
        }

        return when (responseResult) {
            is Result.Success -> {
                try {
                    val networkTransactions = responseResult.data.data
                    transactionDao.insertTransactions(networkTransactions.map { it.toEntity(isSynced = true) })
                    Result.Success(Unit)
                } catch (e: Exception) {
                    Result.Error(DataError.Api("Failed to save synced transactions locally"))
                }
            }
            is Result.Error -> {
                Result.Error(responseResult.error)
            }
        }
    }

    override suspend fun getTransactionById(id: String): Transaction? {
        return transactionDao.getTransactionById(id)?.toDomain()
    }

    override suspend fun updateTransaction(
        id: String,
        accountId: String,
        categoryId: String,
        transferToAccountId: String?,
        amount: Double,
        type: String,
        note: String?,
        transactionDate: String
    ): Result<Transaction, DataError> {
        val request = UpdateTransactionRequestDto(
            accountId = accountId,
            categoryId = categoryId,
            transferToAccountId = transferToAccountId,
            amount = amount,
            type = type,
            note = note,
            transactionDate = transactionDate
        )
        val responseResult = safeCall<TransactionResponseDto> {
            apiService.updateTransaction(id, request)
        }
        return when (responseResult) {
            is Result.Success -> {
                val dto = responseResult.data
                val entity = dto.toEntity(isSynced = true)
                try {
                    transactionDao.insertTransaction(entity)
                    Result.Success(entity.toDomain())
                } catch (e: Exception) {
                    Result.Error(DataError.Api("Failed to save updated transaction locally"))
                }
            }
            is Result.Error -> Result.Error(responseResult.error)
        }
    }

    override suspend fun deleteTransaction(id: String): Result<Unit, DataError> {
        val responseResult = safeCall<Unit> {
            apiService.deleteTransaction(id)
        }
        return when (responseResult) {
            is Result.Success -> {
                try {
                    transactionDao.deleteTransactionById(id)
                    Result.Success(Unit)
                } catch (e: Exception) {
                    Result.Error(DataError.Api("Failed to delete transaction locally"))
                }
            }
            is Result.Error -> Result.Error(responseResult.error)
        }
    }
}

package com.xentoryx.expensey.feature.transaction.data.repository

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.xentoryx.expensey.core.data.database.dao.AccountDao
import com.xentoryx.expensey.core.data.database.dao.CategoryDao
import com.xentoryx.expensey.core.data.database.dao.TransactionDao
import com.xentoryx.expensey.core.data.database.entity.TransactionEntity
import com.xentoryx.expensey.core.data.database.entity.SyncStatus
import com.xentoryx.expensey.core.data.networking.safeCall
import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.core.storage.TokenManager
import com.xentoryx.expensey.core.storage.CurrencyConverter
import com.xentoryx.expensey.core.sync.SyncCategoriesWorker
import com.xentoryx.expensey.core.sync.SyncWorker
import com.xentoryx.expensey.feature.dashboard.data.mapper.toDomain
import com.xentoryx.expensey.feature.dashboard.data.mapper.toEntity
import com.xentoryx.expensey.feature.dashboard.domain.model.Transaction
import com.xentoryx.expensey.feature.transaction.data.remote.api.TransactionApiService
import com.xentoryx.expensey.feature.transaction.data.remote.dto.CreateTransactionRequestDto
import com.xentoryx.expensey.feature.transaction.data.remote.dto.TransactionListResponseDto
import com.xentoryx.expensey.feature.transaction.data.remote.dto.UpdateTransactionRequestDto
import com.xentoryx.expensey.feature.dashboard.data.remote.dto.TransactionResponseDto
import com.xentoryx.expensey.feature.transaction.domain.repository.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

class TransactionRepositoryImpl(
    private val context: Context,
    private val transactionDao: TransactionDao,
    private val tokenManager: TokenManager,
    private val apiService: TransactionApiService,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val currencyConverter: CurrencyConverter
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
        val userId = tokenManager.getUserId()
            ?: return Result.Error(DataError.Api("Unauthorized"))

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
            note = note ?: "",
            transactionDate = transactionDate,
            createdAt = now,
            syncStatus = SyncStatus.PENDING,
            isNewLocal = true,
            isDeleted = false
        )

        try {
            // 1. Insert transaction locally
            transactionDao.insertTransaction(entity)

            // 2. Adjust local account balance(s)
            when (type) {
                "INCOME" -> accountDao.adjustBalance(accountId, amount)
                "EXPENSE" -> accountDao.adjustBalance(accountId, -amount)
                "TRANSFER" -> {
                    accountDao.adjustBalance(accountId, -amount)
                    transferToAccountId?.let { toId ->
                        val sourceAcc = accountDao.getAccountById(accountId)
                        val targetAcc = accountDao.getAccountById(toId)
                        val sourceCurrency = sourceAcc?.currencyCode ?: "BDT"
                        val targetCurrency = targetAcc?.currencyCode ?: "BDT"
                        val converted = currencyConverter.convert(amount, sourceCurrency, targetCurrency)
                        accountDao.adjustBalance(toId, converted)
                    }
                }
            }
        } catch (e: Exception) {
            return Result.Error(DataError.Api("Failed to save transaction locally"))
        }

        // 3. Launch backend sync asynchronously
        //    If the selected category is still local-only (not yet synced to backend),
        //    we skip the immediate sync and rely on the chained WorkManager workers:
        //    SyncCategoriesWorker -> SyncWorker (transactions)
        //    This prevents 404 "Category not found" errors from the backend.
        val request = CreateTransactionRequestDto(
            id = transactionId,
            accountId = accountId,
            categoryId = categoryId,
            transferToAccountId = transferToAccountId,
            amount = amount,
            type = type,
            note = note,
            transactionDate = transactionDate
        )

        val isCategoryLocalOnly = try {
            val cat = categoryDao.getCategoryById(categoryId)
            cat != null && cat.isNewLocal
        } catch (_: Exception) { false }

        if (!isCategoryLocalOnly) {
            // Category is synced — attempt immediate push to backend
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Log.d("TransactionRepository", "Immediate sync: Creating transaction $transactionId on backend...")
                    val networkResult = safeCall<TransactionResponseDto> {
                        apiService.createTransaction(request)
                    }
                    when (networkResult) {
                        is Result.Success -> {
                            transactionDao.markSynced(transactionId)
                            Log.d("TransactionRepository", "Immediate sync: Transaction $transactionId synced successfully.")
                        }
                        is Result.Error -> {
                            val errMsg = when (val err = networkResult.error) {
                                is DataError.Api -> err.message
                                is DataError.Network -> "Network Error"
                                is DataError.EmailNotVerified -> "Email Not Verified"
                            }
                            Log.e("TransactionRepository", "Immediate sync failed for $transactionId: $errMsg")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("TransactionRepository", "Immediate sync crashed for $transactionId", e)
                }
            }
        } else {
            Log.d("TransactionRepository", "Category $categoryId is local-only. Will sync after category sync.")
        }

        // Enqueue WorkManager workers: SyncCategoriesWorker first, then SyncWorker (transactions)
        // This guarantees category IDs are valid on the backend before transactions are pushed.
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val syncCategoriesRequest = OneTimeWorkRequestBuilder<SyncCategoriesWorker>()
                .setConstraints(constraints)
                .build()
            val syncTransactionsRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context)
                .beginWith(syncCategoriesRequest)
                .then(syncTransactionsRequest)
                .enqueue()
        } catch (_: Exception) { }

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
                    transactionDao.insertTransactions(networkTransactions.map { it.toEntity(syncStatus = SyncStatus.SYNCED) })
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
        val localTx = transactionDao.getTransactionById(id)
            ?: return Result.Error(DataError.Api("Transaction not found locally"))

        val userId = tokenManager.getUserId()
            ?: return Result.Error(DataError.Api("Unauthorized"))

        val updatedEntity = TransactionEntity(
            id = id,
            userId = userId,
            accountId = accountId,
            categoryId = categoryId,
            transferToAccountId = transferToAccountId,
            amount = amount,
            type = type,
            note = note ?: "",
            transactionDate = transactionDate,
            createdAt = localTx.createdAt,
            syncStatus = SyncStatus.PENDING,
            isNewLocal = localTx.isNewLocal,
            isDeleted = false
        )

        try {
            // 1. Reverse old local balance adjustment
            val oldAmount = localTx.amount
            when (localTx.type) {
                "INCOME" -> accountDao.adjustBalance(localTx.accountId, -oldAmount)
                "EXPENSE" -> accountDao.adjustBalance(localTx.accountId, oldAmount)
                "TRANSFER" -> {
                    accountDao.adjustBalance(localTx.accountId, oldAmount)
                    localTx.transferToAccountId?.let { toId ->
                        val oldSourceAcc = accountDao.getAccountById(localTx.accountId)
                        val oldTargetAcc = accountDao.getAccountById(toId)
                        val oldSourceCurrency = oldSourceAcc?.currencyCode ?: "BDT"
                        val oldTargetCurrency = oldTargetAcc?.currencyCode ?: "BDT"
                        val oldConverted = currencyConverter.convert(oldAmount, oldSourceCurrency, oldTargetCurrency)
                        accountDao.adjustBalance(toId, -oldConverted)
                    }
                }
            }

            // 2. Apply new local balance adjustment
            when (type) {
                "INCOME" -> accountDao.adjustBalance(accountId, amount)
                "EXPENSE" -> accountDao.adjustBalance(accountId, -amount)
                "TRANSFER" -> {
                    accountDao.adjustBalance(accountId, -amount)
                    transferToAccountId?.let { toId ->
                        val sourceAcc = accountDao.getAccountById(accountId)
                        val targetAcc = accountDao.getAccountById(toId)
                        val sourceCurrency = sourceAcc?.currencyCode ?: "BDT"
                        val targetCurrency = targetAcc?.currencyCode ?: "BDT"
                        val converted = currencyConverter.convert(amount, sourceCurrency, targetCurrency)
                        accountDao.adjustBalance(toId, converted)
                    }
                }
            }

            // 3. Save updated transaction locally
            transactionDao.insertTransaction(updatedEntity)
        } catch (e: Exception) {
            return Result.Error(DataError.Api("Failed to save updated transaction locally"))
        }

        // 4. Launch backend sync asynchronously
        val request = UpdateTransactionRequestDto(
            accountId = accountId,
            categoryId = categoryId,
            transferToAccountId = transferToAccountId,
            amount = amount,
            type = type,
            note = note,
            transactionDate = transactionDate
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("TransactionRepository", "Immediate sync: Updating transaction $id on backend...")
                val networkResult = safeCall<TransactionResponseDto> {
                    apiService.updateTransaction(id, request)
                }
                when (networkResult) {
                    is Result.Success -> {
                        transactionDao.markSynced(id)
                        Log.d("TransactionRepository", "Immediate sync: Transaction $id updated successfully on backend.")
                    }
                    is Result.Error -> {
                        val errMsg = when (val err = networkResult.error) {
                            is DataError.Api -> err.message
                            is DataError.Network -> "Network Error"
                            is DataError.EmailNotVerified -> "Email Not Verified"
                        }
                        Log.e("TransactionRepository", "Immediate sync failed for update of transaction $id: $errMsg")
                    }
                }
            } catch (e: Exception) {
                Log.e("TransactionRepository", "Immediate sync crashed for update of transaction $id", e)
            }
        }

        // Enqueue WorkManager for reliability
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueue(syncRequest)
        } catch (_: Exception) { }

        return Result.Success(updatedEntity.toDomain())
    }

    override suspend fun deleteTransaction(id: String): Result<Unit, DataError> {
        val localTx = transactionDao.getTransactionById(id)
            ?: return Result.Success(Unit)

        try {
            // 1. Delete locally (or soft delete if it was previously synced)
            if (localTx.isNewLocal) {
                transactionDao.deleteTransactionById(id)
            } else {
                transactionDao.markTransactionDeleted(id)
            }

            // 2. Reverse local balance adjustment
            val amount = localTx.amount
            when (localTx.type) {
                "INCOME" -> accountDao.adjustBalance(localTx.accountId, -amount)
                "EXPENSE" -> accountDao.adjustBalance(localTx.accountId, amount)
                "TRANSFER" -> {
                    accountDao.adjustBalance(localTx.accountId, amount)
                    localTx.transferToAccountId?.let { toId ->
                        val sourceAcc = accountDao.getAccountById(localTx.accountId)
                        val targetAcc = accountDao.getAccountById(toId)
                        val sourceCurrency = sourceAcc?.currencyCode ?: "BDT"
                        val targetCurrency = targetAcc?.currencyCode ?: "BDT"
                        val converted = currencyConverter.convert(amount, sourceCurrency, targetCurrency)
                        accountDao.adjustBalance(toId, -converted)
                    }
                }
            }
        } catch (e: Exception) {
            return Result.Error(DataError.Api("Failed to delete transaction locally"))
        }

        // 3. Trigger background sync
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueue(syncRequest)
        } catch (_: Exception) { }

        return Result.Success(Unit)
    }
}

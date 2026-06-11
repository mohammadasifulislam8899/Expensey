package com.xentoryx.expensey.core.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.xentoryx.expensey.core.data.database.dao.TransactionDao
import com.xentoryx.expensey.feature.transaction.data.remote.api.TransactionApiService
import com.xentoryx.expensey.feature.transaction.data.remote.dto.CreateTransactionRequestDto
import com.xentoryx.expensey.core.data.networking.tryToRefreshToken
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val transactionDao: TransactionDao by inject()
    private val apiService: TransactionApiService by inject()

    override suspend fun doWork(): Result {
        Log.d("SyncWorker", "Starting background transaction sync...")
        val unsynced = try {
            transactionDao.getUnsyncedTransactions()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Failed to get unsynced transactions from DB", e)
            return Result.failure()
        }

        if (unsynced.isEmpty()) {
            Log.d("SyncWorker", "No unsynced transactions found.")
            return Result.success()
        }

        Log.d("SyncWorker", "Found ${unsynced.size} unsynced transactions.")
        var allSuccessful = true

        for (tx in unsynced) {
            try {
                Log.d("SyncWorker", "Syncing transaction: id=${tx.id}, type=${tx.type}, amount=${tx.amount}, accountId=${tx.accountId}, categoryId=${tx.categoryId}")
                val request = CreateTransactionRequestDto(
                    id = tx.id,
                    accountId = tx.accountId,
                    categoryId = tx.categoryId,
                    transferToAccountId = tx.transferToAccountId,
                    amount = tx.amount,
                    type = tx.type,
                    note = tx.note,
                    transactionDate = tx.transactionDate
                )
                var response = apiService.createTransaction(request)
                Log.d("SyncWorker", "Initial createTransaction response: status=${response.status.value}")
                
                if (response.status.value == 401) {
                    Log.d("SyncWorker", "Received 401. Trying to refresh token...")
                    if (tryToRefreshToken()) {
                        Log.d("SyncWorker", "Token refreshed successfully. Retrying createTransaction...")
                        response = apiService.createTransaction(request)
                        Log.d("SyncWorker", "Retried createTransaction response: status=${response.status.value}")
                    } else {
                        Log.w("SyncWorker", "Token refresh failed.")
                    }
                }
                
                if (response.status.value in 200..299) {
                    transactionDao.markSynced(tx.id)
                    Log.d("SyncWorker", "Transaction ${tx.id} synced successfully.")
                } else {
                    Log.e("SyncWorker", "Failed to sync transaction ${tx.id}. Server returned status: ${response.status.value}")
                    allSuccessful = false
                }
            } catch (e: Exception) {
                Log.e("SyncWorker", "Exception occurred while syncing transaction ${tx.id}", e)
                allSuccessful = false
            }
        }

        return if (allSuccessful) {
            Log.d("SyncWorker", "All transactions synced successfully.")
            Result.success()
        } else {
            Log.w("SyncWorker", "Some transactions failed to sync. Will retry later.")
            Result.retry()
        }
    }
}

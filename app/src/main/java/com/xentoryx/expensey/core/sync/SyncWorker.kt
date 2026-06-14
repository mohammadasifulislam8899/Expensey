package com.xentoryx.expensey.core.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.xentoryx.expensey.core.data.database.dao.TransactionDao
import com.xentoryx.expensey.feature.transaction.data.remote.api.TransactionApiService
import com.xentoryx.expensey.feature.transaction.data.remote.dto.CreateTransactionRequestDto
import com.xentoryx.expensey.core.data.database.entity.SyncStatus
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
        var allSuccessful = true

        // 1. Process deletions
        val deletions = try {
            transactionDao.getUnsyncedDeletions()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Failed to get unsynced deletions from DB", e)
            return Result.failure()
        }

        for (tx in deletions) {
            try {
                Log.d("SyncWorker", "Syncing deletion of transaction: id=${tx.id}")
                var response = apiService.deleteTransaction(tx.id)
                if (response.status.value == 401) {
                    if (tryToRefreshToken()) {
                        response = apiService.deleteTransaction(tx.id)
                    }
                }
                if (response.status.value in 200..299 || response.status.value == 404) {
                    transactionDao.deleteTransactionById(tx.id)
                    Log.d("SyncWorker", "Transaction deletion ${tx.id} synced and deleted locally.")
                } else {
                    Log.e("SyncWorker", "Failed to sync transaction deletion ${tx.id}. Server returned status: ${response.status.value}")
                    allSuccessful = false
                }
            } catch (e: Exception) {
                Log.e("SyncWorker", "Exception occurred while syncing deletion of transaction ${tx.id}", e)
                allSuccessful = false
            }
        }

        // 2. Process insertions & updates
        val unsynced = try {
            transactionDao.getUnsyncedTransactions()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Failed to get unsynced transactions from DB", e)
            return Result.failure()
        }

        Log.d("SyncWorker", "Found ${unsynced.size} unsynced transactions.")

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
                    transactionDao.insertTransaction(tx.copy(syncStatus = SyncStatus.FAILED))
                    allSuccessful = false
                }
            } catch (e: Exception) {
                Log.e("SyncWorker", "Exception occurred while syncing transaction ${tx.id}", e)
                try {
                    transactionDao.insertTransaction(tx.copy(syncStatus = SyncStatus.FAILED))
                } catch (_: Exception) {}
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

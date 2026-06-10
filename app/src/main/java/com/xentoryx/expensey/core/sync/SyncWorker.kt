package com.xentoryx.expensey.core.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.xentoryx.expensey.core.data.database.dao.TransactionDao
import com.xentoryx.expensey.feature.transaction.data.remote.api.TransactionApiService
import com.xentoryx.expensey.feature.transaction.data.remote.dto.CreateTransactionRequestDto
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val transactionDao: TransactionDao by inject()
    private val apiService: TransactionApiService by inject()

    override suspend fun doWork(): Result {
        val unsynced = try {
            transactionDao.getUnsyncedTransactions()
        } catch (e: Exception) {
            return Result.failure()
        }

        if (unsynced.isEmpty()) {
            return Result.success()
        }

        var allSuccessful = true

        for (tx in unsynced) {
            try {
                val request = CreateTransactionRequestDto(
                    accountId = tx.accountId,
                    categoryId = tx.categoryId,
                    transferToAccountId = tx.transferToAccountId,
                    amount = tx.amount,
                    type = tx.type,
                    note = tx.note,
                    transactionDate = tx.transactionDate
                )
                val response = apiService.createTransaction(request)
                if (response.status.value in 200..299) {
                    transactionDao.markSynced(tx.id)
                } else {
                    allSuccessful = false
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

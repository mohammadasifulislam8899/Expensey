package com.xentoryx.expensey.core.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.xentoryx.expensey.core.data.database.dao.AccountDao
import com.xentoryx.expensey.feature.accounts.data.remote.api.AccountApiService
import com.xentoryx.expensey.feature.accounts.data.remote.dto.CreateAccountRequestDto
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SyncAccountsWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val accountDao: AccountDao by inject()
    private val apiService: AccountApiService by inject()

    override suspend fun doWork(): Result {
        val unsynced = try {
            accountDao.getUnsyncedAccounts()
        } catch (e: Exception) {
            return Result.failure()
        }

        if (unsynced.isEmpty()) {
            return Result.success()
        }

        var allSuccessful = true

        for (account in unsynced) {
            try {
                val request = CreateAccountRequestDto(
                    id = account.accountId,
                    name = account.accountName,
                    type = account.accountType,
                    initialBalance = account.balance,
                    currencyCode = account.currencyCode
                )
                val response = apiService.createAccount(request)
                if (response.status.value in 200..299) {
                    accountDao.markAccountSynced(account.accountId)
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

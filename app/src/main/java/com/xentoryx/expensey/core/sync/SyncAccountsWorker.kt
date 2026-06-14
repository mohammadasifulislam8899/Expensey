package com.xentoryx.expensey.core.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.xentoryx.expensey.core.data.database.dao.AccountDao
import com.xentoryx.expensey.feature.accounts.data.remote.api.AccountApiService
import com.xentoryx.expensey.feature.accounts.data.remote.dto.CreateAccountRequestDto
import com.xentoryx.expensey.feature.accounts.data.remote.dto.UpdateAccountRequestDto
import com.xentoryx.expensey.core.data.database.entity.SyncStatus
import com.xentoryx.expensey.core.data.networking.tryToRefreshToken
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SyncAccountsWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val accountDao: AccountDao by inject()
    private val apiService: AccountApiService by inject()

    override suspend fun doWork(): Result {
        var allSuccessful = true

        // 1. Process deletions
        val deletions = try {
            accountDao.getUnsyncedDeletions()
        } catch (e: Exception) {
            return Result.failure()
        }

        for (account in deletions) {
            try {
                if (account.isNewLocal) {
                    accountDao.deleteAccountById(account.accountId)
                } else {
                    var response = apiService.deleteAccount(account.accountId)
                    if (response.status.value == 401) {
                        if (tryToRefreshToken()) {
                            response = apiService.deleteAccount(account.accountId)
                        }
                    }
                    if (response.status.value in 200..299 || response.status.value == 404) {
                        accountDao.deleteAccountById(account.accountId)
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
            accountDao.getUnsyncedAccounts()
        } catch (e: Exception) {
            return Result.failure()
        }

        for (account in unsynced) {
            try {
                if (account.isNewLocal) {
                    val request = CreateAccountRequestDto(
                        id = account.accountId,
                        name = account.accountName,
                        type = account.accountType,
                        initialBalance = account.balance,
                        currencyCode = account.currencyCode
                    )
                    var response = apiService.createAccount(request)
                    if (response.status.value == 401) {
                        if (tryToRefreshToken()) {
                            response = apiService.createAccount(request)
                        }
                    }
                    if (response.status.value in 200..299) {
                        val updatedEntity = account.copy(syncStatus = SyncStatus.SYNCED, isNewLocal = false)
                        accountDao.insertAccount(updatedEntity)
                    } else {
                        accountDao.insertAccount(account.copy(syncStatus = SyncStatus.FAILED))
                        allSuccessful = false
                    }
                } else {
                    val request = UpdateAccountRequestDto(
                        name = account.accountName,
                        type = account.accountType,
                        currencyCode = account.currencyCode
                    )
                    var response = apiService.updateAccount(account.accountId, request)
                    if (response.status.value == 401) {
                        if (tryToRefreshToken()) {
                            response = apiService.updateAccount(account.accountId, request)
                        }
                    }
                    if (response.status.value in 200..299) {
                        val updatedEntity = account.copy(syncStatus = SyncStatus.SYNCED)
                        accountDao.insertAccount(updatedEntity)
                    } else {
                        accountDao.insertAccount(account.copy(syncStatus = SyncStatus.FAILED))
                        allSuccessful = false
                    }
                }
            } catch (e: Exception) {
                try {
                    accountDao.insertAccount(account.copy(syncStatus = SyncStatus.FAILED))
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

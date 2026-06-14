package com.xentoryx.expensey.feature.accounts.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.xentoryx.expensey.core.data.database.dao.AccountDao
import com.xentoryx.expensey.core.data.database.entity.AccountEntity
import com.xentoryx.expensey.core.data.database.entity.SyncStatus
import com.xentoryx.expensey.core.data.networking.safeCall
import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.core.sync.SyncAccountsWorker
import com.xentoryx.expensey.feature.accounts.data.remote.dto.CreateAccountRequestDto
import com.xentoryx.expensey.feature.dashboard.data.mapper.toDomain
import com.xentoryx.expensey.feature.dashboard.domain.model.AccountSummary
import com.xentoryx.expensey.feature.accounts.data.remote.api.AccountApiService
import com.xentoryx.expensey.feature.accounts.data.remote.dto.AccountResponseDto
import com.xentoryx.expensey.feature.accounts.data.remote.dto.UpdateAccountRequestDto
import com.xentoryx.expensey.feature.accounts.domain.repository.AccountRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

class AccountRepositoryImpl(
    private val context: Context,
    private val accountDao: AccountDao,
    private val apiService: AccountApiService
) : AccountRepository {

    override fun getAccountsFlow(): Flow<List<AccountSummary>> {
        return accountDao.getAccountsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun createAccount(
        name: String,
        type: String,
        initialBalance: Double,
        currencyCode: String
    ): Result<AccountSummary, DataError> {
        val accountId = UUID.randomUUID().toString()

        val entity = AccountEntity(
            accountId = accountId,
            accountName = name,
            accountType = type,
            balance = initialBalance,
            currencyCode = currencyCode,
            syncStatus = SyncStatus.PENDING,
            isNewLocal = true,
            isDeleted = false
        )

        try {
            accountDao.insertAccount(entity)
        } catch (e: Exception) {
            return Result.Error(DataError.Api("Failed to save account locally"))
        }

        triggerSync()

        return Result.Success(entity.toDomain())
    }

    override suspend fun syncAccounts(): Result<Unit, DataError> {
        val responseResult = safeCall<List<AccountResponseDto>> {
            apiService.getAccounts()
        }

        return when (responseResult) {
            is Result.Success -> {
                try {
                    val networkAccounts = responseResult.data.map { dto ->
                        AccountEntity(
                            accountId = dto.id,
                            accountName = dto.name,
                            accountType = dto.type,
                            balance = dto.balance,
                            currencyCode = dto.currencyCode,
                            syncStatus = SyncStatus.SYNCED,
                            isNewLocal = false,
                            isDeleted = false
                        )
                    }
                    accountDao.replaceAccounts(networkAccounts)
                    Result.Success(Unit)
                } catch (e: Exception) {
                    Result.Error(DataError.Api("Failed to save synced accounts locally"))
                }
            }
            is Result.Error -> {
                Result.Error(responseResult.error)
            }
        }
    }

    override suspend fun getAccountById(id: String): AccountSummary? {
        return accountDao.getAccountById(id)?.toDomain()
    }

    override suspend fun updateAccount(
        id: String,
        name: String,
        type: String,
        currencyCode: String
    ): Result<AccountSummary, DataError> {
        val localAccount = accountDao.getAccountById(id)
            ?: return Result.Error(DataError.Api("Account not found locally"))

        val entity = AccountEntity(
            accountId = id,
            accountName = name,
            accountType = type,
            balance = localAccount.balance,
            currencyCode = currencyCode,
            syncStatus = SyncStatus.PENDING,
            isNewLocal = localAccount.isNewLocal,
            isDeleted = false
        )

        try {
            accountDao.insertAccount(entity)
        } catch (e: Exception) {
            return Result.Error(DataError.Api("Failed to save updated account locally"))
        }

        triggerSync()

        return Result.Success(entity.toDomain())
    }

    override suspend fun deleteAccount(id: String): Result<Unit, DataError> {
        val localAccount = accountDao.getAccountById(id)
            ?: return Result.Success(Unit)

        try {
            if (localAccount.isNewLocal) {
                accountDao.deleteAccountById(id)
            } else {
                accountDao.markAccountDeleted(id)
            }
        } catch (e: Exception) {
            return Result.Error(DataError.Api("Failed to delete account locally"))
        }

        triggerSync()

        return Result.Success(Unit)
    }

    private fun triggerSync() {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncAccountsWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(syncRequest)
        } catch (e: Exception) {
            // Ignore WorkManager errors
        }
    }
}

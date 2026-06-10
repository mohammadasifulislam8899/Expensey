package com.xentoryx.expensey.feature.accounts.domain.repository

import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.dashboard.domain.model.AccountSummary
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun getAccountsFlow(): Flow<List<AccountSummary>>
    suspend fun getAccountById(id: String): AccountSummary?
    suspend fun createAccount(
        name: String,
        type: String,
        initialBalance: Double,
        currencyCode: String
    ): Result<AccountSummary, DataError>
    suspend fun updateAccount(
        id: String,
        name: String,
        type: String,
        currencyCode: String
    ): Result<AccountSummary, DataError>
    suspend fun deleteAccount(id: String): Result<Unit, DataError>
    suspend fun syncAccounts(): Result<Unit, DataError>
}

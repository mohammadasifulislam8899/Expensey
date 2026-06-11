package com.xentoryx.expensey.feature.accounts.data.repository

import com.xentoryx.expensey.core.data.networking.safeCall
import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.accounts.data.remote.api.AccountApiService
import com.xentoryx.expensey.feature.accounts.data.remote.dto.AccountResponseDto
import com.xentoryx.expensey.feature.accounts.data.remote.dto.CreateAccountRequestDto
import com.xentoryx.expensey.feature.accounts.data.remote.dto.UpdateAccountRequestDto
import com.xentoryx.expensey.feature.accounts.domain.repository.AccountRepository
import com.xentoryx.expensey.feature.dashboard.domain.model.AccountSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AccountRepositoryImpl(
    private val apiService: AccountApiService
) : AccountRepository {

    private val _accountsFlow = MutableStateFlow<List<AccountSummary>>(emptyList())

    override fun getAccountsFlow(): Flow<List<AccountSummary>> = _accountsFlow.asStateFlow()

    override suspend fun createAccount(
        name: String,
        type: String,
        initialBalance: Double,
        currencyCode: String
    ): Result<AccountSummary, DataError> {
        val result = safeCall<AccountResponseDto> {
            apiService.createAccount(
                CreateAccountRequestDto(
                    name = name,
                    type = type,
                    initialBalance = initialBalance,
                    currencyCode = currencyCode
                )
            )
        }
        return when (result) {
            is Result.Success -> {
                val account = result.data.toDomain()
                _accountsFlow.value = _accountsFlow.value + account
                Result.Success(account)
            }
            is Result.Error -> result
        }
    }

    override suspend fun syncAccounts(): Result<Unit, DataError> {
        val result = safeCall<List<AccountResponseDto>> { apiService.getAccounts() }
        return when (result) {
            is Result.Success -> {
                _accountsFlow.value = result.data.map { it.toDomain() }
                Result.Success(Unit)
            }
            is Result.Error -> result
        }
    }

    override suspend fun getAccountById(id: String): AccountSummary? {
        return _accountsFlow.value.find { it.accountId == id }
    }

    override suspend fun updateAccount(
        id: String,
        name: String,
        type: String,
        currencyCode: String
    ): Result<AccountSummary, DataError> {
        val result = safeCall<AccountResponseDto> {
            apiService.updateAccount(id, UpdateAccountRequestDto(name = name, type = type, currencyCode = currencyCode))
        }
        return when (result) {
            is Result.Success -> {
                val updated = result.data.toDomain()
                _accountsFlow.value = _accountsFlow.value.map { if (it.accountId == id) updated else it }
                Result.Success(updated)
            }
            is Result.Error -> result
        }
    }

    override suspend fun deleteAccount(id: String): Result<Unit, DataError> {
        val result = safeCall<Unit> { apiService.deleteAccount(id) }
        return when (result) {
            is Result.Success -> {
                _accountsFlow.value = _accountsFlow.value.filter { it.accountId != id }
                Result.Success(Unit)
            }
            is Result.Error -> result
        }
    }
}

fun AccountResponseDto.toDomain() = AccountSummary(
    accountId = id,
    accountName = name,
    accountType = type,
    balance = balance,
    currencyCode = currencyCode
)

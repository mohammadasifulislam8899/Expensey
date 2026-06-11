package com.xentoryx.expensey.feature.transaction.data.repository

import com.xentoryx.expensey.core.data.networking.safeCall
import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.dashboard.domain.model.Transaction
import com.xentoryx.expensey.feature.transaction.data.remote.api.TransactionApiService
import com.xentoryx.expensey.feature.transaction.data.remote.dto.CreateTransactionRequestDto
import com.xentoryx.expensey.feature.transaction.data.remote.dto.TransactionListResponseDto
import com.xentoryx.expensey.feature.transaction.data.remote.dto.UpdateTransactionRequestDto
import com.xentoryx.expensey.feature.dashboard.data.remote.dto.TransactionResponseDto
import com.xentoryx.expensey.feature.transaction.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class TransactionRepositoryImpl(
    private val apiService: TransactionApiService
) : TransactionRepository {

    private val _transactionsFlow = MutableStateFlow<List<Transaction>>(emptyList())

    override fun getTransactionsFlow(): Flow<List<Transaction>> = _transactionsFlow.asStateFlow()

    override suspend fun createTransaction(
        accountId: String,
        categoryId: String,
        transferToAccountId: String?,
        amount: Double,
        type: String,
        note: String?,
        transactionDate: String
    ): Result<Transaction, DataError> {
        val result = safeCall<TransactionResponseDto> {
            apiService.createTransaction(
                CreateTransactionRequestDto(
                    accountId = accountId,
                    categoryId = categoryId,
                    transferToAccountId = transferToAccountId,
                    amount = amount,
                    type = type,
                    note = note,
                    transactionDate = transactionDate
                )
            )
        }
        return when (result) {
            is Result.Success -> {
                val tx = result.data.toDomain()
                _transactionsFlow.value = listOf(tx) + _transactionsFlow.value
                Result.Success(tx)
            }
            is Result.Error -> result
        }
    }

    override suspend fun getTransactionById(id: String): Transaction? {
        return _transactionsFlow.value.find { it.id == id }
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
        val result = safeCall<TransactionResponseDto> {
            apiService.updateTransaction(
                id,
                UpdateTransactionRequestDto(
                    accountId = accountId,
                    categoryId = categoryId,
                    transferToAccountId = transferToAccountId,
                    amount = amount,
                    type = type,
                    note = note,
                    transactionDate = transactionDate
                )
            )
        }
        return when (result) {
            is Result.Success -> {
                val updated = result.data.toDomain()
                _transactionsFlow.value = _transactionsFlow.value.map { if (it.id == id) updated else it }
                Result.Success(updated)
            }
            is Result.Error -> result
        }
    }

    override suspend fun deleteTransaction(id: String): Result<Unit, DataError> {
        val result = safeCall<Unit> { apiService.deleteTransaction(id) }
        return when (result) {
            is Result.Success -> {
                _transactionsFlow.value = _transactionsFlow.value.filter { it.id != id }
                Result.Success(Unit)
            }
            is Result.Error -> result
        }
    }

    override suspend fun syncTransactions(page: Int, limit: Int): Result<Unit, DataError> {
        val result = safeCall<TransactionListResponseDto> { apiService.getTransactions(page, limit) }
        return when (result) {
            is Result.Success -> {
                _transactionsFlow.value = result.data.data.map { it.toDomain() }
                Result.Success(Unit)
            }
            is Result.Error -> result
        }
    }
}

fun TransactionResponseDto.toDomain() = Transaction(
    id = id,
    userId = userId,
    accountId = accountId,
    categoryId = categoryId,
    transferToAccountId = transferToAccountId,
    amount = amount,
    type = type,
    note = note,
    transactionDate = transactionDate,
    createdAt = createdAt
)

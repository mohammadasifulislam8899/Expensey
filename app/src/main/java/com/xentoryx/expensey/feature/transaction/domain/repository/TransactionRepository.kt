package com.xentoryx.expensey.feature.transaction.domain.repository

import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.dashboard.domain.model.Transaction

import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    suspend fun createTransaction(
        accountId: String,
        categoryId: String,
        transferToAccountId: String?,
        amount: Double,
        type: String,
        note: String?,
        transactionDate: String
    ): Result<Transaction, DataError>

    fun getTransactionsFlow(): Flow<List<Transaction>>
    suspend fun getTransactionById(id: String): Transaction?
    suspend fun updateTransaction(
        id: String,
        accountId: String,
        categoryId: String,
        transferToAccountId: String?,
        amount: Double,
        type: String,
        note: String?,
        transactionDate: String
    ): Result<Transaction, DataError>
    suspend fun deleteTransaction(id: String): Result<Unit, DataError>
    suspend fun syncTransactions(page: Int, limit: Int): Result<Unit, DataError>
}

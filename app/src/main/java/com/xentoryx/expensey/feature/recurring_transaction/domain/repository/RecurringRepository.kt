package com.xentoryx.expensey.feature.recurring_transaction.domain.repository

import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.recurring_transaction.domain.model.RecurringTransaction
import kotlinx.coroutines.flow.Flow

interface RecurringRepository {
    fun getRecurringTransactionsFlow(): Flow<List<RecurringTransaction>>
    
    suspend fun createRecurringTransaction(
        accountId: String,
        categoryId: String,
        amount: Double,
        type: String,
        frequency: String,
        note: String?,
        startDate: String?,
        endDate: String?
    ): Result<RecurringTransaction, DataError>

    suspend fun updateRecurringTransaction(
        id: String,
        accountId: String,
        categoryId: String,
        amount: Double,
        type: String,
        frequency: String,
        note: String?,
        startDate: String?,
        endDate: String?
    ): Result<RecurringTransaction, DataError>

    suspend fun deleteRecurringTransaction(id: String): Result<Unit, DataError>
    
    suspend fun syncRecurringTransactions(): Result<Unit, DataError>
}

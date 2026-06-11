package com.xentoryx.expensey.feature.transaction.domain.usecase

import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.dashboard.domain.model.Transaction
import com.xentoryx.expensey.feature.transaction.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow

class GetTransactionsUseCase(
    private val repository: TransactionRepository
) {
    fun getLocalTransactions(): Flow<List<Transaction>> {
        return repository.getTransactionsFlow()
    }

    suspend fun syncTransactions(page: Int = 1, limit: Int = 100): Result<Unit, DataError> {
        return repository.syncTransactions(page, limit)
    }
}

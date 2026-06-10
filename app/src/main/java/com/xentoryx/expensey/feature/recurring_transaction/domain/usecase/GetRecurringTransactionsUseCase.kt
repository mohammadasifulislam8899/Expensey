package com.xentoryx.expensey.feature.recurring_transaction.domain.usecase

import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.recurring_transaction.domain.model.RecurringTransaction
import com.xentoryx.expensey.feature.recurring_transaction.domain.repository.RecurringRepository
import kotlinx.coroutines.flow.Flow

class GetRecurringTransactionsUseCase(
    private val repository: RecurringRepository
) {
    operator fun invoke(): Flow<List<RecurringTransaction>> {
        return repository.getRecurringTransactionsFlow()
    }

    suspend fun sync(): Result<Unit, DataError> {
        return repository.syncRecurringTransactions()
    }
}

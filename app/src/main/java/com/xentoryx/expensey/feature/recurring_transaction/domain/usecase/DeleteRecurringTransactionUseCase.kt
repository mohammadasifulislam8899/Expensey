package com.xentoryx.expensey.feature.recurring_transaction.domain.usecase

import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.recurring_transaction.domain.repository.RecurringRepository

class DeleteRecurringTransactionUseCase(
    private val repository: RecurringRepository
) {
    suspend operator fun invoke(id: String): Result<Unit, DataError> {
        if (id.isBlank()) {
            return Result.Error(DataError.Api("ID is required to delete recurring transaction"))
        }
        return repository.deleteRecurringTransaction(id)
    }
}

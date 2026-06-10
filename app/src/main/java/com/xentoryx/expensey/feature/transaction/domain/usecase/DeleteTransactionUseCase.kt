package com.xentoryx.expensey.feature.transaction.domain.usecase

import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.transaction.domain.repository.TransactionRepository

class DeleteTransactionUseCase(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(id: String): Result<Unit, DataError> {
        return repository.deleteTransaction(id)
    }
}

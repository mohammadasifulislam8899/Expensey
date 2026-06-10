package com.xentoryx.expensey.feature.transaction.domain.usecase

import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.dashboard.domain.model.Transaction
import com.xentoryx.expensey.feature.transaction.domain.repository.TransactionRepository

class CreateTransactionUseCase(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(
        accountId: String,
        categoryId: String,
        transferToAccountId: String?,
        amount: Double,
        type: String,
        note: String?,
        transactionDate: String
    ): Result<Transaction, DataError> {
        return repository.createTransaction(
            accountId = accountId,
            categoryId = categoryId,
            transferToAccountId = transferToAccountId,
            amount = amount,
            type = type,
            note = note,
            transactionDate = transactionDate
        )
    }
}

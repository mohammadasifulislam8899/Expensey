package com.xentoryx.expensey.feature.recurring_transaction.domain.usecase

import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.recurring_transaction.domain.model.RecurringTransaction
import com.xentoryx.expensey.feature.recurring_transaction.domain.repository.RecurringRepository

class UpdateRecurringTransactionUseCase(
    private val repository: RecurringRepository
) {
    suspend operator fun invoke(
        id: String,
        accountId: String,
        categoryId: String,
        amount: Double,
        type: String,
        frequency: String,
        note: String? = null,
        startDate: String? = null,
        endDate: String? = null
    ): Result<RecurringTransaction, DataError> {
        if (id.isBlank()) {
            return Result.Error(DataError.Api("ID is required to update recurring transaction"))
        }
        if (accountId.isBlank()) {
            return Result.Error(DataError.Api("Account must be selected"))
        }
        if (categoryId.isBlank()) {
            return Result.Error(DataError.Api("Category must be selected"))
        }
        if (amount <= 0) {
            return Result.Error(DataError.Api("Amount must be greater than zero"))
        }
        if (type.isBlank()) {
            return Result.Error(DataError.Api("Type must be selected"))
        }
        if (frequency.isBlank()) {
            return Result.Error(DataError.Api("Frequency must be selected"))
        }

        return repository.updateRecurringTransaction(
            id = id,
            accountId = accountId,
            categoryId = categoryId,
            amount = amount,
            type = type.uppercase(),
            frequency = frequency.uppercase(),
            note = note?.trim(),
            startDate = startDate,
            endDate = endDate
        )
    }
}

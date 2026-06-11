package com.xentoryx.expensey.feature.accounts.domain.usecase

import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.dashboard.domain.model.AccountSummary
import com.xentoryx.expensey.feature.accounts.domain.repository.AccountRepository

class CreateAccountUseCase(
    private val repository: AccountRepository
) {
    suspend operator fun invoke(
        name: String,
        type: String,
        initialBalance: Double,
        currencyCode: String
    ): Result<AccountSummary, DataError> {
        if (name.isBlank()) {
            return Result.Error(DataError.Api("Account name cannot be empty"))
        }
        return repository.createAccount(
            name = name.trim(),
            type = type,
            initialBalance = initialBalance,
            currencyCode = currencyCode
        )
    }
}

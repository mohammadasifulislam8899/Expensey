package com.xentoryx.expensey.feature.accounts.domain.usecase

import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.accounts.domain.repository.AccountRepository
import com.xentoryx.expensey.feature.dashboard.domain.model.AccountSummary

class UpdateAccountUseCase(
    private val repository: AccountRepository
) {
    suspend operator fun invoke(
        id: String,
        name: String,
        type: String,
        currencyCode: String
    ): Result<AccountSummary, DataError> {
        return repository.updateAccount(
            id = id,
            name = name,
            type = type,
            currencyCode = currencyCode
        )
    }
}

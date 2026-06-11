package com.xentoryx.expensey.feature.accounts.domain.usecase

import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.accounts.domain.repository.AccountRepository

class DeleteAccountUseCase(
    private val repository: AccountRepository
) {
    suspend operator fun invoke(id: String): Result<Unit, DataError> {
        return repository.deleteAccount(id)
    }
}

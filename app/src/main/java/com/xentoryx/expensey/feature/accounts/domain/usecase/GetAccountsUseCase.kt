package com.xentoryx.expensey.feature.accounts.domain.usecase

import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.dashboard.domain.model.AccountSummary
import com.xentoryx.expensey.feature.accounts.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow

class GetAccountsUseCase(
    private val repository: AccountRepository
) {
    operator fun invoke(): Flow<List<AccountSummary>> {
        return repository.getAccountsFlow()
    }

    suspend fun sync(): Result<Unit, DataError> {
        return repository.syncAccounts()
    }
}

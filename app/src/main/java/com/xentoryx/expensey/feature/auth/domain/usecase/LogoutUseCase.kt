package com.xentoryx.expensey.feature.auth.domain.usecase


import com.xentoryx.expensey.core.domain.usecase.NoParamsUseCase
import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.EmptyResult
import com.xentoryx.expensey.feature.auth.domain.repository.AuthRepository

class LogoutUseCase(
    private val repository: AuthRepository
) : NoParamsUseCase<Unit, DataError> {

    override suspend fun invoke(): EmptyResult<DataError> {
        return repository.logout()
    }
}
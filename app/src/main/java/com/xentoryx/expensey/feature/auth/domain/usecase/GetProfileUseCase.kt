package com.xentoryx.expensey.feature.auth.domain.usecase

import com.xentoryx.expensey.core.domain.usecase.NoParamsUseCase
import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.auth.domain.model.User
import com.xentoryx.expensey.feature.auth.domain.repository.AuthRepository

class GetProfileUseCase(
    private val repository: AuthRepository
) : NoParamsUseCase<User, DataError> {

    override suspend fun invoke(): Result<User, DataError> {
        return repository.getProfile()
    }
}
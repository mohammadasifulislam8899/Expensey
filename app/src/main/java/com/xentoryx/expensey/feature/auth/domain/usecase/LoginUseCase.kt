package com.xentoryx.expensey.feature.auth.domain.usecase

import com.xentoryx.expensey.core.domain.usecase.BaseUseCase
import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.feature.auth.domain.model.AuthResult
import com.xentoryx.expensey.feature.auth.domain.repository.AuthRepository
import com.xentoryx.expensey.core.domain.util.Result

data class LoginParams(val email: String, val password: String)

class LoginUseCase(
    private val repository: AuthRepository
) : BaseUseCase<LoginParams, AuthResult, DataError> {

    override suspend fun invoke(params: LoginParams): Result<AuthResult, DataError> {
        val email = params.email.trim().lowercase()

        if (email.isBlank()) {
            return Result.Error(DataError.Api("Email is required"))
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return Result.Error(DataError.Api("Invalid email format"))
        }
        if (params.password.isBlank()) {
            return Result.Error(DataError.Api("Password is required"))
        }

        return repository.login(email, params.password)
    }
}
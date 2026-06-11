package com.xentoryx.expensey.feature.auth.domain.usecase

import com.xentoryx.expensey.core.domain.usecase.BaseUseCase
import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.EmptyResult
import com.xentoryx.expensey.feature.auth.domain.repository.AuthRepository
import com.xentoryx.expensey.core.domain.util.Result

data class ForgotPasswordParams(val email: String)

class ForgotPasswordUseCase(
    private val repository: AuthRepository
) : BaseUseCase<ForgotPasswordParams, Unit, DataError> {

    override suspend fun invoke(params: ForgotPasswordParams): EmptyResult<DataError> {
        val email = params.email.trim().lowercase()
        if (email.isBlank()) {
            return Result.Error(DataError.Api("Email is required"))
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return Result.Error(DataError.Api("Invalid email format"))
        }
        return repository.forgotPassword(email)
    }
}
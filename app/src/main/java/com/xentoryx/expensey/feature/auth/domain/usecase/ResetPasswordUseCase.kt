package com.xentoryx.expensey.feature.auth.domain.usecase


import com.xentoryx.expensey.core.domain.usecase.BaseUseCase
import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.EmptyResult
import com.xentoryx.expensey.feature.auth.domain.repository.AuthRepository
import com.xentoryx.expensey.core.domain.util.Result

data class ResetPasswordParams(val token: String, val newPassword: String, val confirmPassword: String)

class ResetPasswordUseCase(
    private val repository: AuthRepository
) : BaseUseCase<ResetPasswordParams, Unit, DataError> {

    override suspend fun invoke(params: ResetPasswordParams): EmptyResult<DataError> {
        if (params.token.isBlank()) {
            return Result.Error(DataError.Api("Reset token is required"))
        }
        if (params.newPassword.length < 8) {
            return Result.Error(DataError.Api("Password must be at least 8 characters"))
        }
        if (params.newPassword != params.confirmPassword) {
            return Result.Error(DataError.Api("Passwords do not match"))
        }
        return repository.resetPassword(params.token, params.newPassword)
    }
}
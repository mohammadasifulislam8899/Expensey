package com.xentoryx.expensey.feature.auth.domain.usecase

import com.xentoryx.expensey.core.domain.usecase.BaseUseCase
import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.EmptyResult
import com.xentoryx.expensey.feature.auth.domain.repository.AuthRepository
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.auth.domain.model.AuthResult

data class VerifyEmailParams(val userId: String, val otp: String)

class VerifyEmailUseCase(
    private val repository: AuthRepository
) : BaseUseCase<VerifyEmailParams, AuthResult, DataError> {

    override suspend fun invoke(params: VerifyEmailParams): Result<AuthResult, DataError> {
        if (params.otp.isBlank()) {
            return Result.Error(DataError.Api("OTP is required"))
        }
        if (params.otp.length != 6) {
            return Result.Error(DataError.Api("OTP must be 6 digits"))
        }
        return repository.verifyEmail(params.userId, params.otp)
    }
}
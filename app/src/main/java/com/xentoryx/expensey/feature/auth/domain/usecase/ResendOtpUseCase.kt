package com.xentoryx.expensey.feature.auth.domain.usecase

import com.xentoryx.expensey.core.domain.usecase.BaseUseCase
import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.EmptyResult
import com.xentoryx.expensey.feature.auth.domain.repository.AuthRepository

data class ResendOtpParams(val email: String)

class ResendOtpUseCase(
    private val repository: AuthRepository
) : BaseUseCase<ResendOtpParams, Unit, DataError> {

    override suspend fun invoke(params: ResendOtpParams): EmptyResult<DataError> {
        return repository.resendOtp(params.email)
    }
}
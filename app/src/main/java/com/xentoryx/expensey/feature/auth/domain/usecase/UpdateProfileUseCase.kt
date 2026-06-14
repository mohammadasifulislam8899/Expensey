package com.xentoryx.expensey.feature.auth.domain.usecase

import com.xentoryx.expensey.core.domain.usecase.BaseUseCase
import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.feature.auth.domain.model.User
import com.xentoryx.expensey.feature.auth.domain.repository.AuthRepository
import com.xentoryx.expensey.core.domain.util.Result

data class UpdateProfileParams(val fullName: String, val currencyCode: String, val countryCode: String? = null)

class UpdateProfileUseCase(
    private val repository: AuthRepository
) : BaseUseCase<UpdateProfileParams, User, DataError> {

    override suspend fun invoke(params: UpdateProfileParams): Result<User, DataError> {
        if (params.fullName.isBlank()) {
            return Result.Error(DataError.Api("Full name is required"))
        }
        if (params.currencyCode.isBlank()) {
            return Result.Error(DataError.Api("Currency is required"))
        }
        return repository.updateProfile(params.fullName.trim(), params.currencyCode.trim(), params.countryCode?.trim())
    }
}

package com.xentoryx.expensey.feature.auth.domain.usecase

import com.xentoryx.expensey.core.domain.usecase.BaseUseCase
import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.EmptyResult
import com.xentoryx.expensey.feature.auth.domain.repository.AuthRepository
import com.xentoryx.expensey.core.domain.util.Result

data class ChangePasswordParams(val currentPassword: String, val newPassword: String)

class ChangePasswordUseCase(
    private val repository: AuthRepository
) : BaseUseCase<ChangePasswordParams, Unit, DataError> {

    override suspend fun invoke(params: ChangePasswordParams): Result<Unit, DataError> {
        if (params.currentPassword.isBlank()) {
            return Result.Error(DataError.Api("Current password is required"))
        }
        if (params.newPassword.isBlank()) {
            return Result.Error(DataError.Api("New password is required"))
        }
        if (params.newPassword.length < 6) {
            return Result.Error(DataError.Api("New password must be at least 6 characters"))
        }
        return repository.changePassword(params.currentPassword, params.newPassword)
    }
}

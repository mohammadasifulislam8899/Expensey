package com.xentoryx.expensey.feature.auth.domain.usecase

import com.xentoryx.expensey.core.domain.usecase.BaseUseCase
import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.feature.auth.domain.repository.AuthRepository
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.auth.domain.model.RegisterResult

data class RegisterParams(
    val fullName: String,
    val email: String,
    val password: String,
    val confirmPassword: String
)

class RegisterUseCase(
    private val repository: AuthRepository
) : BaseUseCase<RegisterParams, RegisterResult, DataError> {

    override suspend fun invoke(params: RegisterParams): Result<RegisterResult, DataError> {
        val name = params.fullName.trim()
        val email = params.email.trim().lowercase()

        if (name.isBlank()) {
            return Result.Error(DataError.Api("Full name is required"))
        }
        if (name.length < 2) {
            return Result.Error(DataError.Api("Name must be at least 2 characters"))
        }
        if (email.isBlank()) {
            return Result.Error(DataError.Api("Email is required"))
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return Result.Error(DataError.Api("Invalid email format"))
        }
        if (params.password.length < 8) {
            return Result.Error(DataError.Api("Password must be at least 8 characters"))
        }
        if (params.password != params.confirmPassword) {
            return Result.Error(DataError.Api("Passwords do not match"))
        }

        return repository.register(email, params.password, name)
    }
}
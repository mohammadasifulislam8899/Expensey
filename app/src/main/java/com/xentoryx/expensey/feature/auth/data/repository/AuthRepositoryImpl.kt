package com.xentoryx.expensey.feature.auth.data.repository


import com.xentoryx.expensey.core.data.networking.safeCall
import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.EmptyResult
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.core.domain.util.asEmptyDataResult
import com.xentoryx.expensey.core.domain.util.map
import com.xentoryx.expensey.core.storage.TokenManager
import com.xentoryx.expensey.feature.auth.data.mapper.toDomain
import com.xentoryx.expensey.feature.auth.data.remote.api.AuthApiService
import com.xentoryx.expensey.feature.auth.data.remote.dto.AuthResponseDto
import com.xentoryx.expensey.feature.auth.data.remote.dto.ForgotPasswordRequestDto
import com.xentoryx.expensey.feature.auth.data.remote.dto.LoginRequestDto
import com.xentoryx.expensey.feature.auth.data.remote.dto.MessageResponseDto
import com.xentoryx.expensey.feature.auth.data.remote.dto.RegisterRequestDto
import com.xentoryx.expensey.feature.auth.data.remote.dto.RegisterResponseDto
import com.xentoryx.expensey.feature.auth.data.remote.dto.ResendOtpRequestDto
import com.xentoryx.expensey.feature.auth.data.remote.dto.ResetPasswordRequestDto
import com.xentoryx.expensey.feature.auth.data.remote.dto.UserResponseDto
import com.xentoryx.expensey.feature.auth.data.remote.dto.UpdateProfileRequestDto
import com.xentoryx.expensey.feature.auth.data.remote.dto.ChangePasswordRequestDto
import com.xentoryx.expensey.feature.auth.data.remote.dto.VerifyEmailRequestDto
import com.xentoryx.expensey.feature.auth.domain.model.AuthResult
import com.xentoryx.expensey.feature.auth.domain.model.RegisterResult
import com.xentoryx.expensey.feature.auth.domain.model.User
import com.xentoryx.expensey.feature.auth.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

class AuthRepositoryImpl(
    private val apiService: AuthApiService,
    private val tokenManager: TokenManager
) : AuthRepository {

    override suspend fun register(
        email: String,
        password: String,
        fullName: String
    ): Result<RegisterResult, DataError> {
        val result = safeCall<RegisterResponseDto> {
            apiService.register(RegisterRequestDto(email, password, fullName))
        }
        if (result is Result.Success) {
            tokenManager.saveUserId(result.data.user.id)
        }
        return result.map { it.toDomain() }
    }

    override suspend fun login(
        email: String,
        password: String
    ): Result<AuthResult, DataError> {
        val result = safeCall<AuthResponseDto> {
            apiService.login(LoginRequestDto(email, password))
        }
        if (result is Result.Success) {
            tokenManager.saveTokens(result.data.accessToken, result.data.refreshToken)
            tokenManager.saveUserId(result.data.user.id)
            tokenManager.saveUserCurrency(result.data.user.currencyCode)
            tokenManager.saveUserCountry(result.data.user.countryCode)
        }
        return result.map { it.toDomain() }
    }

    override suspend fun verifyEmail(
        userId: String,
        otp: String
    ): Result<AuthResult, DataError> {
        val result = safeCall<AuthResponseDto> {
            apiService.verifyEmail(VerifyEmailRequestDto(userId, otp))
        }
        if (result is Result.Success) {
            tokenManager.saveTokens(result.data.accessToken, result.data.refreshToken)
            tokenManager.saveUserId(result.data.user.id)
            tokenManager.saveUserCurrency(result.data.user.currencyCode)
            tokenManager.saveUserCountry(result.data.user.countryCode)
        }
        return result.map { it.toDomain() }
    }

    override suspend fun resendOtp(
        email: String
    ): EmptyResult<DataError> {
        return safeCall<MessageResponseDto> {
            apiService.resendOtp(ResendOtpRequestDto(email))
        }.asEmptyDataResult()
    }

    override suspend fun forgotPassword(
        email: String
    ): EmptyResult<DataError> {
        return safeCall<MessageResponseDto> {
            apiService.forgotPassword(ForgotPasswordRequestDto(email))
        }.asEmptyDataResult()
    }

    override suspend fun resetPassword(
        token: String,
        newPassword: String
    ): EmptyResult<DataError> {
        return safeCall<MessageResponseDto> {
            apiService.resetPassword(ResetPasswordRequestDto(token, newPassword))
        }.asEmptyDataResult()
    }

    override suspend fun getProfile(): Result<User, DataError> {
        val result = safeCall<UserResponseDto> {
            apiService.getProfile()
        }.map { it.toDomain() }
        if (result is Result.Success) {
            tokenManager.saveUserCurrency(result.data.currencyCode)
            tokenManager.saveUserCountry(result.data.countryCode)
        }
        return result
    }

    override suspend fun updateProfile(
        fullName: String,
        currencyCode: String,
        countryCode: String?
    ): Result<User, DataError> {
        val result = safeCall<UserResponseDto> {
            apiService.updateProfile(UpdateProfileRequestDto(fullName, currencyCode, countryCode))
        }.map { it.toDomain() }
        if (result is Result.Success) {
            tokenManager.saveUserCurrency(result.data.currencyCode)
            tokenManager.saveUserCountry(result.data.countryCode)
        }
        return result
    }

    override suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ): EmptyResult<DataError> {
        return safeCall<MessageResponseDto> {
            apiService.changePassword(ChangePasswordRequestDto(currentPassword, newPassword))
        }.asEmptyDataResult()
    }

    override suspend fun logout(): EmptyResult<DataError> {
        val result = safeCall<MessageResponseDto> {
            apiService.logout()
        }
        tokenManager.clearAll()
        return result.asEmptyDataResult()
    }

    override suspend fun deleteAccount(): EmptyResult<DataError> {
        val result = safeCall<MessageResponseDto> {
            apiService.deleteAccount()
        }
        tokenManager.clearAll()
        return result.asEmptyDataResult()
    }

    override suspend fun resetAllData(): EmptyResult<DataError> {
        return safeCall<MessageResponseDto> {
            apiService.resetAllData()
        }.asEmptyDataResult()
    }

    override fun isLoggedIn(): Flow<Boolean> = tokenManager.isLoggedIn
}
package com.xentoryx.expensey.feature.auth.domain.repository

import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.EmptyResult
import com.xentoryx.expensey.feature.auth.domain.model.*
import kotlinx.coroutines.flow.Flow
import com.xentoryx.expensey.core.domain.util.Result

interface AuthRepository {

    suspend fun register(
        email: String,
        password: String,
        fullName: String
    ): Result<RegisterResult, DataError>

    suspend fun login(
        email: String,
        password: String
    ): Result<AuthResult, DataError>

    suspend fun verifyEmail(
        userId: String,
        otp: String
    ): Result<AuthResult, DataError>

    suspend fun resendOtp(
        email: String
    ): EmptyResult<DataError>

    suspend fun forgotPassword(
        email: String
    ): EmptyResult<DataError>

    suspend fun resetPassword(
        token: String,
        newPassword: String
    ): EmptyResult<DataError>

    suspend fun getProfile(): Result<User, DataError>

    suspend fun updateProfile(
        fullName: String,
        currencyCode: String,
        countryCode: String? = null
    ): Result<User, DataError>

    suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ): EmptyResult<DataError>

    suspend fun logout(): EmptyResult<DataError>

    suspend fun deleteAccount(): EmptyResult<DataError>

    suspend fun resetAllData(): EmptyResult<DataError>

    fun isLoggedIn(): Flow<Boolean>
}
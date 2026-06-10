package com.xentoryx.expensey.feature.auth.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequestDto(
    val email: String,
    val password: String,
    val fullName: String
)

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String
)

@Serializable
data class VerifyEmailRequestDto(
    val userId: String,
    val otp: String
)

@Serializable
data class ResendOtpRequestDto(
    val email: String
)

@Serializable
data class RefreshTokenRequestDto(
    val refreshToken: String
)

@Serializable
data class ForgotPasswordRequestDto(
    val email: String
)

@Serializable
data class ResetPasswordRequestDto(
    val token: String,
    val newPassword: String
)
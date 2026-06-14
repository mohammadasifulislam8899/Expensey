package com.xentoryx.expensey.feature.auth.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserResponseDto(
    val id: String,
    val email: String,
    val fullName: String,
    val currencyCode: String,
    val countryCode: String,
    val isEmailVerified: Boolean,
    val isActive: Boolean
)

@Serializable
data class AuthResponseDto(
    val user: UserResponseDto,
    val accessToken: String,
    val refreshToken: String
)

@Serializable
data class RegisterResponseDto(
    val user: UserResponseDto,
    val message: String
)

@Serializable
data class MessageResponseDto(
    val message: String
)

@Serializable
data class TokenResponseDto(
    val accessToken: String,
    val refreshToken: String
)
package com.xentoryx.expensey.feature.auth.domain.model

data class AuthResult(
    val user: User,
    val accessToken: String,
    val refreshToken: String
)

data class RegisterResult(
    val user: User,
    val message: String
)
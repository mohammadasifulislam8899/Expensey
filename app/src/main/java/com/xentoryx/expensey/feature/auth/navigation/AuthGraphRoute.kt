package com.xentoryx.expensey.feature.auth.navigation

import kotlinx.serialization.Serializable

@Serializable
object AuthGraphRoute

@Serializable
object LoginRoute

@Serializable
object RegisterRoute

@Serializable
data class VerifyEmailRoute(
    val userId: String,
    val email: String
)

@Serializable
object ForgotPasswordRoute

@Serializable
data class ResetPasswordRoute(
    val token: String
)
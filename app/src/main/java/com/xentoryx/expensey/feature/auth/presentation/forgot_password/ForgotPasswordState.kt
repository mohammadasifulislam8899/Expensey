package com.xentoryx.expensey.feature.auth.presentation.forgot_password

data class ForgotPasswordState(
    val email: String = "",
    val isLoading: Boolean = false,
    val isEmailSent: Boolean = false
)
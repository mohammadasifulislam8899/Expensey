package com.xentoryx.expensey.feature.auth.presentation.verify_email

data class VerifyEmailState(
    val userId: String = "",
    val email: String = "",
    val otp: String = "",
    val isLoading: Boolean = false,
    val canResend: Boolean = false,
    val resendCountdown: Int = 60
)
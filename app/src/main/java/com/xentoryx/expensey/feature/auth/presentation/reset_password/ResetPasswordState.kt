package com.xentoryx.expensey.feature.auth.presentation.reset_password

data class ResetPasswordState(
    val token: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val isResetSuccess: Boolean = false
)
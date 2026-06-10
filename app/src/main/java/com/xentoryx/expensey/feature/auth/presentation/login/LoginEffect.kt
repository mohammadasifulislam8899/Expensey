package com.xentoryx.expensey.feature.auth.presentation.login

import com.xentoryx.expensey.core.domain.util.DataError

sealed interface LoginEffect {
    data object NavigateToHome : LoginEffect
    data class NavigateToVerifyEmail(val userId: String, val email: String) : LoginEffect
    data object NavigateToRegister : LoginEffect
    data object NavigateToForgotPassword : LoginEffect
    data class ShowError(val error: DataError) : LoginEffect
}
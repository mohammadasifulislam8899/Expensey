package com.xentoryx.expensey.feature.auth.presentation.forgot_password

import com.xentoryx.expensey.core.domain.util.DataError

sealed interface ForgotPasswordEffect {
    data object NavigateToLogin : ForgotPasswordEffect
    data class ShowError(val error: DataError) : ForgotPasswordEffect
}
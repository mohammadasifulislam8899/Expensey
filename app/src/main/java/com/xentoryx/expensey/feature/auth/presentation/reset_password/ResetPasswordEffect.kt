package com.xentoryx.expensey.feature.auth.presentation.reset_password

import com.xentoryx.expensey.core.domain.util.DataError

sealed interface ResetPasswordEffect {
    data object NavigateToLogin : ResetPasswordEffect
    data class ShowError(val error: DataError) : ResetPasswordEffect
    data class ShowSuccess(val message: String) : ResetPasswordEffect
}
package com.xentoryx.expensey.feature.auth.presentation.register

import com.xentoryx.expensey.core.domain.util.DataError

sealed interface RegisterEffect {
    data class NavigateToVerifyEmail(val userId: String, val email: String) : RegisterEffect
    data object NavigateToLogin : RegisterEffect
    data class ShowError(val error: DataError) : RegisterEffect
    data class ShowSuccess(val message: String) : RegisterEffect
}
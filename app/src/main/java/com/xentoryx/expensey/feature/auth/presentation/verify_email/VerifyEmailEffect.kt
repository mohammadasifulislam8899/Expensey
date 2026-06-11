package com.xentoryx.expensey.feature.auth.presentation.verify_email

import com.xentoryx.expensey.core.domain.util.DataError

sealed interface VerifyEmailEffect {
    data object NavigateToLogin : VerifyEmailEffect
    data class ShowError(val error: DataError) : VerifyEmailEffect
    data class ShowSuccess(val message: String) : VerifyEmailEffect
}
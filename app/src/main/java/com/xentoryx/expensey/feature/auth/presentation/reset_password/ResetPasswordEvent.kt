package com.xentoryx.expensey.feature.auth.presentation.reset_password

sealed interface ResetPasswordEvent {
    data class TokenChanged(val token: String) : ResetPasswordEvent
    data class NewPasswordChanged(val password: String) : ResetPasswordEvent
    data class ConfirmPasswordChanged(val password: String) : ResetPasswordEvent
    data object TogglePasswordVisibility : ResetPasswordEvent
    data object ResetClicked : ResetPasswordEvent
    data object BackToLoginClicked : ResetPasswordEvent
}
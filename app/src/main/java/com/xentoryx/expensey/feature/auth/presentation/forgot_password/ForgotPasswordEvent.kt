package com.xentoryx.expensey.feature.auth.presentation.forgot_password

sealed interface ForgotPasswordEvent {
    data class EmailChanged(val email: String) : ForgotPasswordEvent
    data object SubmitClicked : ForgotPasswordEvent
    data object BackToLoginClicked : ForgotPasswordEvent
}
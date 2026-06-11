package com.xentoryx.expensey.feature.auth.presentation.verify_email

sealed interface VerifyEmailEvent {
    data class OtpChanged(val otp: String) : VerifyEmailEvent
    data object VerifyClicked : VerifyEmailEvent
    data object ResendClicked : VerifyEmailEvent
}
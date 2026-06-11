package com.xentoryx.expensey.feature.auth.presentation.register

sealed interface RegisterEvent {
    data class FullNameChanged(val name: String) : RegisterEvent
    data class EmailChanged(val email: String) : RegisterEvent
    data class PasswordChanged(val password: String) : RegisterEvent
    data class ConfirmPasswordChanged(val password: String) : RegisterEvent
    data object TogglePasswordVisibility : RegisterEvent
    data object RegisterClicked : RegisterEvent
    data object LoginClicked : RegisterEvent
}
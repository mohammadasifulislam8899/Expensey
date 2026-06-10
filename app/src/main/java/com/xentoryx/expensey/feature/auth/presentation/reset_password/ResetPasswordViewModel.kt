package com.xentoryx.expensey.feature.auth.presentation.reset_password

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xentoryx.expensey.feature.auth.domain.usecase.ResetPasswordParams
import com.xentoryx.expensey.feature.auth.domain.usecase.ResetPasswordUseCase
import com.xentoryx.expensey.core.domain.util.onError
import com.xentoryx.expensey.core.domain.util.onSuccess
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ResetPasswordViewModel(
    savedStateHandle: SavedStateHandle,
    private val resetPasswordUseCase: ResetPasswordUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(
        ResetPasswordState(token = savedStateHandle["token"] ?: "")
    )
    val state = _state.asStateFlow()

    private val _effects = Channel<ResetPasswordEffect>()
    val effects = _effects.receiveAsFlow()

    fun onEvent(event: ResetPasswordEvent) {
        when (event) {
            is ResetPasswordEvent.TokenChanged -> _state.update { it.copy(token = event.token) }
            is ResetPasswordEvent.NewPasswordChanged -> _state.update { it.copy(newPassword = event.password) }
            is ResetPasswordEvent.ConfirmPasswordChanged -> _state.update { it.copy(confirmPassword = event.password) }
            is ResetPasswordEvent.TogglePasswordVisibility -> _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            is ResetPasswordEvent.ResetClicked -> reset()
            is ResetPasswordEvent.BackToLoginClicked -> {
                viewModelScope.launch { _effects.send(ResetPasswordEffect.NavigateToLogin) }
            }
        }
    }

    private fun reset() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            resetPasswordUseCase(
                ResetPasswordParams(
                    token = state.value.token,
                    newPassword = state.value.newPassword,
                    confirmPassword = state.value.confirmPassword
                )
            )
                .onSuccess {
                    _state.update { it.copy(isLoading = false, isResetSuccess = true) }
                    _effects.send(ResetPasswordEffect.ShowSuccess("Password reset successfully!"))
                }
                .onError { error ->
                    _state.update { it.copy(isLoading = false) }
                    _effects.send(ResetPasswordEffect.ShowError(error))
                }
        }
    }
}
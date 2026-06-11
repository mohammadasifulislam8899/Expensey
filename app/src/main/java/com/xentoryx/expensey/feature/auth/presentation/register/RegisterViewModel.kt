package com.xentoryx.expensey.feature.auth.presentation.register

import  androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xentoryx.expensey.core.domain.util.onError
import com.xentoryx.expensey.core.domain.util.onSuccess
import com.xentoryx.expensey.feature.auth.domain.usecase.RegisterParams
import com.xentoryx.expensey.feature.auth.domain.usecase.RegisterUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val registerUseCase: RegisterUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterState())
    val state = _state.asStateFlow()

    private val _effects = Channel<RegisterEffect>()
    val effects = _effects.receiveAsFlow()

    fun onEvent(event: RegisterEvent) {
        when (event) {
            is RegisterEvent.FullNameChanged -> _state.update { it.copy(fullName = event.name) }
            is RegisterEvent.EmailChanged -> _state.update { it.copy(email = event.email) }
            is RegisterEvent.PasswordChanged -> _state.update { it.copy(password = event.password) }
            is RegisterEvent.ConfirmPasswordChanged -> _state.update { it.copy(confirmPassword = event.password) }
            is RegisterEvent.TogglePasswordVisibility -> _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            is RegisterEvent.RegisterClicked -> register()
            is RegisterEvent.LoginClicked -> {
                viewModelScope.launch { _effects.send(RegisterEffect.NavigateToLogin) }
            }
        }
    }

    private fun register() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            registerUseCase(
                RegisterParams(
                    fullName = state.value.fullName,
                    email = state.value.email,
                    password = state.value.password,
                    confirmPassword = state.value.confirmPassword
                )
            )
                .onSuccess { result ->
                    _state.update { it.copy(isLoading = false) }
                    _effects.send(RegisterEffect.ShowSuccess(result.message))
                    _effects.send(
                        RegisterEffect.NavigateToVerifyEmail(
                            userId = result.user.id,
                            email = result.user.email
                        )
                    )
                }
                .onError { error ->
                    _state.update { it.copy(isLoading = false) }
                    _effects.send(RegisterEffect.ShowError(error))
                }
        }
    }
}
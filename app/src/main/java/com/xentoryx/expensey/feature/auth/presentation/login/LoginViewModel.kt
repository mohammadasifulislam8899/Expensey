package com.xentoryx.expensey.feature.auth.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xentoryx.expensey.core.domain.util.onError
import com.xentoryx.expensey.core.domain.util.onSuccess
import com.xentoryx.expensey.feature.auth.domain.usecase.LoginParams
import com.xentoryx.expensey.feature.auth.domain.usecase.LoginUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state = _state.asStateFlow()

    private val _effects = Channel<LoginEffect>()
    val effects = _effects.receiveAsFlow()

    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.EmailChanged -> {
                _state.update { it.copy(email = event.email) }
            }
            is LoginEvent.PasswordChanged -> {
                _state.update { it.copy(password = event.password) }
            }
            is LoginEvent.TogglePasswordVisibility -> {
                _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            }
            is LoginEvent.LoginClicked -> login()
            is LoginEvent.ForgotPasswordClicked -> {
                viewModelScope.launch {
                    _effects.send(LoginEffect.NavigateToForgotPassword)
                }
            }
            is LoginEvent.RegisterClicked -> {
                viewModelScope.launch {
                    _effects.send(LoginEffect.NavigateToRegister)
                }
            }
        }
    }

    private fun login() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            loginUseCase(LoginParams(state.value.email, state.value.password))
                .onSuccess { authResult ->
                    _state.update { it.copy(isLoading = false) }
                    if (!authResult.user.isEmailVerified) {
                        _effects.send(
                            LoginEffect.NavigateToVerifyEmail(
                                userId = authResult.user.id,
                                email = authResult.user.email
                            )
                        )
                    } else {
                        _effects.send(LoginEffect.NavigateToHome)
                    }
                }
                .onError { error ->
                    _state.update { it.copy(isLoading = false) }
                    _effects.send(LoginEffect.ShowError(error))
                }
        }
    }
}
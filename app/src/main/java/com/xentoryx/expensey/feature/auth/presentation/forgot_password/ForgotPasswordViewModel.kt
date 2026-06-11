package com.xentoryx.expensey.feature.auth.presentation.forgot_password

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xentoryx.expensey.feature.auth.domain.usecase.ForgotPasswordParams
import com.xentoryx.expensey.feature.auth.domain.usecase.ForgotPasswordUseCase
import com.xentoryx.expensey.core.domain.util.onError
import com.xentoryx.expensey.core.domain.util.onSuccess
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ForgotPasswordViewModel(
    private val forgotPasswordUseCase: ForgotPasswordUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ForgotPasswordState())
    val state = _state.asStateFlow()

    private val _effects = Channel<ForgotPasswordEffect>()
    val effects = _effects.receiveAsFlow()

    fun onEvent(event: ForgotPasswordEvent) {
        when (event) {
            is ForgotPasswordEvent.EmailChanged -> _state.update { it.copy(email = event.email) }
            is ForgotPasswordEvent.SubmitClicked -> submit()
            is ForgotPasswordEvent.BackToLoginClicked -> {
                viewModelScope.launch { _effects.send(ForgotPasswordEffect.NavigateToLogin) }
            }
        }
    }

    private fun submit() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            forgotPasswordUseCase(ForgotPasswordParams(state.value.email))
                .onSuccess {
                    _state.update { it.copy(isLoading = false, isEmailSent = true) }
                }
                .onError { error ->
                    _state.update { it.copy(isLoading = false) }
                    _effects.send(ForgotPasswordEffect.ShowError(error))
                }
        }
    }
}
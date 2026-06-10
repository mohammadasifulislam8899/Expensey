package com.xentoryx.expensey.feature.auth.presentation.verify_email

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xentoryx.expensey.core.domain.util.onError
import com.xentoryx.expensey.core.domain.util.onSuccess
import com.xentoryx.expensey.feature.auth.domain.usecase.ResendOtpParams
import com.xentoryx.expensey.feature.auth.domain.usecase.ResendOtpUseCase
import com.xentoryx.expensey.feature.auth.domain.usecase.VerifyEmailParams
import com.xentoryx.expensey.feature.auth.domain.usecase.VerifyEmailUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VerifyEmailViewModel(
    savedStateHandle: SavedStateHandle,
    private val verifyEmailUseCase: VerifyEmailUseCase,
    private val resendOtpUseCase: ResendOtpUseCase
) : ViewModel() {

    private val userId: String = savedStateHandle["userId"] ?: ""
    private val email: String = savedStateHandle["email"] ?: ""

    private val _state = MutableStateFlow(
        VerifyEmailState(userId = userId, email = email)
    )
    val state = _state.asStateFlow()

    private val _effects = Channel<VerifyEmailEffect>()
    val effects = _effects.receiveAsFlow()

    init {
        startCountdown()
    }

    fun onEvent(event: VerifyEmailEvent) {
        when (event) {
            is VerifyEmailEvent.OtpChanged -> {
                if (event.otp.length <= 6) {
                    _state.update { it.copy(otp = event.otp) }
                }
            }
            is VerifyEmailEvent.VerifyClicked -> verify()
            is VerifyEmailEvent.ResendClicked -> resendOtp()
        }
    }

    private fun verify() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            verifyEmailUseCase(VerifyEmailParams(state.value.userId, state.value.otp))
                .onSuccess {
                    _state.update { it.copy(isLoading = false) }
                    _effects.send(VerifyEmailEffect.ShowSuccess("Email verified successfully!"))
                    _effects.send(VerifyEmailEffect.NavigateToLogin)
                }
                .onError { error ->
                    _state.update { it.copy(isLoading = false) }
                    _effects.send(VerifyEmailEffect.ShowError(error))
                }
        }
    }

    private fun resendOtp() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            resendOtpUseCase(ResendOtpParams(state.value.email))
                .onSuccess {
                    _state.update { it.copy(isLoading = false, otp = "") }
                    _effects.send(VerifyEmailEffect.ShowSuccess("New OTP sent!"))
                    startCountdown()
                }
                .onError { error ->
                    _state.update { it.copy(isLoading = false) }
                    _effects.send(VerifyEmailEffect.ShowError(error))
                }
        }
    }

    private fun startCountdown() {
        _state.update { it.copy(canResend = false, resendCountdown = 60) }
        viewModelScope.launch {
            for (i in 60 downTo 1) {
                _state.update { it.copy(resendCountdown = i) }
                delay(1000L)
            }
            _state.update { it.copy(canResend = true, resendCountdown = 0) }
        }
    }
}
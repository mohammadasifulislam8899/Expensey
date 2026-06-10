package com.xentoryx.expensey.feature.accounts.presentation.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.accounts.domain.usecase.CreateAccountUseCase
import com.xentoryx.expensey.feature.accounts.domain.usecase.UpdateAccountUseCase
import com.xentoryx.expensey.feature.accounts.domain.repository.AccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AddAccountViewModel(
    private val createAccountUseCase: CreateAccountUseCase,
    private val updateAccountUseCase: UpdateAccountUseCase,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AddAccountState())
    val state = _state.asStateFlow()

    fun setEditAccount(accountId: String?) {
        if (accountId == null) {
            _state.update { AddAccountState() }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val account = accountRepository.getAccountById(accountId)
            if (account != null) {
                _state.update {
                    it.copy(
                        accountId = account.accountId,
                        name = account.accountName,
                        type = account.accountType,
                        currencyCode = account.currencyCode,
                        initialBalance = account.balance.toString(),
                        isLoading = false
                    )
                }
            } else {
                _state.update { it.copy(isLoading = false, errorMessage = "Failed to load account") }
            }
        }
    }

    fun onNameChange(name: String) {
        _state.update { it.copy(name = name) }
    }

    fun onTypeChange(type: String) {
        _state.update { it.copy(type = type) }
    }

    fun onBalanceChange(balance: String) {
        _state.update { it.copy(initialBalance = balance) }
    }

    fun onCurrencyChange(currency: String) {
        _state.update { it.copy(currencyCode = currency) }
    }

    fun saveAccount() {
        val currentState = _state.value
        val name = currentState.name
        val type = currentState.type
        val balanceStr = currentState.initialBalance
        val currencyCode = currentState.currencyCode
        val accountId = currentState.accountId

        if (name.isBlank()) {
            _state.update { it.copy(errorMessage = "Account name cannot be empty") }
            return
        }

        val balance = balanceStr.toDoubleOrNull() ?: 0.0

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val result = if (accountId == null) {
                createAccountUseCase(
                    name = name,
                    type = type,
                    initialBalance = balance,
                    currencyCode = currencyCode
                )
            } else {
                updateAccountUseCase(
                    id = accountId,
                    name = name,
                    type = type,
                    currencyCode = currencyCode
                )
            }
            when (result) {
                is Result.Success -> {
                    _state.update { it.copy(isLoading = false, isSuccess = true) }
                }
                is Result.Error -> {
                    val msg = when (val error = result.error) {
                        is DataError.Api -> error.message
                        is DataError.Network -> "Network error occurred"
                    }
                    _state.update { it.copy(isLoading = false, errorMessage = msg) }
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }
}

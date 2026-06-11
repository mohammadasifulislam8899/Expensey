package com.xentoryx.expensey.feature.accounts.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.accounts.domain.usecase.GetAccountsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AccountsListViewModel(
    private val getAccountsUseCase: GetAccountsUseCase
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val state: StateFlow<AccountsListState> = combine(
        getAccountsUseCase(),
        _isLoading,
        _errorMessage
    ) { accounts, isLoading, errorMessage ->
        AccountsListState(
            accounts = accounts,
            isLoading = isLoading,
            errorMessage = errorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AccountsListState()
    )

    init {
        refreshAccounts()
    }

    fun refreshAccounts() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            when (val result = getAccountsUseCase.sync()) {
                is Result.Success -> {
                    // Sync succeeded. Local Flow will emit database changes.
                }
                is Result.Error -> {
                    _errorMessage.value = "Failed to sync accounts from server"
                }
            }
            _isLoading.value = false
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

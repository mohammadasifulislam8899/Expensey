package com.xentoryx.expensey.feature.accounts.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xentoryx.expensey.feature.dashboard.domain.model.Transaction
import com.xentoryx.expensey.feature.transaction.domain.usecase.GetTransactionsUseCase
import com.xentoryx.expensey.feature.accounts.domain.usecase.DeleteAccountUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AccountDetailViewModel(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase
) : ViewModel() {

    private val _accountId = MutableStateFlow<String?>(null)

    val transactions: StateFlow<List<Transaction>> = combine(
        getTransactionsUseCase.getLocalTransactions(),
        _accountId
    ) { allTransactions, accountId ->
        if (accountId == null) {
            emptyList()
        } else {
            allTransactions.filter { it.accountId == accountId }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setAccountId(accountId: String) {
        _accountId.value = accountId
    }

    fun deleteAccount(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val id = _accountId.value ?: return
        viewModelScope.launch {
            when (val result = deleteAccountUseCase(id)) {
                is com.xentoryx.expensey.core.domain.util.Result.Success -> onSuccess()
                is com.xentoryx.expensey.core.domain.util.Result.Error -> {
                    val msg = when (val err = result.error) {
                        is com.xentoryx.expensey.core.domain.util.DataError.Api -> err.message
                        is com.xentoryx.expensey.core.domain.util.DataError.Network -> "Network error"
                        is com.xentoryx.expensey.core.domain.util.DataError.EmailNotVerified -> "Email not verified"
                    }
                    onError(msg)
                }
            }
        }
    }
}

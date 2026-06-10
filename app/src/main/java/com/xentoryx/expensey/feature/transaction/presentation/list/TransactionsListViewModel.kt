package com.xentoryx.expensey.feature.transaction.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.transaction.domain.usecase.GetTransactionsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TransactionsListViewModel(
    private val getTransactionsUseCase: GetTransactionsUseCase
) : ViewModel() {

    private val _selectedType = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val state: StateFlow<TransactionsListState> = combine(
        getTransactionsUseCase.getLocalTransactions(),
        _selectedType,
        _isLoading,
        _errorMessage
    ) { transactions, selectedType, isLoading, errorMessage ->
        val filtered = if (selectedType == null) {
            transactions
        } else {
            transactions.filter { it.type == selectedType }
        }
        TransactionsListState(
            transactions = transactions,
            filteredTransactions = filtered,
            isLoading = isLoading,
            selectedType = selectedType,
            errorMessage = errorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionsListState()
    )

    init {
        refreshTransactions()
    }

    fun selectType(type: String?) {
        _selectedType.value = type
    }

    fun refreshTransactions() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            when (val result = getTransactionsUseCase.syncTransactions()) {
                is Result.Success -> {
                    // Cache updated, Room automatically triggers flow update
                }
                is Result.Error -> {
                    _errorMessage.value = "Failed to sync transactions"
                }
            }
            _isLoading.value = false
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

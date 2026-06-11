package com.xentoryx.expensey.feature.recurring_transaction.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.accounts.domain.repository.AccountRepository
import com.xentoryx.expensey.feature.category.domain.repository.CategoryRepository
import com.xentoryx.expensey.feature.recurring_transaction.domain.usecase.DeleteRecurringTransactionUseCase
import com.xentoryx.expensey.feature.recurring_transaction.domain.usecase.GetRecurringTransactionsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecurringListViewModel(
    private val getRecurringTransactionsUseCase: GetRecurringTransactionsUseCase,
    private val deleteRecurringTransactionUseCase: DeleteRecurringTransactionUseCase,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val state: StateFlow<RecurringListState> = combine(
        getRecurringTransactionsUseCase(),
        accountRepository.getAccountsFlow(),
        categoryRepository.getCategoriesFlow(),
        _isLoading,
        _errorMessage
    ) { recurring, accounts, categories, isLoading, errorMessage ->
        RecurringListState(
            recurringTransactions = recurring,
            accounts = accounts,
            categories = categories,
            isLoading = isLoading,
            errorMessage = errorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RecurringListState()
    )

    init {
        refreshRecurringTransactions()
    }

    fun refreshRecurringTransactions() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            when (getRecurringTransactionsUseCase.sync()) {
                is Result.Success -> {}
                is Result.Error -> _errorMessage.value = "Failed to sync recurring transactions from server"
            }
            _isLoading.value = false
        }
    }

    fun deleteRecurringTransaction(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            when (deleteRecurringTransactionUseCase(id)) {
                is Result.Success -> {}
                is Result.Error -> _errorMessage.value = "Failed to delete recurring transaction"
            }
            _isLoading.value = false
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

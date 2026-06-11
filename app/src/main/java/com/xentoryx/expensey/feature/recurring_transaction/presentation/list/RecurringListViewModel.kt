package com.xentoryx.expensey.feature.recurring_transaction.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xentoryx.expensey.core.data.database.dao.AccountDao
import com.xentoryx.expensey.core.data.database.dao.CategoryDao
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.dashboard.data.mapper.toDomain
import com.xentoryx.expensey.feature.category.data.repository.toDomain
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
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val state: StateFlow<RecurringListState> = combine(
        getRecurringTransactionsUseCase(),
        accountDao.getAccountsFlow(),
        categoryDao.getCategoriesFlow(),
        _isLoading,
        _errorMessage
    ) { recurring, dbAccounts, dbCategories, isLoading, errorMessage ->
        RecurringListState(
            recurringTransactions = recurring,
            accounts = dbAccounts.map { it.toDomain() },
            categories = dbCategories.map { it.toDomain() },
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
                is Result.Error -> {
                    _errorMessage.value = "Failed to sync recurring transactions from server"
                }
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
                is Result.Error -> {
                    _errorMessage.value = "Failed to delete recurring transaction"
                }
            }
            _isLoading.value = false
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

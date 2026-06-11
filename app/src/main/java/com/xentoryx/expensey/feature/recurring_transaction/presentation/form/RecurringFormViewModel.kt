package com.xentoryx.expensey.feature.recurring_transaction.presentation.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.accounts.domain.repository.AccountRepository
import com.xentoryx.expensey.feature.category.domain.repository.CategoryRepository
import com.xentoryx.expensey.feature.recurring_transaction.domain.repository.RecurringRepository
import com.xentoryx.expensey.feature.recurring_transaction.domain.usecase.CreateRecurringTransactionUseCase
import com.xentoryx.expensey.feature.recurring_transaction.domain.usecase.UpdateRecurringTransactionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class RecurringFormViewModel(
    private val createRecurringTransactionUseCase: CreateRecurringTransactionUseCase,
    private val updateRecurringTransactionUseCase: UpdateRecurringTransactionUseCase,
    private val recurringRepository: RecurringRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(
        RecurringFormState(startDate = LocalDate.now().toString())
    )
    val state: StateFlow<RecurringFormState> = _state.asStateFlow()

    init {
        loadAccountsAndCategories()
    }

    private fun loadAccountsAndCategories() {
        viewModelScope.launch {
            accountRepository.getAccountsFlow().collect { accounts ->
                _state.update {
                    it.copy(
                        accounts = accounts,
                        selectedAccountId = it.selectedAccountId ?: accounts.firstOrNull()?.accountId
                    )
                }
            }
        }
        viewModelScope.launch {
            categoryRepository.getCategoriesFlow().collect { categories ->
                _state.update {
                    it.copy(
                        categories = categories,
                        selectedCategoryId = it.selectedCategoryId ?: categories.firstOrNull()?.id
                    )
                }
            }
        }
    }

    fun loadSchedule(id: String) {
        viewModelScope.launch {
            // Sync from API first, then find the item
            recurringRepository.syncRecurringTransactions()
            val list = recurringRepository.getRecurringTransactionsFlow().first()
            val recurringTx = list.find { item -> item.id == id }
            if (recurringTx != null) {
                _state.update {
                    it.copy(
                        recurringTransactionId = recurringTx.id,
                        selectedAccountId = recurringTx.accountId,
                        selectedCategoryId = recurringTx.categoryId,
                        amount = recurringTx.amount.toString(),
                        type = recurringTx.type,
                        frequency = recurringTx.frequency,
                        note = recurringTx.note ?: "",
                        startDate = recurringTx.startDate,
                        endDate = recurringTx.endDate
                    )
                }
            }
        }
    }

    fun onAmountChange(amount: String) = _state.update { it.copy(amount = amount) }
    fun onNoteChange(note: String) = _state.update { it.copy(note = note) }
    fun onTypeChange(type: String) = _state.update { it.copy(type = type) }
    fun onFrequencyChange(frequency: String) = _state.update { it.copy(frequency = frequency) }
    fun onAccountSelected(accountId: String) = _state.update { it.copy(selectedAccountId = accountId) }
    fun onCategorySelected(categoryId: String) = _state.update { it.copy(selectedCategoryId = categoryId) }
    fun onStartDateChange(startDate: String) = _state.update { it.copy(startDate = startDate) }
    fun onEndDateChange(endDate: String?) = _state.update { it.copy(endDate = endDate) }
    fun clearError() = _state.update { it.copy(errorMessage = null) }

    fun saveSchedule() {
        val currentState = _state.value
        val amountVal = currentState.amount.toDoubleOrNull()
        if (amountVal == null || amountVal <= 0.0) {
            _state.update { it.copy(errorMessage = "Please enter a valid amount") }; return
        }
        if (currentState.selectedAccountId == null) {
            _state.update { it.copy(errorMessage = "Please select an account") }; return
        }
        if (currentState.selectedCategoryId == null) {
            _state.update { it.copy(errorMessage = "Please select a category") }; return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val result = if (currentState.recurringTransactionId != null) {
                updateRecurringTransactionUseCase(
                    id = currentState.recurringTransactionId,
                    accountId = currentState.selectedAccountId,
                    categoryId = currentState.selectedCategoryId,
                    amount = amountVal,
                    type = currentState.type,
                    frequency = currentState.frequency,
                    note = currentState.note,
                    startDate = currentState.startDate,
                    endDate = currentState.endDate
                )
            } else {
                createRecurringTransactionUseCase(
                    accountId = currentState.selectedAccountId,
                    categoryId = currentState.selectedCategoryId,
                    amount = amountVal,
                    type = currentState.type,
                    frequency = currentState.frequency,
                    note = currentState.note,
                    startDate = currentState.startDate,
                    endDate = currentState.endDate
                )
            }
            when (result) {
                is Result.Success -> _state.update { it.copy(isLoading = false, isSuccess = true) }
                is Result.Error -> _state.update { it.copy(isLoading = false, errorMessage = "Failed to save schedule template") }
            }
        }
    }
}

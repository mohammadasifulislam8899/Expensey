package com.xentoryx.expensey.feature.recurring_transaction.presentation.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xentoryx.expensey.core.data.database.dao.AccountDao
import com.xentoryx.expensey.core.data.database.dao.CategoryDao
import com.xentoryx.expensey.core.data.database.dao.RecurringTransactionDao
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.dashboard.data.mapper.toDomain
import com.xentoryx.expensey.feature.category.data.repository.toDomain
import com.xentoryx.expensey.feature.recurring_transaction.domain.usecase.CreateRecurringTransactionUseCase
import com.xentoryx.expensey.feature.recurring_transaction.domain.usecase.UpdateRecurringTransactionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class RecurringFormViewModel(
    private val createRecurringTransactionUseCase: CreateRecurringTransactionUseCase,
    private val updateRecurringTransactionUseCase: UpdateRecurringTransactionUseCase,
    private val recurringDao: RecurringTransactionDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao
) : ViewModel() {

    private val _state = MutableStateFlow(
        RecurringFormState(
            startDate = LocalDate.now().toString()
        )
    )
    val state: StateFlow<RecurringFormState> = _state.asStateFlow()

    init {
        loadAccountsAndCategories()
    }

    private fun loadAccountsAndCategories() {
        viewModelScope.launch {
            try {
                val dbAccounts = accountDao.getAccounts().map { it.toDomain() }
                val dbCategories = categoryDao.getCategories().map { it.toDomain() }
                _state.update {
                    it.copy(
                        accounts = dbAccounts,
                        categories = dbCategories,
                        selectedAccountId = it.selectedAccountId ?: dbAccounts.firstOrNull()?.accountId,
                        selectedCategoryId = it.selectedCategoryId ?: dbCategories.firstOrNull()?.id
                    )
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun loadSchedule(id: String) {
        viewModelScope.launch {
            try {
                val entity = recurringDao.getRecurringTransactionById(id)
                if (entity != null) {
                    _state.update {
                        it.copy(
                            recurringTransactionId = entity.id,
                            selectedAccountId = entity.accountId,
                            selectedCategoryId = entity.categoryId,
                            amount = entity.amount.toString(),
                            type = entity.type,
                            frequency = entity.frequency,
                            note = entity.note ?: "",
                            startDate = entity.startDate,
                            endDate = entity.endDate
                        )
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun onAmountChange(amount: String) {
        _state.update { it.copy(amount = amount) }
    }

    fun onNoteChange(note: String) {
        _state.update { it.copy(note = note) }
    }

    fun onTypeChange(type: String) {
        _state.update { it.copy(type = type) }
    }

    fun onFrequencyChange(frequency: String) {
        _state.update { it.copy(frequency = frequency) }
    }

    fun onAccountSelected(accountId: String) {
        _state.update { it.copy(selectedAccountId = accountId) }
    }

    fun onCategorySelected(categoryId: String) {
        _state.update { it.copy(selectedCategoryId = categoryId) }
    }

    fun onStartDateChange(startDate: String) {
        _state.update { it.copy(startDate = startDate) }
    }

    fun onEndDateChange(endDate: String?) {
        _state.update { it.copy(endDate = endDate) }
    }

    fun saveSchedule() {
        val currentState = _state.value
        val amountVal = currentState.amount.toDoubleOrNull()
        if (amountVal == null || amountVal <= 0.0) {
            _state.update { it.copy(errorMessage = "Please enter a valid amount") }
            return
        }
        if (currentState.selectedAccountId == null) {
            _state.update { it.copy(errorMessage = "Please select an account") }
            return
        }
        if (currentState.selectedCategoryId == null) {
            _state.update { it.copy(errorMessage = "Please select a category") }
            return
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
                is Result.Success -> {
                    _state.update { it.copy(isLoading = false, isSuccess = true) }
                }
                is Result.Error -> {
                    _state.update { it.copy(isLoading = false, errorMessage = "Failed to save schedule template") }
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }
}

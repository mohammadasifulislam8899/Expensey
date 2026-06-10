package com.xentoryx.expensey.feature.transaction.presentation.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xentoryx.expensey.core.data.database.dao.AccountDao
import com.xentoryx.expensey.core.data.database.dao.CategoryDao
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.dashboard.data.mapper.toDomain
import com.xentoryx.expensey.feature.dashboard.domain.model.CategoryBreakdown
import com.xentoryx.expensey.feature.transaction.domain.usecase.CreateTransactionUseCase
import com.xentoryx.expensey.feature.transaction.domain.usecase.UpdateTransactionUseCase
import com.xentoryx.expensey.feature.transaction.domain.usecase.DeleteTransactionUseCase
import com.xentoryx.expensey.feature.transaction.domain.repository.TransactionRepository
import com.xentoryx.expensey.core.domain.util.DataError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class AddTransactionViewModel(
    private val createTransactionUseCase: CreateTransactionUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val transactionRepository: TransactionRepository,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao
) : ViewModel() {

    private val _state = MutableStateFlow(
        AddTransactionState(
            dateString = LocalDate.now().toString()
        )
    )
    val state: StateFlow<AddTransactionState> = _state.asStateFlow()

    init {
        loadAccountsAndCategories()
    }

    private fun loadAccountsAndCategories() {
        viewModelScope.launch {
            try {
                val dbAccounts = accountDao.getAccounts().map { it.toDomain() }
                val dbCategories = categoryDao.getCategories().map { entity ->
                    CategoryBreakdown(
                        categoryId = entity.id,
                        categoryName = entity.name,
                        categoryIcon = entity.icon,
                        categoryColor = entity.color,
                        type = entity.type,
                        total = 0.0,
                        percentage = 0.0
                    )
                }
                _state.update {
                    it.copy(
                        accounts = dbAccounts,
                        categories = dbCategories,
                        selectedAccountId = it.selectedAccountId ?: dbAccounts.firstOrNull()?.accountId,
                        selectedCategoryId = it.selectedCategoryId ?: dbCategories.firstOrNull()?.categoryId
                    )
                }
            } catch (e: Exception) {
                // Ignore DB read errors
            }
        }
    }

    fun setEditTransaction(transactionId: String?) {
        if (transactionId == null) {
            _state.update { AddTransactionState(dateString = LocalDate.now().toString()) }
            loadAccountsAndCategories()
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val tx = transactionRepository.getTransactionById(transactionId)
            if (tx != null) {
                _state.update {
                    it.copy(
                        transactionId = tx.id,
                        amount = tx.amount.toString(),
                        note = tx.note ?: "",
                        type = tx.type,
                        selectedAccountId = tx.accountId,
                        selectedCategoryId = tx.categoryId,
                        transferToAccountId = tx.transferToAccountId,
                        dateString = tx.transactionDate,
                        isLoading = false
                    )
                }
                loadAccountsAndCategories()
            } else {
                _state.update { it.copy(isLoading = false, errorMessage = "Failed to load transaction") }
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

    fun onAccountSelected(accountId: String) {
        _state.update { it.copy(selectedAccountId = accountId) }
    }

    fun onCategorySelected(categoryId: String) {
        _state.update { it.copy(selectedCategoryId = categoryId) }
    }

    fun onTransferToAccountSelected(accountId: String?) {
        _state.update { it.copy(transferToAccountId = accountId) }
    }

    fun onDateChange(dateString: String) {
        _state.update { it.copy(dateString = dateString) }
    }

    fun saveTransaction() {
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
        if (currentState.selectedCategoryId == null && currentState.type != "TRANSFER") {
            _state.update { it.copy(errorMessage = "Please select a category") }
            return
        }
        if (currentState.type == "TRANSFER" && currentState.transferToAccountId == null) {
            _state.update { it.copy(errorMessage = "Please select target account for transfer") }
            return
        }
        if (currentState.type == "TRANSFER" && currentState.selectedAccountId == currentState.transferToAccountId) {
            _state.update { it.copy(errorMessage = "Source and target accounts must be different") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val result = if (currentState.transactionId == null) {
                createTransactionUseCase(
                    accountId = currentState.selectedAccountId,
                    categoryId = if (currentState.type == "TRANSFER") "" else (currentState.selectedCategoryId ?: ""),
                    transferToAccountId = if (currentState.type == "TRANSFER") currentState.transferToAccountId else null,
                    amount = amountVal,
                    type = currentState.type,
                    note = currentState.note.ifBlank { null },
                    transactionDate = currentState.dateString
                )
            } else {
                updateTransactionUseCase(
                    id = currentState.transactionId,
                    accountId = currentState.selectedAccountId,
                    categoryId = if (currentState.type == "TRANSFER") "" else (currentState.selectedCategoryId ?: ""),
                    transferToAccountId = if (currentState.type == "TRANSFER") currentState.transferToAccountId else null,
                    amount = amountVal,
                    type = currentState.type,
                    note = currentState.note.ifBlank { null },
                    transactionDate = currentState.dateString
                )
            }

            when (result) {
                is Result.Success -> {
                    _state.update { it.copy(isLoading = false, isSuccess = true) }
                }
                is Result.Error -> {
                    val msg = when (val err = result.error) {
                        is DataError.Api -> err.message
                        is DataError.Network -> "Network error"
                    }
                    _state.update { it.copy(isLoading = false, errorMessage = msg) }
                }
            }
        }
    }

    fun deleteTransaction() {
        val txId = _state.value.transactionId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = deleteTransactionUseCase(txId)) {
                is Result.Success -> {
                    _state.update { it.copy(isLoading = false, isDeleteSuccess = true) }
                }
                is Result.Error -> {
                    val msg = when (val err = result.error) {
                        is DataError.Api -> err.message
                        is DataError.Network -> "Network error"
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

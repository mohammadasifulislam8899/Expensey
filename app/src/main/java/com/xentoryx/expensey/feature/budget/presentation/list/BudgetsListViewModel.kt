package com.xentoryx.expensey.feature.budget.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.budget.domain.usecase.GetBudgetsUseCase
import com.xentoryx.expensey.core.storage.TokenManager
import com.xentoryx.expensey.core.storage.CurrencyConverter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BudgetsListViewModel(
    private val getBudgetsUseCase: GetBudgetsUseCase,
    private val tokenManager: TokenManager,
    private val currencyConverter: CurrencyConverter
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val state: StateFlow<BudgetsListState> = combine(
        getBudgetsUseCase(),
        tokenManager.userCurrency,
        _isLoading,
        _errorMessage
    ) { budgets, userCurrency, isLoading, errorMessage ->
        val convertedBudgets = budgets.map { budget ->
            val limit = currencyConverter.convert(budget.amountLimit, "BDT", userCurrency)
            val spent = currencyConverter.convert(budget.spent, "BDT", userCurrency)
            val remaining = limit - spent
            val percentage = if (limit > 0.0) (spent / limit) * 100.0 else 0.0
            val isExceeded = spent > limit
            budget.copy(
                amountLimit = limit,
                spent = spent,
                remaining = remaining,
                percentage = percentage,
                isExceeded = isExceeded
            )
        }
        BudgetsListState(
            budgets = convertedBudgets,
            isLoading = isLoading,
            errorMessage = errorMessage,
            currencyCode = userCurrency
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BudgetsListState()
    )

    init {
        refreshBudgets()
    }

    fun refreshBudgets() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            when (val result = getBudgetsUseCase.sync()) {
                is Result.Success -> {
                    // Sync completed successfully
                }
                is Result.Error -> {
                    _errorMessage.value = "Failed to sync budgets from server"
                }
            }
            _isLoading.value = false
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

package com.xentoryx.expensey.feature.budget.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.budget.domain.usecase.GetBudgetsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BudgetsListViewModel(
    private val getBudgetsUseCase: GetBudgetsUseCase
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val state: StateFlow<BudgetsListState> = combine(
        getBudgetsUseCase(),
        _isLoading,
        _errorMessage
    ) { budgets, isLoading, errorMessage ->
        BudgetsListState(
            budgets = budgets,
            isLoading = isLoading,
            errorMessage = errorMessage
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

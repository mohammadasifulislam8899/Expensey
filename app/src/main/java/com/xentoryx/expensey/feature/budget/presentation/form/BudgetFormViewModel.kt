package com.xentoryx.expensey.feature.budget.presentation.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xentoryx.expensey.core.data.database.dao.CategoryDao
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.budget.domain.model.Budget
import com.xentoryx.expensey.feature.budget.domain.usecase.CreateBudgetUseCase
import com.xentoryx.expensey.feature.budget.domain.usecase.DeleteBudgetUseCase
import com.xentoryx.expensey.feature.budget.domain.usecase.UpdateBudgetUseCase
import com.xentoryx.expensey.feature.dashboard.domain.model.CategoryBreakdown
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BudgetFormViewModel(
    private val createBudgetUseCase: CreateBudgetUseCase,
    private val updateBudgetUseCase: UpdateBudgetUseCase,
    private val deleteBudgetUseCase: DeleteBudgetUseCase,
    private val categoryDao: CategoryDao
) : ViewModel() {

    private val _state = MutableStateFlow(BudgetFormState())
    val state = _state.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            try {
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
                        categories = dbCategories,
                        selectedCategoryId = it.selectedCategoryId.ifBlank { dbCategories.firstOrNull()?.categoryId ?: "" }
                    )
                }
            } catch (e: Exception) {
                // Ignore DB read errors
            }
        }
    }

    fun initForm(budget: Budget?) {
        if (budget != null) {
            _state.update {
                it.copy(
                    budgetId = budget.id,
                    isEditMode = true,
                    selectedCategoryId = budget.categoryId,
                    amountLimit = budget.amountLimit.toString(),
                    period = budget.period,
                    startDate = budget.startDate,
                    endDate = budget.endDate,
                    isSuccess = false,
                    errorMessage = null
                )
            }
        } else {
            _state.update {
                it.copy(
                    budgetId = null,
                    isEditMode = false,
                    selectedCategoryId = it.categories.firstOrNull()?.categoryId ?: "",
                    amountLimit = "",
                    period = "MONTHLY",
                    startDate = "",
                    endDate = "",
                    isSuccess = false,
                    errorMessage = null
                )
            }
        }
    }

    fun onCategoryChange(categoryId: String) {
        _state.update { it.copy(selectedCategoryId = categoryId) }
    }

    fun onAmountChange(amount: String) {
        _state.update { it.copy(amountLimit = amount) }
    }

    fun onPeriodChange(period: String) {
        _state.update { it.copy(period = period) }
    }

    fun onStartDateChange(date: String) {
        _state.update { it.copy(startDate = date) }
    }

    fun onEndDateChange(date: String) {
        _state.update { it.copy(endDate = date) }
    }

    fun saveBudget() {
        val currentState = _state.value
        val amount = currentState.amountLimit.toDoubleOrNull() ?: 0.0

        if (amount <= 0.0) {
            _state.update { it.copy(errorMessage = "Budget limit must be greater than zero") }
            return
        }

        if (currentState.selectedCategoryId.isBlank()) {
            _state.update { it.copy(errorMessage = "Please select a category") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val result = if (currentState.isEditMode) {
                updateBudgetUseCase(
                    id = currentState.budgetId!!,
                    amountLimit = amount,
                    period = currentState.period,
                    startDate = currentState.startDate.ifBlank { null },
                    endDate = currentState.endDate.ifBlank { null }
                )
            } else {
                createBudgetUseCase(
                    categoryId = currentState.selectedCategoryId,
                    amountLimit = amount,
                    period = currentState.period,
                    startDate = currentState.startDate.ifBlank { null },
                    endDate = currentState.endDate.ifBlank { null }
                )
            }

            when (result) {
                is Result.Success -> {
                    _state.update { it.copy(isLoading = false, isSuccess = true) }
                }
                is Result.Error -> {
                    val msg = when (val err = result.error) {
                        is com.xentoryx.expensey.core.domain.util.DataError.Api -> err.message
                        else -> "Failed to save budget"
                    }
                    _state.update { it.copy(isLoading = false, errorMessage = msg) }
                }
            }
        }
    }

    fun deleteBudget() {
        val currentState = _state.value
        val budgetId = currentState.budgetId ?: return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = deleteBudgetUseCase(budgetId)) {
                is Result.Success -> {
                    _state.update { it.copy(isLoading = false, isSuccess = true) }
                }
                is Result.Error -> {
                    _state.update { it.copy(isLoading = false, errorMessage = "Failed to delete budget") }
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }
}

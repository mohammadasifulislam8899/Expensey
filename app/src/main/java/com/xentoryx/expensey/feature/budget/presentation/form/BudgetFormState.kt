package com.xentoryx.expensey.feature.budget.presentation.form

import com.xentoryx.expensey.feature.dashboard.domain.model.CategoryBreakdown

data class BudgetFormState(
    val budgetId: String? = null,
    val isEditMode: Boolean = false,
    val selectedCategoryId: String = "",
    val amountLimit: String = "",
    val period: String = "MONTHLY",
    val startDate: String = "",
    val endDate: String = "",
    val categories: List<CategoryBreakdown> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val currencyCode: String = "BDT"
)

package com.xentoryx.expensey.feature.budget.presentation.list

import com.xentoryx.expensey.feature.budget.domain.model.Budget

data class BudgetsListState(
    val budgets: List<Budget> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currencyCode: String = "BDT"
)

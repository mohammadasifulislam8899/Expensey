package com.xentoryx.expensey.feature.dashboard.presentation.dashboard

import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.feature.budget.domain.model.Budget
import com.xentoryx.expensey.feature.dashboard.domain.model.DashboardSummary

data class DashboardState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val dashboardSummary: DashboardSummary? = null,
    val budgets: List<Budget> = emptyList(),
    val error: DataError? = null,
    val monthlyTrend: List<MonthlyTrendUiModel> = emptyList(),
    val ratesUpdateTimestamp: Long = 0L
)

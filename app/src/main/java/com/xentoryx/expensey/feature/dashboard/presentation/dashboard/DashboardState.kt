package com.xentoryx.expensey.feature.dashboard.presentation.dashboard

import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.feature.dashboard.domain.model.DashboardSummary

data class DashboardState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val dashboardSummary: DashboardSummary? = null,
    val error: DataError? = null
)

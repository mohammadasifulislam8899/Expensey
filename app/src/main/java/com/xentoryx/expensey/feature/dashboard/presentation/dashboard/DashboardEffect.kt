package com.xentoryx.expensey.feature.dashboard.presentation.dashboard

import com.xentoryx.expensey.core.domain.util.DataError

sealed interface DashboardEffect {
    data class ShowError(val error: DataError) : DashboardEffect
}

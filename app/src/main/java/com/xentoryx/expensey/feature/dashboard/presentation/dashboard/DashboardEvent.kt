package com.xentoryx.expensey.feature.dashboard.presentation.dashboard

sealed interface DashboardEvent {
    data object Refresh : DashboardEvent
    data object LoadSummary : DashboardEvent
    data object SyncExchangeRates : DashboardEvent
}

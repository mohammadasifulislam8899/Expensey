package com.xentoryx.expensey.feature.dashboard.presentation.dashboard

data class MonthlyTrendUiModel(
    val monthLabel: String, // e.g. "Jan"
    val income: Double,
    val expense: Double
)

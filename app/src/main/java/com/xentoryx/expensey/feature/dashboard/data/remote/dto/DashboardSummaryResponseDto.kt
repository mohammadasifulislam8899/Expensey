package com.xentoryx.expensey.feature.dashboard.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class DashboardSummaryResponseDto(
    val totalBalance: Double,
    val totalIncome: Double,
    val totalExpense: Double,
    val savingsRate: Double,
    val accounts: List<AccountSummaryResponseDto>,
    val expenseBreakdown: List<CategoryBreakdownResponseDto>,
    val recentTransactions: List<TransactionResponseDto>
)

package com.xentoryx.expensey.feature.dashboard.domain.model

import com.xentoryx.expensey.core.data.database.entity.SyncStatus

data class DashboardSummary(
    val totalBalance: Double,
    val totalIncome: Double,
    val totalExpense: Double,
    val savingsRate: Double,
    val accounts: List<AccountSummary>,
    val expenseBreakdown: List<CategoryBreakdown>,
    val recentTransactions: List<Transaction>
)

data class AccountSummary(
    val accountId: String,
    val accountName: String,
    val accountType: String,
    val balance: Double,
    val currencyCode: String,
    val syncStatus: SyncStatus? = null
)

data class CategoryBreakdown(
    val categoryId: String,
    val categoryName: String,
    val categoryIcon: String?,
    val categoryColor: String?,
    val type: String,
    val total: Double,
    val percentage: Double
)

data class Transaction(
    val id: String,
    val userId: String,
    val accountId: String,
    val categoryId: String,
    val transferToAccountId: String?,
    val amount: Double,
    val type: String,
    val note: String?,
    val transactionDate: String,
    val createdAt: String,
    val syncStatus: SyncStatus? = null
)

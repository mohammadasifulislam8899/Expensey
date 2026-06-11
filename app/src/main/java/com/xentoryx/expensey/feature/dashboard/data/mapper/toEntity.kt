package com.xentoryx.expensey.feature.dashboard.data.mapper

import com.xentoryx.expensey.core.data.database.entity.AccountEntity
import com.xentoryx.expensey.core.data.database.entity.CategoryBreakdownEntity
import com.xentoryx.expensey.core.data.database.entity.DashboardOverviewEntity
import com.xentoryx.expensey.core.data.database.entity.TransactionEntity
import com.xentoryx.expensey.feature.dashboard.data.remote.dto.AccountSummaryResponseDto
import com.xentoryx.expensey.feature.dashboard.data.remote.dto.CategoryBreakdownResponseDto
import com.xentoryx.expensey.feature.dashboard.data.remote.dto.DashboardSummaryResponseDto
import com.xentoryx.expensey.feature.dashboard.data.remote.dto.TransactionResponseDto
import com.xentoryx.expensey.feature.dashboard.domain.model.AccountSummary
import com.xentoryx.expensey.feature.dashboard.domain.model.CategoryBreakdown
import com.xentoryx.expensey.feature.dashboard.domain.model.DashboardSummary
import com.xentoryx.expensey.feature.dashboard.domain.model.Transaction

// Entities to Domain
fun DashboardOverviewEntity.toDomain(
    accounts: List<AccountSummary>,
    expenseBreakdowns: List<CategoryBreakdown>,
    recentTransactions: List<Transaction>
) = DashboardSummary(
    totalBalance = totalBalance,
    totalIncome = totalIncome,
    totalExpense = totalExpense,
    savingsRate = savingsRate,
    accounts = accounts,
    expenseBreakdown = expenseBreakdowns,
    recentTransactions = recentTransactions
)

fun AccountEntity.toDomain() = AccountSummary(
    accountId = accountId,
    accountName = accountName,
    accountType = accountType,
    balance = balance,
    currencyCode = currencyCode
)

fun CategoryBreakdownEntity.toDomain() = CategoryBreakdown(
    categoryId = categoryId,
    categoryName = categoryName,
    categoryIcon = categoryIcon,
    categoryColor = categoryColor,
    type = type,
    total = total,
    percentage = percentage
)

fun TransactionEntity.toDomain() = Transaction(
    id = id,
    userId = userId,
    accountId = accountId,
    categoryId = categoryId,
    transferToAccountId = transferToAccountId,
    amount = amount,
    type = type,
    note = note,
    transactionDate = transactionDate,
    createdAt = createdAt
)

// DTOs/Domain to Entities
fun DashboardSummaryResponseDto.toOverviewEntity() = DashboardOverviewEntity(
    id = 1,
    totalBalance = totalBalance,
    totalIncome = totalIncome,
    totalExpense = totalExpense,
    savingsRate = savingsRate
)

fun AccountSummaryResponseDto.toEntity() = AccountEntity(
    accountId = accountId,
    accountName = accountName,
    accountType = accountType,
    balance = balance,
    currencyCode = currencyCode
)

fun CategoryBreakdownResponseDto.toEntity() = CategoryBreakdownEntity(
    categoryId = categoryId,
    categoryName = categoryName,
    categoryIcon = categoryIcon,
    categoryColor = categoryColor,
    type = type,
    total = total,
    percentage = percentage
)

fun TransactionResponseDto.toEntity(isSynced: Boolean = true) = TransactionEntity(
    id = id,
    userId = userId,
    accountId = accountId,
    categoryId = categoryId,
    transferToAccountId = transferToAccountId,
    amount = amount,
    type = type,
    note = note,
    transactionDate = transactionDate,
    createdAt = createdAt,
    isSynced = isSynced
)

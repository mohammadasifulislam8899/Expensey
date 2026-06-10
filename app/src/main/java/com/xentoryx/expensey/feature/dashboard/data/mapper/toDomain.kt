package com.xentoryx.expensey.feature.dashboard.data.mapper

import com.xentoryx.expensey.feature.dashboard.data.remote.dto.AccountSummaryResponseDto
import com.xentoryx.expensey.feature.dashboard.data.remote.dto.CategoryBreakdownResponseDto
import com.xentoryx.expensey.feature.dashboard.data.remote.dto.DashboardSummaryResponseDto
import com.xentoryx.expensey.feature.dashboard.data.remote.dto.TransactionResponseDto
import com.xentoryx.expensey.feature.dashboard.domain.model.AccountSummary
import com.xentoryx.expensey.feature.dashboard.domain.model.CategoryBreakdown
import com.xentoryx.expensey.feature.dashboard.domain.model.DashboardSummary
import com.xentoryx.expensey.feature.dashboard.domain.model.Transaction

fun AccountSummaryResponseDto.toDomain(): AccountSummary = AccountSummary(
    accountId = accountId,
    accountName = accountName,
    accountType = accountType,
    balance = balance,
    currencyCode = currencyCode
)

fun CategoryBreakdownResponseDto.toDomain(): CategoryBreakdown = CategoryBreakdown(
    categoryId = categoryId,
    categoryName = categoryName,
    categoryIcon = categoryIcon,
    categoryColor = categoryColor,
    type = type,
    total = total,
    percentage = percentage
)

fun TransactionResponseDto.toDomain(): Transaction = Transaction(
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

fun DashboardSummaryResponseDto.toDomain(): DashboardSummary = DashboardSummary(
    totalBalance = totalBalance,
    totalIncome = totalIncome,
    totalExpense = totalExpense,
    savingsRate = savingsRate,
    accounts = accounts.map { it.toDomain() },
    expenseBreakdown = expenseBreakdown.map { it.toDomain() },
    recentTransactions = recentTransactions.map { it.toDomain() }
)

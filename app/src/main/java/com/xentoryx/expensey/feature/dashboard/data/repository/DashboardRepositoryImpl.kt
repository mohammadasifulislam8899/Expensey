package com.xentoryx.expensey.feature.dashboard.data.repository

import com.xentoryx.expensey.core.data.networking.safeCall
import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.dashboard.data.remote.api.DashboardApiService
import com.xentoryx.expensey.feature.dashboard.data.remote.dto.AccountSummaryResponseDto
import com.xentoryx.expensey.feature.dashboard.data.remote.dto.CategoryBreakdownResponseDto
import com.xentoryx.expensey.feature.dashboard.data.remote.dto.DashboardSummaryResponseDto
import com.xentoryx.expensey.feature.dashboard.data.remote.dto.TransactionResponseDto
import com.xentoryx.expensey.feature.dashboard.domain.model.AccountSummary
import com.xentoryx.expensey.feature.dashboard.domain.model.CategoryBreakdown
import com.xentoryx.expensey.feature.dashboard.domain.model.DashboardSummary
import com.xentoryx.expensey.feature.dashboard.domain.model.Transaction
import com.xentoryx.expensey.feature.dashboard.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class DashboardRepositoryImpl(
    private val apiService: DashboardApiService
) : DashboardRepository {

    override fun getDashboardSummary(): Flow<Result<DashboardSummary, DataError>> = flow {
        val result = safeCall<DashboardSummaryResponseDto> {
            apiService.getDashboardSummary()
        }
        when (result) {
            is Result.Success -> emit(Result.Success(result.data.toDomain()))
            is Result.Error -> emit(result)
        }
    }
}

fun DashboardSummaryResponseDto.toDomain() = DashboardSummary(
    totalBalance = totalBalance,
    totalIncome = totalIncome,
    totalExpense = totalExpense,
    savingsRate = savingsRate,
    accounts = accounts.map { it.toDomain() },
    expenseBreakdown = expenseBreakdown.map { it.toDomain() },
    recentTransactions = recentTransactions.map { it.toDomain() }
)

fun AccountSummaryResponseDto.toDomain() = AccountSummary(
    accountId = accountId,
    accountName = accountName,
    accountType = accountType,
    balance = balance,
    currencyCode = currencyCode
)

fun CategoryBreakdownResponseDto.toDomain() = CategoryBreakdown(
    categoryId = categoryId,
    categoryName = categoryName,
    categoryIcon = categoryIcon,
    categoryColor = categoryColor,
    type = type,
    total = total,
    percentage = percentage
)

fun TransactionResponseDto.toDomain() = Transaction(
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

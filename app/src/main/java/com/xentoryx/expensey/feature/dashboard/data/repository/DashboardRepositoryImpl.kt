package com.xentoryx.expensey.feature.dashboard.data.repository

import com.xentoryx.expensey.core.data.database.dao.AccountDao
import com.xentoryx.expensey.core.data.database.dao.CategoryBreakdownDao
import com.xentoryx.expensey.core.data.database.dao.DashboardDao
import com.xentoryx.expensey.core.data.database.dao.TransactionDao
import com.xentoryx.expensey.core.data.networking.safeCall
import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.core.domain.util.map
import com.xentoryx.expensey.feature.dashboard.data.mapper.toDomain
import com.xentoryx.expensey.feature.dashboard.data.mapper.toEntity
import com.xentoryx.expensey.feature.dashboard.data.mapper.toOverviewEntity
import com.xentoryx.expensey.feature.dashboard.data.remote.api.DashboardApiService
import com.xentoryx.expensey.feature.dashboard.data.remote.dto.DashboardSummaryResponseDto
import com.xentoryx.expensey.feature.dashboard.domain.model.DashboardSummary
import com.xentoryx.expensey.feature.dashboard.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class DashboardRepositoryImpl(
    private val apiService: DashboardApiService,
    private val dashboardDao: DashboardDao,
    private val accountDao: AccountDao,
    private val categoryBreakdownDao: CategoryBreakdownDao,
    private val transactionDao: TransactionDao
) : DashboardRepository {

    override fun getDashboardSummary(): Flow<Result<DashboardSummary, DataError>> = flow {
        // 1. Emit cached dashboard if it exists in Room Database
        try {
            val cachedOverview = dashboardDao.getOverview()
            if (cachedOverview != null) {
                val cachedAccounts = accountDao.getAccounts().map { it.toDomain() }
                val cachedBreakdowns = categoryBreakdownDao.getBreakdowns().map { it.toDomain() }
                val cachedTransactions = transactionDao.getRecentTransactions().map { it.toDomain() }
                emit(Result.Success(cachedOverview.toDomain(cachedAccounts, cachedBreakdowns, cachedTransactions)))
            }
        } catch (e: Exception) {
            // Ignore cache read errors
        }

        // 2. Fetch fresh data from network
        val networkResult = safeCall<DashboardSummaryResponseDto> {
            apiService.getDashboardSummary()
        }

        if (networkResult is Result.Success) {
            try {
                // Save to local Room database
                val data = networkResult.data
                dashboardDao.insertOverview(data.toOverviewEntity())
                accountDao.replaceAccounts(data.accounts.map { it.toEntity() })
                categoryBreakdownDao.replaceBreakdowns(data.expenseBreakdown.map { it.toEntity() })
                transactionDao.insertTransactions(data.recentTransactions.map { it.toEntity(isSynced = true) })
            } catch (e: Exception) {
                // Ignore cache write errors
            }
            emit(networkResult.map { it.toDomain() })
        } else if (networkResult is Result.Error) {
            emit(networkResult)
        }
    }
}

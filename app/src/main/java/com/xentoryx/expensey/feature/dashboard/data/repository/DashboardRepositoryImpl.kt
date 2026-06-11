package com.xentoryx.expensey.feature.dashboard.data.repository

import com.xentoryx.expensey.core.data.database.dao.AccountDao
import com.xentoryx.expensey.core.data.database.dao.CategoryBreakdownDao
import com.xentoryx.expensey.core.data.database.dao.DashboardDao
import com.xentoryx.expensey.core.data.database.dao.TransactionDao
import com.xentoryx.expensey.core.data.database.entity.TransactionEntity
import com.xentoryx.expensey.core.data.networking.safeCall
import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.NetworkError
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.dashboard.data.mapper.toDomain
import com.xentoryx.expensey.feature.dashboard.data.mapper.toEntity
import com.xentoryx.expensey.feature.dashboard.data.mapper.toOverviewEntity
import com.xentoryx.expensey.feature.dashboard.data.remote.api.DashboardApiService
import com.xentoryx.expensey.feature.dashboard.data.remote.dto.DashboardSummaryResponseDto
import com.xentoryx.expensey.feature.dashboard.domain.model.DashboardSummary
import com.xentoryx.expensey.feature.dashboard.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart

class DashboardRepositoryImpl(
    private val apiService: DashboardApiService,
    private val dashboardDao: DashboardDao,
    private val accountDao: AccountDao,
    private val categoryBreakdownDao: CategoryBreakdownDao,
    private val transactionDao: TransactionDao
) : DashboardRepository {

    /**
     * Returns a fully reactive Flow combining 4 Room Flows.
     * Whenever any local data changes (new transaction, account balance update, etc.),
     * the dashboard totals are automatically recalculated.
     *
     * Network fetch is triggered once via onStart{} — it saves fresh data to Room,
     * which then automatically triggers the combine() to re-emit.
     */
    override fun getDashboardSummary(): Flow<Result<DashboardSummary, DataError>> {
        return combine(
            dashboardDao.getOverviewFlow(),
            accountDao.getAccountsFlow(),
            categoryBreakdownDao.getBreakdownsFlow(),
            transactionDao.getRecentTransactionsFlow()
        ) { overview, accounts, breakdowns, transactions ->
            if (overview == null) {
                // No cached data yet; will be populated after network fetch in onStart
                Result.Error(DataError.Network(NetworkError.UNKNOWN))
            } else {
                val domainAccounts = accounts.map { it.toDomain() }
                val domainBreakdowns = breakdowns.map { it.toDomain() }
                val domainTransactions = transactions.map { it.toDomain() }
                val baseSummary = overview.toDomain(domainAccounts, domainBreakdowns, domainTransactions)
                // Adjust with unsynced local transactions so dashboard is always up-to-date
                Result.Success(adjustDashboardSummary(baseSummary, transactions))
            }
        }.onStart {
            // Trigger network fetch — result is saved to Room, which auto-triggers combine() above
            val networkResult = safeCall<DashboardSummaryResponseDto> {
                apiService.getDashboardSummary()
            }
            if (networkResult is Result.Success) {
                try {
                    val data = networkResult.data
                    dashboardDao.insertOverview(data.toOverviewEntity())
                    accountDao.replaceAccounts(data.accounts.map { it.toEntity() })
                    categoryBreakdownDao.replaceBreakdowns(data.expenseBreakdown.map { it.toEntity() })
                    transactionDao.insertTransactions(
                        data.recentTransactions.map { it.toEntity(isSynced = true) }
                    )
                } catch (_: Exception) { /* ignore write errors */ }
            }
        }
    }

    /**
     * Adjusts the server's cached dashboard totals by adding unsynced local transactions.
     * This ensures the dashboard is accurate immediately after adding a transaction,
     * even before the background sync pushes it to the server.
     */
    private fun adjustDashboardSummary(
        summary: DashboardSummary,
        allLocalTransactions: List<TransactionEntity>
    ): DashboardSummary {
        val today = java.time.LocalDate.now()
        val startOfMonth = today.withDayOfMonth(1).toString()
        val endOfMonth = today.withDayOfMonth(today.lengthOfMonth()).toString()

        // Sum unsynced transactions from the already-loaded list — no extra DB query needed
        val unsyncedIncome = allLocalTransactions
            .filter { tx ->
                !tx.isSynced &&
                tx.type == "INCOME" &&
                tx.transactionDate >= startOfMonth &&
                tx.transactionDate <= endOfMonth
            }
            .sumOf { it.amount }

        val unsyncedExpense = allLocalTransactions
            .filter { tx ->
                !tx.isSynced &&
                tx.type == "EXPENSE" &&
                tx.transactionDate >= startOfMonth &&
                tx.transactionDate <= endOfMonth
            }
            .sumOf { it.amount }

        // Balance always comes from local account balances (adjusted on every transaction create/update/delete)
        val totalBalance = summary.accounts.sumOf { it.balance }
        val totalIncome = summary.totalIncome + unsyncedIncome
        val totalExpense = summary.totalExpense + unsyncedExpense
        val savingsRate = if (totalIncome > 0.0) {
            ((totalIncome - totalExpense) / totalIncome) * 100.0
        } else {
            0.0
        }

        return summary.copy(
            totalBalance = totalBalance,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            savingsRate = savingsRate
        )
    }
}

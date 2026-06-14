package com.xentoryx.expensey.feature.dashboard.data.repository

import com.xentoryx.expensey.core.data.database.dao.AccountDao
import com.xentoryx.expensey.core.data.database.dao.CategoryBreakdownDao
import com.xentoryx.expensey.core.data.database.dao.DashboardDao
import com.xentoryx.expensey.core.data.database.dao.TransactionDao
import com.xentoryx.expensey.core.data.database.entity.TransactionEntity
import com.xentoryx.expensey.core.data.database.entity.SyncStatus
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

import com.xentoryx.expensey.core.storage.TokenManager
import com.xentoryx.expensey.core.storage.CurrencyConverter

class DashboardRepositoryImpl(
    private val apiService: DashboardApiService,
    private val dashboardDao: DashboardDao,
    private val accountDao: AccountDao,
    private val categoryBreakdownDao: CategoryBreakdownDao,
    private val transactionDao: TransactionDao,
    private val tokenManager: TokenManager,
    private val currencyConverter: CurrencyConverter
) : DashboardRepository {

    /**
     * Returns a fully reactive Flow combining 5 Flow sources, including the user currency setting.
     * Whenever the user changes their settings currency, totals are automatically recalculated.
     */
    override fun getDashboardSummary(): Flow<Result<DashboardSummary, DataError>> {
        return combine(
            dashboardDao.getOverviewFlow(),
            accountDao.getAccountsFlow(),
            categoryBreakdownDao.getBreakdownsFlow(),
            transactionDao.getRecentTransactionsFlow(),
            tokenManager.userCurrency
        ) { overview, accounts, breakdowns, transactions, targetCurrency ->
            if (overview == null) {
                // No cached data yet; will be populated after network fetch in onStart
                Result.Error(DataError.Network(NetworkError.UNKNOWN))
            } else {
                val domainAccounts = accounts.map { it.toDomain() }
                val domainBreakdowns = breakdowns.map { it.toDomain() }
                val domainTransactions = transactions.map { it.toDomain() }
                val baseSummary = overview.toDomain(domainAccounts, domainBreakdowns, domainTransactions)
                // Adjust with unsynced local transactions and convert values to settings currency
                Result.Success(adjustDashboardSummary(baseSummary, transactions, targetCurrency))
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
                         data.recentTransactions.map { it.toEntity(syncStatus = SyncStatus.SYNCED) }
                     )
                } catch (_: Exception) { /* ignore write errors */ }
            }
        }
    }

    /**
     * Adjusts the server's cached dashboard totals by adding unsynced local transactions and
     * converting all values to the user's preferred settings currency.
     */
    private fun adjustDashboardSummary(
        summary: DashboardSummary,
        allLocalTransactions: List<TransactionEntity>,
        targetCurrency: String
    ): DashboardSummary {
        val today = java.time.LocalDate.now()
        val startOfMonth = today.withDayOfMonth(1).toString()
        val endOfMonth = today.withDayOfMonth(today.lengthOfMonth()).toString()

        val accountCurrencies = summary.accounts.associate { it.accountId to it.currencyCode }

        // Sum unsynced transactions, converting each to the target settings currency
        val unsyncedIncome = allLocalTransactions
            .filter { tx ->
                tx.syncStatus != SyncStatus.SYNCED &&
                tx.type == "INCOME" &&
                tx.transactionDate >= startOfMonth &&
                tx.transactionDate <= endOfMonth
            }
            .sumOf { tx ->
                val txCurrency = accountCurrencies[tx.accountId] ?: "BDT"
                currencyConverter.convert(tx.amount, txCurrency, targetCurrency)
            }

        val unsyncedExpense = allLocalTransactions
            .filter { tx ->
                tx.syncStatus != SyncStatus.SYNCED &&
                tx.type == "EXPENSE" &&
                tx.transactionDate >= startOfMonth &&
                tx.transactionDate <= endOfMonth
            }
            .sumOf { tx ->
                val txCurrency = accountCurrencies[tx.accountId] ?: "BDT"
                currencyConverter.convert(tx.amount, txCurrency, targetCurrency)
            }

        // Sum account balances converted to target settings currency
        val totalBalance = summary.accounts.sumOf { account ->
            currencyConverter.convert(account.balance, account.currencyCode, targetCurrency)
        }

        // Convert base totals (assumed to be in BDT from server) to the target settings currency
        val baseIncomeConverted = currencyConverter.convert(summary.totalIncome, "BDT", targetCurrency)
        val baseExpenseConverted = currencyConverter.convert(summary.totalExpense, "BDT", targetCurrency)

        val totalIncome = baseIncomeConverted + unsyncedIncome
        val totalExpense = baseExpenseConverted + unsyncedExpense
        val savingsRate = if (totalIncome > 0.0) {
            ((totalIncome - totalExpense) / totalIncome) * 100.0
        } else {
            0.0
        }

        // Convert category breakdown totals and re-evaluate percentage
        val convertedBreakdown = summary.expenseBreakdown.map { breakdown ->
            val convertedTotal = currencyConverter.convert(breakdown.total, "BDT", targetCurrency)
            breakdown.copy(total = convertedTotal)
        }
        val breakdownSum = convertedBreakdown.sumOf { it.total }
        val finalBreakdown = convertedBreakdown.map { breakdown ->
            val percentage = if (breakdownSum > 0.0) (breakdown.total / breakdownSum) * 100.0 else 0.0
            breakdown.copy(percentage = percentage)
        }

        return summary.copy(
            totalBalance = totalBalance,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            savingsRate = savingsRate,
            expenseBreakdown = finalBreakdown
        )
    }
}

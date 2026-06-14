package com.xentoryx.expensey.feature.dashboard.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xentoryx.expensey.core.domain.util.onError
import com.xentoryx.expensey.core.domain.util.onSuccess
import com.xentoryx.expensey.core.storage.CurrencyConverter
import com.xentoryx.expensey.core.storage.TokenManager
import com.xentoryx.expensey.feature.budget.domain.usecase.GetBudgetsUseCase
import com.xentoryx.expensey.feature.dashboard.domain.model.AccountSummary
import com.xentoryx.expensey.feature.dashboard.domain.model.Transaction
import com.xentoryx.expensey.feature.dashboard.domain.usecase.GetDashboardUseCase
import com.xentoryx.expensey.feature.transaction.domain.usecase.GetTransactionsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val getDashboardUseCase: GetDashboardUseCase,
    private val getBudgetsUseCase: GetBudgetsUseCase,
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val tokenManager: TokenManager,
    private val currencyConverter: CurrencyConverter
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state = _state.asStateFlow()

    private val _effects = Channel<DashboardEffect>()
    val effects = _effects.receiveAsFlow()

    private var observerJob: Job? = null

    init {
        // Subscribe once to the reactive Room-backed flow.
        // Any local data change (new transaction, balance update) triggers a re-emission.
        startObserving()
    }

    fun onEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.Refresh -> {
                // Re-start observation — this triggers onStart{} in the repository,
                // which fetches fresh data from the network and saves to Room.
                // The Room Flows then auto-emit the updated values.
                _state.update { it.copy(isRefreshing = true, error = null) }
                startObserving()
            }
            is DashboardEvent.LoadSummary -> startObserving()
            is DashboardEvent.SyncExchangeRates -> {
                viewModelScope.launch {
                    try {
                        val currentRates = tokenManager.getExchangeRates() ?: "{}"
                        tokenManager.saveExchangeRates(currentRates, 0L)
                        currencyConverter.syncRatesIfNeeded()
                    } catch (_: Exception) {}
                }
            }
        }
    }

    fun dismissOnboarding() {
        viewModelScope.launch {
            tokenManager.saveOnboardingCompleted(true)
        }
    }

    /**
     * Subscribe to the reactive dashboard Flow.
     * The repository's Flow uses Room Flows + onStart{} network fetch.
     * Any local DB change triggers a re-emit here automatically.
     */
    private fun startObserving() {
        observerJob?.cancel()
        observerJob = viewModelScope.launch {
            val hasCache = state.value.dashboardSummary != null
            if (!hasCache) {
                _state.update { it.copy(isLoading = true, error = null) }
            }

            // Concurrently collect the budgets list from local DB
            launch {
                getBudgetsUseCase().collect { budgetList ->
                    _state.update { it.copy(budgets = budgetList) }
                }
            }

            // Observe exchange rates timestamp reactively
            launch {
                tokenManager.exchangeRatesTimestamp.collect { timestamp ->
                    _state.update { it.copy(ratesUpdateTimestamp = timestamp) }
                }
            }

            // Observe onboarding status reactively
            launch {
                tokenManager.onboardingCompleted.collect { completed ->
                    _state.update { it.copy(showOnboarding = !completed) }
                }
            }

            // Calculate monthly trends reactively based on local transactions flow and preferred settings currency
            launch {
                combine(
                    getTransactionsUseCase.getLocalTransactions(),
                    tokenManager.userCurrency,
                    state.map { it.dashboardSummary }.distinctUntilChanged()
                ) { transactions, targetCurrency, summary ->
                    val accounts = summary?.accounts ?: emptyList()
                    calculateMonthlyTrend(transactions, accounts, targetCurrency)
                }.collect { trend ->
                    _state.update { it.copy(monthlyTrend = trend) }
                }
            }

            getDashboardUseCase().collect { result ->
                result
                    .onSuccess { summary ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                dashboardSummary = summary,
                                error = null
                            )
                        }
                    }
                    .onError { error ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                // Only show error if there's no cached data to display
                                error = if (state.value.dashboardSummary == null) error else null
                            )
                        }
                        viewModelScope.launch {
                            _effects.send(DashboardEffect.ShowError(error))
                        }
                    }
            }
        }
    }

    private fun calculateMonthlyTrend(
        transactions: List<Transaction>,
        accounts: List<AccountSummary>,
        targetCurrency: String
    ): List<MonthlyTrendUiModel> {
        val currentMonth = java.time.YearMonth.now()
        val last6Months = (5 downTo 0).map { currentMonth.minusMonths(it.toLong()) }
        val accountCurrencies = accounts.associate { it.accountId to it.currencyCode }

        return last6Months.map { month ->
            val monthTransactions = transactions.filter { tx ->
                getYearMonth(tx.transactionDate) == month
            }

            val income = monthTransactions
                .filter { it.type.uppercase(java.util.Locale.US) == "INCOME" }
                .sumOf { tx ->
                    val txCurrency = accountCurrencies[tx.accountId] ?: "BDT"
                    currencyConverter.convert(tx.amount, txCurrency, targetCurrency)
                }

            val expense = monthTransactions
                .filter { it.type.uppercase(java.util.Locale.US) == "EXPENSE" }
                .sumOf { tx ->
                    val txCurrency = accountCurrencies[tx.accountId] ?: "BDT"
                    currencyConverter.convert(tx.amount, txCurrency, targetCurrency)
                }

            val label = month.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.US)
            MonthlyTrendUiModel(
                monthLabel = label,
                income = income,
                expense = expense
            )
        }
    }

    private fun getYearMonth(dateStr: String): java.time.YearMonth? {
        return try {
            val parts = dateStr.split("T")[0].split("-")
            if (parts.size >= 2) {
                val year = parts[0].toIntOrNull()
                val month = parts[1].toIntOrNull()
                if (year != null && month != null) {
                    java.time.YearMonth.of(year, month)
                } else null
            } else null
        } catch (_: Exception) {
            null
        }
    }
}

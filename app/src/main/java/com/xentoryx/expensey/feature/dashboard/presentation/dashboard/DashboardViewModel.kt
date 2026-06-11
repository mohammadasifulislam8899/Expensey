package com.xentoryx.expensey.feature.dashboard.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xentoryx.expensey.core.domain.util.onError
import com.xentoryx.expensey.core.domain.util.onSuccess
import com.xentoryx.expensey.feature.dashboard.domain.usecase.GetDashboardUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val getDashboardUseCase: GetDashboardUseCase
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
}

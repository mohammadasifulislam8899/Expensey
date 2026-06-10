package com.xentoryx.expensey.feature.dashboard.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xentoryx.expensey.core.domain.util.onError
import com.xentoryx.expensey.core.domain.util.onSuccess
import com.xentoryx.expensey.feature.dashboard.domain.usecase.GetDashboardUseCase
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

    init {
        loadDashboardSummary()
    }

    fun onEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.Refresh -> refresh()
            is DashboardEvent.LoadSummary -> loadDashboardSummary()
        }
    }

    private fun loadDashboardSummary() {
        viewModelScope.launch {
            // Show loader only if we don't have any cached data to display
            val hasCache = state.value.dashboardSummary != null
            _state.update { it.copy(isLoading = !hasCache, error = null) }

            getDashboardUseCase().collect { result ->
                result.onSuccess { summary ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            dashboardSummary = summary,
                            error = null
                        )
                    }
                }.onError { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            // Only display screen error if we have nothing in the cache
                            error = if (state.value.dashboardSummary == null) error else null
                        )
                    }
                    _effects.send(DashboardEffect.ShowError(error))
                }
            }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, error = null) }
            getDashboardUseCase().collect { result ->
                result.onSuccess { summary ->
                    _state.update {
                        it.copy(
                            isRefreshing = false,
                            dashboardSummary = summary,
                            error = null
                        )
                    }
                }.onError { error ->
                    _state.update {
                        it.copy(
                            isRefreshing = false
                        )
                    }
                    _effects.send(DashboardEffect.ShowError(error))
                }
            }
        }
    }
}

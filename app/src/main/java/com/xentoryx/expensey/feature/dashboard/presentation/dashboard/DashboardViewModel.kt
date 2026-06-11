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

    private var loadJob: Job? = null

    init {
        loadDashboard()
    }

    fun onEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.Refresh -> {
                _state.update { it.copy(isRefreshing = true) }
                loadDashboard()
            }
            is DashboardEvent.LoadSummary -> loadDashboard()
        }
    }

    private fun loadDashboard() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (_state.value.dashboardSummary == null) {
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
                                error = error
                            )
                        }
                        _effects.send(DashboardEffect.ShowError(error))
                    }
            }
        }
    }
}

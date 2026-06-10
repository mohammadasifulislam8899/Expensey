package com.xentoryx.expensey.feature.dashboard.domain.usecase

import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.dashboard.domain.model.DashboardSummary
import com.xentoryx.expensey.feature.dashboard.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow

class GetDashboardUseCase(
    private val repository: DashboardRepository
) {
    operator fun invoke(): Flow<Result<DashboardSummary, DataError>> {
        return repository.getDashboardSummary()
    }
}

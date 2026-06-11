package com.xentoryx.expensey.feature.budget.domain.usecase

import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.budget.domain.model.Budget
import com.xentoryx.expensey.feature.budget.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow

class GetBudgetsUseCase(
    private val repository: BudgetRepository
) {
    operator fun invoke(): Flow<List<Budget>> {
        return repository.getBudgetsFlow()
    }

    suspend fun sync(): Result<Unit, DataError> {
        return repository.syncBudgets()
    }
}

package com.xentoryx.expensey.feature.budget.domain.usecase

import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.budget.domain.model.Budget
import com.xentoryx.expensey.feature.budget.domain.repository.BudgetRepository

class UpdateBudgetUseCase(
    private val repository: BudgetRepository
) {
    suspend operator fun invoke(
        id: String,
        amountLimit: Double,
        period: String,
        startDate: String? = null,
        endDate: String? = null
    ): Result<Budget, DataError> {
        if (amountLimit <= 0.0) {
            return Result.Error(DataError.Api("Budget limit must be greater than zero"))
        }
        return repository.updateBudget(id, amountLimit, period, startDate, endDate)
    }
}

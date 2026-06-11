package com.xentoryx.expensey.feature.budget.domain.usecase

import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.budget.domain.repository.BudgetRepository

class DeleteBudgetUseCase(
    private val repository: BudgetRepository
) {
    suspend operator fun invoke(id: String): Result<Unit, DataError> {
        if (id.isBlank()) {
            return Result.Error(DataError.Api("Budget ID cannot be empty"))
        }
        return repository.deleteBudget(id)
    }
}

package com.xentoryx.expensey.feature.budget.domain.repository

import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.budget.domain.model.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun getBudgetsFlow(): Flow<List<Budget>>
    
    suspend fun createBudget(
        categoryId: String,
        amountLimit: Double,
        period: String,
        startDate: String?,
        endDate: String?
    ): Result<Budget, DataError>

    suspend fun updateBudget(
        id: String,
        amountLimit: Double,
        period: String,
        startDate: String?,
        endDate: String?
    ): Result<Budget, DataError>

    suspend fun deleteBudget(id: String): Result<Unit, DataError>
    
    suspend fun syncBudgets(): Result<Unit, DataError>
}

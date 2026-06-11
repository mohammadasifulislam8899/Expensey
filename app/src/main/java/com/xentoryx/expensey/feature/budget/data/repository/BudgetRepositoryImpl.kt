package com.xentoryx.expensey.feature.budget.data.repository

import com.xentoryx.expensey.core.data.networking.safeCall
import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.budget.data.remote.api.BudgetApiService
import com.xentoryx.expensey.feature.budget.data.remote.dto.BudgetResponseDto
import com.xentoryx.expensey.feature.budget.data.remote.dto.CreateBudgetRequestDto
import com.xentoryx.expensey.feature.budget.data.remote.dto.UpdateBudgetRequestDto
import com.xentoryx.expensey.feature.budget.domain.model.Budget
import com.xentoryx.expensey.feature.budget.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

class BudgetRepositoryImpl(
    private val apiService: BudgetApiService
) : BudgetRepository {

    private val _budgetsFlow = MutableStateFlow<List<Budget>>(emptyList())

    override fun getBudgetsFlow(): Flow<List<Budget>> = _budgetsFlow.asStateFlow()

    override suspend fun createBudget(
        categoryId: String,
        amountLimit: Double,
        period: String,
        startDate: String?,
        endDate: String?
    ): Result<Budget, DataError> {
        val resolvedStartDate = startDate ?: LocalDate.now().withDayOfMonth(1).toString()
        val parsedStart = LocalDate.parse(resolvedStartDate)
        val resolvedEndDate = endDate ?: when (period.uppercase()) {
            "WEEKLY" -> parsedStart.plusWeeks(1).minusDays(1).toString()
            "YEARLY" -> parsedStart.withDayOfYear(parsedStart.lengthOfYear()).toString()
            else -> parsedStart.withDayOfMonth(parsedStart.lengthOfMonth()).toString()
        }

        val result = safeCall<BudgetResponseDto> {
            apiService.createBudget(
                CreateBudgetRequestDto(
                    categoryId = categoryId,
                    amountLimit = amountLimit,
                    period = period,
                    startDate = resolvedStartDate,
                    endDate = resolvedEndDate
                )
            )
        }
        return when (result) {
            is Result.Success -> {
                val budget = result.data.toDomain()
                _budgetsFlow.value = _budgetsFlow.value + budget
                Result.Success(budget)
            }
            is Result.Error -> result
        }
    }

    override suspend fun updateBudget(
        id: String,
        amountLimit: Double,
        period: String,
        startDate: String?,
        endDate: String?
    ): Result<Budget, DataError> {
        val existing = _budgetsFlow.value.find { it.id == id }
        val resolvedStartDate = startDate ?: existing?.startDate ?: LocalDate.now().withDayOfMonth(1).toString()
        val parsedStart = LocalDate.parse(resolvedStartDate)
        val resolvedEndDate = endDate ?: when (period.uppercase()) {
            "WEEKLY" -> parsedStart.plusWeeks(1).minusDays(1).toString()
            "YEARLY" -> parsedStart.withDayOfYear(parsedStart.lengthOfYear()).toString()
            else -> parsedStart.withDayOfMonth(parsedStart.lengthOfMonth()).toString()
        }

        val result = safeCall<BudgetResponseDto> {
            apiService.updateBudget(
                id,
                UpdateBudgetRequestDto(
                    amountLimit = amountLimit,
                    period = period,
                    startDate = resolvedStartDate,
                    endDate = resolvedEndDate
                )
            )
        }
        return when (result) {
            is Result.Success -> {
                val updated = result.data.toDomain()
                _budgetsFlow.value = _budgetsFlow.value.map { if (it.id == id) updated else it }
                Result.Success(updated)
            }
            is Result.Error -> result
        }
    }

    override suspend fun deleteBudget(id: String): Result<Unit, DataError> {
        val result = safeCall<Unit> { apiService.deleteBudget(id) }
        return when (result) {
            is Result.Success -> {
                _budgetsFlow.value = _budgetsFlow.value.filter { it.id != id }
                Result.Success(Unit)
            }
            is Result.Error -> result
        }
    }

    override suspend fun syncBudgets(): Result<Unit, DataError> {
        val result = safeCall<List<BudgetResponseDto>> { apiService.getBudgets() }
        return when (result) {
            is Result.Success -> {
                _budgetsFlow.value = result.data.map { it.toDomain() }
                Result.Success(Unit)
            }
            is Result.Error -> result
        }
    }
}

fun BudgetResponseDto.toDomain() = Budget(
    id = id,
    categoryId = categoryId,
    categoryName = categoryName,
    categoryIcon = categoryIcon,
    categoryColor = categoryColor,
    amountLimit = amountLimit,
    period = period,
    startDate = startDate,
    endDate = endDate,
    spent = spent,
    remaining = remaining,
    percentage = percentage,
    isExceeded = isExceeded
)
